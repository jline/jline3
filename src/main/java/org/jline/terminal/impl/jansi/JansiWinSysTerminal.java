/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jansi;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.fusesource.jansi.WindowsAnsiOutputStream;
import org.fusesource.jansi.internal.Kernel32;
import org.fusesource.jansi.internal.Kernel32.INPUT_RECORD;
import org.fusesource.jansi.internal.Kernel32.KEY_EVENT_RECORD;
import org.fusesource.jansi.internal.WindowsSupport;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractTerminal;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.InputStreamReader;
import org.jline.utils.Log;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.ShutdownHooks;
import org.jline.utils.ShutdownHooks.Task;
import org.jline.utils.Signals;

public class JansiWinSysTerminal extends AbstractTerminal {

    protected final InputStream input;
    protected final OutputStream output;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final Task closer;

    private static final int ENABLE_PROCESSED_INPUT = 0x0001;
    private static final int ENABLE_LINE_INPUT      = 0x0002;
    private static final int ENABLE_ECHO_INPUT      = 0x0004;
    private static final int ENABLE_WINDOW_INPUT    = 0x0008;
    private static final int ENABLE_MOUSE_INPUT     = 0x0010;
    private static final int ENABLE_INSERT_MODE     = 0x0020;
    private static final int ENABLE_QUICK_EDIT_MODE = 0x0040;


    public JansiWinSysTerminal(String name, boolean nativeSignals) throws IOException {
        super(name, "windows");
        input = new DirectInputStream();
        output = new WindowsAnsiOutputStream(new FileOutputStream(FileDescriptor.out));
        String encoding = getConsoleEncoding();
        if (encoding == null) {
            encoding = Charset.defaultCharset().name();
        }
        this.reader = new NonBlockingReader(getName(), new InputStreamReader(input, encoding));
        this.writer = new PrintWriter(new OutputStreamWriter(output, encoding));
        parseInfoCmp();
        // Handle signals
        if (nativeSignals) {
            for (final Signal signal : Signal.values()) {
                nativeHandlers.put(signal, Signals.register(signal.name(), () -> raise(signal)));
            }
        }
        closer = this::close;
        ShutdownHooks.add(closer);
    }

    @SuppressWarnings("InjectedReferences")
    protected static String getConsoleEncoding() {
        int codepage = Kernel32.GetConsoleOutputCP();
        //http://docs.oracle.com/javase/6/docs/technotes/guides/intl/encoding.doc.html
        String charsetMS = "ms" + codepage;
        if (java.nio.charset.Charset.isSupported(charsetMS)) {
            return charsetMS;
        }
        String charsetCP = "cp" + codepage;
        if (java.nio.charset.Charset.isSupported(charsetCP)) {
            return charsetCP;
        }
        return null;
    }

    public NonBlockingReader reader() {
        return reader;
    }

    public PrintWriter writer() {
        return writer;
    }

    @Override
    public InputStream input() {
        return input;
    }

    @Override
    public OutputStream output() {
        return output;
    }

    public Attributes getAttributes() {
        int mode = WindowsSupport.getConsoleMode();
        Attributes attributes = new Attributes();
        if ((mode & ENABLE_ECHO_INPUT) != 0) {
            attributes.setLocalFlag(LocalFlag.ECHO, true);
        }
        if ((mode & ENABLE_LINE_INPUT) != 0) {
            attributes.setLocalFlag(LocalFlag.ICANON, true);
        }
        return attributes;
    }

    public void setAttributes(Attributes attr) {
        int mode = 0;
        if (attr.getLocalFlag(LocalFlag.ECHO)) {
            mode |= ENABLE_ECHO_INPUT;
        }
        if (attr.getLocalFlag(LocalFlag.ICANON)) {
            mode |= ENABLE_LINE_INPUT;
        }
        WindowsSupport.setConsoleMode(mode);
    }

    public Size getSize() {
        Size size = new Size();
        size.setColumns(WindowsSupport.getWindowsTerminalWidth());
        size.setRows(WindowsSupport.getWindowsTerminalHeight());
        return size;
    }

    public void setSize(Size size) {
        throw new UnsupportedOperationException("Can not resize windows terminal");
    }

    public void close() throws IOException {
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
        reader.close();
        writer.close();
    }

    private byte[] readConsoleInput() {
        // XXX does how many events to read in one call matter?
        INPUT_RECORD[] events = null;
        try {
            events = WindowsSupport.readConsoleInput(1);
        } catch (IOException e) {
            Log.debug("read Windows terminal input error: ", e);
        }
        if (events == null) {
            return new byte[0];
        }
        StringBuilder sb = new StringBuilder();
        for (INPUT_RECORD event : events) {
            KEY_EVENT_RECORD keyEvent = event.keyEvent;
            // support some C1 control sequences: ALT + [@-_] (and [a-z]?) => ESC <ascii>
            // http://en.wikipedia.org/wiki/C0_and_C1_control_codes#C1_set
            final int altState = KEY_EVENT_RECORD.LEFT_ALT_PRESSED | KEY_EVENT_RECORD.RIGHT_ALT_PRESSED;
            // Pressing "Alt Gr" is translated to Alt-Ctrl, hence it has to be checked that Ctrl is _not_ pressed,
            // otherwise inserting of "Alt Gr" codes on non-US keyboards would yield errors
            final int ctrlState = KEY_EVENT_RECORD.LEFT_CTRL_PRESSED | KEY_EVENT_RECORD.RIGHT_CTRL_PRESSED;
            // Compute the overall alt state
            boolean isAlt = ((keyEvent.controlKeyState & altState) != 0) && ((keyEvent.controlKeyState & ctrlState) == 0);

            //Log.trace(keyEvent.keyDown? "KEY_DOWN" : "KEY_UP", "key code:", keyEvent.keyCode, "char:", (long)keyEvent.uchar);
            if (keyEvent.keyDown) {
                if (keyEvent.uchar > 0) {
                    if (isAlt) {
                        sb.append('\033');
                    }
                    sb.append(keyEvent.uchar);
                }
                else {
                    // virtual keycodes: http://msdn.microsoft.com/en-us/library/windows/desktop/dd375731(v=vs.85).aspx
                    // TODO: numpad keys, modifiers
                    String escapeSequence = null;
                    switch (keyEvent.keyCode) {
                        case 0x08: // VK_BACK BackSpace
                            escapeSequence = getSequence(Capability.key_backspace);
                            break;
                        case 0x21: // VK_PRIOR PageUp
                            escapeSequence = getSequence(Capability.key_ppage);
                            break;
                        case 0x22: // VK_NEXT PageDown
                            escapeSequence = getSequence(Capability.key_npage);
                            break;
                        case 0x23: // VK_END
                            escapeSequence = getSequence(Capability.key_end);
                            break;
                        case 0x24: // VK_HOME
                            escapeSequence = getSequence(Capability.key_home);
                            break;
                        case 0x25: // VK_LEFT
                            escapeSequence = getSequence(Capability.key_left);
                            break;
                        case 0x26: // VK_UP
                            escapeSequence = getSequence(Capability.key_up);
                            break;
                        case 0x27: // VK_RIGHT
                            escapeSequence = getSequence(Capability.key_right);
                            break;
                        case 0x28: // VK_DOWN
                            escapeSequence = getSequence(Capability.key_down);
                            break;
                        case 0x2D: // VK_INSERT
                            escapeSequence = getSequence(Capability.key_ic);
                            break;
                        case 0x2E: // VK_DELETE
                            escapeSequence = getSequence(Capability.key_dc);
                            break;
                        case 0x70: // VK_F1
                            escapeSequence = getSequence(Capability.key_f1);
                            break;
                        case 0x71: // VK_F2
                            escapeSequence = getSequence(Capability.key_f2);
                            break;
                        case 0x72: // VK_F3
                            escapeSequence = getSequence(Capability.key_f3);
                            break;
                        case 0x73: // VK_F4
                            escapeSequence = getSequence(Capability.key_f4);
                            break;
                        case 0x74: // VK_F5
                            escapeSequence = getSequence(Capability.key_f5);
                            break;
                        case 0x75: // VK_F6
                            escapeSequence = getSequence(Capability.key_f6);
                            break;
                        case 0x76: // VK_F7
                            escapeSequence = getSequence(Capability.key_f7);
                            break;
                        case 0x77: // VK_F8
                            escapeSequence = getSequence(Capability.key_f8);
                            break;
                        case 0x78: // VK_F9
                            escapeSequence = getSequence(Capability.key_f9);
                            break;
                        case 0x79: // VK_F10
                            escapeSequence = getSequence(Capability.key_f10);
                            break;
                        case 0x7A: // VK_F11
                            escapeSequence = getSequence(Capability.key_f11);
                            break;
                        case 0x7B: // VK_F12
                            escapeSequence = getSequence(Capability.key_f12);
                            break;
                        default:
                            break;
                    }
                    if (escapeSequence != null) {
                        for (int k = 0; k < keyEvent.repeatCount; k++) {
                            if (isAlt) {
                                sb.append('\033');
                            }
                            sb.append(escapeSequence);
                        }
                    }
                }
            } else {
                // key up event
                // support ALT+NumPad input method
                if (keyEvent.keyCode == 0x12/*VK_MENU ALT key*/ && keyEvent.uchar > 0) {
                    sb.append(keyEvent.uchar);
                }
            }
        }
        return sb.toString().getBytes();
    }

    private String getSequence(Capability cap) {
        String str = strings.get(cap);
        if (str != null) {
            StringWriter sw = new StringWriter();
            try {
                Curses.tputs(sw, str);
            } catch (IOException e) {
                throw new IOError(e);
            }
            return sw.toString();
        }
        return null;
    }

    private class DirectInputStream extends InputStream {
        private byte[] buf = null;
        int bufIdx = 0;

        @Override
        public int read() throws IOException {
            while (buf == null || bufIdx == buf.length) {
                buf = readConsoleInput();
                bufIdx = 0;
            }
            int c = buf[bufIdx] & 0xFF;
            bufIdx++;
            return c;
        }

        public int read(byte b[], int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int c = read();
            if (c == -1) {
                return -1;
            }
            b[off] = (byte)c;
            return 1;
        }
    }
}

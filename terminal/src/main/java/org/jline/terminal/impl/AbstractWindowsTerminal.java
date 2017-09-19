/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp;
import org.jline.utils.Log;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.ShutdownHooks;
import org.jline.utils.Signals;
import org.jline.utils.WriterOutputStream;

import java.io.BufferedOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The AbstractWindowsTerminal is used as the base class for windows terminal.
 * Due to windows limitations, mostly the missing support for ansi sequences,
 * the only way to create a correct terminal is to use the windows api to set
 * character attributes, move the cursor, erasing, etc...
 *
 * UTF-8 support is also lacking in windows and the code page supposed to
 * emulate UTF-8 is a bit broken. In order to work around this broken
 * code page, windows api WriteConsoleW is used directly.  This means that
 * the writer() becomes the primary output, while the output() is bridged
 * to the writer() using a WriterOutputStream wrapper.
 */
public abstract class AbstractWindowsTerminal extends AbstractTerminal {

    public static final String TYPE_WINDOWS = "windows";

    private static final int PIPE_SIZE = 1024;

    private static final int UTF8_CODE_PAGE = 65001;

    protected static final int ENABLE_PROCESSED_INPUT = 0x0001;
    protected static final int ENABLE_LINE_INPUT      = 0x0002;
    protected static final int ENABLE_ECHO_INPUT      = 0x0004;
    protected static final int ENABLE_WINDOW_INPUT    = 0x0008;
    protected static final int ENABLE_MOUSE_INPUT     = 0x0010;
    protected static final int ENABLE_INSERT_MODE     = 0x0020;
    protected static final int ENABLE_QUICK_EDIT_MODE = 0x0040;

    protected final OutputStream slaveInputPipe;
    protected final InputStream input;
    protected final OutputStream output;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final ShutdownHooks.Task closer;
    protected final Attributes attributes = new Attributes();
    protected final Thread pump;

    protected MouseTracking tracking = MouseTracking.Off;
    private volatile boolean closing;

    public AbstractWindowsTerminal(Writer writer, String name, int codepage, boolean nativeSignals, SignalHandler signalHandler) throws IOException {
        super(name, TYPE_WINDOWS, signalHandler);
        PipedInputStream input = new PipedInputStream(PIPE_SIZE); // UTF-8 encoded
        this.slaveInputPipe = new PipedOutputStream(input); // UTF-8 encoded
        this.input = new FilterInputStream(input) {}; // UTF-8 encoded
        this.reader = new NonBlockingReader(getName(), new org.jline.utils.InputStreamReader(input, StandardCharsets.UTF_8));
        this.writer = new PrintWriter(writer);
        // Grab the console code page and find a matching charset to encode
        Charset charset = getConsoleEncoding(codepage);
        this.output = new WriterOutputStream(writer, charset);
        parseInfoCmp();
        // Attributes
        attributes.setLocalFlag(Attributes.LocalFlag.ISIG, true);
        attributes.setControlChar(Attributes.ControlChar.VINTR, ctrl('C'));
        attributes.setControlChar(Attributes.ControlChar.VEOF,  ctrl('D'));
        attributes.setControlChar(Attributes.ControlChar.VSUSP, ctrl('Z'));
        // Handle signals
        if (nativeSignals) {
            for (final Signal signal : Signal.values()) {
                if (signalHandler == SignalHandler.SIG_DFL) {
                    nativeHandlers.put(signal, Signals.registerDefault(signal.name()));
                } else {
                    nativeHandlers.put(signal, Signals.register(signal.name(), () -> raise(signal)));
                }
            }
        }
        pump = new Thread(this::pump, "WindowsStreamPump");
        pump.setDaemon(true);
        pump.start();
        closer = this::close;
        ShutdownHooks.add(closer);
    }

    protected Charset getConsoleEncoding(int codepage) {
        if (codepage <= 0) {
            codepage = getConsoleOutputCP();
        }

        //http://docs.oracle.com/javase/6/docs/technotes/guides/intl/encoding.doc.html
        if (codepage == UTF8_CODE_PAGE) {
            return StandardCharsets.UTF_8;
        }
        String charsetMS = "ms" + codepage;
        if (Charset.isSupported(charsetMS)) {
            return Charset.forName(charsetMS);
        }
        String charsetCP = "cp" + codepage;
        if (Charset.isSupported(charsetCP)) {
            return Charset.forName(charsetCP);
        }
        return Charset.defaultCharset();
    }

    @Override
    public SignalHandler handle(Signal signal, SignalHandler handler) {
        SignalHandler prev = super.handle(signal, handler);
        if (prev != handler) {
            if (handler == SignalHandler.SIG_DFL) {
                Signals.registerDefault(signal.name());
            } else {
                Signals.register(signal.name(), () -> raise(signal));
            }
        }
        return prev;
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
        int mode = getConsoleMode();
        if ((mode & ENABLE_ECHO_INPUT) != 0) {
            attributes.setLocalFlag(Attributes.LocalFlag.ECHO, true);
        }
        if ((mode & ENABLE_LINE_INPUT) != 0) {
            attributes.setLocalFlag(Attributes.LocalFlag.ICANON, true);
        }
        return new Attributes(attributes);
    }

    public void setAttributes(Attributes attr) {
        attributes.copy(attr);
        updateConsoleMode();
    }

    protected void updateConsoleMode() {
        int mode = ENABLE_WINDOW_INPUT;
        if (attributes.getLocalFlag(Attributes.LocalFlag.ECHO)) {
            mode |= ENABLE_ECHO_INPUT;
        }
        if (attributes.getLocalFlag(Attributes.LocalFlag.ICANON)) {
            mode |= ENABLE_LINE_INPUT;
        }
        if (tracking != MouseTracking.Off) {
            mode |= ENABLE_MOUSE_INPUT;
        }
        setConsoleMode(mode);
    }

    protected int ctrl(char key) {
        return (Character.toUpperCase(key) & 0x1f);
    }

    public void setSize(Size size) {
        throw new UnsupportedOperationException("Can not resize windows terminal");
    }

    public void close() throws IOException {
        closing = true;
        pump.interrupt();
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
        reader.close();
        writer.close();
    }

    static final int SHIFT_FLAG = 0x01;
    static final int ALT_FLAG =   0x02;
    static final int CTRL_FLAG =  0x04;

    static final int RIGHT_ALT_PRESSED =   0x0001;
    static final int LEFT_ALT_PRESSED =    0x0002;
    static final int RIGHT_CTRL_PRESSED =  0x0004;
    static final int LEFT_CTRL_PRESSED =   0x0008;
    static final int SHIFT_PRESSED =       0x0010;
    static final int NUMLOCK_ON =          0x0020;
    static final int SCROLLLOCK_ON =       0x0040;
    static final int CAPSLOCK_ON =         0x0080;

    protected String getEscapeSequenceFromConsoleInput(final boolean isKeyDown, final short virtualKeyCode, final char uchar, final int controlKeyState, final short repeatCount, final short scanCode) {
        final boolean isCtrl = (controlKeyState & (RIGHT_CTRL_PRESSED | LEFT_CTRL_PRESSED)) > 0;
        final boolean isAlt = (controlKeyState & (RIGHT_ALT_PRESSED | LEFT_ALT_PRESSED)) > 0;
        final boolean isShift = (controlKeyState & SHIFT_PRESSED) > 0;
        char ch = uchar;
        StringBuilder sb = new StringBuilder(32);
        // key down event
        if (isKeyDown && ch != '\3') {
            // Pressing "Alt Gr" is translated to Alt-Ctrl, hence it has to be checked that Ctrl is _not_ pressed,
            // otherwise inserting of "Alt Gr" codes on non-US keyboards would yield errors
            if (ch != 0
                    && (controlKeyState & (RIGHT_ALT_PRESSED | LEFT_ALT_PRESSED | RIGHT_CTRL_PRESSED | LEFT_CTRL_PRESSED | SHIFT_PRESSED))
                        == (RIGHT_ALT_PRESSED | LEFT_CTRL_PRESSED)) {
                sb.append(ch);
            } else {
                final String keySeq = getEscapeSequence(virtualKeyCode, (isCtrl ? CTRL_FLAG : 0) + (isAlt ? ALT_FLAG : 0) + (isShift ? SHIFT_FLAG : 0));
                if (keySeq != null) {
                    return keySeq;
                }
                /* uchar value in Windows when CTRL is pressed:
                 * 1). Ctrl +  <0x41 to 0x5e>      : uchar=<keyCode> - 'A' + 1
                 * 2). Ctrl + Backspace(0x08)      : uchar=0x7f
                 * 3). Ctrl + Enter(0x0d)          : uchar=0x0a
                 * 4). Ctrl + Space(0x20)          : uchar=0x20
                 * 5). Ctrl + <Other key>          : uchar=0
                 * 6). Ctrl + Alt + <Any key>      : uchar=0
                */
                if (ch > 0) {
                    if (isAlt) {
                        sb.append("\033");
                    }
                    if (isCtrl && ch != ' ' && ch != '\n' && ch != 0x7f) {
                        sb.append((char) (ch == '?' ? 0x7f : Character.toUpperCase(ch) & 0x1f));
                    } else {
                        sb.append(ch);
                    }
                } else if (isCtrl) { //Handles the ctrl key events(uchar=0)
                    if (virtualKeyCode >= 'A' && virtualKeyCode <= 'Z') {
                        ch = (char) (virtualKeyCode - 0x40);
                    } else if (virtualKeyCode == 191) { //?
                        ch = 127;
                    }
                    if (ch > 0) {
                        if (isAlt) {
                            sb.append("\033");
                        }
                        sb.append(ch);
                    }
                }
            }
        }
        // key up event
        else {
            if (ch == '\3') {
                return "\3";
            }
            // support ALT+NumPad input method
            if (virtualKeyCode == 0x12 /*VK_MENU ALT key*/ && ch > 0) {
                sb.append(ch);  // no such combination in Windows
            }
        }
        return sb.toString();
    }

    protected String getEscapeSequence(short keyCode, int keyState) {
        // virtual keycodes: http://msdn.microsoft.com/en-us/library/windows/desktop/dd375731(v=vs.85).aspx
        // TODO: numpad keys, modifiers
        String escapeSequence = null;
        switch (keyCode) {
            case 0x08: // VK_BACK BackSpace
                escapeSequence = (keyState & ALT_FLAG) > 0 ? "\\E^H" : getRawSequence(InfoCmp.Capability.key_backspace);
                break;
            case 0x09:
                escapeSequence = (keyState & SHIFT_FLAG) > 0 ? getRawSequence(InfoCmp.Capability.key_btab) : null;
                break;
            case 0x21: // VK_PRIOR PageUp
                escapeSequence = getRawSequence(InfoCmp.Capability.key_ppage);
                break;
            case 0x22: // VK_NEXT PageDown
                escapeSequence = getRawSequence(InfoCmp.Capability.key_npage);
                break;
            case 0x23: // VK_END
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dF" : getRawSequence(InfoCmp.Capability.key_end);
                break;
            case 0x24: // VK_HOME
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dH" : getRawSequence(InfoCmp.Capability.key_home);
                break;
            case 0x25: // VK_LEFT
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dD" : getRawSequence(InfoCmp.Capability.key_left);
                break;
            case 0x26: // VK_UP
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dA" : getRawSequence(InfoCmp.Capability.key_up);
                break;
            case 0x27: // VK_RIGHT
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dC" : getRawSequence(InfoCmp.Capability.key_right);
                break;
            case 0x28: // VK_DOWN
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dB" : getRawSequence(InfoCmp.Capability.key_down);
                break;
            case 0x2D: // VK_INSERT
                escapeSequence = getRawSequence(InfoCmp.Capability.key_ic);
                break;
            case 0x2E: // VK_DELETE
                escapeSequence = getRawSequence(InfoCmp.Capability.key_dc);
                break;
            case 0x70: // VK_F1
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dP" : getRawSequence(InfoCmp.Capability.key_f1);
                break;
            case 0x71: // VK_F2
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dQ" : getRawSequence(InfoCmp.Capability.key_f2);
                break;
            case 0x72: // VK_F3
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dR" : getRawSequence(InfoCmp.Capability.key_f3);
                break;
            case 0x73: // VK_F4
                escapeSequence = keyState > 0 ? "\\E[1;%p1%dS" : getRawSequence(InfoCmp.Capability.key_f4);
                break;
            case 0x74: // VK_F5
                escapeSequence = keyState > 0 ? "\\E[15;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f5);
                break;
            case 0x75: // VK_F6
                escapeSequence = keyState > 0 ? "\\E[17;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f6);
                break;
            case 0x76: // VK_F7
                escapeSequence = keyState > 0 ? "\\E[18;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f7);
                break;
            case 0x77: // VK_F8
                escapeSequence = keyState > 0 ? "\\E[19;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f8);
                break;
            case 0x78: // VK_F9
                escapeSequence = keyState > 0 ? "\\E[20;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f9);
                break;
            case 0x79: // VK_F10
                escapeSequence = keyState > 0 ? "\\E[21;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f10);
                break;
            case 0x7A: // VK_F11
                escapeSequence = keyState > 0 ? "\\E[23;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f11);
                break;
            case 0x7B: // VK_F12
                escapeSequence = keyState > 0 ? "\\E[24;%p1%d~" : getRawSequence(InfoCmp.Capability.key_f12);
                break;
            case 0x5D: // VK_CLOSE_BRACKET(Menu key)
            case 0x5B: // VK_OPEN_BRACKET(Window key)
                break;
        }
        return translate(escapeSequence, keyState + 1);
    }

    protected String getRawSequence(InfoCmp.Capability cap) {
        return strings.get(cap);
    }

    protected String getSequence(InfoCmp.Capability cap) {
        return translate(getRawSequence(cap));
    }

    private String translate(String str, Object... params) {
        if (str != null) {
            StringWriter sw = new StringWriter();
            try {
                Curses.tputs(sw, str, params);
            } catch (IOException e) {
                throw new IOError(e);
            }
            return sw.toString();
        }
        return null;
    }

    protected void pump() {
        try {
            while (!closing) {
                String buf = readConsoleInput();
                for (byte b : buf.getBytes(StandardCharsets.UTF_8)) {
                    processInputByte(b);
                }
            }
        } catch (IOException e) {
            if (!closing) {
                Log.warn("Error in WindowsStreamPump", e);
            }
        }
    }

    public void processInputByte(int c) throws IOException {
        if (attributes.getLocalFlag(Attributes.LocalFlag.ISIG)) {
            if (c == attributes.getControlChar(Attributes.ControlChar.VINTR)) {
                raise(Signal.INT);
                return;
            } else if (c == attributes.getControlChar(Attributes.ControlChar.VQUIT)) {
                raise(Signal.QUIT);
                return;
            } else if (c == attributes.getControlChar(Attributes.ControlChar.VSUSP)) {
                raise(Signal.TSTP);
                return;
            } else if (c == attributes.getControlChar(Attributes.ControlChar.VSTATUS)) {
                raise(Signal.INFO);
            }
        }
        if (c == '\r') {
            if (attributes.getInputFlag(Attributes.InputFlag.IGNCR)) {
                return;
            }
            if (attributes.getInputFlag(Attributes.InputFlag.ICRNL)) {
                c = '\n';
            }
        } else if (c == '\n' && attributes.getInputFlag(Attributes.InputFlag.INLCR)) {
            c = '\r';
        }
//        if (attributes.getLocalFlag(Attributes.LocalFlag.ECHO)) {
//            processOutputByte(c);
//            masterOutput.flush();
//        }
        slaveInputPipe.write(c);
        slaveInputPipe.flush();
    }

    @Override
    public boolean trackMouse(MouseTracking tracking) {
        this.tracking = tracking;
        updateConsoleMode();
        return true;
    }

    protected abstract int getConsoleOutputCP();

    protected abstract int getConsoleMode();

    protected abstract void setConsoleMode(int mode);

    protected abstract String readConsoleInput() throws IOException;

}


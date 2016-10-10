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
import org.jline.utils.NonBlockingReader;
import org.jline.utils.ShutdownHooks;
import org.jline.utils.Signals;

import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWindowsTerminal extends AbstractTerminal {

    protected static final int ENABLE_PROCESSED_INPUT = 0x0001;
    protected static final int ENABLE_LINE_INPUT      = 0x0002;
    protected static final int ENABLE_ECHO_INPUT      = 0x0004;
    protected static final int ENABLE_WINDOW_INPUT    = 0x0008;
    protected static final int ENABLE_MOUSE_INPUT     = 0x0010;
    protected static final int ENABLE_INSERT_MODE     = 0x0020;
    protected static final int ENABLE_QUICK_EDIT_MODE = 0x0040;

    protected final InputStream input;
    protected final OutputStream output;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final ShutdownHooks.Task closer;

    public AbstractWindowsTerminal(OutputStream output, String name, boolean nativeSignals, SignalHandler signalHandler) throws IOException {
        super(name, "windows", signalHandler);
        input = new DirectInputStream();
        this.output = output;
        String encoding = getConsoleEncoding();
        if (encoding == null) {
            encoding = Charset.defaultCharset().name();
        }
        this.reader = new NonBlockingReader(getName(), new org.jline.utils.InputStreamReader(input, encoding));
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

    @Override
    protected void handleDefaultSignal(Signal signal) {
        Object handler = nativeHandlers.get(signal);
        if (handler != null) {
            Signals.invokeHandler(signal.name(), handler);
        }
    }

    protected String getConsoleEncoding() {
        int codepage = getConsoleOutputCP();
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

    protected abstract int getConsoleOutputCP();

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
        Attributes attributes = new Attributes();
        if ((mode & ENABLE_ECHO_INPUT) != 0) {
            attributes.setLocalFlag(Attributes.LocalFlag.ECHO, true);
        }
        if ((mode & ENABLE_LINE_INPUT) != 0) {
            attributes.setLocalFlag(Attributes.LocalFlag.ICANON, true);
        }
        attributes.setControlChar(Attributes.ControlChar.VINTR, ctrl('C'));
        attributes.setControlChar(Attributes.ControlChar.VEOF,  ctrl('D'));
        attributes.setControlChar(Attributes.ControlChar.VSUSP, ctrl('Z'));
        return attributes;
    }

    public void setAttributes(Attributes attr) {
        int mode = 0;
        if (attr.getLocalFlag(Attributes.LocalFlag.ECHO)) {
            mode |= ENABLE_ECHO_INPUT;
        }
        if (attr.getLocalFlag(Attributes.LocalFlag.ICANON)) {
            mode |= ENABLE_LINE_INPUT;
        }
        setConsoleMode(mode);
    }

    protected int ctrl(char key) {
        return (Character.toUpperCase(key) & 0x1f);
    }

    protected abstract int getConsoleMode();

    protected abstract void setConsoleMode(int mode);

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

    protected abstract byte[] readConsoleInput();

    protected String getEscapeSequence(short keyCode) {
        String escapeSequence = null;
        switch (keyCode) {
            case 0x08: // VK_BACK BackSpace
                escapeSequence = getSequence(InfoCmp.Capability.key_backspace);
                break;
            case 0x21: // VK_PRIOR PageUp
                escapeSequence = getSequence(InfoCmp.Capability.key_ppage);
                break;
            case 0x22: // VK_NEXT PageDown
                escapeSequence = getSequence(InfoCmp.Capability.key_npage);
                break;
            case 0x23: // VK_END
                escapeSequence = getSequence(InfoCmp.Capability.key_end);
                break;
            case 0x24: // VK_HOME
                escapeSequence = getSequence(InfoCmp.Capability.key_home);
                break;
            case 0x25: // VK_LEFT
                escapeSequence = getSequence(InfoCmp.Capability.key_left);
                break;
            case 0x26: // VK_UP
                escapeSequence = getSequence(InfoCmp.Capability.key_up);
                break;
            case 0x27: // VK_RIGHT
                escapeSequence = getSequence(InfoCmp.Capability.key_right);
                break;
            case 0x28: // VK_DOWN
                escapeSequence = getSequence(InfoCmp.Capability.key_down);
                break;
            case 0x2D: // VK_INSERT
                escapeSequence = getSequence(InfoCmp.Capability.key_ic);
                break;
            case 0x2E: // VK_DELETE
                escapeSequence = getSequence(InfoCmp.Capability.key_dc);
                break;
            case 0x70: // VK_F1
                escapeSequence = getSequence(InfoCmp.Capability.key_f1);
                break;
            case 0x71: // VK_F2
                escapeSequence = getSequence(InfoCmp.Capability.key_f2);
                break;
            case 0x72: // VK_F3
                escapeSequence = getSequence(InfoCmp.Capability.key_f3);
                break;
            case 0x73: // VK_F4
                escapeSequence = getSequence(InfoCmp.Capability.key_f4);
                break;
            case 0x74: // VK_F5
                escapeSequence = getSequence(InfoCmp.Capability.key_f5);
                break;
            case 0x75: // VK_F6
                escapeSequence = getSequence(InfoCmp.Capability.key_f6);
                break;
            case 0x76: // VK_F7
                escapeSequence = getSequence(InfoCmp.Capability.key_f7);
                break;
            case 0x77: // VK_F8
                escapeSequence = getSequence(InfoCmp.Capability.key_f8);
                break;
            case 0x78: // VK_F9
                escapeSequence = getSequence(InfoCmp.Capability.key_f9);
                break;
            case 0x79: // VK_F10
                escapeSequence = getSequence(InfoCmp.Capability.key_f10);
                break;
            case 0x7A: // VK_F11
                escapeSequence = getSequence(InfoCmp.Capability.key_f11);
                break;
            case 0x7B: // VK_F12
                escapeSequence = getSequence(InfoCmp.Capability.key_f12);
                break;
            default:
                break;
        }
        return escapeSequence;
    }

    private String getSequence(InfoCmp.Capability cap) {
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

}


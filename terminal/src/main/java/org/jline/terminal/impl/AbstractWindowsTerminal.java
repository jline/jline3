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

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractWindowsTerminal extends AbstractTerminal {

    public static final String TYPE_WINDOWS = "windows";

    private static final int PIPE_SIZE = 1024;

    private static final Charset CHARSET = Charset.forName("UTF-8");
    private static final int CODE_PAGE = 65001;

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
    protected final int consoleOutputCP;

    protected MouseTracking tracking = MouseTracking.Off;
    private volatile boolean closing;

    public AbstractWindowsTerminal(OutputStream output, String name, boolean nativeSignals, SignalHandler signalHandler) throws IOException {
        super(name, TYPE_WINDOWS, signalHandler);
        PipedInputStream input = new PipedInputStream(PIPE_SIZE);
        this.slaveInputPipe = new PipedOutputStream(input);
        this.input = new FilterInputStream(input) {};
        this.output = output;
        this.consoleOutputCP = getConsoleOutputCP();
        setConsoleOutputCP(CODE_PAGE);
        this.reader = new NonBlockingReader(getName(), new org.jline.utils.InputStreamReader(input, CHARSET));
        this.writer = new PrintWriter(new OutputStreamWriter(output, CHARSET));
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
        setConsoleOutputCP(consoleOutputCP);
    }

    final int ctrlFlag = 4;
    final int altFlag = 2;
    final int shiftFlag = 1;

    protected String getEscapeSequenceFromConsoleInput(final boolean isKeyDown, final short virtualKeyCode, final char uchar, final int controlKeyState, final short repeatCount, final short scanCode) {
        final int altState = 0x0002 | 0x0001;
        final int ctrlState = 0x0008 | 0x0004;
        final int shiftState = 0x0010;
        final boolean isCtrl = (controlKeyState & ctrlState) > 0;
        final boolean isAlt = (controlKeyState & altState) > 0;
        final boolean isShift = (controlKeyState & shiftState) > 0;
        char ch = uchar;
        StringBuilder sb = new StringBuilder(32);
        // key down event
        if (isKeyDown && ch != '\3') {
            if (isShift && ch == '\t') return getSequence(InfoCmp.Capability.key_btab);
            final String keySeq = getEscapeSequence(virtualKeyCode, (isCtrl ? ctrlFlag : 0) + (isAlt ? altFlag : 0) + (isShift ? shiftFlag : 0));
            if (keySeq != null) return keySeq;
            /* uchar value in Windows when CTRL is pressed:
             * 1). Ctrl +  <0x41 to 0x5e>      : uchar=<keyCode> - 'A' + 1
             * 2). Ctrl + Backspace(0x08)      : uchar=0x7f
             * 3). Ctrl + Enter(0x0d)          : uchar=0x0a
             * 4). Ctrl + Space(0x20)          : uchar=0x20
             * 5). Ctrl + <Other key>          : uchar=0
             * 6). Ctrl + Alt + <Any key>      : uchar=0
            */
            if (ch > 0) {
                if (isAlt) sb.append("\033");
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
                    if (isAlt) sb.append("\033");
                    sb.append(ch);
                }
            }
        } else {// key up event
            if (ch == '\3') return "\3";
            // support ALT+NumPad input method
            if (virtualKeyCode == 0x12 /*VK_MENU ALT key*/ && ch > 0)
                sb.append(ch);  // no such combination in Windows
        }
        return sb.toString();
    }

    protected String getEscapeSequence(short keyCode, int keyState) {
        // virtual keycodes: http://msdn.microsoft.com/en-us/library/windows/desktop/dd375731(v=vs.85).aspx
        // TODO: numpad keys, modifiers
        String escapeSequence = null;
        String fmt = null;
        switch (keyCode) {
            case 0x08: // VK_BACK BackSpace
                escapeSequence = getSequence(InfoCmp.Capability.key_backspace);
                if ((keyState & altFlag) > 0)
                    fmt = "\\E^H";
                break;
            case 0x21: // VK_PRIOR PageUp
                escapeSequence = getSequence(InfoCmp.Capability.key_ppage);
                break;
            case 0x22: // VK_NEXT PageDown
                escapeSequence = getSequence(InfoCmp.Capability.key_npage);
                break;
            case 0x23: // VK_END
                escapeSequence = getSequence(InfoCmp.Capability.key_end);
                fmt = "\\E[1;%dF";
                break;
            case 0x24: // VK_HOME
                escapeSequence = getSequence(InfoCmp.Capability.key_home);
                fmt = "\\E[1;%dH";
                break;
            case 0x25: // VK_LEFT
                escapeSequence = getSequence(InfoCmp.Capability.key_left);
                fmt = "\\E[1;%dD";
                break;
            case 0x26: // VK_UP
                escapeSequence = getSequence(InfoCmp.Capability.key_up);
                fmt = "\\E[1;%dA";
                break;
            case 0x27: // VK_RIGHT
                escapeSequence = getSequence(InfoCmp.Capability.key_right);
                fmt = "\\E[1;%dC";
                break;
            case 0x28: // VK_DOWN
                escapeSequence = getSequence(InfoCmp.Capability.key_down);
                fmt = "\\E[1;%dB";
                break;
            case 0x2D: // VK_INSERT
                escapeSequence = getSequence(InfoCmp.Capability.key_ic);
                break;
            case 0x2E: // VK_DELETE
                escapeSequence = getSequence(InfoCmp.Capability.key_dc);
                break;
            case 0x70: // VK_F1
                escapeSequence = getSequence(InfoCmp.Capability.key_f1);
                fmt = "\\E[1;%dP";
                break;
            case 0x71: // VK_F2
                escapeSequence = getSequence(InfoCmp.Capability.key_f2);
                fmt = "\\E[1;%dQ";
                break;
            case 0x72: // VK_F3
                escapeSequence = getSequence(InfoCmp.Capability.key_f3);
                fmt = "\\E[1;%dR";
                break;
            case 0x73: // VK_F4
                escapeSequence = getSequence(InfoCmp.Capability.key_f4);
                fmt = "\\E[1;%dS";
                break;
            case 0x74: // VK_F5
                escapeSequence = getSequence(InfoCmp.Capability.key_f5);
                fmt = "\\E[15;%d";
                break;
            case 0x75: // VK_F6
                escapeSequence = getSequence(InfoCmp.Capability.key_f6);
                fmt = "\\E[17;%d";
                break;
            case 0x76: // VK_F7
                escapeSequence = getSequence(InfoCmp.Capability.key_f7);
                fmt = "\\E[18;%d";
                break;
            case 0x77: // VK_F8
                escapeSequence = getSequence(InfoCmp.Capability.key_f8);
                fmt = "\\E[19;%d";
                break;
            case 0x78: // VK_F9
                escapeSequence = getSequence(InfoCmp.Capability.key_f9);
                fmt = "\\E[20;%d";
                break;
            case 0x79: // VK_F10
                escapeSequence = getSequence(InfoCmp.Capability.key_f10);
                fmt = "\\E[21;%d";
                break;
            case 0x7A: // VK_F11
                escapeSequence = getSequence(InfoCmp.Capability.key_f11);
                fmt = "\\E[23;%d";
                break;
            case 0x7B: // VK_F12
                escapeSequence = getSequence(InfoCmp.Capability.key_f12);
                fmt = "\\E[24;%d";
                break;
            case 0x5D: // VK_CLOSE_BRACKET(Menu key)
            case 0x5B: // VK_OPEN_BRACKET(Window key)
                break;
        }
        if (fmt != null && keyState > 0) {
            if (fmt.indexOf("%d") > -1) fmt = String.format(fmt, keyState + 1);
            StringWriter sw = new StringWriter();
            try {
                Curses.tputs(sw, fmt);
            } catch (IOException e) {
                throw new IOError(e);
            }
            return sw.toString();
        }
        return escapeSequence;
    }

    protected String getSequence(InfoCmp.Capability cap) {
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

    protected void pump() {
        try {
            while (!closing) {
                String buf = readConsoleInput();
                for (byte b : buf.getBytes(CHARSET)) {
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

    protected abstract void setConsoleOutputCP(int cp);

    protected abstract int getConsoleMode();

    protected abstract void setConsoleMode(int mode);

    protected abstract String readConsoleInput() throws IOException;

}


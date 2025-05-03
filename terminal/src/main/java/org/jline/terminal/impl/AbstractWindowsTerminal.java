/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp;
import org.jline.utils.Log;
import org.jline.utils.NonBlocking;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingPumpReader;
import org.jline.utils.NonBlockingReader;
import org.jline.utils.ShutdownHooks;
import org.jline.utils.Signals;
import org.jline.utils.WriterOutputStream;

/**
 * Base implementation for terminals on Windows systems.
 *
 * <p>
 * The AbstractWindowsTerminal class provides a foundation for terminal implementations
 * on Windows operating systems. It addresses Windows-specific limitations and
 * peculiarities, particularly related to console handling, character encoding,
 * and ANSI sequence support.
 * </p>
 *
 * <p>
 * Due to Windows limitations, particularly the historically limited support for ANSI
 * sequences, this implementation uses Windows-specific APIs to handle terminal
 * operations such as setting character attributes, moving the cursor, erasing content,
 * and other terminal functions. This approach provides better compatibility and
 * performance compared to emulating ANSI sequences on Windows.
 * </p>
 *
 * <p>
 * UTF-8 support has also been historically problematic on Windows, with the code page
 * meant to emulate UTF-8 being somewhat broken. To work around these issues, this
 * implementation uses the Windows API WriteConsoleW directly. As a result, the
 * writer() method becomes the primary output mechanism, while the output() stream
 * is bridged to the writer using a WriterOutputStream wrapper.
 * </p>
 *
 * <p>
 * Key features provided by this class include:
 * </p>
 * <ul>
 *   <li>Windows console API integration</li>
 *   <li>Color attribute handling</li>
 *   <li>Cursor positioning and manipulation</li>
 *   <li>Proper UTF-8 and Unicode support</li>
 *   <li>Input processing for Windows console events</li>
 * </ul>
 *
 * <p>
 * This class is designed to be extended by concrete implementations that use
 * specific Windows API access mechanisms (e.g., JNA, JNI, FFM).
 * </p>
 *
 * @see org.jline.terminal.impl.AbstractTerminal
 * @param <Console> the Windows console type used by the specific implementation
 */
public abstract class AbstractWindowsTerminal<Console> extends AbstractTerminal {

    public static final String TYPE_WINDOWS = "windows";
    public static final String TYPE_WINDOWS_256_COLOR = "windows-256color";

    // Windows console color constants
    protected static final int FOREGROUND_BLUE = 0x0001;
    protected static final int FOREGROUND_GREEN = 0x0002;
    protected static final int FOREGROUND_RED = 0x0004;
    protected static final int FOREGROUND_INTENSITY = 0x0008;
    protected static final int BACKGROUND_BLUE = 0x0010;
    protected static final int BACKGROUND_GREEN = 0x0020;
    protected static final int BACKGROUND_RED = 0x0040;
    protected static final int BACKGROUND_INTENSITY = 0x0080;
    public static final String TYPE_WINDOWS_CONEMU = "windows-conemu";
    public static final String TYPE_WINDOWS_VTP = "windows-vtp";

    public static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    private static final int UTF8_CODE_PAGE = 65001;

    protected static final int ENABLE_PROCESSED_INPUT = 0x0001;
    protected static final int ENABLE_LINE_INPUT = 0x0002;
    protected static final int ENABLE_ECHO_INPUT = 0x0004;
    protected static final int ENABLE_WINDOW_INPUT = 0x0008;
    protected static final int ENABLE_MOUSE_INPUT = 0x0010;
    protected static final int ENABLE_INSERT_MODE = 0x0020;
    protected static final int ENABLE_QUICK_EDIT_MODE = 0x0040;
    protected static final int ENABLE_EXTENDED_FLAGS = 0x0080;

    protected final Writer slaveInputPipe;
    protected final NonBlockingInputStream input;
    protected final OutputStream output;
    protected final NonBlockingReader reader;
    protected final PrintWriter writer;
    protected final Map<Signal, Object> nativeHandlers = new HashMap<>();
    protected final ShutdownHooks.Task closer;
    protected final Attributes attributes = new Attributes();
    protected final Console inConsole;
    protected final Console outConsole;
    protected final int originalInConsoleMode;
    protected final int originalOutConsoleMode;
    private final TerminalProvider provider;
    private final SystemStream systemStream;

    protected final Object lock = new Object();
    protected boolean paused = true;
    protected Thread pump;

    protected MouseTracking tracking = MouseTracking.Off;
    protected boolean focusTracking = false;
    private volatile boolean closing;
    protected boolean skipNextLf;

    @SuppressWarnings("this-escape")
    public AbstractWindowsTerminal(
            TerminalProvider provider,
            SystemStream systemStream,
            Writer writer,
            String name,
            String type,
            Charset encoding,
            boolean nativeSignals,
            SignalHandler signalHandler,
            Console inConsole,
            int inConsoleMode,
            Console outConsole,
            int outConsoleMode)
            throws IOException {
        super(name, type, encoding, signalHandler);
        this.provider = provider;
        this.systemStream = systemStream;
        NonBlockingPumpReader reader = NonBlocking.nonBlockingPumpReader();
        this.slaveInputPipe = reader.getWriter();
        this.reader = reader;
        this.input = NonBlocking.nonBlockingStream(reader, encoding());
        this.writer = new PrintWriter(writer);
        this.output = new WriterOutputStream(writer, encoding());
        this.inConsole = inConsole;
        this.outConsole = outConsole;
        parseInfoCmp();
        // Attributes
        this.originalInConsoleMode = inConsoleMode;
        this.originalOutConsoleMode = outConsoleMode;
        attributes.setLocalFlag(Attributes.LocalFlag.ISIG, true);
        attributes.setControlChar(Attributes.ControlChar.VINTR, ctrl('C'));
        attributes.setControlChar(Attributes.ControlChar.VEOF, ctrl('D'));
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
        closer = this::close;
        ShutdownHooks.add(closer);
        // ConEMU extended fonts support
        if (TYPE_WINDOWS_CONEMU.equals(getType())
                && !Boolean.getBoolean("org.jline.terminal.conemu.disable-activate")) {
            writer.write("\u001b[9999E");
            writer.flush();
        }
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
        int mode = getConsoleMode(inConsole);
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
        if (attributes.getLocalFlag(Attributes.LocalFlag.ISIG)) {
            mode |= ENABLE_PROCESSED_INPUT;
        }
        if (attributes.getLocalFlag(Attributes.LocalFlag.ECHO)) {
            mode |= ENABLE_ECHO_INPUT;
        }
        if (attributes.getLocalFlag(Attributes.LocalFlag.ICANON)) {
            mode |= ENABLE_LINE_INPUT;
        }
        if (tracking != MouseTracking.Off) {
            mode |= ENABLE_MOUSE_INPUT;
            // mouse events not send with quick edit mode
            // to disable ENABLE_QUICK_EDIT_MODE just set extended flag
            mode |= ENABLE_EXTENDED_FLAGS;
        }
        setConsoleMode(inConsole, mode);
    }

    protected int ctrl(char key) {
        return (Character.toUpperCase(key) & 0x1f);
    }

    public void setSize(Size size) {
        throw new UnsupportedOperationException("Can not resize windows terminal");
    }

    protected void doClose() throws IOException {
        super.doClose();
        closing = true;
        if (pump != null) {
            pump.interrupt();
        }
        ShutdownHooks.remove(closer);
        for (Map.Entry<Signal, Object> entry : nativeHandlers.entrySet()) {
            Signals.unregister(entry.getKey().name(), entry.getValue());
        }
        reader.close();
        writer.close();
        setConsoleMode(inConsole, originalInConsoleMode);
        setConsoleMode(outConsole, originalOutConsoleMode);
    }

    static final int SHIFT_FLAG = 0x01;
    static final int ALT_FLAG = 0x02;
    static final int CTRL_FLAG = 0x04;

    static final int RIGHT_ALT_PRESSED = 0x0001;
    static final int LEFT_ALT_PRESSED = 0x0002;
    static final int RIGHT_CTRL_PRESSED = 0x0004;
    static final int LEFT_CTRL_PRESSED = 0x0008;
    static final int SHIFT_PRESSED = 0x0010;
    static final int NUMLOCK_ON = 0x0020;
    static final int SCROLLLOCK_ON = 0x0040;
    static final int CAPSLOCK_ON = 0x0080;

    protected void processKeyEvent(
            final boolean isKeyDown, final short virtualKeyCode, char ch, final int controlKeyState)
            throws IOException {
        final boolean isCtrl = (controlKeyState & (RIGHT_CTRL_PRESSED | LEFT_CTRL_PRESSED)) > 0;
        final boolean isAlt = (controlKeyState & (RIGHT_ALT_PRESSED | LEFT_ALT_PRESSED)) > 0;
        final boolean isShift = (controlKeyState & SHIFT_PRESSED) > 0;
        // key down event
        if (isKeyDown && ch != '\3') {
            // Pressing "Alt Gr" is translated to Alt-Ctrl, hence it has to be checked that Ctrl is _not_ pressed,
            // otherwise inserting of "Alt Gr" codes on non-US keyboards would yield errors
            if (ch != 0
                    && (controlKeyState
                                    & (RIGHT_ALT_PRESSED | LEFT_ALT_PRESSED | RIGHT_CTRL_PRESSED | LEFT_CTRL_PRESSED))
                            == (RIGHT_ALT_PRESSED | LEFT_CTRL_PRESSED)) {
                processInputChar(ch);
            } else {
                final String keySeq = getEscapeSequence(
                        virtualKeyCode, (isCtrl ? CTRL_FLAG : 0) + (isAlt ? ALT_FLAG : 0) + (isShift ? SHIFT_FLAG : 0));
                if (keySeq != null) {
                    for (char c : keySeq.toCharArray()) {
                        processInputChar(c);
                    }
                    return;
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
                        processInputChar('\033');
                    }
                    if (isCtrl && ch != '\n' && ch != 0x7f) {
                        processInputChar((char) (ch == '?' ? 0x7f : Character.toUpperCase(ch) & 0x1f));
                    } else {
                        processInputChar(ch);
                    }
                } else if (isCtrl) { // Handles the ctrl key events(uchar=0)
                    if (virtualKeyCode >= 'A' && virtualKeyCode <= 'Z') {
                        ch = (char) (virtualKeyCode - 0x40);
                    } else if (virtualKeyCode == 191) { // ?
                        ch = 127;
                    }
                    if (ch > 0) {
                        if (isAlt) {
                            processInputChar('\033');
                        }
                        processInputChar(ch);
                    }
                }
            }
        } else if (isKeyDown && ch == '\3') {
            processInputChar('\3');
        }
        // key up event
        else {
            // support ALT+NumPad input method
            if (virtualKeyCode == 0x12 /*VK_MENU ALT key*/ && ch > 0) {
                processInputChar(ch); // no such combination in Windows
            }
        }
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
            default:
                return null;
        }
        return Curses.tputs(escapeSequence, keyState + 1);
    }

    protected String getRawSequence(InfoCmp.Capability cap) {
        return strings.get(cap);
    }

    @Override
    public boolean hasFocusSupport() {
        return true;
    }

    @Override
    public boolean trackFocus(boolean tracking) {
        focusTracking = tracking;
        return true;
    }

    /**
     * Get the default foreground color for Windows terminals.
     *
     * @return the RGB value of the default foreground color, or -1 if not available
     */
    public abstract int getDefaultForegroundColor();

    /**
     * Get the default background color for Windows terminals.
     *
     * @return the RGB value of the default background color, or -1 if not available
     */
    public abstract int getDefaultBackgroundColor();

    /**
     * Convert Windows console attribute to RGB color.
     *
     * @param attribute the Windows console attribute
     * @param foreground true for foreground color, false for background color
     * @return the RGB value of the color
     */
    protected int convertAttributeToRgb(int attribute, boolean foreground) {
        // Map Windows console attributes to ANSI colors
        int index = 0;
        if (foreground) {
            if ((attribute & FOREGROUND_RED) != 0) index |= 0x1;
            if ((attribute & FOREGROUND_GREEN) != 0) index |= 0x2;
            if ((attribute & FOREGROUND_BLUE) != 0) index |= 0x4;
            if ((attribute & FOREGROUND_INTENSITY) != 0) index |= 0x8;
        } else {
            if ((attribute & BACKGROUND_RED) != 0) index |= 0x1;
            if ((attribute & BACKGROUND_GREEN) != 0) index |= 0x2;
            if ((attribute & BACKGROUND_BLUE) != 0) index |= 0x4;
            if ((attribute & BACKGROUND_INTENSITY) != 0) index |= 0x8;
        }
        return ANSI_COLORS[index];
    }

    /**
     * ANSI colors mapping.
     */
    protected static final int[] ANSI_COLORS = {
        0x000000, // black
        0xcd0000, // red
        0x00cd00, // green
        0xcdcd00, // yellow
        0x0000ee, // blue
        0xcd00cd, // magenta
        0x00cdcd, // cyan
        0xe5e5e5, // white
        0x7f7f7f, // bright black
        0xff0000, // bright red
        0x00ff00, // bright green
        0xffff00, // bright yellow
        0x5c5cff, // bright blue
        0xff00ff, // bright magenta
        0x00ffff, // bright cyan
        0xffffff // bright white
    };

    @Override
    public boolean canPauseResume() {
        return true;
    }

    @Override
    public void pause() {
        synchronized (lock) {
            paused = true;
        }
    }

    @Override
    public void pause(boolean wait) throws InterruptedException {
        Thread p;
        synchronized (lock) {
            paused = true;
            p = pump;
        }
        if (p != null) {
            p.interrupt();
            p.join();
        }
    }

    @Override
    public void resume() {
        synchronized (lock) {
            paused = false;
            if (pump == null) {
                pump = new Thread(this::pump, "WindowsStreamPump");
                pump.setDaemon(true);
                pump.start();
            }
        }
    }

    @Override
    public boolean paused() {
        synchronized (lock) {
            return paused;
        }
    }

    protected void pump() {
        try {
            while (!closing) {
                synchronized (lock) {
                    if (paused) {
                        pump = null;
                        break;
                    }
                }
                if (processConsoleInput()) {
                    slaveInputPipe.flush();
                }
            }
        } catch (IOException e) {
            if (!closing) {
                Log.warn("Error in WindowsStreamPump", e);
                try {
                    close();
                } catch (IOException e1) {
                    Log.warn("Error closing terminal", e);
                }
            }
        } finally {
            synchronized (lock) {
                pump = null;
            }
        }
    }

    public void processInputChar(char c) throws IOException {
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
        if (attributes.getInputFlag(Attributes.InputFlag.INORMEOL)) {
            if (c == '\r') {
                skipNextLf = true;
                c = '\n';
            } else if (c == '\n') {
                if (skipNextLf) {
                    skipNextLf = false;
                    return;
                }
            } else {
                skipNextLf = false;
            }
        } else if (c == '\r') {
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
    }

    @Override
    public boolean trackMouse(MouseTracking tracking) {
        this.tracking = tracking;
        updateConsoleMode();
        return true;
    }

    protected abstract int getConsoleMode(Console console);

    protected abstract void setConsoleMode(Console console, int mode);

    /**
     * Read a single input event from the input buffer and process it.
     *
     * @return true if new input was generated from the event
     * @throws IOException if anything wrong happens
     */
    protected abstract boolean processConsoleInput() throws IOException;

    @Override
    public TerminalProvider getProvider() {
        return provider;
    }

    @Override
    public SystemStream getSystemStream() {
        return systemStream;
    }
}

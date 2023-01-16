/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi.win;

import java.io.BufferedWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.IntConsumer;

import org.fusesource.jansi.internal.Kernel32;
import org.fusesource.jansi.internal.Kernel32.CONSOLE_SCREEN_BUFFER_INFO;
import org.fusesource.jansi.internal.Kernel32.INPUT_RECORD;
import org.fusesource.jansi.internal.Kernel32.KEY_EVENT_RECORD;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractWindowsTerminal;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.InfoCmp;
import org.jline.utils.OSUtils;

import static org.fusesource.jansi.internal.Kernel32.FORMAT_MESSAGE_FROM_SYSTEM;
import static org.fusesource.jansi.internal.Kernel32.FormatMessageW;
import static org.fusesource.jansi.internal.Kernel32.GetConsoleScreenBufferInfo;
import static org.fusesource.jansi.internal.Kernel32.GetLastError;
import static org.fusesource.jansi.internal.Kernel32.GetStdHandle;
import static org.fusesource.jansi.internal.Kernel32.INVALID_HANDLE_VALUE;
import static org.fusesource.jansi.internal.Kernel32.STD_ERROR_HANDLE;
import static org.fusesource.jansi.internal.Kernel32.STD_INPUT_HANDLE;
import static org.fusesource.jansi.internal.Kernel32.STD_OUTPUT_HANDLE;
import static org.fusesource.jansi.internal.Kernel32.WaitForSingleObject;
import static org.fusesource.jansi.internal.Kernel32.readConsoleInputHelper;

public class JansiWinSysTerminal extends AbstractWindowsTerminal {

    private static final long consoleIn = GetStdHandle(STD_INPUT_HANDLE);
    private static final long consoleOut = GetStdHandle(STD_OUTPUT_HANDLE);
    private static final long consoleErr = GetStdHandle(STD_ERROR_HANDLE);

    public static JansiWinSysTerminal createTerminal(String name, String type, boolean ansiPassThrough, Charset encoding,
                                                     boolean nativeSignals, SignalHandler signalHandler, boolean paused,
                                                     TerminalProvider.Stream consoleStream) throws IOException {
        Writer writer;
        int[] mode = new int[1];
        long console;
        switch (consoleStream) {
            case Output:
                console = consoleOut;
                break;
            case Error:
                console = consoleErr;
                break;
            default:
                throw new IllegalArgumentException("Unsupport stream for console: " + consoleStream);
        }
        if (ansiPassThrough) {
            if (type == null) {
                type = OSUtils.IS_CONEMU ? TYPE_WINDOWS_CONEMU : TYPE_WINDOWS;
            }
            writer = newConsoleWriter(console);
        } else {
            if (Kernel32.GetConsoleMode(console, mode) == 0 ) {
                throw new IOException("Failed to get console mode: " + getLastErrorMessage());
            }
            if (Kernel32.SetConsoleMode(console, mode[0] | AbstractWindowsTerminal.ENABLE_VIRTUAL_TERMINAL_PROCESSING) != 0) {
                if (type == null) {
                    type = TYPE_WINDOWS_VTP;
                }
                writer = newConsoleWriter(console);
            } else if (OSUtils.IS_CONEMU) {
                if (type == null) {
                    type = TYPE_WINDOWS_CONEMU;
                }
                writer = newConsoleWriter(console);
            } else {
                if (type == null) {
                    type = TYPE_WINDOWS;
                }
                writer = new WindowsAnsiWriter(new BufferedWriter(newConsoleWriter(console)));
            }
        }
        if (Kernel32.GetConsoleMode(consoleIn, mode) == 0) {
            throw new IOException("Failed to get console mode: " + getLastErrorMessage());
        }
        JansiWinSysTerminal terminal = new JansiWinSysTerminal(writer, name, type, encoding, nativeSignals,
                signalHandler, consoleIn, console);
        // Start input pump thread
        if (!paused) {
            terminal.resume();
        }
        return terminal;
    }

    private static Writer newConsoleWriter(long console) {
        return new JansiWinConsoleWriter(console);
    }

    public static boolean isWindowsSystemStream(TerminalProvider.Stream stream) {
        int[] mode = new int[1];
        long console;
        switch (stream) {
            case Input: console = consoleIn; break;
            case Output: console = consoleOut; break;
            case Error: console = consoleErr; break;
            default: return false;
        }
        return Kernel32.GetConsoleMode(console, mode) != 0;
    }

    private long console;
    private long outputHandle;

    JansiWinSysTerminal(Writer writer, String name, String type, Charset encoding, boolean nativeSignals, SignalHandler signalHandler,
                         long console, long outputHandle) throws IOException {
        super(writer, name, type, encoding, nativeSignals, signalHandler);
        this.console = console;
        this.outputHandle = outputHandle;
    }

    @Override
    protected int getConsoleMode() {
        int[] mode = new int[1];
        if (Kernel32.GetConsoleMode(console, mode) == 0) {
            return -1;
        }
        return mode[0];
    }

    @Override
    protected void setConsoleMode(int mode) {
        Kernel32.SetConsoleMode(console, mode);
    }

    public Size getSize() {
        CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
        Kernel32.GetConsoleScreenBufferInfo(outputHandle, info);
        return new Size(info.windowWidth(), info.windowHeight());
    }

    @Override
    public Size getBufferSize() {
        CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
        Kernel32.GetConsoleScreenBufferInfo(outputHandle, info);
        return new Size(info.size.x, info.size.y);
    }

    protected boolean processConsoleInput() throws IOException {
        INPUT_RECORD[] events;
        if (console != INVALID_HANDLE_VALUE
                && WaitForSingleObject(console, 100) == 0) {
            events = readConsoleInputHelper(console, 1, false);
        } else {
            return false;
        }

        boolean flush = false;
        for (INPUT_RECORD event : events) {
            if (event.eventType == INPUT_RECORD.KEY_EVENT) {
                KEY_EVENT_RECORD keyEvent = event.keyEvent;
                processKeyEvent(keyEvent.keyDown , keyEvent.keyCode, keyEvent.uchar, keyEvent.controlKeyState);
                flush = true;
            } else if (event.eventType == INPUT_RECORD.WINDOW_BUFFER_SIZE_EVENT) {
                raise(Signal.WINCH);
            } else if (event.eventType == INPUT_RECORD.MOUSE_EVENT) {
                processMouseEvent(event.mouseEvent);
                flush = true;
            } else if (event.eventType == INPUT_RECORD.FOCUS_EVENT) {
                processFocusEvent(event.focusEvent.setFocus);
            }
        }

        return flush;
    }

    private char[] focus = new char[] { '\033', '[', ' ' };

    private void processFocusEvent(boolean hasFocus) throws IOException {
        if (focusTracking) {
            focus[2] = hasFocus ? 'I' : 'O';
            slaveInputPipe.write(focus);
        }
    }

    private char[] mouse = new char[] { '\033', '[', 'M', ' ', ' ', ' ' };

    private void processMouseEvent(Kernel32.MOUSE_EVENT_RECORD mouseEvent) throws IOException {
        int dwEventFlags = mouseEvent.eventFlags;
        int dwButtonState = mouseEvent.buttonState;
        if (tracking == MouseTracking.Off
                || tracking == MouseTracking.Normal && dwEventFlags == Kernel32.MOUSE_EVENT_RECORD.MOUSE_MOVED
                || tracking == MouseTracking.Button && dwEventFlags == Kernel32.MOUSE_EVENT_RECORD.MOUSE_MOVED && dwButtonState == 0) {
            return;
        }
        int cb = 0;
        dwEventFlags &= ~ Kernel32.MOUSE_EVENT_RECORD.DOUBLE_CLICK; // Treat double-clicks as normal
        if (dwEventFlags == Kernel32.MOUSE_EVENT_RECORD.MOUSE_WHEELED) {
            cb |= 64;
            if ((dwButtonState >> 16) < 0) {
                cb |= 1;
            }
        } else if (dwEventFlags == Kernel32.MOUSE_EVENT_RECORD.MOUSE_HWHEELED) {
            return;
        } else if ((dwButtonState & Kernel32.MOUSE_EVENT_RECORD.FROM_LEFT_1ST_BUTTON_PRESSED) != 0) {
            cb |= 0x00;
        } else if ((dwButtonState & Kernel32.MOUSE_EVENT_RECORD.RIGHTMOST_BUTTON_PRESSED) != 0) {
            cb |= 0x01;
        } else if ((dwButtonState & Kernel32.MOUSE_EVENT_RECORD.FROM_LEFT_2ND_BUTTON_PRESSED) != 0) {
            cb |= 0x02;
        } else {
            cb |= 0x03;
        }
        int cx = mouseEvent.mousePosition.x;
        int cy = mouseEvent.mousePosition.y;
        mouse[3] = (char) (' ' + cb);
        mouse[4] = (char) (' ' + cx + 1);
        mouse[5] = (char) (' ' + cy + 1);
        slaveInputPipe.write(mouse);
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
        if (GetConsoleScreenBufferInfo(outputHandle, info) == 0) {
            throw new IOError(new IOException("Could not get the cursor position: " + getLastErrorMessage()));
        }
        return new Cursor(info.cursorPosition.x, info.cursorPosition.y);
    }

    public void disableScrolling() {
        strings.remove(InfoCmp.Capability.insert_line);
        strings.remove(InfoCmp.Capability.parm_insert_line);
        strings.remove(InfoCmp.Capability.delete_line);
        strings.remove(InfoCmp.Capability.parm_delete_line);
    }

    static String getLastErrorMessage() {
        int errorCode = GetLastError();
        return getErrorMessage(errorCode);
    }

    static String getErrorMessage(int errorCode) {
        int bufferSize = 160;
        byte[] data = new byte[bufferSize];
        FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM, 0, errorCode, 0, data, bufferSize, null);
        return new String(data, StandardCharsets.UTF_16LE).trim();
    }

}

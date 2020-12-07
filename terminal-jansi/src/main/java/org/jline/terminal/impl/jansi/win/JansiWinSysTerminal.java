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
import org.jline.utils.InfoCmp;
import org.jline.utils.OSUtils;

import static org.fusesource.jansi.internal.Kernel32.FORMAT_MESSAGE_FROM_SYSTEM;
import static org.fusesource.jansi.internal.Kernel32.FormatMessageW;
import static org.fusesource.jansi.internal.Kernel32.GetConsoleScreenBufferInfo;
import static org.fusesource.jansi.internal.Kernel32.GetLastError;
import static org.fusesource.jansi.internal.Kernel32.GetStdHandle;
import static org.fusesource.jansi.internal.Kernel32.INVALID_HANDLE_VALUE;
import static org.fusesource.jansi.internal.Kernel32.STD_INPUT_HANDLE;
import static org.fusesource.jansi.internal.Kernel32.STD_OUTPUT_HANDLE;
import static org.fusesource.jansi.internal.Kernel32.WaitForSingleObject;
import static org.fusesource.jansi.internal.Kernel32.readConsoleInputHelper;

public class JansiWinSysTerminal extends AbstractWindowsTerminal {
    private static final long consoleOut = GetStdHandle(STD_OUTPUT_HANDLE);
    private static final long consoleIn = GetStdHandle(STD_INPUT_HANDLE);

    public static JansiWinSysTerminal createTerminal(String name, String type, boolean ansiPassThrough, Charset encoding, int codepage, boolean nativeSignals, SignalHandler signalHandler, boolean paused) throws IOException {
        Writer writer;
        int[] mode = new int[1];
        if (ansiPassThrough) {
            if (type == null) {
                type = OSUtils.IS_CONEMU ? TYPE_WINDOWS_CONEMU : TYPE_WINDOWS;
            }
            writer = new JansiWinConsoleWriter();
        } else {
            if (Kernel32.GetConsoleMode(consoleOut, mode) == 0 ) {
                throw new IOException("Failed to get console mode: " + getLastErrorMessage());
            }
            if (Kernel32.SetConsoleMode(consoleOut, mode[0] | AbstractWindowsTerminal.ENABLE_VIRTUAL_TERMINAL_PROCESSING) != 0) {
                if (type == null) {
                    type = TYPE_WINDOWS_VTP;
                }
                writer = new JansiWinConsoleWriter();
            } else if (OSUtils.IS_CONEMU) {
                if (type == null) {
                    type = TYPE_WINDOWS_CONEMU;
                }
                writer = new JansiWinConsoleWriter();
            } else {
                if (type == null) {
                    type = TYPE_WINDOWS;
                }
                writer = new WindowsAnsiWriter(new BufferedWriter(new JansiWinConsoleWriter()));
            }
        }
        if (Kernel32.GetConsoleMode(consoleIn, mode) == 0) {
            throw new IOException("Failed to get console mode: " + getLastErrorMessage());
        }
        JansiWinSysTerminal terminal = new JansiWinSysTerminal(writer, name, type, encoding, codepage, nativeSignals, signalHandler);
        // Start input pump thread
        if (!paused) {
            terminal.resume();
        }
        return terminal;
    }

    public static boolean isWindowsConsole() {
        int[] mode = new int[1];
        return Kernel32.GetConsoleMode(consoleOut, mode) != 0 && Kernel32.GetConsoleMode(consoleIn, mode) != 0;
    }

    public static boolean isConsoleOutput() {
        int[] mode = new int[1];
        return Kernel32.GetConsoleMode(consoleOut, mode) != 0;
    }

    public static boolean isConsoleInput() {
        int[] mode = new int[1];
        return Kernel32.GetConsoleMode(consoleIn, mode) != 0;
    }

    JansiWinSysTerminal(Writer writer, String name, String type, Charset encoding, int codepage, boolean nativeSignals, SignalHandler signalHandler) throws IOException {
        super(writer, name, type, encoding, codepage, nativeSignals, signalHandler);
    }

    @Override
    protected int getConsoleMode() {
        long console = GetStdHandle(STD_INPUT_HANDLE);
        int[] mode = new int[1];
        if (Kernel32.GetConsoleMode(console, mode) == 0) {
            return -1;
        }
        return mode[0];
    }

    @Override
    protected void setConsoleMode(int mode) {
        long console = GetStdHandle (STD_INPUT_HANDLE);
        Kernel32.SetConsoleMode(console, mode);
    }

    public Size getSize() {
        long outputHandle = Kernel32.GetStdHandle(STD_OUTPUT_HANDLE);
        CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
        Kernel32.GetConsoleScreenBufferInfo(outputHandle, info);
        return new Size(info.windowWidth(), info.windowHeight());
    }

    @Override
    public Size getBufferSize() {
        long outputHandle = Kernel32.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);
        CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
        Kernel32.GetConsoleScreenBufferInfo(outputHandle, info);
        return new Size(info.size.x, info.size.y);
    }

    protected boolean processConsoleInput() throws IOException {
        INPUT_RECORD[] events;
        long console = GetStdHandle (STD_INPUT_HANDLE);
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
        long console = GetStdHandle(STD_OUTPUT_HANDLE);
        if (GetConsoleScreenBufferInfo(console, info) == 0) {
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

/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jna.win;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.IntConsumer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractWindowsTerminal;
import org.jline.utils.InfoCmp;
import org.jline.utils.Log;

public class JnaWinSysTerminal extends AbstractWindowsTerminal {

    private static final Pointer consoleIn = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_INPUT_HANDLE);
    private static final Pointer consoleOut = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);

    private int prevButtonState;

    public JnaWinSysTerminal(String name, boolean nativeSignals) throws IOException {
        this(name, nativeSignals, SignalHandler.SIG_DFL);
    }

    public JnaWinSysTerminal(String name, boolean nativeSignals, SignalHandler signalHandler) throws IOException {
        super(new WindowsAnsiOutputStream(new FileOutputStream(FileDescriptor.out), consoleOut),
              name, nativeSignals, signalHandler);
        strings.put(InfoCmp.Capability.key_mouse, "\\E[M");
    }

    @Override
    protected int getConsoleOutputCP() {
        return Kernel32.INSTANCE.GetConsoleOutputCP();
    }

    @Override
    protected void setConsoleOutputCP(int cp) {
        Kernel32.INSTANCE.SetConsoleCP(cp);
    }

    @Override
    protected int getConsoleMode() {
        IntByReference mode = new IntByReference();
        Kernel32.INSTANCE.GetConsoleMode(consoleIn, mode);
        return mode.getValue();
    }

    @Override
    protected void setConsoleMode(int mode) {
        Kernel32.INSTANCE.SetConsoleMode(consoleIn, mode);
    }

    public Size getSize() {
        Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
        Kernel32.INSTANCE.GetConsoleScreenBufferInfo(consoleOut, info);
        return new Size(info.windowWidth(), info.windowHeight());
    }

    private char[] mouse = new char[] { '\033', '[', 'M', ' ', ' ', ' ' };

    protected String readConsoleInput() throws IOException {
        Kernel32.INPUT_RECORD[] events = doReadConsoleInput();
        if (events == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Kernel32.INPUT_RECORD event : events) {
            if (event.EventType == Kernel32.INPUT_RECORD.KEY_EVENT) {
                Kernel32.KEY_EVENT_RECORD keyEvent = event.Event.KeyEvent;
                // support some C1 control sequences: ALT + [@-_] (and [a-z]?) => ESC <ascii>
                // http://en.wikipedia.org/wiki/C0_and_C1_control_codes#C1_set
                final int altState = Kernel32.LEFT_ALT_PRESSED | Kernel32.RIGHT_ALT_PRESSED;
                // Pressing "Alt Gr" is translated to Alt-Ctrl, hence it has to be checked that Ctrl is _not_ pressed,
                // otherwise inserting of "Alt Gr" codes on non-US keyboards would yield errors
                final int ctrlState = Kernel32.LEFT_CTRL_PRESSED | Kernel32.RIGHT_CTRL_PRESSED;
                // Compute the overall alt state
                boolean isAlt = ((keyEvent.dwControlKeyState & altState) != 0) && ((keyEvent.dwControlKeyState & ctrlState) == 0);

                //Log.trace(keyEvent.keyDown? "KEY_DOWN" : "KEY_UP", "key code:", keyEvent.keyCode, "char:", (long)keyEvent.uchar);
                if (keyEvent.bKeyDown) {
                    if (keyEvent.uChar.UnicodeChar > 0) {
                        boolean shiftPressed = (keyEvent.dwControlKeyState & Kernel32.SHIFT_PRESSED) != 0;
                        if (keyEvent.uChar.UnicodeChar == '\t' && shiftPressed) {
                            sb.append(getSequence(InfoCmp.Capability.key_btab));
                        } else {
                            if (isAlt) {
                                sb.append('\033');
                            }
                            sb.append(keyEvent.uChar.UnicodeChar);
                        }
                    } else {
                        String escapeSequence = getEscapeSequence(keyEvent.wVirtualKeyCode);
                        if (escapeSequence != null) {
                            for (int k = 0; k < keyEvent.wRepeatCount; k++) {
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
                    if (keyEvent.wVirtualKeyCode == 0x12/*VK_MENU ALT key*/ && keyEvent.uChar.UnicodeChar > 0) {
                        sb.append(keyEvent.uChar.UnicodeChar);
                    }
                }
            } else if (event.EventType == Kernel32.INPUT_RECORD.WINDOW_BUFFER_SIZE_EVENT) {
                raise(Signal.WINCH);
            } else if (event.EventType == Kernel32.INPUT_RECORD.MOUSE_EVENT) {
                Kernel32.MOUSE_EVENT_RECORD mouseEvent = event.Event.MouseEvent;
                int dwEventFlags = mouseEvent.dwEventFlags;
                int dwButtonState = mouseEvent.dwButtonState;
                if (tracking == MouseTracking.Off
                        || tracking == MouseTracking.Normal && dwEventFlags == Kernel32.MOUSE_MOVED
                        || tracking == MouseTracking.Button && dwEventFlags == Kernel32.MOUSE_MOVED && dwButtonState == 0) {
                    continue;
                }
                int cb = 0;
                dwEventFlags &= ~ Kernel32.DOUBLE_CLICK; // Treat double-clicks as normal
                if (dwEventFlags == Kernel32.MOUSE_WHEELED) {
                    cb |= 64;
                    if ((dwButtonState >> 16) < 0) {
                        cb |= 1;
                    }
                } else if (dwEventFlags == Kernel32.MOUSE_HWHEELED) {
                    continue;
                } else if ((dwButtonState & Kernel32.FROM_LEFT_1ST_BUTTON_PRESSED) != 0) {
                    cb |= 0x00;
                } else if ((dwButtonState & Kernel32.RIGHTMOST_BUTTON_PRESSED) != 0) {
                    cb |= 0x01;
                } else if ((dwButtonState & Kernel32.FROM_LEFT_2ND_BUTTON_PRESSED) != 0) {
                    cb |= 0x02;
                } else {
                    cb |= 0x03;
                }
                int cx = mouseEvent.dwMousePosition.X;
                int cy = mouseEvent.dwMousePosition.Y;
                mouse[3] = (char) (' ' + cb);
                mouse[4] = (char) (' ' + cx + 1);
                mouse[5] = (char) (' ' + cy + 1);
                sb.append(mouse);
                prevButtonState = dwButtonState;
            }
        }
        return sb.toString();
    }

    private Kernel32.INPUT_RECORD[] doReadConsoleInput() throws IOException {
        Kernel32.INPUT_RECORD[] ir = new Kernel32.INPUT_RECORD[1];
        IntByReference r = new IntByReference();
        Kernel32.INSTANCE.ReadConsoleInput(consoleIn, ir, ir.length, r);
        for (int i = 0; i < r.getValue(); ++i) {
            switch (ir[i].EventType) {
                case Kernel32.INPUT_RECORD.KEY_EVENT:
                case Kernel32.INPUT_RECORD.WINDOW_BUFFER_SIZE_EVENT:
                case Kernel32.INPUT_RECORD.MOUSE_EVENT:
                    return ir;
            }
        }
        return null;
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
        Kernel32.INSTANCE.GetConsoleScreenBufferInfo(consoleOut, info);
        return new Cursor(info.dwCursorPosition.X, info.dwCursorPosition.Y);
    }

}

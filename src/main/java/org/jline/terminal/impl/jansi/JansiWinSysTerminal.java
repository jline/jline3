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
import java.io.IOException;

import org.fusesource.jansi.WindowsAnsiOutputStream;
import org.fusesource.jansi.internal.Kernel32;
import org.fusesource.jansi.internal.Kernel32.INPUT_RECORD;
import org.fusesource.jansi.internal.Kernel32.KEY_EVENT_RECORD;
import org.fusesource.jansi.internal.WindowsSupport;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractWindowsTerminal;
import org.jline.utils.InfoCmp;
import org.jline.utils.Log;

public class JansiWinSysTerminal extends AbstractWindowsTerminal {

    public JansiWinSysTerminal(String name, boolean nativeSignals) throws IOException {
        this(name, nativeSignals, SignalHandler.SIG_DFL);
    }

    public JansiWinSysTerminal(String name, boolean nativeSignals, SignalHandler signalHandler) throws IOException {
        super(new WindowsAnsiOutputStream(new FileOutputStream(FileDescriptor.out)),
              name, nativeSignals, signalHandler);
    }

    protected int getConsoleOutputCP() {
        return Kernel32.GetConsoleOutputCP();
    }

    @Override
    protected int getConsoleMode() {
        return WindowsSupport.getConsoleMode();
    }

    @Override
    protected void setConsoleMode(int mode) {
        WindowsSupport.setConsoleMode(mode);
    }

    public Size getSize() {
        Size size = new Size();
        size.setColumns(WindowsSupport.getWindowsTerminalWidth());
        size.setRows(WindowsSupport.getWindowsTerminalHeight());
        return size;
    }

    protected byte[] readConsoleInput() {
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
                    boolean shiftPressed = (keyEvent.controlKeyState & KEY_EVENT_RECORD.SHIFT_PRESSED) != 0;
                    if (keyEvent.uchar == '\t' && shiftPressed) {
                        sb.append(getSequence(InfoCmp.Capability.key_btab));
                    } else {
                        if (isAlt) {
                            sb.append('\033');
                        }
                        sb.append(keyEvent.uchar);
                    }
                }
                else {
                    // virtual keycodes: http://msdn.microsoft.com/en-us/library/windows/desktop/dd375731(v=vs.85).aspx
                    // TODO: numpad keys, modifiers
                    String escapeSequence = getEscapeSequence(keyEvent.keyCode);
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

}

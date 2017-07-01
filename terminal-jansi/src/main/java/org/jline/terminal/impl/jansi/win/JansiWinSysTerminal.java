/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jansi.win;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.function.IntConsumer;

import org.fusesource.jansi.WindowsAnsiOutputStream;
import org.fusesource.jansi.internal.Kernel32;
import org.fusesource.jansi.internal.Kernel32.CONSOLE_SCREEN_BUFFER_INFO;
import org.fusesource.jansi.internal.Kernel32.INPUT_RECORD;
import org.fusesource.jansi.internal.Kernel32.KEY_EVENT_RECORD;
import org.fusesource.jansi.internal.WindowsSupport;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractWindowsTerminal;
import org.jline.utils.InfoCmp;

import static org.fusesource.jansi.internal.Kernel32.GetConsoleScreenBufferInfo;
import static org.fusesource.jansi.internal.Kernel32.GetStdHandle;
import static org.fusesource.jansi.internal.Kernel32.STD_OUTPUT_HANDLE;

public class JansiWinSysTerminal extends AbstractWindowsTerminal {

    public JansiWinSysTerminal(String name, boolean nativeSignals) throws IOException {
        this(name, nativeSignals, SignalHandler.SIG_DFL);
    }

    public JansiWinSysTerminal(String name, boolean nativeSignals, SignalHandler signalHandler) throws IOException {
        super(new WindowsAnsiOutputStream(new BufferedOutputStream(new FileOutputStream(FileDescriptor.out))),
              name, nativeSignals, signalHandler);
    }

    @Override
    protected int getConsoleOutputCP() {
        return Kernel32.GetConsoleOutputCP();
    }

    @Override
    protected void setConsoleOutputCP(int cp) {
        Kernel32.SetConsoleOutputCP(cp);
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

    protected String readConsoleInput() throws IOException {
        INPUT_RECORD[] events = WindowsSupport.readConsoleInput(1);
        if (events == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (INPUT_RECORD event : events) {
            KEY_EVENT_RECORD keyEvent = event.keyEvent;
            sb.append(getEscapeSequenceFromConsoleInput(new int[]{keyEvent.keyDown?1:0 , keyEvent.keyCode, keyEvent.uchar, keyEvent.controlKeyState, keyEvent.repeatCount,keyEvent.scanCode}));
        }
        return sb.toString();
    }

    @Override
    public Cursor getCursorPosition(IntConsumer discarded) {
        CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
        long console = GetStdHandle(STD_OUTPUT_HANDLE);
        if (GetConsoleScreenBufferInfo(console, info) == 0) {
            throw new IOError(new IOException("Could not get the cursor position: " + WindowsSupport.getLastErrorMessage()));
        }
        return new Cursor(info.cursorPosition.x, info.cursorPosition.y);
    }

    public void disableScrolling() {
        strings.remove(InfoCmp.Capability.insert_line);
        strings.remove(InfoCmp.Capability.parm_insert_line);
        strings.remove(InfoCmp.Capability.delete_line);
        strings.remove(InfoCmp.Capability.parm_delete_line);
    }
}

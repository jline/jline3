/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jansi.win;

import java.io.BufferedWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.function.IntConsumer;

import org.fusesource.jansi.internal.Kernel32;
import org.fusesource.jansi.internal.Kernel32.CONSOLE_SCREEN_BUFFER_INFO;
import org.fusesource.jansi.internal.Kernel32.INPUT_RECORD;
import org.fusesource.jansi.internal.Kernel32.KEY_EVENT_RECORD;
import org.fusesource.jansi.internal.WindowsSupport;
import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractWindowsTerminal;
import org.jline.utils.InfoCmp;
import org.jline.utils.Log;

import static org.fusesource.jansi.internal.Kernel32.GetConsoleScreenBufferInfo;
import static org.fusesource.jansi.internal.Kernel32.GetStdHandle;
import static org.fusesource.jansi.internal.Kernel32.STD_OUTPUT_HANDLE;

public class JansiWinSysTerminal extends AbstractWindowsTerminal {

    private static final int ENABLE_VIRTUAL_TERMINAL_PROCESSING = 0x0004;

    public JansiWinSysTerminal(String name, boolean nativeSignals) throws IOException {
        this(name, null, 0, nativeSignals, SignalHandler.SIG_DFL);
    }

    public JansiWinSysTerminal(String name, Charset encoding, int codepage, boolean nativeSignals, SignalHandler signalHandler) throws IOException {
        super(createAnsiWriter(new BufferedWriter(new JansiWinConsoleWriter())),
              name, encoding, codepage, nativeSignals, signalHandler);

        // Start input pump thread
        resume();
    }

    private static Writer createAnsiWriter(Writer writer) throws IOException {
        long console = GetStdHandle(STD_OUTPUT_HANDLE);

        int[] mode = new int[1];
        if (Kernel32.GetConsoleMode(console, mode) == 0) {
            throw new IOException("Failed to get console mode: " + WindowsSupport.getLastErrorMessage());
        }

        if (Kernel32.SetConsoleMode(console, mode[0] | ENABLE_VIRTUAL_TERMINAL_PROCESSING) != 0) {
            return writer;
        }

        Log.debug("Unable to enable virtual terminal processing, using AnsiWriter instead: " + WindowsSupport.getLastErrorMessage());
        return new WindowsAnsiWriter(writer);
    }


    @Override
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

    protected boolean processConsoleInput() throws IOException {
        INPUT_RECORD[] events = WindowsSupport.readConsoleInput(1, 100);
        if (events == null) {
            return false;
        }

        for (INPUT_RECORD event : events) {
            KEY_EVENT_RECORD keyEvent = event.keyEvent;
            processKeyEvent(keyEvent.keyDown , keyEvent.keyCode, keyEvent.uchar, keyEvent.controlKeyState);
        }

        return true;
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

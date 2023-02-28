/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi.win;

import java.io.IOException;

import org.fusesource.jansi.WindowsSupport;
import org.jline.terminal.impl.AbstractWindowsConsoleWriter;

import static org.fusesource.jansi.internal.Kernel32.WriteConsoleW;

class JansiWinConsoleWriter extends AbstractWindowsConsoleWriter {

    private final long console;
    private final int[] writtenChars = new int[1];

    public JansiWinConsoleWriter(long console) {
        this.console = console;
    }

    @Override
    protected void writeConsole(char[] text, int len) throws IOException {
        if (WriteConsoleW(console, text, len, writtenChars, 0) == 0) {
            throw new IOException("Failed to write to console: " + WindowsSupport.getLastErrorMessage());
        }
    }
}

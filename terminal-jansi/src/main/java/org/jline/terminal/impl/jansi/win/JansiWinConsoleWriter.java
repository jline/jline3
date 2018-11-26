/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi.win;

import org.fusesource.jansi.internal.WindowsSupport;
import org.jline.terminal.impl.AbstractWindowsConsoleWriter;

import java.io.IOException;

import static org.fusesource.jansi.internal.Kernel32.GetStdHandle;
import static org.fusesource.jansi.internal.Kernel32.STD_OUTPUT_HANDLE;
import static org.fusesource.jansi.internal.Kernel32.WriteConsoleW;

class JansiWinConsoleWriter extends AbstractWindowsConsoleWriter {

    private static final long console = GetStdHandle(STD_OUTPUT_HANDLE);
    private final int[] writtenChars = new int[1];

    @Override
    protected void writeConsole(char[] text, int len) throws IOException {
        if (WriteConsoleW(console, text, len, writtenChars, 0) == 0) {
            throw new IOException("Failed to write to console: " + WindowsSupport.getLastErrorMessage());
        }
    }

}

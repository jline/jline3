/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jni.win;

import java.io.IOException;

import org.jline.nativ.Kernel32;
import org.jline.terminal.impl.AbstractWindowsConsoleWriter;

class NativeWinConsoleWriter extends AbstractWindowsConsoleWriter {

    private final long console;
    private final int[] writtenChars = new int[1];

    public NativeWinConsoleWriter(long console) {
        this.console = console;
    }

    @Override
    protected void writeConsole(char[] text, int len) throws IOException {
        if (Kernel32.WriteConsoleW(console, text, len, writtenChars, 0) == 0) {
            throw new IOException("Failed to write to console: " + Kernel32.getLastErrorMessage());
        }
    }
}

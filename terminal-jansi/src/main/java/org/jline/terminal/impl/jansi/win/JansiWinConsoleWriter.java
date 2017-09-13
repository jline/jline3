/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jansi.win;

import org.fusesource.jansi.internal.WindowsSupport;

import java.io.IOException;
import java.io.Writer;

import static org.fusesource.jansi.internal.Kernel32.GetStdHandle;
import static org.fusesource.jansi.internal.Kernel32.STD_OUTPUT_HANDLE;
import static org.fusesource.jansi.internal.Kernel32.WriteConsoleW;

class JansiWinConsoleWriter extends Writer {

    private static final long console = GetStdHandle(STD_OUTPUT_HANDLE);

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        char[] text = cbuf;
        if (off != 0) {
            text = new char[len];
            System.arraycopy(cbuf, off, text, 0, len);
        }

        if (WriteConsoleW(console, text, len, new int[1], 0) == 0) {
            throw new IOException("Failed to write to console: " + WindowsSupport.getLastErrorMessage());
        }
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

}

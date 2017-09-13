/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jna.win;

import com.sun.jna.LastErrorException;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.IOException;
import java.io.Writer;

class JnaWinConsoleWriter extends Writer {

    private final Pointer consoleHandle;

    JnaWinConsoleWriter(Pointer consoleHandle) {
        this.consoleHandle = consoleHandle;
    }

    @Override
    public synchronized void write(char[] cbuf, int off, int len) throws IOException {
        char[] text = cbuf;
        if (off != 0) {
            text = new char[len];
            System.arraycopy(cbuf, off, text, 0, len);
        }

        try {
            Kernel32.INSTANCE.WriteConsoleW(this.consoleHandle, text, len, new IntByReference(), null);
        } catch (LastErrorException e) {
            throw new IOException("Failed to write to console", e);
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

}

/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.AnsiWriter;
import org.junit.Test;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class AbstractWindowsTerminalTest {

    @Test
    public void testWriterBuffering() throws Exception {
        StringWriter sw = new StringWriter();
        Terminal terminal = new AbstractWindowsTerminal(new AnsiWriter(new BufferedWriter(sw)), "name", Charset.defaultCharset(),0,
                false, Terminal.SignalHandler.SIG_DFL) {
            @Override
            protected int getConsoleOutputCP() {
                return 0;
            }
            @Override
            protected int getConsoleMode() {
                return 0;
            }
            @Override
            protected void setConsoleMode(int mode) {
            }
            @Override
            protected boolean processConsoleInput() throws IOException {
                return false;
            }
            @Override
            public Size getSize() {
                return new Size(80, 25);
            }
        };
        terminal.output().write("This is a char.\n".getBytes());
        assertEquals("This is a char.\n", sw.toString());
        terminal.writer().print("This is a string.\n");
        assertEquals("This is a char.\n", sw.toString());
        terminal.writer().flush();
        assertEquals("This is a char.\nThis is a string.\n", sw.toString());
    }

}

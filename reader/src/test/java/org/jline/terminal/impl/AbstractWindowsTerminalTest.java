/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.BufferedWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Size;
import org.jline.utils.AnsiWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AbstractWindowsTerminalTest {

    @Test
    public void testBracketingPasteSmall() throws Exception {
        StringWriter sw = new StringWriter();
        TestTerminal terminal = new TestTerminal(sw);
        String str = LineReaderImpl.BRACKETED_PASTE_BEGIN + "abcd";
        str.chars().forEachOrdered(c -> process(terminal, c));
        new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    LineReaderImpl.BRACKETED_PASTE_END.chars().forEachOrdered(c -> process(terminal, c));
                    "\n".chars().forEachOrdered(c -> process(terminal, c));
                })
                .start();
        LineReaderImpl reader = new LineReaderImpl(terminal);
        String res = reader.readLine();
        assertEquals("abcd", res);
    }

    @Test
    public void testBracketingPasteHuge() throws Exception {
        StringWriter sw = new StringWriter();
        TestTerminal terminal = new TestTerminal(sw);
        new Thread(() -> {
                    StringBuilder str = new StringBuilder(LineReaderImpl.BRACKETED_PASTE_BEGIN);
                    for (int i = 0; i < 100000; i++) {
                        str.append("0123456789");
                    }
                    str.toString().chars().forEachOrdered(c -> process(terminal, c));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    str.setLength(0);
                    for (int i = 0; i < 100000; i++) {
                        str.append("0123456789");
                    }
                    str.append(LineReaderImpl.BRACKETED_PASTE_END);
                    str.append("\n");
                    str.toString().chars().forEachOrdered(c -> process(terminal, c));
                })
                .start();
        LineReaderImpl reader = new LineReaderImpl(terminal);
        String res = reader.readLine();
    }

    private void process(TestTerminal terminal, int c) {
        try {
            terminal.processInputChar((char) c);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static class TestTerminal extends AbstractWindowsTerminal<Object> {
        @Override
        public int getDefaultForegroundColor() {
            return -1;
        }

        @Override
        public int getDefaultBackgroundColor() {
            return -1;
        }

        public TestTerminal(StringWriter sw) throws IOException {
            super(
                    null,
                    null,
                    new AnsiWriter(new BufferedWriter(sw)),
                    "name",
                    "windows",
                    Charset.defaultCharset(),
                    false,
                    SignalHandler.SIG_DFL,
                    null,
                    0,
                    null,
                    0);
        }

        @Override
        protected int getConsoleMode(Object console) {
            return 0;
        }

        @Override
        protected void setConsoleMode(Object console, int mode) {}

        @Override
        protected boolean processConsoleInput() throws IOException {
            return false;
        }

        @Override
        public Size getSize() {
            return new Size(10000000, 10000000);
        }
    }
}

/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.AbstractWindowsTerminal;
import org.junit.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;

public class AttributedCharSequenceTest {

    @Test
    public void testBoldOnWindows() throws IOException {
        String HIC = "\33[36;1m";
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.appendAnsi(HIC);
        sb.append("the buffer");
        //sb.append(NOR);
        AttributedString as = sb.toAttributedString();
        Terminal terminal = new DumbWindowsTerminal();

        assertEquals("\33[1;36mthe buffer\33[0m", as.toAnsi(null));
        assertEquals("\33[96mthe buffer\33[0m", as.toAnsi(terminal));
    }

    private static class DumbWindowsTerminal extends AbstractWindowsTerminal {

        public DumbWindowsTerminal() throws IOException {
            super(new StringWriter(), "windows", null,0, false, SignalHandler.SIG_DFL);
        }

        @Override
        public Size getSize() {
            return null;
        }

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
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new InterruptedIOException();
            }

            return false;
        }
    }

}

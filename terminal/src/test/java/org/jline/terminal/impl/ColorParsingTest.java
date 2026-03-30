/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.easymock.EasyMock;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.spi.Pty;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColorParsingTest {

    @Test
    void testParseColorResponseReturnsMinusOneOnEof() throws Exception {
        // EOF occurs while reading RGB hex values (after rgb: prefix but before terminator)
        NonBlockingReader reader = createReader("\033]10;rgb:ff/00");

        AbstractPosixTerminal terminal = createTerminal();
        try {
            int result = terminal.parseColorResponse(reader, 10);
            assertEquals(-1, result, "parseColorResponse should return -1 when EOF during RGB values");
        } finally {
            terminal.close();
        }
    }

    @Test
    void testParseColorResponseReturnsMinusOneOnImmediateEof() throws Exception {
        // EOF on very first peek
        NonBlockingReader reader = createReader("");

        AbstractPosixTerminal terminal = createTerminal();
        try {
            int result = terminal.parseColorResponse(reader, 10);
            assertEquals(-1, result, "parseColorResponse should return -1 on immediate EOF");
        } finally {
            terminal.close();
        }
    }

    @Test
    void testParseColorResponseWithValidBellTerminator() throws Exception {
        // Valid OSC 10 response terminated by BEL (\007)
        NonBlockingReader reader = createReader("\033]10;rgb:ff/ff/ff\007");

        AbstractPosixTerminal terminal = createTerminal();
        try {
            int result = terminal.parseColorResponse(reader, 10);
            assertEquals(0xFFFFFF, result, "parseColorResponse should parse white color correctly");
        } finally {
            terminal.close();
        }
    }

    @Test
    void testParseColorResponseWithValidStTerminator() throws Exception {
        // Valid OSC 11 response terminated by ST (ESC \)
        NonBlockingReader reader = createReader("\033]11;rgb:00/00/00\033\\");

        AbstractPosixTerminal terminal = createTerminal();
        try {
            int result = terminal.parseColorResponse(reader, 11);
            assertEquals(0x000000, result, "parseColorResponse should parse black color correctly");
        } finally {
            terminal.close();
        }
    }

    @Test
    void testParseColorResponseEofAfterPartialRgb() throws Exception {
        // EOF after writing only part of the rgb: prefix
        NonBlockingReader reader = createReader("\033]10;rg");

        AbstractPosixTerminal terminal = createTerminal();
        try {
            int result = terminal.parseColorResponse(reader, 10);
            assertEquals(-1, result, "parseColorResponse should return -1 when EOF during rgb: prefix");
        } finally {
            terminal.close();
        }
    }

    /**
     * Creates a NonBlockingReader that returns characters from the given string,
     * then returns -1 (EOF) for all subsequent reads. This avoids the ClosedException
     * that NonBlockingPumpReader throws when its writer is closed.
     */
    private NonBlockingReader createReader(String data) {
        return new NonBlockingReader() {
            private final char[] chars = data.toCharArray();
            private int pos = 0;

            @Override
            protected int read(long timeout, boolean isPeek) throws IOException {
                if (pos >= chars.length) {
                    return -1;
                }
                if (isPeek) {
                    return chars[pos];
                }
                return chars[pos++];
            }

            @Override
            public int readBuffered(char[] b, int off, int len, long timeout) throws IOException {
                if (pos >= chars.length) {
                    return -1;
                }
                int count = Math.min(len, chars.length - pos);
                System.arraycopy(chars, pos, b, off, count);
                pos += count;
                return count;
            }
        };
    }

    private AbstractPosixTerminal createTerminal() throws Exception {
        Pty pty = EasyMock.createNiceMock(Pty.class);
        EasyMock.expect(pty.getAttr()).andReturn(new Attributes()).anyTimes();
        EasyMock.expect(pty.getSlaveInput())
                .andReturn(new ByteArrayInputStream(new byte[0]))
                .anyTimes();
        EasyMock.expect(pty.getSlaveOutput())
                .andReturn(new ByteArrayOutputStream())
                .anyTimes();
        EasyMock.replay(pty);
        return new PosixSysTerminal("test", "ansi", pty, null, true, SignalHandler.SIG_DFL);
    }
}

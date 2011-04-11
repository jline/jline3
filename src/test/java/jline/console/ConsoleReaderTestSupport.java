/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.console;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jline.UnixTerminal;
import org.junit.Before;

import static org.junit.Assert.assertEquals;

/**
 * Provides support for console reader tests.
 */
public abstract class ConsoleReaderTestSupport
{
    protected ConsoleReader console;

    @Before
    public void setUp() throws Exception {
        console = new ConsoleReader(null, new ByteArrayOutputStream(), new UnixTerminal());
    }

    protected void assertBuffer(final String expected, final Buffer buffer) throws IOException {
        assertBuffer(expected, buffer, true);
    }

    protected void assertBuffer(final String expected, final Buffer buffer, final boolean clear) throws IOException {
        // clear current buffer, if any
        if (clear) {
            console.finishBuffer();
            console.getHistory().clear();
        }

        console.setInput(new ByteArrayInputStream(buffer.getBytes()));

        // run it through the reader
        while (console.readLine((String) null) != null) {
            // ignore
        }

        assertEquals(expected, console.getCursorBuffer().toString());
    }

    private String getKeyForAction(final Operation key) {
        switch (key) {
            case BACKWARD_WORD:        return "\u001Bb";
            case BEGINNING_OF_LINE:    return "\033[H";
            case END_OF_LINE:          return "\u0005";
            case UNIX_WORD_RUBOUT:     return "\u0017";
            case ACCEPT_LINE:          return "\n";
            case PREVIOUS_HISTORY:     return "\033[A";
            case NEXT_HISTORY:         return "\033[B";
            case BACKWARD_CHAR:        return "\u0002";
            case COMPLETE:             return "\011";
            case BACKWARD_DELETE_CHAR: return "\010";
        }
        throw new IllegalArgumentException(key.toString());
    }

    protected class Buffer
    {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        public Buffer() {
            // nothing
        }

        public Buffer(final String str) {
            append(str);
        }

        public byte[] getBytes() {
            return out.toByteArray();
        }

        public Buffer op(final Operation op) {
            return append(getKeyForAction(op));
        }

        public Buffer ctrlA() {
            return append("\001");
        }

        public Buffer ctrlU() {
            return append("\025");
        }

        public Buffer tab() {
            return op(Operation.COMPLETE);
        }

        public Buffer back() {
            return op(Operation.BACKWARD_DELETE_CHAR);
        }

        public Buffer left() {
            return append("\033[D");
        }

        public Buffer right() {
            return append("\033[C");
        }

        public Buffer up() {
            return append(getKeyForAction(Operation.PREVIOUS_HISTORY));
        }

        public Buffer down() {
            return append("\033[B");
        }

        public Buffer append(final String str) {
            for (byte b : str.getBytes()) {
                append(b);
            }
            return this;
        }

        public Buffer append(final int i) {
            out.write((byte) i);
            return this;
        }
    }
}

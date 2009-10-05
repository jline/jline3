/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.console;

import jline.UnixTerminal;
import static org.junit.Assert.*;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * Provides support for console reader tests.
 */
public abstract class ConsoleReaderTestSupport
{
    protected ConsoleReader console;

    @Before
    public void setUp() throws Exception {
        console = new ConsoleReader(null, new PrintWriter(
            new OutputStreamWriter(new ByteArrayOutputStream())), null,
            new UnixTerminal());
    }

    public void assertBuffer(String expected, Buffer buffer) throws IOException {
        assertBuffer(expected, buffer, true);
    }

    public void assertBuffer(String expected, Buffer buffer, boolean clear) throws IOException {
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

    private int getKeyForAction(Operation key) {
        return getKeyForAction(key.code);
    }

    private int getKeyForAction(short logicalAction) {
        int action = console.getKeyForAction(logicalAction);

        if (action == -1) {
            fail("Keystroke for logical action " + logicalAction + " was not bound in the console");
        }

        return action;
    }

    /**
     * TODO: Fix this so tests don't break on windows machines.
     *
     * @author Ryan
     */
    class Buffer
    {
        private final ByteArrayOutputStream bout = new ByteArrayOutputStream();

        public Buffer() {
        }

        public Buffer(String str) {
            append(str);
        }

        public byte[] getBytes() {
            return bout.toByteArray();
        }

        public Buffer op(short operation) {
            return append(getKeyForAction(operation));
        }

        public Buffer op(Operation op) {
            return op(op.code);
        }

        public Buffer ctrlA() {
            return append(getKeyForAction(Operation.MOVE_TO_BEG));
        }

        public Buffer ctrlU() {
            return append(getKeyForAction(Operation.KILL_LINE_PREV));
        }

        public Buffer tab() {
            return append(getKeyForAction(Operation.COMPLETE));
        }

        public Buffer back() {
            return append(getKeyForAction(Operation.DELETE_PREV_CHAR));
        }

        public Buffer left() {
            return append(UnixTerminal.ARROW_START).append(
                UnixTerminal.ARROW_PREFIX).append(UnixTerminal.ARROW_LEFT);
        }

        public Buffer right() {
            return append(UnixTerminal.ARROW_START).append(
                UnixTerminal.ARROW_PREFIX).append(UnixTerminal.ARROW_RIGHT);
        }

        public Buffer up() {
            return append(UnixTerminal.ARROW_START).append(
                UnixTerminal.ARROW_PREFIX).append(UnixTerminal.ARROW_UP);
        }

        public Buffer down() {
            return append(UnixTerminal.ARROW_START).append(
                UnixTerminal.ARROW_PREFIX).append(UnixTerminal.ARROW_DOWN);
        }

        public Buffer append(String str) {
            for (byte b : str.getBytes()) {
                append(b);
            }

            return this;
        }

        public Buffer append(int i) {
            return append((byte) i);
        }

        public Buffer append(byte b) {
            bout.write(b);

            return this;
        }
    }
}

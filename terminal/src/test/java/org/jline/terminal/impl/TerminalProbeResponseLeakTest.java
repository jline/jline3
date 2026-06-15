/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the grapheme-cluster / DA1 probe leaking its response to
 * the console.
 *
 * <p>On native (FFM/JNI) system terminals the probe runs with VMIN=0/VTIME=0,
 * so a slave-tty {@code read()} that finds no data yet returns zero bytes --
 * which {@code FileInputStream} surfaces as a spurious EOF ({@code -1}) even
 * though the terminal's reply is merely still in flight (even a sub-millisecond
 * round trip loses the race against the non-blocking read). The probe read
 * loops used to abort on that first negative result, give up, restore
 * cooked-mode/echo, and let the late-arriving reply echo to the console. They
 * must instead keep polling until the probe deadline.</p>
 *
 * @see AbstractTerminal#readProbeChar(NonBlockingReader, long)
 */
class TerminalProbeResponseLeakTest {

    /**
     * A {@link NonBlockingReader} that replays a fixed script of return values,
     * reproducing the spurious EOF ({@code -1}) a non-blocking
     * {@code FileInputStream} emits before the terminal's reply lands. Once the
     * script is exhausted every read returns {@code exhausted}.
     */
    private static final class ScriptedReader extends NonBlockingReader {
        private final Deque<Integer> script = new ArrayDeque<>();
        private final int exhausted;

        ScriptedReader(int exhausted, int... values) {
            this.exhausted = exhausted;
            for (int v : values) {
                script.add(v);
            }
        }

        boolean isExhausted() {
            return script.isEmpty();
        }

        @Override
        protected int read(long timeout, boolean isPeek) {
            Integer next = isPeek ? script.peekFirst() : script.pollFirst();
            return next != null ? next : exhausted;
        }

        @Override
        public int readBuffered(char[] b, int off, int len, long timeout) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {}
    }

    @Test
    void readProbeCharPollsPastTransientEof() throws IOException {
        // Three spurious EOFs, then 'X'. Must not give up on the first EOF.
        ScriptedReader in = new ScriptedReader(
                NonBlockingReader.READ_EXPIRED,
                NonBlockingReader.EOF,
                NonBlockingReader.EOF,
                NonBlockingReader.EOF,
                'X');
        long deadline = System.currentTimeMillis() + 1000;
        assertEquals('X', AbstractTerminal.readProbeChar(in, deadline));
    }

    @Test
    void readProbeCharReturnsEofOnlyAfterDeadline() throws IOException {
        // Nothing but spurious EOFs: poll until the deadline, then report -1
        // rather than bailing on the first one.
        ScriptedReader in = new ScriptedReader(NonBlockingReader.EOF);
        long start = System.currentTimeMillis();
        assertEquals(-1, AbstractTerminal.readProbeChar(in, start + 50));
        assertTrue(
                System.currentTimeMillis() - start >= 40,
                "should have polled until the deadline, not returned on the first EOF");
    }

    @Test
    void probeCapturesResponseDespiteTransientEof() throws Exception {
        // The reply (DECRPM "mode 2027 set" + DA1 sentinel) is preceded by
        // spurious EOFs, as on a real ssh/native terminal where the round trip
        // loses the race against the non-blocking read.
        String reply = "\033[?2027;1$y\033[?64c";
        int[] script = new int[3 + reply.length()];
        script[0] = NonBlockingReader.EOF;
        script[1] = NonBlockingReader.EOF;
        script[2] = NonBlockingReader.EOF;
        for (int i = 0; i < reply.length(); i++) {
            script[3 + i] = reply.charAt(i);
        }

        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        ScriptedReader in = new ScriptedReader(NonBlockingReader.READ_EXPIRED, script);
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8) {
                    @Override
                    public NonBlockingReader reader() {
                        return in;
                    }
                }) {
            // Before the fix this returned false: the first spurious EOF aborted
            // the read, the reply was never consumed, and it leaked to the console.
            assertTrue(terminal.supportsGraphemeClusterMode());
            // The probe query was still emitted to the terminal.
            assertTrue(masterOutput.toString(StandardCharsets.UTF_8).contains("\033[?2027$p"));
            // The whole reply was consumed, so nothing is left to leak.
            assertTrue(in.isExhausted());
        }
    }
}

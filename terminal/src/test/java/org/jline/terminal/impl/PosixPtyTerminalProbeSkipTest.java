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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests that the hard-timeout mechanism in {@code ensureModesProbed()} prevents
 * indefinite hangs when a terminal's reader blocks in native code.
 *
 * <p>When a PTY is created via {@code openpty()} and no terminal emulator is
 * attached, the native {@code read()} on the slave fd can block indefinitely
 * despite {@code VMIN=0/VTIME=0} being set. The fix runs the probe on a daemon
 * thread with a hard deadline; if the thread does not complete in time, it is
 * abandoned and all modes default to {@code NO_RESPONSE}.</p>
 *
 * @see <a href="https://github.com/jline/jline3/issues/2209">#2209</a>
 */
class PosixPtyTerminalProbeSkipTest {

    /**
     * A {@link NonBlockingReader} whose {@code read()} blocks until the thread
     * is interrupted, simulating a native PTY read that never returns.
     */
    private static final class BlockingReader extends NonBlockingReader {
        private final CountDownLatch latch = new CountDownLatch(1);

        @Override
        protected int read(long timeout, boolean isPeek) {
            // Simulate a native PTY read that blocks indefinitely and does
            // NOT respond to Thread.interrupt() — just like a real
            // FileInputStream.read0() stuck on a PTY slave fd.
            // Only unblocks when close() releases the latch.
            boolean interrupted = false;
            try {
                while (true) {
                    try {
                        latch.await();
                        return -1;
                    } catch (InterruptedException e) {
                        // Ignore — native reads are not interruptible.
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        @Override
        public int readBuffered(char[] b, int off, int len, long timeout) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void close() {
            // Release the blocking read so the abandoned probe thread can exit.
            latch.countDown();
        }
    }

    /**
     * Verifies that {@code isModeSupported()} returns within a bounded time
     * even when the terminal's reader blocks indefinitely — the hard-timeout
     * daemon thread in {@code ensureModesProbed()} breaks the hang.
     */
    @Test
    void probeTimesOutWhenReaderBlocksIndefinitely() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        BlockingReader blockingReader = new BlockingReader();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8) {
                    @Override
                    public NonBlockingReader reader() {
                        return blockingReader;
                    }
                }) {
            // This must complete within a few seconds — without the hard timeout,
            // the blocking reader would hang ensureModesProbed() indefinitely.
            CompletableFuture<Boolean> result =
                    CompletableFuture.supplyAsync(() -> terminal.isModeSupported(Terminal.Mode.GRAPHEME_CLUSTER));

            // Hard deadline is probe(200) + drain(25) + 500ms margin = ~725ms.
            // Give 5 seconds for the test to account for slow CI.
            boolean supported = result.get(5, TimeUnit.SECONDS);
            assertFalse(supported, "modes should not be supported when reader blocks");
        } finally {
            // LineDisciplineTerminal.doClose() does not close the overridden reader.
            // Release the latch so the abandoned daemon probe thread can exit.
            blockingReader.close();
        }
    }

    /**
     * Verifies that {@code supportsGraphemeClusterMode()} returns {@code false}
     * within a bounded time when the reader blocks indefinitely.
     */
    @Test
    void graphemeClusterProbeTimesOutWhenReaderBlocks() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        BlockingReader blockingReader = new BlockingReader();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8) {
                    @Override
                    public NonBlockingReader reader() {
                        return blockingReader;
                    }
                }) {
            CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(terminal::supportsGraphemeClusterMode);

            boolean supported = result.get(5, TimeUnit.SECONDS);
            assertFalse(supported, "grapheme cluster mode should not be supported when reader blocks");
        } finally {
            blockingReader.close();
        }
    }

    /**
     * Verifies that all modes report unsupported when the reader blocks and
     * the probe times out.
     */
    @Test
    void allModesUnsupportedWhenProbeTimesOut() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        BlockingReader blockingReader = new BlockingReader();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8) {
                    @Override
                    public NonBlockingReader reader() {
                        return blockingReader;
                    }
                }) {
            // Run on a separate thread because the first isModeSupported() call
            // triggers the hard-timeout probe.
            CompletableFuture<Void> result = CompletableFuture.runAsync(() -> {
                for (Terminal.Mode mode : Terminal.Mode.values()) {
                    assertFalse(
                            terminal.isModeSupported(mode),
                            "mode " + mode + " should not be supported when probe times out");
                }
            });

            result.get(5, TimeUnit.SECONDS);
        } finally {
            blockingReader.close();
        }
    }
}

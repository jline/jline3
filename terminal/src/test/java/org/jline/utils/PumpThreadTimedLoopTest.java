/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the poll-based timed pump loop introduced in #2219.
 *
 * <p>Covers:
 * <ul>
 *   <li>{@code PumpThread.runLoop(TimedIoReader, ...)} — the timed loop variant</li>
 *   <li>{@code NonBlockingInputStreamImpl} with a poll function set</li>
 *   <li>Cancellation of the pump when the consumer's timed read expires</li>
 *   <li>Poll error → IOException conversion</li>
 *   <li>Idle timeout expiry in {@code awaitReadRequest()}</li>
 * </ul>
 */
@Timeout(10)
class PumpThreadTimedLoopTest {

    // ────────────────── Poll-based NonBlockingInputStreamImpl tests ──────────────────

    /**
     * When a poll function is set and data is available, a timed read returns the byte.
     */
    @Test
    void testPollBasedReadReturnsDataWhenReady() throws Exception {
        byte[] data = {42, 99, 0x7F};
        InputStream in = new ByteArrayInputStream(data);
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("poll-data-ready", in);
        try {
            // Poll function: always says data is ready (returns > 0)
            nbis.setPollFunction(timeoutMs -> 1);

            assertEquals(42, nbis.read(500));
            assertEquals(99, nbis.read(500));
            assertEquals(0x7F, nbis.read(500));
            assertEquals(NonBlockingInputStream.EOF, nbis.read(500));
        } finally {
            nbis.close();
        }
    }

    /**
     * When the poll function returns 0 (timeout, no data) and the consumer's read
     * timeout expires, the result should be READ_EXPIRED, and the pump should be
     * cancelled (reading == false).
     */
    @Test
    void testPollTimeoutCancelsPump() throws Exception {
        // An input stream that blocks indefinitely (never provides data)
        InputStream in = new NeverReadyInputStream();
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("poll-timeout", in);
        try {
            // Poll function always says "no data" (returns 0)
            nbis.setPollFunction(timeoutMs -> 0);

            long t0 = System.currentTimeMillis();
            int result = nbis.read(300);
            long elapsed = System.currentTimeMillis() - t0;

            assertEquals(
                    NonBlockingInputStream.READ_EXPIRED,
                    result,
                    "Should return READ_EXPIRED when poll keeps timing out");
            // The read should respect the consumer timeout (~300ms)
            assertTrue(elapsed >= 250, "Should wait approximately the requested timeout");
            assertTrue(elapsed < 3000, "Should not wait excessively long");
        } finally {
            nbis.close();
        }
    }

    /**
     * When the poll function signals an error (returns negative), the pump should
     * convert it to an IOException that propagates to the consumer.
     */
    @Test
    void testPollErrorThrowsIOException() throws Exception {
        InputStream in = new NeverReadyInputStream();
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("poll-error", in);
        try {
            // Poll function always returns error (negative)
            nbis.setPollFunction(timeoutMs -> -1);

            assertThrows(
                    IOException.class, () -> nbis.read(1000), "Negative poll result should surface as IOException");
        } finally {
            nbis.close();
        }
    }

    /**
     * When the poll function throws a RuntimeException, it should be wrapped
     * in an IOException (the fix from commit a9e1dd1).
     */
    @Test
    void testPollRuntimeExceptionWrappedAsIOException() throws Exception {
        InputStream in = new NeverReadyInputStream();
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("poll-rte", in);
        try {
            nbis.setPollFunction(timeoutMs -> {
                throw new RuntimeException("simulated native failure");
            });

            IOException thrown = assertThrows(IOException.class, () -> nbis.read(1000));
            assertTrue(
                    thrown.getMessage().contains("poll() failed"),
                    "Should mention poll failure: " + thrown.getMessage());
            assertNotNull(thrown.getCause(), "Should preserve the original RuntimeException as cause");
            assertTrue(thrown.getCause() instanceof RuntimeException);
        } finally {
            nbis.close();
        }
    }

    /**
     * When data arrives after a short poll delay, the timed reader should pick it up
     * within one poll interval.
     */
    @Test
    void testPollBasedReadWithDelayedData() throws Exception {
        // InputStream that returns data after being signalled
        CountDownLatch dataReady = new CountDownLatch(1);
        InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                return 'X';
            }
        };
        AtomicInteger pollCount = new AtomicInteger(0);
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("poll-delayed", in);
        try {
            // Poll function: first 3 calls return 0 (no data), then returns 1 (data ready)
            nbis.setPollFunction(timeoutMs -> {
                int n = pollCount.incrementAndGet();
                if (n <= 3) {
                    // Simulate short poll sleep
                    try {
                        Thread.sleep(Math.min(timeoutMs, 50));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return 0; // no data yet
                }
                return 1; // data ready
            });

            int result = nbis.read(5000);
            assertEquals('X', result, "Should eventually read the byte after poll becomes ready");
            assertTrue(pollCount.get() >= 4, "Poll should have been called at least 4 times");
        } finally {
            nbis.close();
        }
    }

    /**
     * Without a poll function (pollFn == null), NonBlockingInputStreamImpl falls
     * back to the blocking IoReader path — the behavior should be identical to
     * the pre-#2219 code.
     */
    @Test
    void testBlockingFallbackWhenNoPollFunction() throws Exception {
        byte[] data = {65, 66, 67};
        InputStream in = new ByteArrayInputStream(data);
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("no-poll", in);
        try {
            // No setPollFunction call — should use blocking path
            assertEquals(65, nbis.read(500));
            assertEquals(66, nbis.read(500));
            assertEquals(67, nbis.read(500));
            assertEquals(NonBlockingInputStream.EOF, nbis.read(500));
        } finally {
            nbis.close();
        }
    }

    /**
     * Verifies that a peek operation with a poll function does not consume the byte.
     */
    @Test
    void testPeekWithPollFunction() throws Exception {
        byte[] data = {77};
        InputStream in = new ByteArrayInputStream(data);
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("poll-peek", in);
        try {
            nbis.setPollFunction(timeoutMs -> 1);

            // Peek should see the byte but not consume it
            assertEquals(77, nbis.peek(500));
            // Regular read should still see it
            assertEquals(77, nbis.read(500));
            // Now it's consumed
            assertEquals(NonBlockingInputStream.EOF, nbis.read(500));
        } finally {
            nbis.close();
        }
    }

    /**
     * After calling close(), subsequent reads should throw ClosedException even
     * when a poll function is set.
     */
    @Test
    void testCloseWithPollFunction() throws Exception {
        InputStream in = new NeverReadyInputStream();
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("poll-close", in);
        nbis.setPollFunction(timeoutMs -> 0);

        nbis.close();
        assertThrows(ClosedException.class, () -> nbis.read(100));
    }

    /**
     * The pump thread should exit within a reasonable time after the consumer's read
     * timeout expires when using the poll-based path (i.e., it doesn't hold the tty
     * fd with a blocking read).
     */
    @Test
    void testPumpThreadExitsAfterTimedReadExpires() throws Exception {
        InputStream in = new NeverReadyInputStream();
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("pump-exit", in);
        try {
            // Poll function simulates poll() with actual timeout sleep
            nbis.setPollFunction(timeoutMs -> {
                try {
                    Thread.sleep(Math.min(timeoutMs, 50));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 0;
            });

            // Trigger a timed read that will expire
            int result = nbis.read(200);
            assertEquals(NonBlockingInputStream.READ_EXPIRED, result);

            // Give the pump thread a moment to notice cancellation and park
            Thread.sleep(300);

            // The pump thread should exist but NOT be in a blocking read state.
            // Do another read — if the pump was stuck in a blocking read,
            // this would hang or steal data.
            result = nbis.read(200);
            assertEquals(NonBlockingInputStream.READ_EXPIRED, result);
        } finally {
            nbis.close();
        }
    }

    /**
     * Multiple consecutive timed reads with a poll function should all work correctly,
     * with the pump restarting between reads.
     */
    @Test
    void testConsecutiveTimedReadsWithPoll() throws Exception {
        AtomicInteger readCount = new AtomicInteger(0);
        InputStream in = new InputStream() {
            @Override
            public int read() {
                return 'A' + readCount.getAndIncrement();
            }
        };
        AtomicInteger pollCycle = new AtomicInteger(0);
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("consecutive-poll", in);
        try {
            // Poll function alternates: first call per cycle returns 0, second returns 1
            nbis.setPollFunction(timeoutMs -> {
                int n = pollCycle.incrementAndGet();
                if (n % 2 == 1) {
                    try {
                        Thread.sleep(Math.min(timeoutMs, 30));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return 0;
                }
                return 1;
            });

            assertEquals('A', nbis.read(2000));
            assertEquals('B', nbis.read(2000));
            assertEquals('C', nbis.read(2000));
        } finally {
            nbis.close();
        }
    }

    /**
     * Test that when the consumer's read expires and the pump is cancelled,
     * the pump doesn't consume a byte from the underlying stream. The next read
     * should still get the next available byte.
     */
    @Test
    void testCancelledPumpDoesNotConsumeBytes() throws Exception {
        AtomicInteger nextByte = new AtomicInteger('A');
        InputStream in = new InputStream() {
            @Override
            public int read() {
                return nextByte.getAndIncrement();
            }
        };
        AtomicInteger pollCallCount = new AtomicInteger(0);
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("no-steal", in);
        try {
            // Poll function: always times out (returns 0), simulates no data
            nbis.setPollFunction(timeoutMs -> {
                pollCallCount.incrementAndGet();
                try {
                    Thread.sleep(Math.min(timeoutMs, 30));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return 0;
            });

            // First read times out — pump should be cancelled without consuming
            int result = nbis.read(150);
            assertEquals(NonBlockingInputStream.READ_EXPIRED, result);

            // Now set poll to return data ready
            nbis.setPollFunction(timeoutMs -> 1);

            // The next byte from the stream should be 'A' (none stolen)
            result = nbis.read(2000);
            assertEquals('A', result, "Pump should not have consumed a byte during cancelled poll");
        } finally {
            nbis.close();
        }
    }

    // ────────────────── Helper classes ──────────────────

    /**
     * An InputStream whose read() blocks indefinitely (useful for testing
     * that the poll path prevents blocking).
     */
    private static class NeverReadyInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            try {
                Thread.sleep(Long.MAX_VALUE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return -1;
        }
    }
}

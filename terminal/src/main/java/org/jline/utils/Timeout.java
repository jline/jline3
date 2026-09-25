/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.util.concurrent.TimeUnit;

/**
 * Helper class for managing timeouts during I/O operations.
 *
 * <p>
 * The Timeout class provides a simple mechanism for tracking timeouts during I/O
 * operations. It helps with implementing operations that should complete within
 * a specified time limit, such as non-blocking reads with timeouts.
 * </p>
 *
 * <p>
 * This class supports both finite timeouts (specified in milliseconds) and infinite
 * timeouts (indicated by zero or negative timeout values). It provides methods for
 * starting the timeout countdown, checking if the timeout has expired, and calculating
 * the remaining time.
 * </p>
 *
 * <p>
 * The class is designed to be used in scenarios where multiple I/O operations need
 * to share a single timeout, ensuring that the total time for all operations does
 * not exceed the specified limit.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 * // Create a timeout of 5 seconds
 * Timeout timeout = new Timeout(5000);
 *
 * // Start the timeout countdown
 * timeout.start();
 *
 * // Perform I/O operations, checking for timeout
 * while (!timeout.isExpired() &amp;&amp; !operationComplete()) {
 *     // Perform a partial operation with the remaining time
 *     long remaining = timeout.remaining();
 *     performPartialOperation(remaining);
 * }
 * </pre>
 */
public class Timeout {

    private final long timeout;
    private long curNanos = 0;
    private long endNanos = Long.MAX_VALUE;

    public Timeout(long timeout) {
        this.timeout = timeout;
    }

    public boolean isInfinite() {
        return timeout <= 0;
    }

    public boolean isFinite() {
        return timeout > 0;
    }

    public boolean elapsed() {
        if (timeout > 0) {
            curNanos = System.nanoTime();
            if (endNanos == Long.MAX_VALUE) {
                long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeout);
                endNanos = saturatingAddNanos(curNanos, timeoutNanos);
            }
            // Use sub-millisecond threshold: if less than 1ms remains,
            // treat as elapsed since the API contract is in milliseconds
            return (endNanos - curNanos) < 1_000_000L;
        } else {
            return false;
        }
    }

    public long timeout() {
        if (timeout > 0) {
            long remainingMs = (endNanos - curNanos) / 1_000_000L;
            return Math.max(1, remainingMs);
        }
        return timeout;
    }

    /**
     * Adds a nanoTime base and a non-negative duration, clamping to
     * {@code Long.MAX_VALUE} on overflow.
     */
    public static long saturatingAddNanos(long baseNanos, long deltaNanos) {
        long sum = baseNanos + deltaNanos;
        // Overflow: both operands positive-ish but sum wrapped negative,
        // or deltaNanos is Long.MAX_VALUE (saturated by TimeUnit conversion)
        if (deltaNanos > 0 && sum < baseNanos) {
            return Long.MAX_VALUE;
        }
        return sum;
    }
}

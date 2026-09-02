/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

/**
 * This class wraps a regular input stream and allows it to appear as if it
 * is non-blocking; that is, reads can be performed against it that timeout
 * if no data is seen for a period of time.  This effect is achieved by having
 * a separate thread perform all non-blocking read requests and then
 * waiting on the thread to complete.
 *
 * <p>VERY IMPORTANT NOTES
 * <ul>
 *   <li> This class is not thread safe. It expects at most one reader.
 *   <li> The {@link #shutdown()} method must be called in order to shut down
 *          the thread that handles blocking I/O.
 * </ul>
 */
public class NonBlockingInputStreamImpl extends NonBlockingInputStream {

    /** Per-poll timeout used by the timed pump loop (ms). */
    private static final int POLL_INTERVAL_MS = 100;

    private InputStream in; // The actual input stream
    private TimedReader timedReader; // Optional poll-based reader (non-null on POSIX sys terminals)
    private int b = READ_EXPIRED; // Recently read byte

    private String name;
    private IOException exception = null;
    private final PumpThread pump;

    /**
     * Creates a <code>NonBlockingInputStream</code> out of a normal blocking
     * stream. Note that this call also spawns a separate thread to perform the
     * blocking I/O on behalf of the thread that is using this class. The
     * {@link #shutdown()} method must be called in order to shut this thread down.
     * @param name The stream name
     * @param in The stream to wrap
     */
    @SuppressWarnings("this-escape")
    public NonBlockingInputStreamImpl(String name, InputStream in) {
        this(name, in, null);
    }

    /**
     * Creates a <code>NonBlockingInputStream</code> with an optional timed reader.
     *
     * <p>When a {@code timedReader} is provided (typically backed by {@code poll(2)}
     * on a tty fd), the pump thread uses short-timeout reads and can be cancelled
     * promptly when the consumer's timed read expires.  This prevents the pump from
     * stealing keystrokes from subprocesses that share the same tty.</p>
     *
     * @param name        stream name
     * @param in          the stream to wrap
     * @param timedReader optional timed reader; {@code null} falls back to blocking reads
     */
    @SuppressWarnings("this-escape")
    public NonBlockingInputStreamImpl(String name, InputStream in, TimedReader timedReader) {
        this.in = in;
        this.name = name;
        this.timedReader = timedReader;
        this.pump = new PumpThread(this, 60_000);
    }

    public void shutdown() {
        pump.shutdown();
    }

    @Override
    public void close() throws IOException {
        super.close();
        try {
            in.close();
        } finally {
            pump.shutdown();
        }
    }

    /**
     * Attempts to read a byte from the input stream for a specific
     * period of time.
     * @param timeout The amount of time to wait for the character
     * @param isPeek <code>true</code>if the byte read must not be consumed
     * @return The byte read, -1 if EOF is reached, or -2 if the
     *   read timed out.
     * @throws IOException if anything wrong happens
     */
    public synchronized int read(long timeout, boolean isPeek) throws IOException {
        // Check if this input stream has been closed
        checkClosed();

        /*
         * If the thread hit an IOException, we report it.
         */
        if (exception != null) {
            assert b == READ_EXPIRED;
            IOException toBeThrown = exception;
            if (!isPeek) exception = null;
            throw toBeThrown;
        }

        /*
         * If there was a pending character from the thread, then
         * we send it. If the timeout is 0L or the thread was shut down
         * then do a local read.
         */
        if (b >= -1) {
            assert exception == null;
        } else if (!isPeek && timeout <= 0L && !pump.isReading()) {
            b = in.read();
        } else {
            /*
             * If the thread isn't reading already, then ask it to do so.
             */
            if (!pump.isReading()) {
                pump.setReading(true);
                pump.startIfNeeded(this::run, name);
                notifyAll();
            }

            /*
             * So the thread is currently doing the reading for us. So
             * now we play the waiting game.
             */
            Timeout t = new Timeout(timeout);
            while (!t.elapsed()) {
                try {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    wait(t.timeout());
                } catch (InterruptedException e) {
                    exception = (IOException) new InterruptedIOException().initCause(e);
                }

                if (exception != null) {
                    assert b == READ_EXPIRED;

                    IOException toBeThrown = exception;
                    if (!isPeek) exception = null;
                    throw toBeThrown;
                }

                if (b >= -1) {
                    assert exception == null;
                    break;
                }
            }
        }

        /*
         * If the pump is still reading and we have a timed reader, cancel the
         * pump's outstanding read so it stops polling the tty fd.  This prevents
         * the pump from stealing keystrokes from a subprocess that the caller
         * is about to spawn (see #2219).
         *
         * The pump will see reading==false at its next poll timeout (≤100 ms)
         * and return to the idle wait state without consuming a byte.
         */
        if (b == READ_EXPIRED && pump.isReading() && timedReader != null) {
            pump.setReading(false);
        }

        /*
         * b is the character that was just read. Either we set it because
         * a local read was performed or the read thread set it (or failed to
         * change it).  We will return it's value, but if this was a peek
         * operation, then we leave it in place.
         */
        int ret = b;
        if (!isPeek) {
            b = READ_EXPIRED;
        }
        return ret;
    }

    private void run() {
        PumpThread.ResultHandler handler = (value, failure) -> {
            exception = failure;
            b = value;
        };
        if (timedReader != null) {
            pump.runLoop(timedReader, POLL_INTERVAL_MS, handler, "NonBlockingInputStream");
        } else {
            pump.runLoop(in::read, handler, "NonBlockingInputStream");
        }
    }
}

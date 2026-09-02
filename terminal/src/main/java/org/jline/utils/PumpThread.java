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

/**
 * Manages the lifecycle of a background pump thread used by
 * {@link NonBlockingInputStreamImpl} and {@link NonBlockingReaderImpl}.
 *
 * <p>All state-access methods ({@link #isReading()}, {@link #setReading(boolean)},
 * {@link #startIfNeeded}, {@link #clearThread()}) must be called while the caller
 * holds the monitor on {@link #lock} (the owning NonBlocking* instance).
 * {@link #shutdown} manages its own synchronization.</p>
 */
final class PumpThread {

    @FunctionalInterface
    interface IoReader {
        int read() throws IOException;
    }

    @FunctionalInterface
    interface TimedIoReader {
        int read(int timeoutMs) throws IOException;
    }

    @FunctionalInterface
    interface ResultHandler {
        void accept(int value, IOException failure);
    }

    private final Object lock;
    private final long idleTimeout;
    private Thread thread;
    private boolean reading;

    PumpThread(Object lock, long idleTimeout) {
        this.lock = lock;
        this.idleTimeout = idleTimeout;
    }

    void startIfNeeded(Runnable task, String name) {
        if (thread == null) {
            thread = new Thread(task);
            thread.setName(name + " non blocking reader thread");
            thread.setDaemon(true);
            thread.start();
        }
    }

    void shutdown() {
        Thread t;
        synchronized (lock) {
            t = thread;
            if (t != null) {
                reading = false;
                t.interrupt();
                lock.notifyAll();
            }
        }
        if (t != null) {
            try {
                t.join(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            synchronized (lock) {
                thread = null;
            }
        }
    }

    void runLoop(IoReader reader, ResultHandler handler, String logName) {
        // A blocking reader never returns READ_EXPIRED, so the timed
        // loop behaves identically: the inner poll loop breaks on the
        // first iteration and the READ_EXPIRED guard is a no-op.
        runLoop(timeoutMs -> reader.read(), 0, handler, logName);
    }

    /**
     * Runs the pump loop using a timed reader that returns periodically,
     * allowing the loop to check whether the consumer still needs data.
     *
     * <p>Unlike the blocking {@link #runLoop(IoReader, ResultHandler, String)}
     * variant, this loop calls the timed reader with short poll intervals so
     * that a cancelled read request (consumer set {@code reading = false}) is
     * detected within one poll period — rather than blocking until a byte
     * arrives on the underlying stream.  This prevents the pump from stealing
     * keystrokes from subprocesses that share the same tty fd.</p>
     *
     * @param reader         timed reader (e.g. poll-then-read on a tty fd)
     * @param pollIntervalMs per-iteration poll timeout in milliseconds
     * @param handler        callback that receives the result
     * @param logName        label for debug logging
     */
    void runLoop(TimedIoReader reader, int pollIntervalMs, ResultHandler handler, String logName) {
        Log.debug(logName + " start");

        try {
            while (true) {
                if (!awaitReadRequest()) {
                    return;
                }
                readAndDispatch(reader, pollIntervalMs, handler);
            }
        } catch (Throwable t) {
            Log.warn("Error in " + logName + " thread", t);
        } finally {
            Log.debug(logName + " shutdown");
            synchronized (lock) {
                clearThread();
            }
        }
    }

    /**
     * Blocks until the consumer requests a read, or the idle timeout expires.
     *
     * @return {@code true} if a read was requested, {@code false} if the thread
     *         should exit (idle timeout or interruption)
     */
    private boolean awaitReadRequest() {
        synchronized (lock) {
            if (!reading) {
                try {
                    lock.wait(idleTimeout);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return reading;
        }
    }

    /**
     * Performs a single timed read and dispatches the result to the handler.
     */
    private void readAndDispatch(TimedIoReader reader, int pollIntervalMs, ResultHandler handler) {
        int value = NonBlockingInputStream.READ_EXPIRED;
        IOException failure = null;
        try {
            value = timedRead(reader, pollIntervalMs);
        } catch (IOException e) {
            failure = e;
        }

        synchronized (lock) {
            if (value != NonBlockingInputStream.READ_EXPIRED || failure != null) {
                handler.accept(value, failure);
            }
            reading = false;
            lock.notifyAll();
        }
    }

    /**
     * Polls in short intervals until data arrives or the consumer cancels.
     *
     * @return the byte/char read, {@link NonBlockingInputStream#EOF EOF}, or
     *         {@link NonBlockingInputStream#READ_EXPIRED READ_EXPIRED} if cancelled
     */
    private int timedRead(TimedIoReader reader, int pollIntervalMs) throws IOException {
        while (true) {
            int value = reader.read(pollIntervalMs);
            if (value != NonBlockingInputStream.READ_EXPIRED) {
                return value;
            }
            synchronized (lock) {
                if (!reading) {
                    return NonBlockingInputStream.READ_EXPIRED;
                }
            }
        }
    }

    boolean isReading() {
        return reading;
    }

    void setReading(boolean reading) {
        this.reading = reading;
    }

    long idleTimeout() {
        return idleTimeout;
    }

    void clearThread() {
        thread = null;
        reading = false;
    }
}

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
 * holds the monitor on the owning NonBlocking* instance.
 * {@link #shutdown} manages its own synchronization on the provided lock.</p>
 */
final class PumpThread {

    @FunctionalInterface
    interface IoReader {
        int read() throws IOException;
    }

    @FunctionalInterface
    interface ResultHandler {
        void accept(int value, IOException failure);
    }

    private final long idleTimeout;
    private Thread thread;
    private boolean reading;

    PumpThread(long idleTimeout) {
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

    void shutdown(Object lock) {
        Thread t;
        synchronized (lock) {
            t = thread;
            if (t != null) {
                reading = false;
                t.interrupt();
                lock.notify();
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

    void runLoop(Object lock, IoReader reader, ResultHandler handler, String logName) {
        Log.debug(logName + " start");
        boolean needToRead;

        try {
            while (true) {
                synchronized (lock) {
                    needToRead = reading;
                    try {
                        if (!needToRead) {
                            lock.wait(idleTimeout);
                        }
                    } catch (InterruptedException e) {
                        /* IGNORED */
                    }
                    needToRead = reading;
                    if (!needToRead) {
                        return;
                    }
                }

                int value = NonBlockingInputStream.READ_EXPIRED;
                IOException failure = null;
                try {
                    value = reader.read();
                } catch (IOException e) {
                    failure = e;
                }

                synchronized (lock) {
                    handler.accept(value, failure);
                    reading = false;
                    lock.notify();
                }
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

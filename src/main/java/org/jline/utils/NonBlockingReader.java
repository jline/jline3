/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;

/**
 * This class wraps a regular reader and allows it to appear as if it
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
 * @since 2.7
 * @author Scott C. Gray <scottgray1@gmail.com>
 */
public class NonBlockingReader
    extends Reader
    implements Runnable
{
    public static final int READ_EXPIRED = -2;

    private Reader in;                  // The actual input stream
    private int    ch   = READ_EXPIRED; // Recently read character

    private String      name;
    private boolean     threadIsReading      = false;
    private IOException exception            = null;
    private long        threadDelay          = 60 * 1000;
    private Thread      thread;

    /**
     * Creates a <code>NonBlockingReader</code> out of a normal blocking
     * reader. Note that this call also spawn a separate thread to perform the
     * blocking I/O on behalf of the thread that is using this class. The
     * {@link #shutdown()} method must be called in order to shut this thread down.
     * @param in The reader to wrap
     */
    public NonBlockingReader(String name, Reader in) {
        this.in = in;
        this.name = name;
    }

    private synchronized void startReadingThreadIfNeeded() {
        if (thread == null) {
            thread = new Thread(this);
            thread.setName(name + " non blocking reader thread");
            thread.setDaemon(true);
            thread.start();
        }
    }

    /**
     * Shuts down the thread that is handling blocking I/O. Note that if the
     * thread is currently blocked waiting for I/O it will not actually
     * shut down until the I/O is received.
     */
    public synchronized void shutdown() {
        if (thread != null) {
            notify();
        }
    }

    @Override
    public void close() throws IOException {
        /*
         * The underlying input stream is closed first. This means that if the
         * I/O thread was blocked waiting on input, it will be woken for us.
         */
        in.close();
        shutdown();
    }

    @Override
    public int read() throws IOException {
        return read(0L, false);
    }

    /**
     * Peeks to see if there is a byte waiting in the input stream without
     * actually consuming the byte.
     *
     * @param timeout The amount of time to wait, 0 == forever
     * @return -1 on eof, -2 if the timeout expired with no available input
     *   or the character that was read (without consuming it).
     */
    public int peek(long timeout) throws IOException {
        return read(timeout, true);
    }

    /**
     * Attempts to read a character from the input stream for a specific
     * period of time.
     * @param timeout The amount of time to wait for the character
     * @return The character read, -1 if EOF is reached, or -2 if the
     *   read timed out.
     */
    public int read(long timeout) throws IOException {
        return read(timeout, false);
    }

    @Override
    public synchronized boolean ready() throws IOException {
        return ch >= 0 || in.ready();
    }

    /**
     * Attempts to read a character from the input stream for a specific
     * period of time.
     * @param timeout The amount of time to wait for the character
     * @return The character read, -1 if EOF is reached, or -2 if the
     *   read timed out.
     */
    private synchronized int read(long timeout, boolean isPeek) throws IOException {
        /*
         * If the thread hit an IOException, we report it.
         */
        if (exception != null) {
            assert ch == READ_EXPIRED;
            IOException toBeThrown = exception;
            if (!isPeek)
                exception = null;
            throw toBeThrown;
        }

        /*
         * If there was a pending character from the thread, then
         * we send it. If the timeout is 0L or the thread was shut down
         * then do a local read.
         */
        if (ch >= -1) {
            assert exception == null;
        }
        else if (!isPeek && timeout <= 0L && !threadIsReading) {
            ch = in.read();
        }
        else {
            /*
             * If the thread isn't reading already, then ask it to do so.
             */
            if (!threadIsReading) {
                threadIsReading = true;
                startReadingThreadIfNeeded();
                notify();
            }

            boolean isInfinite = (timeout <= 0L);

            /*
             * So the thread is currently doing the reading for us. So
             * now we play the waiting game.
             */
            while (isInfinite || timeout > 0L)  {
                long start = System.currentTimeMillis ();

                try {
                    wait(timeout);
                }
                catch (InterruptedException e) {
                    exception = (IOException) new InterruptedIOException().initCause(e);
                }

                if (exception != null) {
                    assert ch == READ_EXPIRED;

                    IOException toBeThrown = exception;
                    if (!isPeek)
                        exception = null;
                    throw toBeThrown;
                }

                if (ch >= -1) {
                    assert exception == null;
                    break;
                }

                if (!isInfinite) {
                    timeout -= System.currentTimeMillis() - start;
                }
            }
        }

        /*
         * ch is the character that was just read. Either we set it because
         * a local read was performed or the read thread set it (or failed to
         * change it).  We will return it's value, but if this was a peek
         * operation, then we leave it in place.
         */
        int ret = ch;
        if (!isPeek) {
            ch = READ_EXPIRED;
        }
        return ret;
    }

    /**
     * This version of read() is very specific to jline's purposes, it
     * will always always return a single byte at a time, rather than filling
     * the entire buffer.
     */
    @Override
    public int read (char[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int c = this.read(0L);

        if (c == -1) {
            return -1;
        }
        b[off] = (char)c;
        return 1;
    }

    //@Override
    public void run () {
        Log.debug("NonBlockingReader start");
        boolean needToRead;

        try {
            while (true) {

                /*
                 * Synchronize to grab variables accessed by both this thread
                 * and the accessing thread.
                 */
                synchronized (this) {
                    needToRead = this.threadIsReading;

                    try {
                        /*
                         * Nothing to do? Then wait.
                         */
                        if (!needToRead) {
                            wait(threadDelay);
                        }
                    } catch (InterruptedException e) {
                        /* IGNORED */
                    }

                    needToRead = this.threadIsReading;
                    if (!needToRead) {
                        return;
                    }
                }

                /*
                 * We're not shutting down, but we need to read. This cannot
                 * happen while we are holding the lock (which we aren't now).
                 */
                int charRead = READ_EXPIRED;
                IOException failure = null;
                try {
                    charRead = in.read();
                } catch (IOException e) {
                    failure = e;
                }

                /*
                 * Re-grab the lock to update the state.
                 */
                synchronized (this) {
                    exception = failure;
                    ch = charRead;
                    threadIsReading = false;
                    notify();
                }
            }
        } finally {
            Log.debug("NonBlockingReader shutdown");
            synchronized (this) {
                thread = null;
            }
        }
    }

    public synchronized void clear() throws IOException {
        while (ready()) {
            read();
        }
    }
}

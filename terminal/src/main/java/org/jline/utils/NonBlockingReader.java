/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.io.Reader;

/**
 * A reader that provides non-blocking read operations.
 *
 * <p>
 * The NonBlockingReader class extends the standard Reader class to provide
 * non-blocking read operations. Unlike standard readers, which block until
 * data is available or the end of the stream is reached, non-blocking readers
 * can be configured to return immediately or after a specified timeout if no
 * data is available.
 * </p>
 *
 * <p>
 * This class is particularly useful for terminal applications that need to
 * perform other tasks while waiting for user input, or that need to implement
 * features like input timeouts or polling.
 * </p>
 *
 * <p>
 * The class defines two special return values:
 * </p>
 * <ul>
 *   <li>{@link #EOF} (-1) - Indicates that the end of the stream has been reached</li>
 *   <li>{@link #READ_EXPIRED} (-2) - Indicates that the read operation timed out</li>
 * </ul>
 *
 * <p>
 * Implementations of this class typically use a separate thread to handle
 * blocking I/O operations, allowing the main thread to continue execution.
 * The {@link #shutdown()} method can be used to terminate this background
 * thread when it is no longer needed.
 * </p>
 */
public abstract class NonBlockingReader extends Reader {

    /**
     * Default constructor.
     */
    public NonBlockingReader() {
        // Default constructor
    }

    public static final int EOF = -1;
    public static final int READ_EXPIRED = -2;

    /**
     * Shuts down the thread that is handling blocking I/O.
     *
     * <p>
     * This method terminates the background thread that is used to handle
     * blocking I/O operations. This allows the application to clean up resources
     * and prevent thread leaks when the reader is no longer needed.
     * </p>
     *
     * <p>
     * Note that if the thread is currently blocked waiting for I/O, it will not
     * actually shut down until the I/O is received or the thread is interrupted.
     * In some implementations, this method may interrupt the thread to force it
     * to shut down immediately.
     * </p>
     *
     * <p>
     * After calling this method, the reader should not be used anymore, as
     * subsequent read operations may fail or block indefinitely.
     * </p>
     */
    public void shutdown() {}

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
     * or the character that was read (without consuming it).
     * @throws IOException if anything wrong happens
     */
    public int peek(long timeout) throws IOException {
        return read(timeout, true);
    }

    /**
     * Attempts to read a character from the input stream for a specific
     * period of time.
     *
     * @param timeout The amount of time to wait for the character
     * @return The character read, -1 if EOF is reached, or -2 if the
     * read timed out.
     * @throws IOException if anything wrong happens
     */
    public int read(long timeout) throws IOException {
        return read(timeout, false);
    }

    /**
     * This version of read() is very specific to jline's purposes, it
     * will always always return a single byte at a time, rather than filling
     * the entire buffer.
     * @param b the buffer
     * @param off the offset in the buffer
     * @param len the maximum number of chars to read
     * @throws IOException if anything wrong happens
     */
    @Override
    public int read(char[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int c = this.read(0L);

        if (c == EOF) {
            return EOF;
        }
        b[off] = (char) c;
        return 1;
    }

    public int readBuffered(char[] b) throws IOException {
        return readBuffered(b, 0L);
    }

    public int readBuffered(char[] b, long timeout) throws IOException {
        return readBuffered(b, 0, b.length, timeout);
    }

    public abstract int readBuffered(char[] b, int off, int len, long timeout) throws IOException;

    public int available() {
        return 0;
    }

    /**
     * Attempts to read a character from the input stream for a specific
     * period of time.
     * @param timeout The amount of time to wait for the character
     * @param isPeek <code>true</code>if the character read must not be consumed
     * @return The character read, -1 if EOF is reached, or -2 if the
     *   read timed out.
     * @throws IOException if anything wrong happens
     */
    protected abstract int read(long timeout, boolean isPeek) throws IOException;
}

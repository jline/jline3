/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jline.utils.NonBlockingReader;

/**
 * A NonBlockingReader wrapper that checks if the terminal is closed before each operation.
 * <p>
 * In JLine 3.x, this class provides backward compatibility by default: when a closed terminal's
 * reader is accessed, it logs a WARNING instead of throwing an exception. This allows
 * existing code to continue working while alerting developers to the issue.
 * </p>
 * <p>
 * To enable strict mode (throwing IllegalStateException on access to closed terminal streams),
 * set the system property {@code jline.terminal.strictClose=true}.
 * </p>
 */
class ClosedCheckingReader extends NonBlockingReader {

    private static final Logger LOG = Logger.getLogger(ClosedCheckingReader.class.getName());
    private static final boolean STRICT_CLOSE = Boolean.getBoolean("jline.terminal.strictClose");

    private final NonBlockingReader reader;
    private final Supplier<Boolean> closedChecker;
    private boolean warningLogged = false;

    public ClosedCheckingReader(NonBlockingReader reader, Supplier<Boolean> closedChecker) {
        this.reader = reader;
        this.closedChecker = closedChecker;
    }

    private void checkClosed() {
        if (closedChecker.get()) {
            if (STRICT_CLOSE) {
                throw new IllegalStateException("Terminal has been closed");
            } else {
                // Log warning only once per stream instance to avoid log spam
                if (!warningLogged) {
                    LOG.log(
                            Level.WARNING,
                            "Accessing reader of a closed terminal. "
                                    + "This may indicate a resource management issue. "
                                    + "Set -Djline.terminal.strictClose=true to make this an error.",
                            new Throwable("Stack trace"));
                    warningLogged = true;
                }
            }
        }
    }

    @Override
    protected int read(long timeout, boolean isPeek) throws IOException {
        checkClosed();
        // Use public methods instead of protected read(long, boolean)
        if (isPeek) {
            return reader.peek(timeout);
        } else {
            return reader.read(timeout);
        }
    }

    @Override
    public int readBuffered(char[] b, int off, int len, long timeout) throws IOException {
        checkClosed();
        return reader.readBuffered(b, off, len, timeout);
    }

    @Override
    public int read() throws IOException {
        checkClosed();
        return reader.read();
    }

    @Override
    public int read(char[] cbuf) throws IOException {
        checkClosed();
        return reader.read(cbuf);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        checkClosed();
        return reader.read(cbuf, off, len);
    }

    @Override
    public int read(CharBuffer target) throws IOException {
        checkClosed();
        return reader.read(target);
    }

    @Override
    public long skip(long n) throws IOException {
        checkClosed();
        return reader.skip(n);
    }

    @Override
    public boolean ready() throws IOException {
        checkClosed();
        return reader.ready();
    }

    @Override
    public boolean markSupported() {
        checkClosed();
        return reader.markSupported();
    }

    @Override
    public void mark(int readAheadLimit) throws IOException {
        checkClosed();
        reader.mark(readAheadLimit);
    }

    @Override
    public void reset() throws IOException {
        checkClosed();
        reader.reset();
    }

    @Override
    public void close() throws IOException {
        checkClosed();
        reader.close();
    }

    @Override
    public int peek(long timeout) throws IOException {
        checkClosed();
        return reader.peek(timeout);
    }

    @Override
    public int available() {
        checkClosed();
        return reader.available();
    }
}

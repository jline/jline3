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
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jline.utils.NonBlockingInputStream;

/**
 * A NonBlockingInputStream wrapper that checks if the terminal is closed before each operation.
 * <p>
 * In JLine 3.x, this class provides backward compatibility by default: when a closed terminal's
 * input stream is accessed, it logs a WARNING instead of throwing an exception. This allows
 * existing code to continue working while alerting developers to the issue.
 * </p>
 * <p>
 * To enable strict mode (throwing IllegalStateException on access to closed terminal streams),
 * set the system property {@code jline.terminal.strictClose=true}.
 * </p>
 */
class ClosedCheckingInputStream extends NonBlockingInputStream {

    private static final Logger LOG = Logger.getLogger(ClosedCheckingInputStream.class.getName());
    private static final boolean STRICT_CLOSE = Boolean.getBoolean("jline.terminal.strictClose");

    private final NonBlockingInputStream in;
    private final Supplier<Boolean> closedChecker;
    private boolean warningLogged = false;

    public ClosedCheckingInputStream(NonBlockingInputStream in, Supplier<Boolean> closedChecker) {
        this.in = in;
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
                            "Accessing input stream of a closed terminal. "
                                    + "This may indicate a resource management issue. "
                                    + "Set -Djline.terminal.strictClose=true to make this an error.",
                            new Throwable("Stack trace"));
                    warningLogged = true;
                }
            }
        }
    }

    @Override
    public int read(long timeout, boolean isPeek) throws IOException {
        checkClosed();
        return in.read(timeout, isPeek);
    }

    @Override
    public int read() throws IOException {
        checkClosed();
        return in.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        checkClosed();
        return in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkClosed();
        return in.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        checkClosed();
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        checkClosed();
        return in.available();
    }

    @Override
    public void close() throws IOException {
        checkClosed();
        in.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        checkClosed();
        in.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        checkClosed();
        in.reset();
    }

    @Override
    public boolean markSupported() {
        checkClosed();
        return in.markSupported();
    }
}

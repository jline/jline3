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
import java.io.OutputStream;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An OutputStream wrapper that checks if the terminal is closed before each operation.
 * <p>
 * In JLine 3.x, this class provides backward compatibility by default: when a closed terminal's
 * output stream is accessed, it logs a WARNING instead of throwing an exception. This allows
 * existing code to continue working while alerting developers to the issue.
 * </p>
 * <p>
 * To enable strict mode (throwing IllegalStateException on access to closed terminal streams),
 * set the system property {@code jline.terminal.strictClose=true}.
 * </p>
 */
class ClosedCheckingOutputStream extends OutputStream {

    private static final Logger LOG = Logger.getLogger(ClosedCheckingOutputStream.class.getName());
    private static final boolean STRICT_CLOSE = Boolean.getBoolean("jline.terminal.strictClose");

    private final OutputStream out;
    private final Supplier<Boolean> closedChecker;
    private boolean warningLogged = false;

    public ClosedCheckingOutputStream(OutputStream out, Supplier<Boolean> closedChecker) {
        this.out = out;
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
                            "Accessing output stream of a closed terminal. "
                                    + "This may indicate a resource management issue. "
                                    + "Set -Djline.terminal.strictClose=true to make this an error.",
                            new Throwable("Stack trace"));
                    warningLogged = true;
                }
            }
        }
    }

    @Override
    public void write(int b) throws IOException {
        checkClosed();
        out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkClosed();
        out.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkClosed();
        out.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkClosed();
        out.flush();
    }

    @Override
    public void close() throws IOException {
        checkClosed();
        out.close();
    }
}

/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A PrintWriter wrapper that checks if the terminal is closed before each operation.
 * <p>
 * In JLine 3.x, this class provides backward compatibility by default: when a closed terminal's
 * writer is accessed, it logs a WARNING instead of throwing an exception. This allows
 * existing code to continue working while alerting developers to the issue.
 * </p>
 * <p>
 * To enable strict mode (throwing IllegalStateException on access to closed terminal streams),
 * set the system property {@code jline.terminal.strictClose=true}.
 * </p>
 */
class ClosedCheckingPrintWriter extends PrintWriter {

    private static final Logger LOG = Logger.getLogger(ClosedCheckingPrintWriter.class.getName());
    private static final boolean STRICT_CLOSE = Boolean.getBoolean("jline.terminal.strictClose");

    private final Supplier<Boolean> closedChecker;
    private boolean warningLogged = false;

    public ClosedCheckingPrintWriter(Writer out, Supplier<Boolean> closedChecker) {
        super(out);
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
                            "Accessing writer of a closed terminal. "
                                    + "This may indicate a resource management issue. "
                                    + "Set -Djline.terminal.strictClose=true to make this an error.",
                            new Throwable("Stack trace"));
                    warningLogged = true;
                }
            }
        }
    }

    @Override
    public void flush() {
        checkClosed();
        super.flush();
    }

    @Override
    public void close() {
        checkClosed();
        super.close();
    }

    @Override
    public boolean checkError() {
        checkClosed();
        return super.checkError();
    }

    @Override
    public void write(int c) {
        checkClosed();
        super.write(c);
    }

    @Override
    public void write(char[] buf, int off, int len) {
        checkClosed();
        super.write(buf, off, len);
    }

    @Override
    public void write(char[] buf) {
        checkClosed();
        super.write(buf);
    }

    @Override
    public void write(String s, int off, int len) {
        checkClosed();
        super.write(s, off, len);
    }

    @Override
    public void write(String s) {
        checkClosed();
        super.write(s);
    }

    @Override
    public void print(boolean b) {
        checkClosed();
        super.print(b);
    }

    @Override
    public void print(char c) {
        checkClosed();
        super.print(c);
    }

    @Override
    public void print(int i) {
        checkClosed();
        super.print(i);
    }

    @Override
    public void print(long l) {
        checkClosed();
        super.print(l);
    }

    @Override
    public void print(float f) {
        checkClosed();
        super.print(f);
    }

    @Override
    public void print(double d) {
        checkClosed();
        super.print(d);
    }

    @Override
    public void print(char[] s) {
        checkClosed();
        super.print(s);
    }

    @Override
    public void print(String s) {
        checkClosed();
        super.print(s);
    }

    @Override
    public void print(Object obj) {
        checkClosed();
        super.print(obj);
    }

    @Override
    public void println() {
        checkClosed();
        super.println();
    }

    @Override
    public void println(boolean x) {
        checkClosed();
        super.println(x);
    }

    @Override
    public void println(char x) {
        checkClosed();
        super.println(x);
    }

    @Override
    public void println(int x) {
        checkClosed();
        super.println(x);
    }

    @Override
    public void println(long x) {
        checkClosed();
        super.println(x);
    }

    @Override
    public void println(float x) {
        checkClosed();
        super.println(x);
    }

    @Override
    public void println(double x) {
        checkClosed();
        super.println(x);
    }

    @Override
    public void println(char[] x) {
        checkClosed();
        super.println(x);
    }

    @Override
    public void println(String x) {
        checkClosed();
        super.println(x);
    }

    @Override
    public void println(Object x) {
        checkClosed();
        super.println(x);
    }

    @Override
    public PrintWriter printf(String format, Object... args) {
        checkClosed();
        return super.printf(format, args);
    }

    @Override
    public PrintWriter printf(Locale l, String format, Object... args) {
        checkClosed();
        return super.printf(l, format, args);
    }

    @Override
    public PrintWriter format(String format, Object... args) {
        checkClosed();
        return super.format(format, args);
    }

    @Override
    public PrintWriter format(Locale l, String format, Object... args) {
        checkClosed();
        return super.format(l, format, args);
    }

    @Override
    public PrintWriter append(CharSequence csq) {
        checkClosed();
        return super.append(csq);
    }

    @Override
    public PrintWriter append(CharSequence csq, int start, int end) {
        checkClosed();
        return super.append(csq, start, end);
    }

    @Override
    public PrintWriter append(char c) {
        checkClosed();
        return super.append(c);
    }
}

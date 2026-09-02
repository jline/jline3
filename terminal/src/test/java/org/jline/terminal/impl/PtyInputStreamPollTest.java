/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.function.IntUnaryOperator;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Sized;
import org.jline.utils.NonBlockingInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.jline.terminal.TerminalBuilder.PROP_NON_BLOCKING_READS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@code PtyInputStream} uses {@code poll(2)} when available,
 * falling back to the VMIN/VTIME heuristic when not.
 */
class PtyInputStreamPollTest {

    private String savedNonBlockingReads;

    @BeforeEach
    void setUp() {
        savedNonBlockingReads = System.getProperty(PROP_NON_BLOCKING_READS);
        System.setProperty(PROP_NON_BLOCKING_READS, "true");
    }

    @AfterEach
    void tearDown() {
        if (savedNonBlockingReads != null) {
            System.setProperty(PROP_NON_BLOCKING_READS, savedNonBlockingReads);
        } else {
            System.clearProperty(PROP_NON_BLOCKING_READS);
        }
    }

    /**
     * Creates a test PTY with a simulated poll function.
     *
     * @param pipedIn the input stream to read from
     * @param writerClosed set to true when the writer is closed, so the
     *                     poll function can simulate POLLHUP
     */
    private AbstractPty createPtyWithPoll(
            PipedInputStream pipedIn, java.util.concurrent.atomic.AtomicBoolean writerClosed) {
        return new AbstractPty(null, null) {
            @Override
            protected IntUnaryOperator createSlavePollFunction() {
                // Simulate poll(2):
                // data available → return 1 (POLLIN)
                // writer closed  → return 1 (POLLHUP — let read() discover EOF)
                // no data, open  → sleep up to timeout, return 0
                return timeoutMs -> {
                    try {
                        long deadline = System.currentTimeMillis() + timeoutMs;
                        do {
                            if (pipedIn.available() > 0 || writerClosed.get()) {
                                return 1;
                            }
                            Thread.sleep(Math.min(5, Math.max(1, deadline - System.currentTimeMillis())));
                        } while (System.currentTimeMillis() < deadline);
                        return writerClosed.get() ? 1 : 0;
                    } catch (Exception e) {
                        return 1;
                    }
                };
            }

            @Override
            protected void doSetAttr(Attributes attr) {}

            @Override
            protected InputStream doGetSlaveInput() {
                return pipedIn;
            }

            @Override
            public InputStream getMasterInput() {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream getMasterOutput() {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream getSlaveOutput() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Attributes getAttr() {
                return new Attributes();
            }

            @Override
            public Size getSize() {
                return Size.of(80, 24);
            }

            @Override
            public void setSize(Sized size) {}

            @Override
            public void close() {}
        };
    }

    @Test
    @Timeout(10)
    void pollBasedReadReturnsData() throws Exception {
        PipedInputStream pipedIn = new PipedInputStream();
        PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
        java.util.concurrent.atomic.AtomicBoolean writerClosed = new java.util.concurrent.atomic.AtomicBoolean();

        AbstractPty pty = createPtyWithPoll(pipedIn, writerClosed);
        // Initialize terminal attributes (normally done by PosixPtyTerminal)
        pty.setAttr(new Attributes());
        InputStream slaveInput = pty.getSlaveInput();
        assertTrue(slaveInput instanceof NonBlockingInputStream);
        NonBlockingInputStream nbis = (NonBlockingInputStream) slaveInput;

        // Write data and verify it's read correctly via poll path
        pipedOut.write('X');
        int ch = nbis.read(1000);
        assertEquals('X', ch);
    }

    @Test
    @Timeout(10)
    void pollBasedReadTimeoutReturnsExpired() throws Exception {
        PipedInputStream pipedIn = new PipedInputStream();
        new PipedOutputStream(pipedIn); // keep pipe open
        java.util.concurrent.atomic.AtomicBoolean writerClosed = new java.util.concurrent.atomic.AtomicBoolean();

        AbstractPty pty = createPtyWithPoll(pipedIn, writerClosed);
        // Initialize terminal attributes (normally done by PosixPtyTerminal)
        pty.setAttr(new Attributes());
        InputStream slaveInput = pty.getSlaveInput();
        NonBlockingInputStream nbis = (NonBlockingInputStream) slaveInput;

        // No data written — should timeout and return READ_EXPIRED
        long start = System.currentTimeMillis();
        int result = nbis.read(100);
        long elapsed = System.currentTimeMillis() - start;

        assertEquals(NonBlockingInputStream.READ_EXPIRED, result);
        assertTrue(elapsed >= 50, "Timeout should have waited at least 50ms, was " + elapsed + "ms");
    }

    @Test
    @Timeout(10)
    void pollBasedReadDetectsEof() throws Exception {
        PipedInputStream pipedIn = new PipedInputStream();
        PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
        java.util.concurrent.atomic.AtomicBoolean writerClosed = new java.util.concurrent.atomic.AtomicBoolean();

        AbstractPty pty = createPtyWithPoll(pipedIn, writerClosed);
        // Initialize terminal attributes (normally done by PosixPtyTerminal)
        pty.setAttr(new Attributes());
        InputStream slaveInput = pty.getSlaveInput();
        NonBlockingInputStream nbis = (NonBlockingInputStream) slaveInput;

        // Write, close, then read should get data followed by EOF
        pipedOut.write('A');
        pipedOut.close();
        writerClosed.set(true);

        int ch = nbis.read(1000);
        assertEquals('A', ch);

        int eof = nbis.read(1000);
        assertEquals(-1, eof, "Expected EOF (-1) after pipe closed");
    }
}

/*
 * Copyright (c) 2026, the original author(s).
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

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.utils.NonBlockingInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.jline.terminal.TerminalBuilder.PROP_NON_BLOCKING_READS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PtyInputStreamTest {

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

    @Test
    @Timeout(10)
    void eofPropagation() throws Exception {
        PipedInputStream pipedIn = new PipedInputStream();
        PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);

        NonBlockingInputStream nbis = createPtyInputStream(pipedIn);

        pipedOut.write('A');
        pipedOut.close();

        int ch = nbis.read(1000);
        assertEquals('A', ch);

        // Without the fix, PtyInputStream loops until timeout and returns READ_EXPIRED (-2).
        // With the fix, it detects instant EOF and returns -1.
        int eof = nbis.read(1000);
        assertEquals(-1, eof, "Expected EOF (-1) after pipe closed");
    }

    /**
     * Tests that PtyInputStream.read() returns EOF when the stream has been
     * closed externally (e.g., by pumpIn() after detecting EOF on the user's
     * input stream). This is the fix for #2077.
     */
    @Test
    @Timeout(10)
    void readReturnsEofAfterClose() throws Exception {
        PipedInputStream pipedIn = new PipedInputStream();
        // Keep the pipe open so the timing heuristic alone won't detect EOF
        PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);

        NonBlockingInputStream nbis = createPtyInputStream(pipedIn);

        // Simulate what pumpIn() does when it detects EOF: close the PtyInputStream
        nbis.close();

        // Without the closed-flag check, this would block on the pipe read forever.
        // With the fix, it returns EOF (-1) immediately.
        int result = nbis.read(1000);
        assertEquals(-1, result, "Expected EOF (-1) after stream closed");

        pipedOut.close();
    }

    private NonBlockingInputStream createPtyInputStream(InputStream slaveInput) throws Exception {
        AbstractPty pty = new AbstractPty(null, null) {
            @Override
            protected void doSetAttr(Attributes attr) {}

            @Override
            protected InputStream doGetSlaveInput() {
                return slaveInput;
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
                return new Size(80, 24);
            }

            @Override
            public void setSize(Size size) {}

            @Override
            public void close() {}
        };

        InputStream input = pty.getSlaveInput();
        assertTrue(input instanceof NonBlockingInputStream);
        return (NonBlockingInputStream) input;
    }
}

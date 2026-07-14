/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class LineReaderEofTest {

    @Test
    @Timeout(10)
    void eofWithExecProvider() throws Exception {
        checkEof(TerminalBuilder.PROP_PROVIDER_EXEC);
    }

    @Test
    @Timeout(10)
    void eofWithDefaultProvider() throws Exception {
        checkEof(null);
    }

    /**
     * Tests that piped input is correctly read even when the input stream
     * is already at EOF before readLine() is called. This simulates the
     * scenario of {@code printf 'cmd1\ncmd2\n' | ssh host}.
     * See https://github.com/jline/jline3/issues/2054
     */
    @Test
    @Timeout(10)
    void pipedInputReadBeforeEof() throws Exception {
        checkPipedInput(null);
    }

    @Test
    @Timeout(10)
    void pipedInputReadBeforeEofWithExecProvider() throws Exception {
        checkPipedInput(TerminalBuilder.PROP_PROVIDER_EXEC);
    }

    private void checkPipedInput(String providerType) throws Exception {
        // Simulate piped input: all data written and stream closed before terminal reads.
        // The CountDownLatch fires when the pump's read() returns -1, which is after
        // all processInputBytes() calls — so all data is already in the buffer.
        CountDownLatch inputEof = new CountDownLatch(1);
        InputStream in = new ByteArrayInputStream("command1\ncommand2\n".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public int read(byte[] b, int off, int len) {
                int result = super.read(b, off, len);
                if (result == -1) {
                    inputEof.countDown();
                }
                return result;
            }
        };
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        TerminalBuilder builder = TerminalBuilder.builder().streams(in, out);
        if (providerType != null) {
            builder.providers(providerType);
        }

        Terminal terminal;
        try {
            terminal = builder.build();
        } catch (Exception e) {
            if (providerType == null) {
                throw e;
            }
            assumeTrue(false, "Provider '" + providerType + "' not available: " + e.getMessage());
            return;
        }

        try {
            // Wait for the pump thread to observe EOF on the input stream,
            // guaranteeing all data has been written to the terminal's buffer.
            assertTrue(inputEof.await(5, TimeUnit.SECONDS), "Pump did not reach EOF within timeout");

            LineReader lr = LineReaderBuilder.builder().terminal(terminal).build();
            assertEquals("command1", lr.readLine());
            assertEquals("command2", lr.readLine());
            assertThrows(EndOfFileException.class, () -> lr.readLine());
        } finally {
            terminal.close();
        }
    }

    /**
     * Tests that an empty input stream causes readLine() to throw
     * EndOfFileException rather than blocking forever.
     * See https://github.com/jline/jline3/issues/2077
     */
    @Test
    @Timeout(10)
    void emptyInputStreamThrowsEof() throws Exception {
        checkEmptyInput(null);
    }

    @Test
    @Timeout(10)
    void emptyInputStreamThrowsEofWithExecProvider() throws Exception {
        checkEmptyInput(TerminalBuilder.PROP_PROVIDER_EXEC);
    }

    private void checkEmptyInput(String providerType) throws Exception {
        InputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        TerminalBuilder builder = TerminalBuilder.builder().streams(in, out);
        if (providerType != null) {
            builder.providers(providerType);
        }

        Terminal terminal;
        try {
            terminal = builder.build();
        } catch (Exception e) {
            if (providerType == null) {
                throw e;
            }
            assumeTrue(false, "Provider '" + providerType + "' not available: " + e.getMessage());
            return;
        }

        try {
            LineReader lr = LineReaderBuilder.builder().terminal(terminal).build();
            // Give pumpIn thread time to detect EOF on the empty stream
            Thread.sleep(200);
            assertThrows(EndOfFileException.class, () -> lr.readLine());
        } finally {
            terminal.close();
        }
    }

    private void checkEof(String providerType) throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("hello\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        TerminalBuilder builder = TerminalBuilder.builder().streams(in, out);
        if (providerType != null) {
            builder.providers(providerType);
        }

        Terminal terminal;
        try {
            terminal = builder.build();
        } catch (Exception e) {
            if (providerType == null) {
                throw e;
            }
            assumeTrue(false, "Provider '" + providerType + "' not available: " + e.getMessage());
            return;
        }

        try {
            LineReader lr = LineReaderBuilder.builder().terminal(terminal).build();
            assertEquals("hello", lr.readLine());
            outIn.close();
            // readLine() will block until the pump detects the closed pipe and signals EOF
            assertThrows(EndOfFileException.class, () -> lr.readLine());
        } finally {
            terminal.close();
        }
    }
}

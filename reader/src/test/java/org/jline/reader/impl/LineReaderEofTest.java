/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        // Simulate piped input: all data written and stream closed before terminal reads
        ByteArrayInputStream in = new ByteArrayInputStream("command1\ncommand2\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        TerminalBuilder builder = TerminalBuilder.builder().streams(in, out);
        if (providerType != null) {
            builder.providers(providerType);
        }

        Terminal terminal;
        try {
            terminal = builder.build();
        } catch (Exception e) {
            assumeTrue(false, "Provider '" + providerType + "' not available: " + e.getMessage());
            return;
        }

        try (terminal) {
            // Wait for the pump thread to consume all piped input.
            // Poll available bytes instead of Thread.sleep() to avoid brittle timing.
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (terminal.input().available() == 0 && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }

            LineReader lr = LineReaderBuilder.builder().terminal(terminal).build();
            assertEquals("command1", lr.readLine());
            assertEquals("command2", lr.readLine());
            assertThrows(EndOfFileException.class, () -> lr.readLine());
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
            assumeTrue(false, "Provider '" + providerType + "' not available: " + e.getMessage());
            return;
        }

        try (terminal) {
            LineReader lr = LineReaderBuilder.builder().terminal(terminal).build();
            assertEquals("hello", lr.readLine());
            outIn.close();
            // readLine() will block until the pump detects the closed pipe and signals EOF
            assertThrows(EndOfFileException.class, () -> lr.readLine());
        }
    }
}

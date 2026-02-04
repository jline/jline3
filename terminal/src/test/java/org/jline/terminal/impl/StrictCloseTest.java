/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jline.utils.ClosedException;
import org.jline.utils.NonBlockingInputStream;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test that verifies strict close mode behavior (opt-in in JLine 3.x).
 * <p>
 * In JLine 3.x, soft mode is enabled by default for backward compatibility.
 * To enable strict mode (throwing ClosedException on access to closed streams),
 * set the system property {@code jline.terminal.strictClose=true}.
 * </p>
 * <p>
 * These tests are disabled by default because they require strict mode to be enabled.
 * To run these tests, use: {@code mvn test -Dtest=StrictCloseTest -Djline.terminal.strictClose=true}
 * </p>
 */
public class StrictCloseTest {

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=true for strict close mode")
    public void testHeldReaderReferenceThrowsInStrictMode() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

        // Get a reference to the reader before closing
        NonBlockingReader reader = terminal.reader();

        // Close the terminal
        terminal.close();

        // In strict mode, the held reference should throw ClosedException
        assertThrows(
                ClosedException.class,
                () -> reader.read(100),
                "Held reader reference should throw ClosedException in strict mode");
    }

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=true for strict close mode")
    public void testHeldInputStreamReferenceThrowsInStrictMode() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

        // Get a reference to the input stream before closing
        NonBlockingInputStream inputStream = (NonBlockingInputStream) terminal.input();

        // Close the terminal
        terminal.close();

        // In strict mode, the held reference should throw ClosedException
        assertThrows(
                ClosedException.class,
                () -> inputStream.read(100),
                "Held input stream reference should throw ClosedException in strict mode");
    }

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=true for strict close mode")
    public void testHeldExternalTerminalReaderThrowsInStrictMode() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ExternalTerminal terminal = new ExternalTerminal("test", "ansi", input, output, StandardCharsets.UTF_8);

        // Get a reference to the reader before closing
        NonBlockingReader reader = terminal.reader();

        // Close the terminal
        terminal.close();

        // In strict mode, the held reference should throw ClosedException
        assertThrows(
                ClosedException.class,
                () -> reader.read(100),
                "Held reader reference should throw ClosedException in strict mode");
    }
}


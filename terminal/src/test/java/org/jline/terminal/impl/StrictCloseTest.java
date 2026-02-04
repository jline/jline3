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
 * Test that verifies strict close mode behavior when jline.terminal.strictClose=true.
 * <p>
 * When strict mode is enabled, accessing closed streams throws {@code ClosedException}
 * instead of just logging a warning.
 * </p>
 * <p>
 * <b>Note:</b> These tests require the system property {@code jline.terminal.strictClose=true}
 * to be set when the JVM starts, as the property is read at class loading time.
 * To run these tests:
 * <pre>
 * mvn test -Dtest=StrictCloseTest -Djline.terminal.strictClose=true
 * </pre>
 * </p>
 * <p>
 * These tests are disabled by default (using {@code @Disabled}) and should be run manually
 * with the system property set.
 * </p>
 */
public class StrictCloseTest {

    // Note: We cannot set the system property in @BeforeEach because the STRICT_CLOSE
    // field in NonBlockingReader/NonBlockingInputStream is static final and evaluated
    // at class loading time, before any test methods run.

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=true to be set when JVM starts")
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
    @Disabled("Requires -Djline.terminal.strictClose=true to be set when JVM starts")
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
    @Disabled("Requires -Djline.terminal.strictClose=true to be set when JVM starts")
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


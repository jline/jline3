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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Test that verifies held references to terminal streams behave correctly after terminal closure
 * in soft close mode (backward compatibility mode).
 * <p>
 * In JLine 4.x, the default behavior is "strict close": accessing streams after terminal closure
 * throws {@code ClosedException}. These tests verify the "soft close" mode which can be enabled
 * by setting the system property {@code jline.terminal.strictClose=false}.
 * </p>
 * <p>
 * <b>Note:</b> These tests require {@code -Djline.terminal.strictClose=false} to run properly.
 * </p>
 */
public class HeldStreamReferenceTest {

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=false for soft close mode")
    public void testHeldWriterReferenceLogsWarningAfterClose() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

        // Get a reference to the writer before closing
        PrintWriter writer = terminal.writer();

        // Close the terminal
        terminal.close();

        // In soft close mode (default), the held reference should log a warning but not throw
        assertDoesNotThrow(
                () -> writer.println("test"), "Held writer reference should not throw in soft close mode (default)");
    }

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=false for soft close mode")
    public void testHeldReaderReferenceLogsWarningAfterClose() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

        // Get a reference to the reader before closing
        NonBlockingReader reader = terminal.reader();

        // Close the terminal
        terminal.close();

        // In soft close mode (default), the held reference should log a warning but not throw
        assertDoesNotThrow(
                () -> reader.read(100), "Held reader reference should not throw in soft close mode (default)");
    }

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=false for soft close mode")
    public void testHeldInputStreamReferenceLogsWarningAfterClose() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

        // Get a reference to the input stream before closing
        InputStream inputStream = terminal.input();

        // Close the terminal
        terminal.close();

        // In soft close mode (default), the held reference should log a warning but not throw
        assertDoesNotThrow(
                () -> inputStream.read(), "Held input stream reference should not throw in soft close mode (default)");
    }

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=false for soft close mode")
    public void testHeldOutputStreamReferenceLogsWarningAfterClose() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

        // Get a reference to the output stream before closing
        OutputStream outputStream = terminal.output();

        // Close the terminal
        terminal.close();

        // In soft close mode (default), the held reference should log a warning but not throw
        assertDoesNotThrow(
                () -> outputStream.write(65),
                "Held output stream reference should not throw in soft close mode (default)");
    }

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=false for soft close mode")
    public void testHeldExternalTerminalWriterReferenceLogsWarningAfterClose() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ExternalTerminal terminal = new ExternalTerminal("test", "ansi", input, output, StandardCharsets.UTF_8);

        // Get a reference to the writer before closing
        PrintWriter writer = terminal.writer();

        // Close the terminal
        terminal.close();

        // In soft close mode (default), the held reference should log a warning but not throw
        assertDoesNotThrow(
                () -> writer.println("test"), "Held writer reference should not throw in soft close mode (default)");
    }

    @Test
    @Disabled("Requires -Djline.terminal.strictClose=false for soft close mode")
    public void testHeldExternalTerminalReaderReferenceLogsWarningAfterClose() throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        ExternalTerminal terminal = new ExternalTerminal("test", "ansi", input, output, StandardCharsets.UTF_8);

        // Get a reference to the reader before closing
        NonBlockingReader reader = terminal.reader();

        // Close the terminal
        terminal.close();

        // In soft close mode (default), the held reference should log a warning but not throw
        assertDoesNotThrow(
                () -> reader.read(100), "Held reader reference should not throw in soft close mode (default)");
    }
}

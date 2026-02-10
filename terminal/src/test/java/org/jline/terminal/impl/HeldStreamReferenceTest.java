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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Test that verifies held references to terminal streams behave correctly after terminal closure
 * in warn mode (backward compatibility mode).
 * <p>
 * In JLine 4.x, the default behavior is "strict" mode: accessing streams after terminal closure
 * throws {@code ClosedException}. These tests verify the "warn" mode which can be enabled
 * by setting the system property {@code jline.terminal.closeMode=warn}.
 * </p>
 * <p>
 * Each test sets the system property before creating the terminal to enable warn mode,
 * then restores the previous value after the test completes.
 * </p>
 */
public class HeldStreamReferenceTest {

    @Test
    public void testHeldWriterReferenceLogsWarningAfterClose() throws IOException {
        // Save the previous value and set warn mode before creating the terminal
        String previousValue = System.getProperty("jline.terminal.closeMode");
        System.setProperty("jline.terminal.closeMode", "warn");
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

            // Get a reference to the writer before closing
            PrintWriter writer = terminal.writer();

            // Close the terminal
            terminal.close();

            // In warn mode, the held reference should log a warning but not throw
            assertDoesNotThrow(() -> writer.println("test"), "Held writer reference should not throw in warn mode");
        } finally {
            // Restore previous value
            if (previousValue != null) {
                System.setProperty("jline.terminal.closeMode", previousValue);
            } else {
                System.clearProperty("jline.terminal.closeMode");
            }
        }
    }

    @Test
    public void testHeldReaderReferenceLogsWarningAfterClose() throws IOException {
        // Save the previous value and set soft close mode before creating the terminal
        String previousValue = System.getProperty("jline.terminal.closeMode");
        System.setProperty("jline.terminal.closeMode", "warn");
        try {
            ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

            // Get a reference to the reader before closing
            NonBlockingReader reader = terminal.reader();

            // Close the terminal
            terminal.close();

            // In soft close mode, the held reference should log a warning but not throw
            assertDoesNotThrow(() -> reader.read(100), "Held reader reference should not throw in soft close mode");
        } finally {
            // Restore previous value
            if (previousValue != null) {
                System.setProperty("jline.terminal.closeMode", previousValue);
            } else {
                System.clearProperty("jline.terminal.closeMode");
            }
        }
    }

    @Test
    public void testHeldInputStreamReferenceLogsWarningAfterClose() throws IOException {
        // Save the previous value and set soft close mode before creating the terminal
        String previousValue = System.getProperty("jline.terminal.closeMode");
        System.setProperty("jline.terminal.closeMode", "warn");
        try {
            ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

            // Get a reference to the input stream before closing
            InputStream inputStream = terminal.input();

            // Close the terminal
            terminal.close();

            // In soft close mode, the held reference should log a warning but not throw
            assertDoesNotThrow(
                    () -> inputStream.read(), "Held input stream reference should not throw in soft close mode");
        } finally {
            // Restore previous value
            if (previousValue != null) {
                System.setProperty("jline.terminal.closeMode", previousValue);
            } else {
                System.clearProperty("jline.terminal.closeMode");
            }
        }
    }

    @Test
    public void testHeldOutputStreamReferenceLogsWarningAfterClose() throws IOException {
        // Save the previous value and set soft close mode before creating the terminal
        String previousValue = System.getProperty("jline.terminal.closeMode");
        System.setProperty("jline.terminal.closeMode", "warn");
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            DumbTerminal terminal = new DumbTerminal("test", "dumb", input, output, StandardCharsets.UTF_8);

            // Get a reference to the output stream before closing
            OutputStream outputStream = terminal.output();

            // Close the terminal
            terminal.close();

            // In soft close mode, the held reference should log a warning but not throw
            assertDoesNotThrow(
                    () -> outputStream.write(65), "Held output stream reference should not throw in soft close mode");
        } finally {
            // Restore previous value
            if (previousValue != null) {
                System.setProperty("jline.terminal.closeMode", previousValue);
            } else {
                System.clearProperty("jline.terminal.closeMode");
            }
        }
    }

    @Test
    public void testHeldExternalTerminalWriterReferenceLogsWarningAfterClose() throws IOException {
        // Save the previous value and set soft close mode before creating the terminal
        String previousValue = System.getProperty("jline.terminal.closeMode");
        System.setProperty("jline.terminal.closeMode", "warn");
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            ExternalTerminal terminal = new ExternalTerminal("test", "ansi", input, output, StandardCharsets.UTF_8);

            // Get a reference to the writer before closing
            PrintWriter writer = terminal.writer();

            // Close the terminal
            terminal.close();

            // In soft close mode, the held reference should log a warning but not throw
            assertDoesNotThrow(
                    () -> writer.println("test"), "Held writer reference should not throw in soft close mode");
        } finally {
            // Restore previous value
            if (previousValue != null) {
                System.setProperty("jline.terminal.closeMode", previousValue);
            } else {
                System.clearProperty("jline.terminal.closeMode");
            }
        }
    }

    @Test
    public void testHeldExternalTerminalReaderReferenceLogsWarningAfterClose() throws IOException {
        // Save the previous value and set soft close mode before creating the terminal
        String previousValue = System.getProperty("jline.terminal.closeMode");
        System.setProperty("jline.terminal.closeMode", "warn");
        try {
            ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            ExternalTerminal terminal = new ExternalTerminal("test", "ansi", input, output, StandardCharsets.UTF_8);

            // Get a reference to the reader before closing
            NonBlockingReader reader = terminal.reader();

            // Close the terminal
            terminal.close();

            // In soft close mode, the held reference should log a warning but not throw
            assertDoesNotThrow(() -> reader.read(100), "Held reader reference should not throw in soft close mode");
        } finally {
            // Restore previous value
            if (previousValue != null) {
                System.setProperty("jline.terminal.closeMode", previousValue);
            } else {
                System.clearProperty("jline.terminal.closeMode");
            }
        }
    }
}

/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for terminal functionality with multiple encodings.
 */
public class MultiEncodingTerminalTest {

    /**
     * Test reading from stdin with a specific encoding.
     */
    @Test
    public void testReadWithEncoding() throws IOException {
        // Create input with ISO-8859-1 encoded text
        String testString = "café"; // é is 0xE9 in ISO-8859-1
        byte[] isoBytes = testString.getBytes(StandardCharsets.ISO_8859_1);

        ByteArrayInputStream in = new ByteArrayInputStream(isoBytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Create terminal with ISO-8859-1 for stdin
        Terminal terminal = new DumbTerminal(
                null,
                null,
                "test",
                "dumb",
                in,
                out,
                StandardCharsets.UTF_8,
                StandardCharsets.ISO_8859_1,
                StandardCharsets.UTF_8,
                Terminal.SignalHandler.SIG_DFL);

        // Read characters from the terminal
        NonBlockingReader reader = terminal.reader();
        StringBuilder result = new StringBuilder();
        int c;
        int timeoutCount = 0;
        while (timeoutCount < 1000) { // Allow up to 1000 timeouts before giving up
            c = reader.read(1);
            if (c == -1) { // EOF
                break;
            } else if (c == -2) { // READ_EXPIRED (timeout)
                timeoutCount++;
                continue; // Keep trying
            } else if (c >= 0) { // Valid character
                result.append((char) c);
            }
        }

        // Verify the text was correctly decoded using ISO-8859-1
        assertEquals(testString, result.toString());
    }

    /**
     * Test writing to stdout with a specific encoding.
     */
    @Test
    public void testWriteWithEncoding() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Create terminal with UTF-16 for stdout
        Terminal terminal = new DumbTerminal(
                null,
                null,
                "test",
                "dumb",
                in,
                out,
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_8,
                StandardCharsets.UTF_16,
                Terminal.SignalHandler.SIG_DFL);

        // Write a string with non-ASCII characters
        String testString = "こんにちは"; // Hello in Japanese
        PrintWriter writer = terminal.writer();
        writer.write(testString);
        writer.flush();

        // Verify the output was encoded using UTF-16
        byte[] expectedBytes = testString.getBytes(StandardCharsets.UTF_16);
        byte[] actualBytes = out.toByteArray();

        // UTF-16 includes a BOM (Byte Order Mark) at the beginning
        // We need to compare the actual content
        String expected = new String(expectedBytes, StandardCharsets.UTF_16);
        String actual = new String(actualBytes, StandardCharsets.UTF_16);

        assertEquals(expected, actual);
    }

    /**
     * Test that different encodings can be used simultaneously.
     */
    @Test
    public void testMultipleEncodings() throws IOException {
        // Create input with ISO-8859-1 encoded text
        String inputString = "café"; // é is 0xE9 in ISO-8859-1
        byte[] isoBytes = inputString.getBytes(StandardCharsets.ISO_8859_1);

        ByteArrayInputStream in = new ByteArrayInputStream(isoBytes);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();

        // Create terminal with different encodings for each stream
        // DumbTerminal doesn't have a constructor that takes an error stream
        // So we'll use a regular DumbTerminal and test stdin/stdout only
        DumbTerminal terminal = new DumbTerminal(
                null,
                null,
                "test",
                "dumb",
                in,
                out,
                StandardCharsets.UTF_8,
                StandardCharsets.ISO_8859_1,
                StandardCharsets.UTF_16,
                Terminal.SignalHandler.SIG_DFL);

        // Read from stdin (ISO-8859-1)
        NonBlockingReader reader = terminal.reader();
        StringBuilder result = new StringBuilder();
        int c;
        int timeoutCount = 0;
        while (timeoutCount < 1000) { // Allow up to 1000 timeouts before giving up
            c = reader.read(1);
            if (c == -1) { // EOF
                break;
            } else if (c == -2) { // READ_EXPIRED (timeout)
                timeoutCount++;
                continue; // Keep trying
            } else if (c >= 0) { // Valid character
                result.append((char) c);
            }
        }

        // Write to stdout (UTF-16)
        String outputString = "こんにちは"; // Hello in Japanese
        terminal.writer().write(outputString);
        terminal.writer().flush();

        // Verify stdin was correctly decoded using ISO-8859-1
        assertEquals(inputString, result.toString());

        // Verify stdout was correctly encoded using UTF-16
        String outputResult = new String(out.toByteArray(), StandardCharsets.UTF_16);
        assertEquals(outputString, outputResult);
    }
}

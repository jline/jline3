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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for terminal encoding functionality.
 */
@SuppressWarnings("missing-explicit-ctor")
public class EncodingTest {

    /**
     * Test that the default encoding methods return the main encoding when no specific encodings are set.
     */
    @Test
    public void testDefaultEncodings() throws IOException {
        Terminal terminal = createTestTerminal(StandardCharsets.UTF_8, null, null, null);

        assertEquals(StandardCharsets.UTF_8, terminal.encoding());
        assertEquals(StandardCharsets.UTF_8, terminal.inputEncoding());
        assertEquals(StandardCharsets.UTF_8, terminal.outputEncoding());
    }

    /**
     * Test that specific encodings are used when set.
     */
    @Test
    public void testSpecificEncodings() throws IOException {
        Terminal terminal = createTestTerminal(
                StandardCharsets.UTF_8,
                StandardCharsets.ISO_8859_1,
                StandardCharsets.UTF_16,
                StandardCharsets.US_ASCII);

        assertEquals(StandardCharsets.UTF_8, terminal.encoding());
        assertEquals(StandardCharsets.ISO_8859_1, terminal.inputEncoding());
        // Output encoding should use stdout encoding since this is not bound to stderr
        assertEquals(StandardCharsets.UTF_16, terminal.outputEncoding());
    }

    /**
     * Test that output encoding uses stderr encoding when terminal is bound to stderr.
     */
    @Test
    public void testStderrOutputEncoding() throws IOException {
        Terminal terminal = createTestTerminalWithSystemStream(
                StandardCharsets.UTF_8,
                StandardCharsets.ISO_8859_1,
                StandardCharsets.UTF_16,
                StandardCharsets.US_ASCII,
                org.jline.terminal.spi.SystemStream.Error);

        assertEquals(StandardCharsets.UTF_8, terminal.encoding());
        assertEquals(StandardCharsets.ISO_8859_1, terminal.inputEncoding());
        // Output encoding should use stderr encoding since this is bound to stderr
        assertEquals(StandardCharsets.US_ASCII, terminal.outputEncoding());
    }

    /**
     * Test that output encoding uses stdout encoding when terminal is bound to stdout.
     */
    @Test
    public void testStdoutOutputEncoding() throws IOException {
        Terminal terminal = createTestTerminalWithSystemStream(
                StandardCharsets.UTF_8,
                StandardCharsets.ISO_8859_1,
                StandardCharsets.UTF_16,
                StandardCharsets.US_ASCII,
                org.jline.terminal.spi.SystemStream.Output);

        assertEquals(StandardCharsets.UTF_8, terminal.encoding());
        assertEquals(StandardCharsets.ISO_8859_1, terminal.inputEncoding());
        // Output encoding should use stdout encoding since this is bound to stdout
        assertEquals(StandardCharsets.UTF_16, terminal.outputEncoding());
    }

    /**
     * Test that the TerminalBuilder correctly sets encodings.
     */
    @Test
    public void testTerminalBuilderEncodings() throws IOException {
        Terminal terminal = TerminalBuilder.builder()
                .dumb(true)
                .encoding(StandardCharsets.UTF_8)
                .stdinEncoding(StandardCharsets.ISO_8859_1)
                .stdoutEncoding(StandardCharsets.UTF_16)
                .stderrEncoding(StandardCharsets.US_ASCII)
                .build();

        assertEquals(StandardCharsets.UTF_8, terminal.encoding());
        assertEquals(StandardCharsets.ISO_8859_1, terminal.inputEncoding());
        // Output encoding should use stdout encoding since this is not bound to stderr
        assertEquals(StandardCharsets.UTF_16, terminal.outputEncoding());
    }

    /**
     * Test that JLine system properties are correctly used for encodings.
     */
    @Test
    public void testJLineSystemPropertyEncodings() throws IOException {
        String oldEncoding = System.getProperty(TerminalBuilder.PROP_ENCODING);
        String oldStdinEncoding = System.getProperty(TerminalBuilder.PROP_STDIN_ENCODING);
        String oldStdoutEncoding = System.getProperty(TerminalBuilder.PROP_STDOUT_ENCODING);
        String oldStderrEncoding = System.getProperty(TerminalBuilder.PROP_STDERR_ENCODING);

        try {
            System.setProperty(TerminalBuilder.PROP_ENCODING, "UTF-8");
            System.setProperty(TerminalBuilder.PROP_STDIN_ENCODING, "ISO-8859-1");
            System.setProperty(TerminalBuilder.PROP_STDOUT_ENCODING, "UTF-16");
            System.setProperty(TerminalBuilder.PROP_STDERR_ENCODING, "US-ASCII");

            Terminal terminal = TerminalBuilder.builder().dumb(true).build();

            assertEquals(StandardCharsets.UTF_8, terminal.encoding());
            assertEquals(StandardCharsets.ISO_8859_1, terminal.inputEncoding());
            // Output encoding should use stdout encoding since this is not bound to stderr
            assertEquals(StandardCharsets.UTF_16, terminal.outputEncoding());
        } finally {
            // Restore original system properties
            if (oldEncoding != null) {
                System.setProperty(TerminalBuilder.PROP_ENCODING, oldEncoding);
            } else {
                System.clearProperty(TerminalBuilder.PROP_ENCODING);
            }

            if (oldStdinEncoding != null) {
                System.setProperty(TerminalBuilder.PROP_STDIN_ENCODING, oldStdinEncoding);
            } else {
                System.clearProperty(TerminalBuilder.PROP_STDIN_ENCODING);
            }

            if (oldStdoutEncoding != null) {
                System.setProperty(TerminalBuilder.PROP_STDOUT_ENCODING, oldStdoutEncoding);
            } else {
                System.clearProperty(TerminalBuilder.PROP_STDOUT_ENCODING);
            }

            if (oldStderrEncoding != null) {
                System.setProperty(TerminalBuilder.PROP_STDERR_ENCODING, oldStderrEncoding);
            } else {
                System.clearProperty(TerminalBuilder.PROP_STDERR_ENCODING);
            }
        }
    }

    /**
     * Test that standard Java system properties are correctly used for encodings.
     */
    @Test
    public void testStandardJavaSystemPropertyEncodings() throws IOException {
        String oldStdinEncoding = System.getProperty("stdin.encoding");
        String oldStdoutEncoding = System.getProperty("stdout.encoding");
        String oldStderrEncoding = System.getProperty("stderr.encoding");

        try {
            System.setProperty("stdin.encoding", "ISO-8859-1");
            System.setProperty("stdout.encoding", "UTF-16");
            System.setProperty("stderr.encoding", "US-ASCII");

            Terminal terminal = TerminalBuilder.builder().dumb(true).build();

            assertEquals(StandardCharsets.ISO_8859_1, terminal.inputEncoding());
            // Output encoding should use stdout encoding since this is not bound to stderr
            assertEquals(StandardCharsets.UTF_16, terminal.outputEncoding());
        } finally {
            // Restore original system properties
            if (oldStdinEncoding != null) {
                System.setProperty("stdin.encoding", oldStdinEncoding);
            } else {
                System.clearProperty("stdin.encoding");
            }

            if (oldStdoutEncoding != null) {
                System.setProperty("stdout.encoding", oldStdoutEncoding);
            } else {
                System.clearProperty("stdout.encoding");
            }

            if (oldStderrEncoding != null) {
                System.setProperty("stderr.encoding", oldStderrEncoding);
            } else {
                System.clearProperty("stderr.encoding");
            }
        }
    }

    /**
     * Test that JLine system properties take precedence over standard Java system properties.
     */
    @Test
    public void testSystemPropertyPrecedence() throws IOException {
        String oldJLineStdinEncoding = System.getProperty(TerminalBuilder.PROP_STDIN_ENCODING);
        String oldJLineStdoutEncoding = System.getProperty(TerminalBuilder.PROP_STDOUT_ENCODING);
        String oldJLineStderrEncoding = System.getProperty(TerminalBuilder.PROP_STDERR_ENCODING);
        String oldStdinEncoding = System.getProperty("stdin.encoding");
        String oldStdoutEncoding = System.getProperty("stdout.encoding");
        String oldStderrEncoding = System.getProperty("stderr.encoding");

        try {
            // Set both JLine and standard properties with different values
            System.setProperty(TerminalBuilder.PROP_STDIN_ENCODING, "ISO-8859-1");
            System.setProperty(TerminalBuilder.PROP_STDOUT_ENCODING, "UTF-16");
            System.setProperty(TerminalBuilder.PROP_STDERR_ENCODING, "US-ASCII");
            System.setProperty("stdin.encoding", "UTF-8");
            System.setProperty("stdout.encoding", "UTF-8");
            System.setProperty("stderr.encoding", "UTF-8");

            Terminal terminal = TerminalBuilder.builder().dumb(true).build();

            // JLine properties should take precedence
            assertEquals(StandardCharsets.ISO_8859_1, terminal.inputEncoding());
            // Output encoding should use stdout encoding since this is not bound to stderr
            assertEquals(StandardCharsets.UTF_16, terminal.outputEncoding());
        } finally {
            // Restore original system properties
            if (oldJLineStdinEncoding != null) {
                System.setProperty(TerminalBuilder.PROP_STDIN_ENCODING, oldJLineStdinEncoding);
            } else {
                System.clearProperty(TerminalBuilder.PROP_STDIN_ENCODING);
            }

            if (oldJLineStdoutEncoding != null) {
                System.setProperty(TerminalBuilder.PROP_STDOUT_ENCODING, oldJLineStdoutEncoding);
            } else {
                System.clearProperty(TerminalBuilder.PROP_STDOUT_ENCODING);
            }

            if (oldJLineStderrEncoding != null) {
                System.setProperty(TerminalBuilder.PROP_STDERR_ENCODING, oldJLineStderrEncoding);
            } else {
                System.clearProperty(TerminalBuilder.PROP_STDERR_ENCODING);
            }

            if (oldStdinEncoding != null) {
                System.setProperty("stdin.encoding", oldStdinEncoding);
            } else {
                System.clearProperty("stdin.encoding");
            }

            if (oldStdoutEncoding != null) {
                System.setProperty("stdout.encoding", oldStdoutEncoding);
            } else {
                System.clearProperty("stdout.encoding");
            }

            if (oldStderrEncoding != null) {
                System.setProperty("stderr.encoding", oldStderrEncoding);
            } else {
                System.clearProperty("stderr.encoding");
            }
        }
    }

    /**
     * Test that the compute methods in TerminalBuilder work correctly.
     */
    @Test
    public void testComputeEncodings() {
        TerminalBuilder builder = TerminalBuilder.builder()
                .encoding(StandardCharsets.UTF_8)
                .stdinEncoding(StandardCharsets.ISO_8859_1)
                .stdoutEncoding(StandardCharsets.UTF_16)
                .stderrEncoding(StandardCharsets.US_ASCII);

        assertEquals(StandardCharsets.UTF_8, builder.computeEncoding());
        assertEquals(StandardCharsets.ISO_8859_1, builder.computeStdinEncoding());
        assertEquals(StandardCharsets.UTF_16, builder.computeStdoutEncoding());
        assertEquals(StandardCharsets.US_ASCII, builder.computeStderrEncoding());
    }

    /**
     * Test that the compute methods in TerminalBuilder fall back correctly.
     */
    @Test
    public void testComputeEncodingsFallback() {
        TerminalBuilder builder = TerminalBuilder.builder().encoding(StandardCharsets.UTF_8);

        String stdin = System.clearProperty("stdin.encoding");
        String stdout = System.clearProperty("stdout.encoding");
        String stderr = System.clearProperty("stderr.encoding");
        try {
            assertEquals(StandardCharsets.UTF_8, builder.computeEncoding());
            assertEquals(StandardCharsets.UTF_8, builder.computeStdinEncoding());
            assertEquals(StandardCharsets.UTF_8, builder.computeStdoutEncoding());
            assertEquals(StandardCharsets.UTF_8, builder.computeStderrEncoding());
        } finally {
            if (stdin != null) {
                System.setProperty("stdin.encoding", stdin);
            }
            if (stdout != null) {
                System.setProperty("stdout.encoding", stdout);
            }
            if (stderr != null) {
                System.setProperty("stderr.encoding", stderr);
            }
        }
    }

    /**
     * Helper method to create a test terminal with specific encodings.
     */
    private Terminal createTestTerminal(
            Charset encoding, Charset stdinEncoding, Charset stdoutEncoding, Charset stderrEncoding)
            throws IOException {

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        return new DumbTerminal(
                null,
                null,
                "test",
                "dumb",
                in,
                out,
                encoding,
                stdinEncoding != null ? stdinEncoding : encoding,
                stdoutEncoding != null ? stdoutEncoding : encoding,
                Terminal.SignalHandler.SIG_DFL);
    }

    /**
     * Helper method to create a test terminal with specific encodings and SystemStream.
     */
    private Terminal createTestTerminalWithSystemStream(
            Charset encoding,
            Charset stdinEncoding,
            Charset stdoutEncoding,
            Charset stderrEncoding,
            org.jline.terminal.spi.SystemStream systemStream)
            throws IOException {

        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Use the appropriate output encoding based on the system stream
        Charset outputEncoding =
                systemStream == org.jline.terminal.spi.SystemStream.Error ? stderrEncoding : stdoutEncoding;

        return new DumbTerminal(
                null,
                systemStream,
                "test",
                "dumb",
                in,
                out,
                encoding,
                stdinEncoding != null ? stdinEncoding : encoding,
                outputEncoding != null ? outputEncoding : encoding,
                Terminal.SignalHandler.SIG_DFL);
    }
}

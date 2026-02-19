/*
 * Copyright (c) the original author(s).
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

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DoubleSizeCharacters utility class.
 */
public class DoubleSizeCharactersTest {

    @Test
    public void testDoubleSizeSupport() {
        // Test terminals that should support double-size characters
        assertTrue(DoubleSizeCharacters.isDoubleSizeSupported(createMockTerminal("xterm")));
        assertTrue(DoubleSizeCharacters.isDoubleSizeSupported(createMockTerminal("xterm-256color")));
        assertTrue(DoubleSizeCharacters.isDoubleSizeSupported(createMockTerminal("vt100")));
        assertTrue(DoubleSizeCharacters.isDoubleSizeSupported(createMockTerminal("ansi")));
        assertTrue(DoubleSizeCharacters.isDoubleSizeSupported(createMockTerminal("screen")));
        assertTrue(DoubleSizeCharacters.isDoubleSizeSupported(createMockTerminal("tmux")));

        // Test terminals that might not support double-size characters
        assertFalse(DoubleSizeCharacters.isDoubleSizeSupported(createMockTerminal("dumb")));
        assertFalse(DoubleSizeCharacters.isDoubleSizeSupported(createMockTerminal("unknown")));
    }

    @Test
    public void testSetMode() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = createMockTerminalWithOutput(output);

        // Test normal mode
        DoubleSizeCharacters.setMode(terminal, DoubleSizeCharacters.Mode.NORMAL);
        String result = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(result.contains("\u001b#5"), "Should contain normal size escape sequence");

        // Reset output
        output.reset();

        // Test double width mode
        DoubleSizeCharacters.setMode(terminal, DoubleSizeCharacters.Mode.DOUBLE_WIDTH);
        result = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(result.contains("\u001b#6"), "Should contain double width escape sequence");

        // Reset output
        output.reset();

        // Test double height top mode
        DoubleSizeCharacters.setMode(terminal, DoubleSizeCharacters.Mode.DOUBLE_HEIGHT_TOP);
        result = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(result.contains("\u001b#3"), "Should contain double height top escape sequence");

        // Reset output
        output.reset();

        // Test double height bottom mode
        DoubleSizeCharacters.setMode(terminal, DoubleSizeCharacters.Mode.DOUBLE_HEIGHT_BOTTOM);
        result = output.toString(StandardCharsets.UTF_8.name());
        assertTrue(result.contains("\u001b#4"), "Should contain double height bottom escape sequence");
    }

    @Test
    public void testPrintNormal() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = createMockTerminalWithOutput(output);

        DoubleSizeCharacters.printNormal(terminal, "Test text");
        String result = output.toString(StandardCharsets.UTF_8.name());

        assertTrue(result.contains("\u001b#5"), "Should set normal mode");
        assertTrue(result.contains("Test text"), "Should contain the text");
    }

    @Test
    public void testPrintDoubleWidth() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = createMockTerminalWithOutput(output);

        DoubleSizeCharacters.printDoubleWidth(terminal, "Wide text");
        String result = output.toString(StandardCharsets.UTF_8.name());

        assertTrue(result.contains("\u001b#6"), "Should set double width mode");
        assertTrue(result.contains("Wide text"), "Should contain the text");
    }

    @Test
    public void testPrintDoubleHeight() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = createMockTerminalWithOutput(output);

        DoubleSizeCharacters.printDoubleHeight(terminal, "Tall text");
        String result = output.toString(StandardCharsets.UTF_8.name());

        assertTrue(result.contains("\u001b#3"), "Should set double height top mode");
        assertTrue(result.contains("\u001b#4"), "Should set double height bottom mode");
        assertTrue(result.contains("\u001b#5"), "Should reset to normal mode");
        assertTrue(result.contains("Tall text"), "Should contain the text");
    }

    @Test
    public void testPrintBannerWithSupport() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = createMockTerminalWithOutput("xterm", output);

        DoubleSizeCharacters.printBanner(terminal, "Banner", '*');
        String result = output.toString(StandardCharsets.UTF_8.name());

        assertTrue(result.contains("Banner"), "Should contain the banner text");
        assertTrue(result.contains("*"), "Should contain the border character");
        assertTrue(result.contains("\u001b#"), "Should contain double-size escape sequences");
    }

    @Test
    public void testPrintBannerWithoutSupport() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = createMockTerminalWithOutput("dumb", output);

        DoubleSizeCharacters.printBanner(terminal, "Banner", '*');
        String result = output.toString(StandardCharsets.UTF_8.name());

        assertTrue(result.contains("Banner"), "Should contain the banner text");
        assertTrue(result.contains("*"), "Should contain the border character");
        // Should not contain double-size escape sequences for unsupported terminal
    }

    @Test
    public void testReset() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Terminal terminal = createMockTerminalWithOutput(output);

        DoubleSizeCharacters.reset(terminal);
        String result = output.toString(StandardCharsets.UTF_8.name());

        assertTrue(result.contains("\u001b#5"), "Should reset to normal mode");
    }

    /**
     * Creates a mock terminal for testing purposes.
     */
    private Terminal createMockTerminal(String type) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            return createMockTerminalWithOutput(type, output);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create mock terminal", e);
        }
    }

    /**
     * Creates a mock terminal with specified output stream.
     */
    private Terminal createMockTerminalWithOutput(ByteArrayOutputStream output) throws IOException {
        return createMockTerminalWithOutput("xterm", output);
    }

    /**
     * Creates a mock terminal with specified type and output stream.
     */
    private Terminal createMockTerminalWithOutput(String type, ByteArrayOutputStream output) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);

        return TerminalBuilder.builder().type(type).streams(input, output).build();
    }
}

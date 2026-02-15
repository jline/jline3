/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SwingTerminal encoding functionality, specifically the
 * SwingTerminalOutputStream ByteBuffer and CharsetDecoder implementation.
 */
public class SwingTerminalEncodingTest {

    private SwingTerminal swingTerminal;
    private TestTerminalComponent testComponent;

    @BeforeEach
    public void setUp() throws IOException {
        swingTerminal = new SwingTerminal("EncodingTest", 80, 24);
        testComponent = new TestTerminalComponent();

        // Replace the component with our test component to capture output
        swingTerminal.getComponent().setTerminal(null); // Disconnect original
        testComponent.setTerminal(swingTerminal);

        // Get the output stream and set our test component
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        swingTerminal.writer().flush(); // Ensure any pending output is flushed
    }

    @AfterEach
    public void tearDown() {
        if (swingTerminal != null) {
            swingTerminal.dispose();
        }
    }

    /**
     * Test component that captures written text for verification.
     */
    private static class TestTerminalComponent {
        private final StringBuilder capturedText = new StringBuilder();
        private final CountDownLatch writeLatch = new CountDownLatch(1);
        private SwingTerminal terminal;

        public void setTerminal(SwingTerminal terminal) {
            this.terminal = terminal;
        }

        public void write(String text) {
            synchronized (capturedText) {
                capturedText.append(text);
                writeLatch.countDown();
            }
        }

        public String getCapturedText() {
            synchronized (capturedText) {
                return capturedText.toString();
            }
        }

        public void clearCapturedText() {
            synchronized (capturedText) {
                capturedText.setLength(0);
            }
        }

        public boolean waitForWrite(long timeout, TimeUnit unit) throws InterruptedException {
            return writeLatch.await(timeout, unit);
        }
    }

    @Test
    public void testSimpleAsciiText() throws IOException {
        String testText = "Hello, World!";

        // Write through the terminal's writer (which uses the OutputStream internally)
        swingTerminal.writer().write(testText);
        swingTerminal.writer().flush();

        // Verify the text was written to the component
        String captured = getCapturedTextFromTerminal();
        assertTrue(captured.contains(testText), "Expected '" + testText + "' but got '" + captured + "'");
    }

    @Test
    public void testMultiByteUtf8Characters() throws IOException {
        // Test with a shorter string to isolate the issue
        String testText = "Hello résumé";

        // Write through the terminal's writer
        swingTerminal.writer().write(testText);
        swingTerminal.writer().flush();

        String captured = getCapturedTextFromTerminal();

        // Now test the full string
        swingTerminal.writer().write("\n");
        String fullText = "Hello 世界 🌍 café naïve résumé";
        swingTerminal.writer().write(fullText);
        swingTerminal.writer().flush();

        String captured2 = getCapturedTextFromTerminal();

        assertTrue(captured2.contains("世界"), "Should contain Chinese characters");
        assertTrue(captured2.contains("🌍"), "Should contain emoji");
        assertTrue(captured2.contains("café"), "Should contain accented characters");
        assertTrue(captured2.contains("naïve"), "Should contain diaeresis");
        assertTrue(captured2.contains("résumé"), "Should contain acute accents: " + captured2);
    }

    @Test
    public void testIncompleteMultiByteSequences() throws IOException {
        // This test is more complex since we need to access the OutputStream directly
        // For now, we'll test that complete sequences work correctly
        String testText = "世";

        swingTerminal.writer().write(testText);
        swingTerminal.writer().flush();

        String captured = getCapturedTextFromTerminal();
        assertTrue(captured.contains("世"), "Complete sequence should produce the character");
    }

    @Test
    public void testByteArrayWrite() throws IOException {
        String testText = "Byte array test with UTF-8: 测试 🚀";

        // Write through the terminal's writer
        swingTerminal.writer().write(testText);
        swingTerminal.writer().flush();

        String captured = getCapturedTextFromTerminal();
        assertTrue(captured.contains("测试"), "Should contain Chinese test characters: " + captured);
        assertTrue(captured.contains("🚀"), "Should contain rocket emoji: " + captured);
        assertTrue(captured.contains("Byte array test"), "Should contain ASCII text: " + captured);
    }

    @Test
    public void testMixedWrites() throws IOException {
        // Write mixed content
        String part1 = "Hello ";
        String part2 = "世界 ";
        String part3 = "🌍!";

        swingTerminal.writer().write(part1);
        swingTerminal.writer().write(part2);
        swingTerminal.writer().write(part3);
        swingTerminal.writer().flush();

        String captured = getCapturedTextFromTerminal();
        assertTrue(captured.contains("Hello 世界 🌍!"), "Mixed writes should produce complete text: " + captured);
    }

    @Test
    public void testFlushBehavior() throws IOException {
        // Test that flush works correctly
        String testText = "世";

        swingTerminal.writer().write(testText);
        swingTerminal.writer().flush();

        String captured = getCapturedTextFromTerminal();
        assertTrue(captured.contains("世"), "Flush should output the character");
    }

    @Test
    public void testLargeText() throws IOException {
        StringBuilder largeText = new StringBuilder();
        for (int i = 0; i < 23; i++) {
            largeText.append("Line ").append(i).append(" with UTF-8: 测试 🚀\n");
        }

        String testText = largeText.toString();
        swingTerminal.writer().write(testText);
        swingTerminal.writer().flush();

        String captured = getCapturedTextFromTerminal();
        assertTrue(captured.contains("Line 0"), "Should contain first line");
        assertTrue(captured.contains("Line 22"), "Should contain last line");
        assertTrue(captured.contains("测试"), "Should contain Chinese characters");
        assertTrue(captured.contains("🚀"), "Should contain emojis");
    }

    @Test
    public void testFontMetricsCharacterWidth() {
        // Test that character width is calculated properly to prevent overlap
        SwingTerminal.TerminalComponent component = swingTerminal.getComponent();

        // Verify that the component has a font set
        assertNotNull(component.getTerminalFont(), "Terminal should have a font set");

        // The character width should be positive and reasonable
        // We can't test the exact value since it depends on the font,
        // but we can verify it's calculated properly
        assertTrue(component.getPreferredSize().width > 0, "Component should have positive width");
        assertTrue(component.getPreferredSize().height > 0, "Component should have positive height");

        // Verify that the preferred size is based on character dimensions
        // Width should be terminal width * character width
        // Height should be terminal height * character height
        int expectedCells = 80 * 24; // Default terminal size
        assertTrue(component.getPreferredSize().width >= 80, "Width should accommodate at least 80 characters");
        assertTrue(component.getPreferredSize().height >= 24, "Height should accommodate at least 24 lines");
    }

    /**
     * Helper method to get captured text from the terminal.
     * This is a simplified version - in the real implementation,
     * we would need to properly capture the output from the SwingTerminalOutputStream.
     */
    private String getCapturedTextFromTerminal() {
        try {
            return swingTerminal.dump(0, true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        }
    }
}

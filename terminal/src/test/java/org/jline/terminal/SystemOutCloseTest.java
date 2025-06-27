/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for issue #1336 - System.out stops working after closing a dumb terminal.
 */
public class SystemOutCloseTest {

    @Test
    public void testSystemOutWorksAfterTerminalClose() throws IOException {
        // Capture System.out to verify output
        PrintStream originalOut = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOutput));

        try {
            // Test output before terminal creation
            System.out.println("Before terminal");

            // Create and close a dumb terminal (this should not affect System.out)
            try (Terminal terminal = TerminalBuilder.builder().dumb(true).build()) {
                System.out.println("Inside terminal");
                // Verify terminal is working
                terminal.writer().println("Terminal output");
                terminal.writer().flush();
            }

            // Test output after terminal close - this should still work
            System.out.println("After terminal");

            // Verify all output was captured
            String output = capturedOutput.toString();
            String normalizedOutput = output.replace("\r\n", "\n");

            // Check that all three lines are present
            assertTrue(normalizedOutput.contains("Before terminal"), "Should contain 'Before terminal'");
            assertTrue(normalizedOutput.contains("Inside terminal"), "Should contain 'Inside terminal'");
            assertTrue(normalizedOutput.contains("After terminal"), "Should contain 'After terminal'");

            // The exact format might vary, but all three messages should be there
            assertEquals("Before terminal\nInside terminal\nAfter terminal\n", normalizedOutput);

        } finally {
            // Restore original System.out
            System.setOut(originalOut);
        }
    }
}

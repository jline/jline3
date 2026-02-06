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
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.jline.utils.ClosedException;
import org.jline.utils.NonBlockingReader;

/**
 * Demonstration of terminal stream closure behavior.
 * This class shows how held references to terminal streams behave after terminal closure.
 *
 * Run this class to see the behavior in action.
 */
public class StreamClosureDemonstration {

    // Helper method for Java 8 compatibility (String.repeat() was added in Java 11)
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        demonstrateCorrectBehavior();
        System.out.println("\n" + repeat("=", 80) + "\n");
        demonstrateHeldReferenceBehavior();
    }

    /**
     * Demonstrates the correct behavior: accessing streams through terminal after close throws.
     */
    private static void demonstrateCorrectBehavior() {
        System.out.println("DEMONSTRATION 1: Accessing streams through terminal after close");
        System.out.println(repeat("-", 80));

        try {
            ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DumbTerminal terminal = new DumbTerminal("demo", "dumb", input, output, StandardCharsets.UTF_8);

            // Use the terminal normally
            terminal.writer().println("Hello, World!");
            System.out.println("✓ Writing through terminal.writer() works before close");

            // Close the terminal
            terminal.close();
            System.out.println("✓ Terminal closed successfully");

            // Try to access writer through terminal - this should throw
            try {
                terminal.writer().println("This should fail");
                System.out.println("✗ ERROR: terminal.writer() should have thrown IllegalStateException!");
            } catch (IllegalStateException e) {
                System.out.println("✓ terminal.writer() correctly throws: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("✗ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Demonstrates the enhanced behavior: held references also fail after terminal close.
     * <p>
     * Note: PrintWriter doesn't throw exceptions - it sets an error flag instead.
     * NonBlockingReader throws ClosedException (an IOException) in strict mode.
     * </p>
     */
    private static void demonstrateHeldReferenceBehavior() {
        System.out.println("DEMONSTRATION 2: Held stream references after terminal close");
        System.out.println(repeat("-", 80));

        try {
            ByteArrayInputStream input = new ByteArrayInputStream("test\n".getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DumbTerminal terminal = new DumbTerminal("demo", "dumb", input, output, StandardCharsets.UTF_8);

            // Get references to streams BEFORE closing
            PrintWriter writer = terminal.writer();
            NonBlockingReader reader = terminal.reader();

            System.out.println("✓ Obtained references to writer and reader");

            // Use them before close - should work
            writer.println("Before close");
            System.out.println("✓ Writing through held writer reference works before close");

            // Close the terminal
            terminal.close();
            System.out.println("✓ Terminal closed successfully");

            // Try to use held writer reference
            // Note: PrintWriter doesn't throw exceptions - it just sets an error flag
            // The underlying stream (NonBlockingReader wrapping the output) will throw,
            // but PrintWriter swallows the exception
            writer.println("This will silently fail");
            writer.flush();
            if (writer.checkError()) {
                System.out.println("✓ Held writer reference has error flag set (PrintWriter doesn't throw)");
            } else {
                System.out.println("✗ ERROR: Held writer reference should have error flag set!");
            }

            // Try to use held reader reference - should throw ClosedException (an IOException)
            try {
                reader.read(100);
                System.out.println("✗ ERROR: Held reader reference should have thrown ClosedException!");
            } catch (ClosedException e) {
                System.out.println("✓ Held reader reference correctly throws ClosedException: " + e.getMessage());
            } catch (IOException e) {
                System.out.println("✓ Held reader reference correctly throws IOException: " + e.getMessage());
            }

            System.out.println("\n" + "Summary: Held references correctly fail after terminal closure!");

        } catch (Exception e) {
            System.out.println("✗ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Additional demonstration showing the recommended pattern.
     */
    @SuppressWarnings("unused")
    private static void demonstrateRecommendedPattern() {
        System.out.println("\nRECOMMENDED PATTERN: Use try-with-resources");
        System.out.println(repeat("-", 80));

        try {
            ByteArrayInputStream input = new ByteArrayInputStream(new byte[0]);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            // Recommended: Use try-with-resources
            try (DumbTerminal terminal = new DumbTerminal("demo", "dumb", input, output, StandardCharsets.UTF_8)) {
                terminal.writer().println("Use streams within try block");
                System.out.println("✓ Streams used safely within try-with-resources");
            }
            // Terminal is automatically closed here
            System.out.println("✓ Terminal automatically closed by try-with-resources");

            // Don't use streams after this point

        } catch (Exception e) {
            System.out.println("✗ Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

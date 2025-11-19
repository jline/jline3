/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Simple JLine terminal to test mouse behavior:
 * - Cursor positioning (click to move cursor)
 * - Text selection (drag to select)
 *
 * Compile and run:
 * javac -cp "reader/target/classes:terminal/target/classes" MouseTestTerminal.java
 * java -cp ".:reader/target/classes:terminal/target/classes" MouseTestTerminal
 */
public class MouseTestTerminal {

    public static void main(String[] args) throws IOException {
        System.out.println("=== JLine Mouse Test Terminal ===");
        System.out.println("Testing mouse cursor positioning and text selection");
        System.out.println("Commands: 'quit' to exit, anything else echoed back");
        System.out.println();

        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Test 1: JLine mouse only
        // testJLineMouseOnly(terminal);

        // Test 2: Terminal mouse only
        // testTerminalMouseOnly(terminal);

        // Test 3: Smart switching
        testSmartMouseSwitching(terminal);

        terminal.close();
    }

    private static void testJLineMouseOnly(Terminal terminal) throws IOException {
        System.out.println("\n--- TEST 1: JLine Mouse Only ---");
        System.out.println("Expected: Text selection works, cursor positioning doesn't");

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.MOUSE, true)
                .build();

        runTest(reader, terminal, "jline-only");
    }

    private static void testTerminalMouseOnly(Terminal terminal) throws IOException {
        System.out.println("\n--- TEST 2: Terminal Mouse Only ---");
        System.out.println("Expected: Cursor positioning works, text selection doesn't");

        terminal.trackMouse(Terminal.MouseTracking.Button);

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.MOUSE, false)
                .build();

        runTest(reader, terminal, "terminal-only");

        terminal.trackMouse(Terminal.MouseTracking.Off);
    }

    private static void testSmartMouseSwitching(Terminal terminal) throws IOException {
        System.out.println("\n--- TEST 3: Smart Mouse Switching ---");
        System.out.println("Expected: Both cursor positioning AND text selection work");

        terminal.trackMouse(Terminal.MouseTracking.Button);

        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.MOUSE, false)
                .build();

        String input;
        int count = 0;
        while (true) {
            count++;

            // Disable mouse tracking during input for text selection
            terminal.trackMouse(Terminal.MouseTracking.Off);
            System.out.println("[DEBUG] Mouse OFF - text selection should work");

            input = reader.readLine("smart-test-" + count + "> ");

            // Re-enable mouse tracking after input for cursor positioning
            terminal.trackMouse(Terminal.MouseTracking.Button);
            System.out.println("[DEBUG] Mouse ON - cursor positioning should work");

            if ("quit".equals(input.trim())) {
                break;
            }

            System.out.println("You entered: " + input);
            System.out.println("Try clicking to position cursor before next prompt...");
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
            } // Give time to test cursor positioning
        }

        terminal.trackMouse(Terminal.MouseTracking.Off);
    }

    private static void runTest(LineReader reader, Terminal terminal, String testName) {
        String input;
        int count = 0;

        while (true) {
            count++;
            input = reader.readLine(testName + "-" + count + "> ");

            if ("quit".equals(input.trim())) {
                break;
            }

            System.out.println("You entered: " + input);
        }
    }
}

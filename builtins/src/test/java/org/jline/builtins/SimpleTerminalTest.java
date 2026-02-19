/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

/**
 * Simple test to verify that our terminal implementations compile correctly.
 * This test doesn't require the full Maven build environment.
 */
public class SimpleTerminalTest {

    public static void main(String[] args) {
        System.out.println("Testing terminal implementations...");

        try {
            // Test WebTerminal creation
            WebTerminal webTerminal = new WebTerminal("localhost", 8081, 40, 20);
            System.out.println("✓ WebTerminal created successfully");
            System.out.println("  URL: " + webTerminal.getUrl());
            System.out.println("  Running: " + webTerminal.isRunning());

            // Test basic write operation
            webTerminal.write("Hello, WebTerminal!");
            System.out.println("✓ WebTerminal write operation successful");

            // Test SwingTerminal creation
            SwingTerminal swingTerminal = new SwingTerminal(40, 20);
            System.out.println("✓ SwingTerminal created successfully");

            // Test component access
            SwingTerminal.TerminalComponent component = swingTerminal.getComponent();
            System.out.println("✓ SwingTerminal component access successful");
            System.out.println("  Component class: " + component.getClass().getSimpleName());

            // Test basic write operation
            swingTerminal.write("Hello, SwingTerminal!");
            System.out.println("✓ SwingTerminal write operation successful");

            // Test frame creation
            javax.swing.JFrame frame = swingTerminal.createFrame("Test Frame");
            System.out.println("✓ SwingTerminal frame creation successful");
            System.out.println("  Frame title: " + frame.getTitle());

            // Clean up
            swingTerminal.dispose();
            frame.dispose();

            System.out.println("\n✓ All tests passed! Terminal implementations are working correctly.");

        } catch (Exception e) {
            System.err.println("✗ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

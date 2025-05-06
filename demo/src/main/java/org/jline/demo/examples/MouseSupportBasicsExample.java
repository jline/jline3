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

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating basic mouse support in JLine.
 */
public class MouseSupportBasicsExample {

    // SNIPPET_START: MouseSupportBasicsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Enable mouse tracking
            terminal.trackMouse(Terminal.MouseTracking.Normal);
            terminal.flush();

            System.out.println("Mouse tracking enabled. Click anywhere in the terminal...");
            System.out.println("Press Enter to exit.");

            // Simple event loop
            while (true) {
                int c = terminal.reader().read();
                if (c == '\r' || c == '\n') {
                    break;
                }

                // Process input (including mouse events)
                // Mouse events come as escape sequences
                // We'll see how to properly handle these in the next examples
            }
        } finally {
            // Disable mouse tracking before exiting
            terminal.trackMouse(Terminal.MouseTracking.Off);
            terminal.flush();

            terminal.close();
        }
    }
    // SNIPPET_END: MouseSupportBasicsExample
}

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
 * Example demonstrating mouse tracking modes in JLine.
 */
public class MouseTrackingModesExample {

    // SNIPPET_START: MouseTrackingModesExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Different mouse tracking modes

            // 1. Basic mouse tracking (clicks only)
            terminal.trackMouse(Terminal.MouseTracking.Button);

            // 2. Extended mouse tracking (clicks and movement)
            // This is terminal-dependent and may require specific escape sequences
            terminal.writer().write("\033[?1000;1002;1006;1015h");

            // 3. Any event tracking (clicks, movement, and position reports)
            // This is terminal-dependent and may require specific escape sequences
            terminal.writer().write("\033[?1000;1003;1006;1015h");

            terminal.flush();

            System.out.println("Enhanced mouse tracking enabled.");
            System.out.println("Try clicking, moving, and scrolling the mouse.");
            System.out.println("Press Enter to exit.");

            // Wait for Enter key
            while (terminal.reader().read() != '\n') {
                // Process events
            }
        } finally {
            // Disable all mouse tracking modes
            terminal.trackMouse(Terminal.MouseTracking.Off);
            terminal.writer().write("\033[?1000;1002;1003;1006;1015l");
            terminal.flush();

            terminal.close();
        }
    }
    // SNIPPET_END: MouseTrackingModesExample
}

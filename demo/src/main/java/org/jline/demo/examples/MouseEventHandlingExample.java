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
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

/**
 * Example demonstrating mouse event handling in JLine.
 */
public class MouseEventHandlingExample {

    // SNIPPET_START: MouseEventHandlingExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Set up signal handler for CTRL+C
            terminal.handle(Signal.INT, SignalHandler.SIG_IGN);

            // Enable mouse tracking
            terminal.trackMouse(Terminal.MouseTracking.Normal);

            System.out.println("Mouse tracking enabled. Click or move the mouse in the terminal...");
            System.out.println("Press 'q' to exit.");

            NonBlockingReader reader = terminal.reader();
            StringBuilder buffer = new StringBuilder();
            boolean esc = false;
            boolean bracket = false;
            boolean mouse = false;

            // Event loop
            while (true) {
                int c = reader.read();

                // Check for 'q' to exit
                if (c == 'q') {
                    break;
                }

                // Parse escape sequences for mouse events
                if (c == '\033') {
                    // ESC character - start of escape sequence
                    esc = true;
                    buffer.setLength(0);
                } else if (esc && c == '[') {
                    // [ character after ESC - potential mouse event
                    bracket = true;
                } else if (esc && bracket && c == 'M') {
                    // M character after ESC[ - confirmed mouse event
                    mouse = true;
                    buffer.setLength(0);
                } else if (mouse && buffer.length() < 3) {
                    // Collect the 3 bytes that define the mouse event
                    buffer.append((char) c);

                    if (buffer.length() == 3) {
                        // We have a complete mouse event
                        int b = buffer.charAt(0) - 32;
                        int x = buffer.charAt(1) - 32;
                        int y = buffer.charAt(2) - 32;

                        // Decode the event type
                        boolean press = (b & 3) != 3;
                        boolean release = (b & 3) == 3;
                        boolean wheel = (b & 64) != 0;

                        // Determine which button was used
                        String button = "unknown";
                        if ((b & 3) == 0) button = "left";
                        if ((b & 3) == 1) button = "middle";
                        if ((b & 3) == 2) button = "right";

                        // Print the event details
                        terminal.writer()
                                .println(String.format(
                                        "Mouse event: %s button %s at position (%d,%d)",
                                        press ? "pressed" : (release ? "released" : (wheel ? "wheel" : "moved")),
                                        button,
                                        x,
                                        y));
                        terminal.flush();

                        // Reset state
                        esc = false;
                        bracket = false;
                        mouse = false;
                    }
                } else {
                    // Not a mouse event or incomplete sequence
                    esc = false;
                    bracket = false;
                    mouse = false;
                }
            }
        } finally {
            // Disable mouse tracking before exiting
            terminal.trackMouse(Terminal.MouseTracking.Off);

            terminal.close();
        }
    }
    // SNIPPET_END: MouseEventHandlingExample
}

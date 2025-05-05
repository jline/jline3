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

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

/**
 * Example demonstrating raw mode in JLine.
 */
public class RawModeExample {

    // SNIPPET_START: RawModeExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Save original terminal attributes
            Attributes originalAttributes = terminal.getAttributes();

            // Enter raw mode
            Attributes rawAttributes = terminal.enterRawMode();

            terminal.writer().println("Terminal is in raw mode. Press keys (q to quit):");
            terminal.writer().flush();

            NonBlockingReader reader = terminal.reader();

            // Read characters until 'q' is pressed
            while (true) {
                int c = reader.read(100);
                if (c != -1) {
                    terminal.writer().println("Read: " + (char) c + " (ASCII: " + c + ")");
                    terminal.writer().flush();

                    if (c == 'q' || c == 'Q') {
                        break;
                    }
                }
            }

            // Restore original terminal attributes
            terminal.setAttributes(originalAttributes);
        } finally {
            terminal.close();
        }
    }
    // SNIPPET_END: RawModeExample
}

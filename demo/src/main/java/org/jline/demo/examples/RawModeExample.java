/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

/**
 * Example demonstrating raw mode in JLine.
 *
 * <p>In raw mode, ISIG is cleared so the kernel no longer translates control
 * characters into signals.  If your application needs to react to Ctrl+C
 * (interrupt), read the VINTR byte from the terminal attributes and handle it
 * in your own input loop as shown below.</p>
 */
public class RawModeExample {

    // SNIPPET_START: RawModeExample
    public static void main(String[] args) throws IOException {
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            // Save original terminal attributes
            Attributes originalAttributes = terminal.getAttributes();

            // Retrieve the interrupt character (typically 0x03 / Ctrl+C)
            // *before* entering raw mode, so we can handle it ourselves.
            int intrChar = originalAttributes.getControlChar(ControlChar.VINTR);

            // Enter raw mode — ISIG is cleared, so the terminal will no longer
            // raise signals for control characters; they arrive as raw bytes.
            terminal.enterRawMode();

            terminal.writer().println("Terminal is in raw mode. Press keys (Ctrl+C or q to quit):");
            terminal.writer().flush();

            NonBlockingReader reader = terminal.reader();

            // Read characters until 'q' or the interrupt character is pressed
            while (true) {
                int c = reader.read(100);
                if (c != -1) {
                    if (c == intrChar) {
                        terminal.writer().println("Interrupt received (Ctrl+C) — exiting.");
                        terminal.writer().flush();
                        break;
                    }

                    terminal.writer().println("Read: " + (char) c + " (ASCII: " + c + ")");
                    terminal.writer().flush();

                    if (c == 'q' || c == 'Q') {
                        break;
                    }
                }
            }

            // Restore original terminal attributes
            terminal.setAttributes(originalAttributes);
        }
    }
    // SNIPPET_END: RawModeExample
}

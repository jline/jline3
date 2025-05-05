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
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating control characters in JLine.
 */
public class ControlCharsExample {

    // SNIPPET_START: ControlCharsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Get current attributes
            Attributes attributes = terminal.getAttributes();

            // Display current control characters
            terminal.writer().println("Current control characters:");

            // Common control characters
            for (ControlChar cc : new ControlChar[] {
                ControlChar.VEOF, // EOF character (usually Ctrl-D)
                ControlChar.VEOL, // EOL character
                ControlChar.VERASE, // Erase character (usually Backspace)
                ControlChar.VINTR, // Interrupt character (usually Ctrl-C)
                ControlChar.VKILL, // Kill character (usually Ctrl-U)
                ControlChar.VMIN, // Minimum number of characters for non-canonical read
                ControlChar.VQUIT, // Quit character (usually Ctrl-\\)
                ControlChar.VSTART, // Start character (usually Ctrl-Q)
                ControlChar.VSTOP, // Stop character (usually Ctrl-S)
                ControlChar.VSUSP, // Suspend character (usually Ctrl-Z)
                ControlChar.VTIME, // Timeout in deciseconds for non-canonical read
            }) {
                try {
                    int value = attributes.getControlChar(cc);
                    terminal.writer()
                            .println("  " + cc + ": " + value + (value > 0 ? " ('" + (char) value + "')" : ""));
                } catch (Exception e) {
                    terminal.writer().println("  " + cc + ": unsupported");
                }
            }

            terminal.writer().flush();
        } finally {
            terminal.close();
        }
    }
    // SNIPPET_END: ControlCharsExample
}

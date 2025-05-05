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
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating custom terminal behavior in JLine.
 */
public class CustomTerminalBehaviorExample {

    // SNIPPET_START: CustomTerminalBehaviorExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Save original attributes
            Attributes originalAttributes = terminal.getAttributes();

            // Create custom attributes
            Attributes customAttributes = new Attributes(originalAttributes);

            // Customize input behavior
            customAttributes.setLocalFlag(LocalFlag.ECHO, true); // Enable echo
            customAttributes.setLocalFlag(LocalFlag.ICANON, true); // Enable canonical mode
            customAttributes.setInputFlag(InputFlag.ICRNL, true); // Map CR to NL

            // Customize control characters
            customAttributes.setControlChar(ControlChar.VINTR, 3); // Set Ctrl+C as interrupt
            customAttributes.setControlChar(ControlChar.VEOF, 4); // Set Ctrl+D as EOF
            customAttributes.setControlChar(ControlChar.VSUSP, 26); // Set Ctrl+Z as suspend

            // Apply custom attributes
            terminal.setAttributes(customAttributes);

            terminal.writer().println("Terminal configured with custom attributes");
            terminal.writer().println("Type some text and press Enter (Ctrl+D to exit):");
            terminal.writer().flush();

            // Read lines until EOF
            String line;
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            while ((line = reader.readLine(">")) != null) {
                terminal.writer().println("You typed: " + line);
                terminal.writer().println("Type another line (Ctrl+D to exit):");
                terminal.writer().flush();
            }

            // Restore original attributes
            terminal.setAttributes(originalAttributes);
        } finally {
            terminal.close();
        }
    }
    // SNIPPET_END: CustomTerminalBehaviorExample
}

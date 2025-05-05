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
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating terminal attributes in JLine.
 */
public class TerminalAttributesExample {

    // SNIPPET_START: TerminalAttributesExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Get current attributes
        Attributes attributes = terminal.getAttributes();

        // Display some attributes
        terminal.writer().println("Terminal attributes:");
        terminal.writer().printf("  ECHO: %b%n", attributes.getLocalFlag(LocalFlag.ECHO));
        terminal.writer().printf("  ICANON: %b%n", attributes.getLocalFlag(LocalFlag.ICANON));
        terminal.writer().printf("  INTR char: %c%n", (char) attributes.getControlChar(ControlChar.VINTR));
        terminal.writer().flush();

        // Modify attributes for raw mode
        Attributes raw = new Attributes(attributes);
        raw.setLocalFlag(LocalFlag.ECHO, false);
        raw.setLocalFlag(LocalFlag.ICANON, false);
        raw.setInputFlag(InputFlag.ICRNL, false);
        raw.setControlChar(ControlChar.VMIN, 1);
        raw.setControlChar(ControlChar.VTIME, 0);

        // Set the new attributes
        terminal.setAttributes(raw);
        terminal.writer().println("Switched to raw mode. Press 'q' to exit.");
        terminal.writer().flush();

        // Read a character
        int c = terminal.reader().read();

        // Restore original attributes
        terminal.setAttributes(attributes);
        terminal.writer().printf("You pressed: %c%n", (char) c);
        terminal.writer().println("Restored original attributes.");
        terminal.writer().flush();

        terminal.close();
    }
    // SNIPPET_END: TerminalAttributesExample
}

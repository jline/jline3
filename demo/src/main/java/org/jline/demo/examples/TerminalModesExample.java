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
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating terminal modes in JLine.
 */
public class TerminalModesExample {

    // SNIPPET_START: TerminalModesExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Save original attributes
            Attributes originalAttributes = terminal.getAttributes();

            // Display current mode
            boolean canonicalMode = originalAttributes.getLocalFlag(LocalFlag.ICANON);
            terminal.writer().println("Terminal is currently in " + (canonicalMode ? "canonical" : "raw") + " mode");
            terminal.writer().flush();

            // Switch to raw mode
            Attributes rawAttributes = new Attributes(originalAttributes);
            rawAttributes.setLocalFlag(LocalFlag.ICANON, false); // Disable canonical mode
            rawAttributes.setLocalFlag(LocalFlag.ECHO, false); // Disable echo
            rawAttributes.setLocalFlag(LocalFlag.ISIG, false); // Disable signals
            rawAttributes.setLocalFlag(LocalFlag.IEXTEN, false); // Disable extended functions
            rawAttributes.setInputFlag(InputFlag.IXON, false); // Disable xon
            rawAttributes.setInputFlag(InputFlag.ICRNL, false); // Disable cr/nl
            rawAttributes.setInputFlag(InputFlag.INLCR, false); // Disable nl/cr

            // Set control characters for non-canonical mode
            rawAttributes.setControlChar(Attributes.ControlChar.VMIN, 1); // Read at least 1 character
            rawAttributes.setControlChar(Attributes.ControlChar.VTIME, 0); // No timeout

            // Apply raw mode attributes
            terminal.setAttributes(rawAttributes);

            terminal.writer().println("Switched to raw mode. Press any key to continue...");
            terminal.writer().flush();

            // Read a single character
            int c = terminal.reader().read();

            // Restore original attributes
            terminal.setAttributes(originalAttributes);

            terminal.writer().println("\nSwitched back to canonical mode");
            terminal.writer().println("You pressed: " + (char) c + " (ASCII: " + c + ")");
            terminal.writer().flush();

            terminal.writer().println("\nPress Enter to exit...");
            terminal.writer().flush();
            terminal.reader().read();
        } finally {
            terminal.close();
        }
    }
    // SNIPPET_END: TerminalModesExample
}

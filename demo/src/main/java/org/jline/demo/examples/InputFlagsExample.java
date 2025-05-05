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
 * Example demonstrating input flags in JLine.
 */
public class InputFlagsExample {

    // SNIPPET_START: InputFlagsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        try {
            // Get current attributes
            Attributes attributes = terminal.getAttributes();

            // Current input flags
            terminal.writer().println("Current input flags:");
            for (InputFlag flag : InputFlag.values()) {
                try {
                    boolean value = attributes.getInputFlag(flag);
                    terminal.writer().println("  " + flag + ": " + value);
                } catch (Exception e) {
                    terminal.writer().println("  " + flag + ": unsupported");
                }
            }

            // Current local flags
            terminal.writer().println("Current local flags:");
            for (LocalFlag flag : LocalFlag.values()) {
                try {
                    boolean value = attributes.getLocalFlag(flag);
                    terminal.writer().println("  " + flag + ": " + value);
                } catch (Exception e) {
                    terminal.writer().println("  " + flag + ": unsupported");
                }
            }

            terminal.writer().flush();
        } finally {
            terminal.close();
        }
    }
    // SNIPPET_END: InputFlagsExample
}

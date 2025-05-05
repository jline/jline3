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
import org.jline.utils.InfoCmp.Capability;

/**
 * Example demonstrating terminal colors in JLine.
 */
public class TerminalColorsExample {

    // SNIPPET_START: TerminalColorsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Clear screen
        terminal.puts(Capability.clear_screen);

        // Set foreground color to red
        terminal.puts(Capability.set_a_foreground, 1);
        terminal.writer().println("This text is red");

        // Set foreground color to green
        terminal.puts(Capability.set_a_foreground, 2);
        terminal.writer().println("This text is green");

        // Set foreground color to yellow
        terminal.puts(Capability.set_a_foreground, 3);
        terminal.writer().println("This text is yellow");

        // Set background color to blue
        terminal.puts(Capability.set_a_background, 4);
        terminal.writer().println("This text has blue background");

        // Reset colors
        terminal.puts(Capability.orig_pair);
        terminal.writer().println("This text has default colors");

        // Set bold attribute
        terminal.puts(Capability.enter_bold_mode);
        terminal.writer().println("This text is bold");

        // Reset attributes
        terminal.puts(Capability.exit_attribute_mode);
        terminal.writer().println("This text has default attributes");

        terminal.flush();
        terminal.close();
    }
    // SNIPPET_END: TerminalColorsExample
}

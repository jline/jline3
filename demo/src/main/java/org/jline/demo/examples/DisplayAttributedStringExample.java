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
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating how to display AttributedString in JLine.
 */
public class DisplayAttributedStringExample {

    // SNIPPET_START: DisplayAttributedStringExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create an attributed string
        AttributedString message = new AttributedString(
                "This is a styled message",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN).bold());

        // Print the attributed string to the terminal
        message.println(terminal);

        // Or get the ANSI escape sequence string
        String ansiString = message.toAnsi(terminal);
        System.out.println("ANSI string: " + ansiString);

        terminal.close();
    }
    // SNIPPET_END: DisplayAttributedStringExample
}

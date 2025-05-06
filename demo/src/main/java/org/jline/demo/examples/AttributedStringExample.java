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
 * Example demonstrating the use of AttributedString in JLine.
 */
public class AttributedStringExample {

    // SNIPPET_START: AttributedStringExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create an AttributedString with a style
        AttributedString errorMessage = new AttributedString(
                "Error: File not found",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold());

        // Create an AttributedString with default style
        AttributedString plainText = new AttributedString("Plain text");

        // Display the strings
        errorMessage.println(terminal);
        plainText.println(terminal);

        terminal.close();
    }
    // SNIPPET_END: AttributedStringExample
}

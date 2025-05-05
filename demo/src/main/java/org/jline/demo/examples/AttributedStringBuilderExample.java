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
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating the use of AttributedStringBuilder in JLine.
 */
public class AttributedStringBuilderExample {

    // SNIPPET_START: AttributedStringBuilderExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a builder
        AttributedStringBuilder builder = new AttributedStringBuilder();

        // Append text with different styles
        builder.append("System status: ")
                .style(AttributedStyle.DEFAULT.bold())
                .append("ONLINE")
                .style(AttributedStyle.DEFAULT)
                .append(" (")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append("All systems operational")
                .style(AttributedStyle.DEFAULT)
                .append(")");

        // Build the final AttributedString
        AttributedString result = builder.toAttributedString();

        // Print to terminal
        result.println(terminal);

        terminal.close();
    }
    // SNIPPET_END: AttributedStringBuilderExample

    // SNIPPET_START: StylingSpecificSectionsExample
    public static void stylingSpecificSections(Terminal terminal) throws IOException {
        // Create a builder
        AttributedStringBuilder builder = new AttributedStringBuilder();

        // Append text with default style
        builder.append("Command: ");

        // Append text with a specific style
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold())
                .append("git")
                .style(AttributedStyle.DEFAULT)
                .append(" ");

        // Append more styled text
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE))
                .append("commit")
                .style(AttributedStyle.DEFAULT)
                .append(" ");

        // Append an option with a different style
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                .append("-m")
                .style(AttributedStyle.DEFAULT)
                .append(" ");

        // Append a quoted string with yet another style
        builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA))
                .append("\"Fix critical bug\"");

        // Print to terminal
        builder.toAttributedString().println(terminal);
    }
    // SNIPPET_END: StylingSpecificSectionsExample
}

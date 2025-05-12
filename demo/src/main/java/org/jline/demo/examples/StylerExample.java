/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating styling in JLine.
 */
public class StylerExample {

    // SNIPPET_START: StylerExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a builder for styled text
        AttributedStringBuilder builder = new AttributedStringBuilder();

        // Create different styles for different types of content
        AttributedStyle errorStyle = AttributedStyle.BOLD.foreground(AttributedStyle.RED);
        AttributedStyle warningStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
        AttributedStyle infoStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);
        AttributedStyle successStyle = AttributedStyle.BOLD.foreground(AttributedStyle.GREEN);

        // Apply styles to strings
        AttributedString errorText =
                builder.style(errorStyle).append("ERROR: File not found").toAttributedString();
        builder = new AttributedStringBuilder();

        AttributedString warningText =
                builder.style(warningStyle).append("WARNING: Disk space low").toAttributedString();
        builder = new AttributedStringBuilder();

        AttributedString infoText =
                builder.style(infoStyle).append("INFO: Operation in progress").toAttributedString();
        builder = new AttributedStringBuilder();

        AttributedString successText = builder.style(successStyle)
                .append("SUCCESS: Operation completed")
                .toAttributedString();
        builder = new AttributedStringBuilder();

        AttributedString plainText = builder.append("This is plain text").toAttributedString();
        builder = new AttributedStringBuilder();

        // Print the styled text
        terminal.writer().println(errorText);
        terminal.writer().println(warningText);
        terminal.writer().println(infoText);
        terminal.writer().println(successText);
        terminal.writer().println(plainText);

        // Create a combined style
        AttributedStyle combinedStyle =
                AttributedStyle.BOLD.foreground(AttributedStyle.RED).underline();

        // Apply combined style
        AttributedString combinedText =
                builder.style(combinedStyle).append("Bold, red, and underlined").toAttributedString();

        terminal.writer().println();
        terminal.writer().println(combinedText);

        terminal.flush();
        terminal.close();
    }
    // SNIPPET_END: StylerExample
}

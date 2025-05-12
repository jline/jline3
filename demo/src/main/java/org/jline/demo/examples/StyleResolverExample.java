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
 * Example demonstrating the StyleResolver in JLine.
 */
public class StyleResolverExample {

    // SNIPPET_START: StyleResolverExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create an AttributedStringBuilder for building styled strings
        AttributedStringBuilder builder = new AttributedStringBuilder();

        // Define some common styles
        AttributedStyle errorStyle = AttributedStyle.BOLD.foreground(AttributedStyle.RED);
        AttributedStyle warningStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
        AttributedStyle infoStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE);
        AttributedStyle successStyle = AttributedStyle.BOLD.foreground(AttributedStyle.GREEN);
        AttributedStyle headerStyle = AttributedStyle.BOLD.foreground(AttributedStyle.CYAN);

        // Create a default styled text
        AttributedString defaultText = builder.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                .append("Error: ")
                .style(AttributedStyle.DEFAULT)
                .append("File not found")
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Create styled text with error style
        AttributedString errorText = builder.style(errorStyle)
                .append("Error: ")
                .style(AttributedStyle.DEFAULT)
                .append("File not found")
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Create styled text with warning style
        AttributedString warningText = builder.style(warningStyle)
                .append("Warning: ")
                .style(AttributedStyle.DEFAULT)
                .append("Disk space low")
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Create styled text with info style
        AttributedString infoText = builder.style(infoStyle)
                .append("Info: ")
                .style(AttributedStyle.DEFAULT)
                .append("Operation in progress")
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Create styled text with success style
        AttributedString successText = builder.style(successStyle)
                .append("Success: ")
                .style(AttributedStyle.DEFAULT)
                .append("Operation completed")
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Create styled text with header style
        AttributedString headerText = builder.style(headerStyle)
                .append("System Status Report")
                .style(AttributedStyle.DEFAULT)
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Print the styled text
        terminal.writer().println("Default resolver:");
        terminal.writer().println(defaultText);
        terminal.writer().println();

        terminal.writer().println("Custom resolver with named styles:");
        terminal.writer().println(headerText);
        terminal.writer().println(errorText);
        terminal.writer().println(warningText);
        terminal.writer().println(infoText);
        terminal.writer().println(successText);

        terminal.flush();
        terminal.close();
    }
    // SNIPPET_END: StyleResolverExample
}

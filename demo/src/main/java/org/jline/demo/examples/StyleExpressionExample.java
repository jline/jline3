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
 * Example demonstrating style expressions in JLine.
 */
public class StyleExpressionExample {

    // SNIPPET_START: StyleExpressionExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create styled text using AttributedStringBuilder
        AttributedStringBuilder builder = new AttributedStringBuilder();

        // Create bold red text
        AttributedString text1 = builder.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                .append("This is bold red text")
                .style(AttributedStyle.DEFAULT)
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Create text with yellow foreground on blue background
        AttributedString text2 = builder.style(
                        AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW).background(AttributedStyle.BLUE))
                .append("Text with yellow foreground on blue background")
                .style(AttributedStyle.DEFAULT)
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Create text with underlined italic section
        AttributedString text3 = builder.append("Normal text with ")
                .style(AttributedStyle.DEFAULT.underline().italic())
                .append("underlined italic")
                .style(AttributedStyle.DEFAULT)
                .append(" section")
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Create mixed styled text
        AttributedString text4 = builder.append("Mix of ")
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                .append("bold red")
                .style(AttributedStyle.DEFAULT)
                .append(" and ")
                .style(AttributedStyle.DEFAULT.italic().foreground(AttributedStyle.BLUE))
                .append("italic blue")
                .style(AttributedStyle.DEFAULT)
                .append(" text")
                .toAttributedString();
        builder = new AttributedStringBuilder();

        // Create a complex example with different styles
        AttributedString complex = builder.append("Status: ")
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                .append("SUCCESS")
                .style(AttributedStyle.DEFAULT)
                .append(" | Errors: ")
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.RED))
                .append("0")
                .style(AttributedStyle.DEFAULT)
                .append(" | Warnings: ")
                .style(AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW))
                .append("2")
                .style(AttributedStyle.DEFAULT)
                .toAttributedString();

        // Print the styled text
        terminal.writer().println(text1);
        terminal.writer().println(text2);
        terminal.writer().println(text3);
        terminal.writer().println(text4);
        terminal.writer().println();
        terminal.writer().println(complex);

        terminal.flush();
        terminal.close();
    }
    // SNIPPET_END: StyleExpressionExample
}

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
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating basic styling with JLine.
 */
public class BasicStylingExample {

    // SNIPPET_START: BasicStylingExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create styled text with foreground colors
        AttributedString redText =
                new AttributedString("This is red text", AttributedStyle.BOLD.foreground(AttributedStyle.RED));

        AttributedString greenText =
                new AttributedString("This is green text", AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));

        AttributedString blueText =
                new AttributedString("This is blue text", AttributedStyle.BOLD.foreground(AttributedStyle.BLUE));

        // Create styled text with background colors
        AttributedString yellowBg = new AttributedString(
                "Text with yellow background", AttributedStyle.DEFAULT.background(AttributedStyle.YELLOW));

        // Create styled text with both foreground and background
        AttributedString fgBg = new AttributedString(
                "White on black",
                AttributedStyle.BOLD.foreground(AttributedStyle.WHITE).background(AttributedStyle.BLACK));

        // Create styled text with other attributes
        AttributedString underlined = new AttributedString("Underlined text", AttributedStyle.DEFAULT.underline());

        AttributedString blink = new AttributedString("Blinking text", AttributedStyle.DEFAULT.blink());

        AttributedString inverse = new AttributedString("Inverse text", AttributedStyle.DEFAULT.inverse());

        // Print the styled text
        terminal.writer().println(redText);
        terminal.writer().println(greenText);
        terminal.writer().println(blueText);
        terminal.writer().println();
        terminal.writer().println(yellowBg);
        terminal.writer().println(fgBg);
        terminal.writer().println();
        terminal.writer().println(underlined);
        terminal.writer().println(blink);
        terminal.writer().println(inverse);

        terminal.flush();
        terminal.close();
    }
    // SNIPPET_END: BasicStylingExample
}

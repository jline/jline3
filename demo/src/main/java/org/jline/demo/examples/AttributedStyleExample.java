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
 * Example demonstrating the use of AttributedStyle in JLine.
 */
public class AttributedStyleExample {

    // SNIPPET_START: AttributedStyleExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a default style
        AttributedStyle defaultStyle = AttributedStyle.DEFAULT;

        // Create a style with foreground color
        AttributedStyle redText = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);

        // Create a style with background color
        AttributedStyle blueBackground = AttributedStyle.DEFAULT.background(AttributedStyle.BLUE);

        // Create a style with text attributes
        AttributedStyle boldText = AttributedStyle.DEFAULT.bold();
        AttributedStyle italicText = AttributedStyle.DEFAULT.italic();
        AttributedStyle underlinedText = AttributedStyle.DEFAULT.underline();

        // Combine multiple attributes
        AttributedStyle boldRedOnBlue = AttributedStyle.DEFAULT
                .foreground(AttributedStyle.RED)
                .background(AttributedStyle.BLUE)
                .bold();

        // Display examples
        new AttributedString("Default style", defaultStyle).println(terminal);
        new AttributedString("Red text", redText).println(terminal);
        new AttributedString("Blue background", blueBackground).println(terminal);
        new AttributedString("Bold text", boldText).println(terminal);
        new AttributedString("Italic text", italicText).println(terminal);
        new AttributedString("Underlined text", underlinedText).println(terminal);
        new AttributedString("Bold red on blue", boldRedOnBlue).println(terminal);

        // Use bright variants
        AttributedStyle brightRed =
                AttributedStyle.DEFAULT.foreground(AttributedStyle.RED).bold();
        new AttributedString("Bright red (bold red)", brightRed).println(terminal);

        // Use 256-color mode
        AttributedStyle color123 = AttributedStyle.DEFAULT.foreground(123);
        new AttributedString("Color 123 from 256-color palette", color123).println(terminal);

        terminal.close();
    }
    // SNIPPET_END: AttributedStyleExample
}

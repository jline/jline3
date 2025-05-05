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
 * Example demonstrating basic usage of AttributedString in JLine.
 */
public class AttributedStringBasicsExample {

    // SNIPPET_START: AttributedStringBasicsExample
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a simple AttributedString with default style
        AttributedString simple = new AttributedString("This is a simple AttributedString");
        simple.println(terminal);

        // Create an AttributedString with a specific style
        AttributedString colored =
                new AttributedString("This text is blue", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
        colored.println(terminal);

        // Create an AttributedString with bold style
        AttributedString bold = new AttributedString("This text is bold", AttributedStyle.DEFAULT.bold());
        bold.println(terminal);

        // Create an AttributedString with multiple attributes
        AttributedString fancy = new AttributedString(
                "This text is bold, italic, and red",
                AttributedStyle.DEFAULT.bold().italic().foreground(AttributedStyle.RED));
        fancy.println(terminal);

        // Create an AttributedString with background color
        AttributedString background = new AttributedString(
                "This text has a yellow background", AttributedStyle.DEFAULT.background(AttributedStyle.YELLOW));
        background.println(terminal);

        terminal.flush();
    }
    // SNIPPET_END: AttributedStringBasicsExample
}

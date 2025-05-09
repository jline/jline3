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

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating a basic highlighter that colors all text blue.
 */
public class BasicHighlighterExample {

    // SNIPPET_START: BasicHighlighterExample
    public static void main(String[] args) throws IOException {
        // Create a simple highlighter that colors all text blue
        Highlighter blueHighlighter = new Highlighter() {
            @Override
            public AttributedString highlight(LineReader reader, String buffer) {
                // Apply blue color to the entire buffer
                return new AttributedString(buffer, AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));
            }
        };

        // Create a terminal and reader with our highlighter
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(blueHighlighter)
                .build();

        // Read input with blue highlighting
        String line = reader.readLine("prompt> ");
        terminal.writer().println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: BasicHighlighterExample
}

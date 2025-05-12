/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultHighlighter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating a custom highlighter.
 */
public class HighlighterExample {

    // SNIPPET_START: HighlighterExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a custom highlighter
        Highlighter highlighter = new DefaultHighlighter() {
            @Override
            public AttributedString highlight(LineReader reader, String buffer) {
                // Create a builder for the highlighted text
                AttributedStringBuilder builder = new AttributedStringBuilder();

                // Apply different styles based on content
                if (buffer.contains("error")) {
                    // Highlight "error" in red
                    int index = buffer.indexOf("error");
                    builder.append(buffer.substring(0, index));
                    builder.styled(
                            AttributedStyle.BOLD.foreground(AttributedStyle.RED), buffer.substring(index, index + 5));
                    builder.append(buffer.substring(index + 5));
                } else if (buffer.contains("warning")) {
                    // Highlight "warning" in yellow
                    int index = buffer.indexOf("warning");
                    builder.append(buffer.substring(0, index));
                    builder.styled(
                            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW),
                            buffer.substring(index, index + 7));
                    builder.append(buffer.substring(index + 7));
                } else if (buffer.startsWith("command")) {
                    // Highlight commands in blue
                    builder.styled(AttributedStyle.BOLD.foreground(AttributedStyle.BLUE), buffer);
                } else {
                    // Default highlighting
                    builder.append(buffer);
                }

                return builder.toAttributedString();
            }
        };

        // Create a line reader with the custom highlighter
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(highlighter)
                .build();

        // Read input with custom highlighting
        // Try typing: "command", "error message", or "warning message"
        String line = reader.readLine("Enter text (try 'error' or 'warning'): ");
        System.out.println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: HighlighterExample
}

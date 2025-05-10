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
import java.util.Stack;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating a highlighter that checks for balanced parentheses.
 */
public class ErrorHighlightingExample {

    // SNIPPET_START: ErrorHighlightingExample
    public static void main(String[] args) throws IOException {
        // Create a highlighter that checks for balanced parentheses
        Highlighter parenthesesHighlighter = new Highlighter() {
            @Override
            public AttributedString highlight(LineReader reader, String buffer) {
                AttributedStringBuilder builder = new AttributedStringBuilder();

                // Check for balanced parentheses
                Stack<Integer> stack = new Stack<>();
                boolean hasError = false;

                for (int i = 0; i < buffer.length(); i++) {
                    char c = buffer.charAt(i);

                    if (c == '(') {
                        // Push the position of the opening parenthesis
                        stack.push(i);
                    } else if (c == ')') {
                        if (stack.isEmpty()) {
                            // Unmatched closing parenthesis - highlight it as an error
                            hasError = true;

                            // Add text before the error
                            if (i > 0) {
                                builder.append(buffer.substring(0, i));
                            }

                            // Add the error character with red background
                            builder.styled(
                                    AttributedStyle.DEFAULT
                                            .foreground(AttributedStyle.WHITE)
                                            .background(AttributedStyle.RED),
                                    String.valueOf(c));

                            // Add text after the error
                            if (i < buffer.length() - 1) {
                                builder.append(buffer.substring(i + 1));
                            }

                            break;
                        }

                        // Matched parenthesis - pop from stack
                        stack.pop();
                    }
                }

                // If we have unmatched opening parentheses, highlight the first one
                if (!hasError && !stack.isEmpty()) {
                    int errorPos = stack.firstElement();

                    // Add text before the error
                    if (errorPos > 0) {
                        builder.append(buffer.substring(0, errorPos));
                    }

                    // Add the error character with red background
                    builder.styled(
                            AttributedStyle.DEFAULT
                                    .foreground(AttributedStyle.WHITE)
                                    .background(AttributedStyle.RED),
                            String.valueOf(buffer.charAt(errorPos)));

                    // Add text after the error
                    if (errorPos < buffer.length() - 1) {
                        builder.append(buffer.substring(errorPos + 1));
                    }
                } else if (!hasError) {
                    // No errors - return the buffer as is
                    builder.append(buffer);
                }

                return builder.toAttributedString();
            }
        };

        // Create a terminal and reader with our parentheses highlighter
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(parenthesesHighlighter)
                .build();

        // Display instructions
        terminal.writer().println("Parentheses Error Highlighting Example");
        terminal.writer().println("Try typing expressions with parentheses like: (1 + 2) * (3 - 4))");
        terminal.writer().println("Unbalanced parentheses will be highlighted in red");
        terminal.writer().println();

        // Read input with parentheses error highlighting
        String line = reader.readLine("expr> ");
        terminal.writer().println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: ErrorHighlightingExample
}

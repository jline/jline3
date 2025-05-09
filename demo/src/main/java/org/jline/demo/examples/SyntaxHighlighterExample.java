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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Example demonstrating a more complex syntax highlighter for a simple programming language.
 */
public class SyntaxHighlighterExample {

    // SNIPPET_START: SyntaxHighlighterExample
    public static void main(String[] args) throws IOException {
        // Create a syntax highlighter for a simple programming language
        Highlighter syntaxHighlighter = new Highlighter() {
            // Patterns for different syntax elements
            private final Pattern KEYWORDS = Pattern.compile("\\b(if|else|while|for|return|function|var|let|const)\\b");

            private final Pattern STRINGS =
                    Pattern.compile("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(\\\\.[^'\\\\]*)*'");

            private final Pattern NUMBERS = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");

            private final Pattern COMMENTS = Pattern.compile("//.*$|/\\*[\\s\\S]*?\\*/", Pattern.MULTILINE);

            @Override
            public AttributedString highlight(LineReader reader, String buffer) {
                // First, create a copy of the buffer that we can modify
                // as we process each syntax element
                String workingBuffer = buffer;

                // Create a builder for our highlighted string
                AttributedStringBuilder builder = new AttributedStringBuilder();

                // Process each type of syntax element in a specific order

                // 1. First highlight comments (green)
                workingBuffer = highlightPattern(
                        builder, workingBuffer, COMMENTS, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));

                // 2. Then highlight strings (yellow)
                workingBuffer = highlightPattern(
                        builder, workingBuffer, STRINGS, AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));

                // 3. Then highlight numbers (cyan)
                workingBuffer = highlightPattern(
                        builder, workingBuffer, NUMBERS, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));

                // 4. Finally highlight keywords (bold magenta)
                workingBuffer = highlightPattern(
                        builder, workingBuffer, KEYWORDS, AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA));

                // Add any remaining text with default style
                if (!workingBuffer.isEmpty()) {
                    builder.append(workingBuffer);
                }

                return builder.toAttributedString();
            }

            // Helper method to highlight a specific pattern
            private String highlightPattern(
                    AttributedStringBuilder builder, String buffer, Pattern pattern, AttributedStyle style) {

                StringBuilder result = new StringBuilder();
                Matcher matcher = pattern.matcher(buffer);
                int lastEnd = 0;

                // Find all matches of the pattern
                while (matcher.find()) {
                    // Add the text before this match to the result
                    result.append(buffer, lastEnd, matcher.start());

                    // Add the matched text with the specified style to the builder
                    builder.styled(style, buffer.substring(matcher.start(), matcher.end()));

                    lastEnd = matcher.end();
                }

                // Add any remaining text to the result
                if (lastEnd < buffer.length()) {
                    result.append(buffer.substring(lastEnd));
                }

                return result.toString();
            }
        };

        // Create a terminal and reader with our syntax highlighter
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(syntaxHighlighter)
                .build();

        // Display instructions
        terminal.writer().println("Syntax Highlighter Example");
        terminal.writer().println("Try typing code like: function add(a, b) { return a + b; // Add numbers }");
        terminal.writer().println();

        // Read input with syntax highlighting
        String line = reader.readLine("code> ");
        terminal.writer().println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: SyntaxHighlighterExample
}

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
 * Example demonstrating a highlighter that highlights SQL keywords.
 */
public class KeywordHighlighterExample {

    // SNIPPET_START: KeywordHighlighterExample
    public static void main(String[] args) throws IOException {
        // Create a highlighter for SQL keywords
        Highlighter sqlHighlighter = new Highlighter() {
            // Pattern to match SQL keywords (case insensitive)
            private final Pattern SQL_KEYWORDS = Pattern.compile(
                    "\\b(SELECT|FROM|WHERE|JOIN|ON|GROUP BY|ORDER BY|HAVING|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER)\\b",
                    Pattern.CASE_INSENSITIVE);

            @Override
            public AttributedString highlight(LineReader reader, String buffer) {
                AttributedStringBuilder builder = new AttributedStringBuilder();

                // Find all SQL keywords in the buffer
                Matcher matcher = SQL_KEYWORDS.matcher(buffer);
                int lastEnd = 0;

                while (matcher.find()) {
                    // Add text before the keyword with default style
                    builder.append(buffer.substring(lastEnd, matcher.start()));

                    // Add the keyword with bold blue style
                    builder.styled(
                            AttributedStyle.BOLD.foreground(AttributedStyle.BLUE),
                            buffer.substring(matcher.start(), matcher.end()));

                    lastEnd = matcher.end();
                }

                // Add any remaining text
                if (lastEnd < buffer.length()) {
                    builder.append(buffer.substring(lastEnd));
                }

                return builder.toAttributedString();
            }
        };

        // Create a terminal and reader with our SQL highlighter
        Terminal terminal = TerminalBuilder.builder().build();
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .highlighter(sqlHighlighter)
                .build();

        // Display instructions
        terminal.writer().println("SQL Keyword Highlighter Example");
        terminal.writer().println("Try typing SQL queries like: SELECT * FROM users WHERE id = 1");
        terminal.writer().println();

        // Read input with SQL keyword highlighting
        String line = reader.readLine("sql> ");
        terminal.writer().println("You entered: " + line);

        terminal.close();
    }
    // SNIPPET_END: KeywordHighlighterExample
}

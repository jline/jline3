/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.regex.Pattern;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.shell.CommandDispatcher;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Command-aware syntax highlighter for the shell.
 * <p>
 * Highlights the first word of the command line based on whether it is a known
 * command in the dispatcher:
 * <ul>
 *   <li>Known commands are highlighted in bold</li>
 *   <li>Unknown commands are highlighted in red</li>
 *   <li>Pipeline operators ({@code |}, {@code &&}, {@code ||}, {@code ;}, etc.)
 *       are highlighted in cyan</li>
 * </ul>
 * <p>
 * An optional delegate {@link Highlighter} can be provided for composability.
 * If provided, the delegate is called first, then command highlighting is applied
 * on top.
 *
 * @since 4.0
 */
public class CommandHighlighter implements Highlighter {

    private static final Pattern OPERATOR_PATTERN = Pattern.compile("(\\|;|&&|\\|\\||>>|[|>;])");

    private final CommandDispatcher dispatcher;
    private final Highlighter delegate;

    /**
     * Creates a command highlighter with no delegate.
     *
     * @param dispatcher the command dispatcher for command lookup
     */
    public CommandHighlighter(CommandDispatcher dispatcher) {
        this(dispatcher, null);
    }

    /**
     * Creates a command highlighter with an optional delegate.
     * <p>
     * When a delegate is provided, it is used to highlight the buffer first,
     * and then command-specific highlighting is applied on top.
     *
     * @param dispatcher the command dispatcher for command lookup
     * @param delegate the delegate highlighter, or null
     */
    public CommandHighlighter(CommandDispatcher dispatcher, Highlighter delegate) {
        this.dispatcher = dispatcher;
        this.delegate = delegate;
    }

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        if (buffer == null || buffer.isEmpty()) {
            return new AttributedString(buffer != null ? buffer : "");
        }

        // If we have a delegate, start with its result
        if (delegate != null) {
            return delegate.highlight(reader, buffer);
        }

        AttributedStringBuilder sb = new AttributedStringBuilder();
        // Split the buffer by pipeline operators and highlight each segment
        highlightBuffer(sb, buffer);
        return sb.toAttributedString();
    }

    private void highlightBuffer(AttributedStringBuilder sb, String buffer) {
        // Find the segments separated by operators
        int i = 0;
        boolean firstWordInSegment = true;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        StringBuilder word = new StringBuilder();

        while (i < buffer.length()) {
            char c = buffer.charAt(i);

            // Handle quotes
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                word.append(c);
                i++;
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                word.append(c);
                i++;
                continue;
            }

            if (inSingleQuote || inDoubleQuote) {
                word.append(c);
                i++;
                continue;
            }

            // Check for operators
            String op = matchOperator(buffer, i);
            if (op != null) {
                // Flush accumulated word
                if (word.length() > 0) {
                    flushWord(sb, word.toString(), firstWordInSegment);
                    word.setLength(0);
                }
                // Highlight operator
                sb.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN), op);
                i += op.length();
                firstWordInSegment = true;
                continue;
            }

            // Handle whitespace
            if (Character.isWhitespace(c)) {
                if (word.length() > 0) {
                    flushWord(sb, word.toString(), firstWordInSegment);
                    word.setLength(0);
                    firstWordInSegment = false;
                }
                sb.append(c);
                i++;
                continue;
            }

            word.append(c);
            i++;
        }

        // Flush remaining
        if (word.length() > 0) {
            flushWord(sb, word.toString(), firstWordInSegment);
        }
    }

    private void flushWord(AttributedStringBuilder sb, String word, boolean isCommand) {
        if (isCommand) {
            if (dispatcher.findCommand(word) != null) {
                sb.styled(AttributedStyle.BOLD, word);
            } else {
                sb.styled(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED), word);
            }
        } else {
            sb.append(word);
        }
    }

    private String matchOperator(String line, int pos) {
        // Check two-character operators first
        if (pos + 1 < line.length()) {
            String two = line.substring(pos, pos + 2);
            if (two.equals(">>") || two.equals("&&") || two.equals("||") || two.equals("|;")) {
                return two;
            }
        }
        // Single-character operators
        char c = line.charAt(pos);
        if (c == '|' || c == '>' || c == ';') {
            return String.valueOf(c);
        }
        return null;
    }

    @Override
    public void setErrorPattern(Pattern errorPattern) {
        if (delegate != null) {
            delegate.setErrorPattern(errorPattern);
        }
    }

    @Override
    public void setErrorIndex(int errorIndex) {
        if (delegate != null) {
            delegate.setErrorIndex(errorIndex);
        }
    }
}

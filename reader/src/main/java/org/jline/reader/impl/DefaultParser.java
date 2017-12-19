/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.Parser.ParseContext;

public class DefaultParser implements Parser {

    private char[] quoteChars = {'\'', '"'};

    private char[] escapeChars = {'\\'};

    private boolean eofOnUnclosedQuote;

    private boolean eofOnEscapedNewLine;

    //
    // Chainable setters
    //

    public DefaultParser quoteChars(final char[] chars) {
        this.quoteChars = chars;
        return this;
    }

    public DefaultParser escapeChars(final char[] chars) {
        this.escapeChars = chars;
        return this;
    }

    public DefaultParser eofOnUnclosedQuote(boolean eofOnUnclosedQuote) {
        this.eofOnUnclosedQuote = eofOnUnclosedQuote;
        return this;
    }

    public DefaultParser eofOnEscapedNewLine(boolean eofOnEscapedNewLine) {
        this.eofOnEscapedNewLine = eofOnEscapedNewLine;
        return this;
    }

    //
    // Java bean getters and setters
    //

    public void setQuoteChars(final char[] chars) {
        this.quoteChars = chars;
    }

    public char[] getQuoteChars() {
        return this.quoteChars;
    }

    public void setEscapeChars(final char[] chars) {
        this.escapeChars = chars;
    }

    public char[] getEscapeChars() {
        return this.escapeChars;
    }

    public void setEofOnUnclosedQuote(boolean eofOnUnclosedQuote) {
        this.eofOnUnclosedQuote = eofOnUnclosedQuote;
    }

    public boolean isEofOnUnclosedQuote() {
        return eofOnUnclosedQuote;
    }

    public void setEofOnEscapedNewLine(boolean eofOnEscapedNewLine) {
        this.eofOnEscapedNewLine = eofOnEscapedNewLine;
    }

    public boolean isEofOnEscapedNewLine() {
        return eofOnEscapedNewLine;
    }

    public ParsedLine parse(final String line, final int cursor, ParseContext context) {
        List<String> words = new LinkedList<>();
        StringBuilder current = new StringBuilder();
        int wordCursor = -1;
        int wordIndex = -1;
        int quoteStart = -1;

        for (int i = 0; (line != null) && (i < line.length()); i++) {
            // once we reach the cursor, set the
            // position of the selected index
            if (i == cursor) {
                wordIndex = words.size();
                // the position in the current argument is just the
                // length of the current argument
                wordCursor = current.length();
            }

            if (quoteStart < 0 && isQuoteChar(line, i)) {
                // Start a quote block
                quoteStart = i;
            } else if (quoteStart >= 0) {
                // In a quote block
                if (line.charAt(quoteStart) == line.charAt(i) && !isEscaped(line, i)) {
                    // End the block; arg could be empty, but that's fine
                    words.add(current.toString());
                    current.setLength(0);
                    quoteStart = -1;
                } else if (!isEscapeChar(line, i)) {
                    // Take the next character
                    current.append(line.charAt(i));
                }
            } else {
                // Not in a quote block
                if (isDelimiter(line, i)) {
                    if (current.length() > 0) {
                        words.add(current.toString());
                        current.setLength(0); // reset the arg
                    }
                } else if (!isEscapeChar(line, i)) {
                    current.append(line.charAt(i));
                }
            }
        }

        if (current.length() > 0 || cursor == line.length()) {
            words.add(current.toString());
        }

        if (cursor == line.length()) {
            wordIndex = words.size() - 1;
            wordCursor = words.get(words.size() - 1).length();
        }

        if (eofOnEscapedNewLine && isEscapeChar(line, line.length() - 1)) {
            throw new EOFError(-1, -1, "Escaped new line", "newline");
        }
        if (eofOnUnclosedQuote && quoteStart >= 0) {
            throw new EOFError(-1, -1, "Missing closing quote", line.charAt(quoteStart) == '\''
                    ? "quote" : "dquote");
        }

        return new ArgumentList(line, words, wordIndex, wordCursor, cursor);
    }

    /**
     * Returns true if the specified character is a whitespace parameter. Check to ensure that the character is not
     * escaped by any of {@link #getQuoteChars}, and is not escaped by ant of the {@link #getEscapeChars}, and
     * returns true from {@link #isDelimiterChar}.
     *
     * @param buffer    The complete command buffer
     * @param pos       The index of the character in the buffer
     * @return          True if the character should be a delimiter
     */
    public boolean isDelimiter(final CharSequence buffer, final int pos) {
        return !isQuoted(buffer, pos) && !isEscaped(buffer, pos) && isDelimiterChar(buffer, pos);
    }

    public boolean isQuoted(final CharSequence buffer, final int pos) {
        return false;
    }

    public boolean isQuoteChar(final CharSequence buffer, final int pos) {
        if (pos < 0) {
            return false;
        }

        for (int i = 0; (quoteChars != null) && (i < quoteChars.length); i++) {
            if (buffer.charAt(pos) == quoteChars[i]) {
                return !isEscaped(buffer, pos);
            }
        }

        return false;
    }

    /**
     * Check if this character is a valid escape char (i.e. one that has not been escaped)
     */
    public boolean isEscapeChar(final CharSequence buffer, final int pos) {
        if (pos < 0) {
            return false;
        }

        for (int i = 0; (escapeChars != null) && (i < escapeChars.length); i++) {
            if (buffer.charAt(pos) == escapeChars[i]) {
                return !isEscaped(buffer, pos); // escape escape
            }
        }

        return false;
    }

    /**
     * Check if a character is escaped (i.e. if the previous character is an escape)
     *
     * @param buffer
     *          the buffer to check in
     * @param pos
     *          the position of the character to check
     * @return true if the character at the specified position in the given buffer is an escape character and the character immediately preceding it is not an
     *         escape character.
     */
    public boolean isEscaped(final CharSequence buffer, final int pos) {
        if (pos <= 0) {
            return false;
        }

        return isEscapeChar(buffer, pos - 1);
    }

    /**
     * Returns true if the character at the specified position if a delimiter. This method will only be called if
     * the character is not enclosed in any of the {@link #getQuoteChars}, and is not escaped by ant of the
     * {@link #getEscapeChars}. To perform escaping manually, override {@link #isDelimiter} instead.
     */
    public boolean isDelimiterChar(CharSequence buffer, int pos) {
        return Character.isWhitespace(buffer.charAt(pos));
    }

    /**
     * The result of a delimited buffer.
     *
     * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
     */
    public static class ArgumentList implements ParsedLine
    {
        private final String line;

        private final List<String> words;

        private final int wordIndex;

        private final int wordCursor;

        private final int cursor;

        public ArgumentList(final String line, final List<String> words, final int wordIndex, final int wordCursor, final int cursor) {
            this.line = line;
            this.words = Collections.unmodifiableList(Objects.requireNonNull(words));
            this.wordIndex = wordIndex;
            this.wordCursor = wordCursor;
            this.cursor = cursor;
        }

        public int wordIndex() {
            return this.wordIndex;
        }

        public String word() {
            // TODO: word() should always be contained in words()
            if ((wordIndex < 0) || (wordIndex >= words.size())) {
                return "";
            }
            return words.get(wordIndex);
        }

        public int wordCursor() {
            return this.wordCursor;
        }

        public List<String> words() {
            return this.words;
        }

        public int cursor() {
            return this.cursor;
        }

        public String line() {
            return line;
        }

    }

}

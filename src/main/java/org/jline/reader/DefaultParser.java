/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.jline.utils.Preconditions.checkNotNull;

public class DefaultParser implements Parser {

    private char[] quoteChars = {'\'', '"'};

    private char[] escapeChars = {'\\'};

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

    public ParsedLine parse(final String buffer, final int cursor) {
        List<String> args = new LinkedList<>();
        StringBuilder arg = new StringBuilder();
        int argpos = -1;
        int bindex = -1;
        int quoteStart = -1;

        for (int i = 0; (buffer != null) && (i < buffer.length()); i++) {
            // once we reach the cursor, set the
            // position of the selected index
            if (i == cursor) {
                bindex = args.size();
                // the position in the current argument is just the
                // length of the current argument
                argpos = arg.length();
            }

            if (quoteStart < 0 && isQuoteChar(buffer, i)) {
                // Start a quote block
                quoteStart = i;
            } else if (quoteStart >= 0) {
                // In a quote block
                if (buffer.charAt(quoteStart) == buffer.charAt(i) && !isEscaped(buffer, i)) {
                    // End the block; arg could be empty, but that's fine
                    args.add(arg.toString());
                    arg.setLength(0);
                    quoteStart = -1;
                } else if (!isEscapeChar(buffer, i)) {
                    // Take the next character
                    arg.append(buffer.charAt(i));
                }
            } else {
                // Not in a quote block
                if (isDelimiter(buffer, i)) {
                    if (arg.length() > 0) {
                        args.add(arg.toString());
                        arg.setLength(0); // reset the arg
                    }
                } else if (!isEscapeChar(buffer, i)) {
                    arg.append(buffer.charAt(i));
                }
            }
        }

        if (cursor == buffer.length()) {
            bindex = args.size();
            // the position in the current argument is just the
            // length of the current argument
            argpos = arg.length();
        }
        if (arg.length() > 0) {
            args.add(arg.toString());
        }

        return new ArgumentList(
                buffer, args, bindex, argpos, cursor,
                quoteStart < 0,
                quoteStart < 0
                        ? null
                        : buffer.charAt(quoteStart) == '\''
                                ? "quote" : "dquote");
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

        private final List<String> arguments;

        private final int cursorArgumentIndex;

        private final int argumentPosition;

        private final int bufferPosition;

        private final boolean complete;

        private final String missingPrompt;

        /**
         * @param arguments             The array of tokens
         * @param cursorArgumentIndex   The token index of the cursor
         * @param argumentPosition      The position of the cursor in the current token
         * @param bufferPosition        The position of the cursor in the whole buffer
         * @param complete
         * @param missingPrompt
         */
        public ArgumentList(final String line, final List<String> arguments, final int cursorArgumentIndex, final int argumentPosition, final int bufferPosition, boolean complete, String missingPrompt) {
            this.line = line;
            this.arguments = Collections.unmodifiableList(checkNotNull(arguments));
            this.cursorArgumentIndex = cursorArgumentIndex;
            this.argumentPosition = argumentPosition;
            this.bufferPosition = bufferPosition;
            this.complete = complete;
            this.missingPrompt = missingPrompt;
        }

        public int wordIndex() {
            return this.cursorArgumentIndex;
        }

        public String word() {
            if ((cursorArgumentIndex < 0) || (cursorArgumentIndex >= arguments.size())) {
                return null;
            }
            return arguments.get(cursorArgumentIndex);
        }

        public int wordCursor() {
            return this.argumentPosition;
        }

        public List<String> words() {
            return this.arguments;
        }

        public int cursor() {
            return this.bufferPosition;
        }

        public String line() {
            return line;
        }

        @Override
        public boolean complete() {
            return complete;
        }

        @Override
        public String missingPrompt() {
            return missingPrompt;
        }
    }

}

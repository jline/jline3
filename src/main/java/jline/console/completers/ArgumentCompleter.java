/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.console.completers;

import jline.console.Completer;
import jline.console.ConsoleReader;

import java.util.LinkedList;
import java.util.List;

/**
 * A {@link Completer} implementation that invokes a child completor
 * using the appropriate <i>separator</i> argument. This
 * can be used instead of the individual completors having to
 * know about argument parsing semantics.
 *
 * <p>
 * <strong>Example 1</strong>: Any argument of the command line can
 * use file completion.
 * <p>
 * <pre>
 *        consoleReader.addCompletor (new ArgumentCompletor (
 *                new {@link FileNameCompleter} ()))
 *  </pre>
 * <p>
 * <strong>Example 2</strong>: The first argument of the command line
 * can be completed with any of "foo", "bar", or "baz", and remaining
 * arguments can be completed with a file name.
 * <p>
 * <pre>
 *        consoleReader.addCompletor (new ArgumentCompletor (
 *                new {@link SimpleCompleter} (new String [] { "foo", "bar", "baz"})));
 *        consoleReader.addCompletor (new ArgumentCompletor (
 *                new {@link FileNameCompleter} ()));
 *  </pre>
 * <p/>
 * <p>
 * When the argument index is past the last embedded completors, the last
 * completors is always used. To disable this behavior, have the last
 * completor be a {@link NullCompleter}. For example:
 * </p>
 * <p/>
 * <pre>
 *        consoleReader.addCompletor (new ArgumentCompletor (
 *                new {@link SimpleCompleter} (new String [] { "foo", "bar", "baz"}),
 *                new {@link SimpleCompleter} (new String [] { "xxx", "yyy", "xxx"}),
 *                new {@link NullCompleter}
 *                ));
 *        </pre>
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 *
 * @since 2.0
 */
public class ArgumentCompleter
    implements Completer
{
    // TODO: handle argument quoting and escape characters
    
    private final Completer[] completers;

    private final ArgumentDelimiter delim;

    private boolean strict = true;

    /**
     * Constuctor: create a new completor with the specified
     * argument delimiter.
     *
     * @param delim      the delimiter for parsing arguments
     * @param completers the embedded completors
     */
    public ArgumentCompleter(final ArgumentDelimiter delim, final Completer... completers) {
        this.completers = completers;
        this.delim = delim;
    }

    /**
     * Constuctor: create a new completor with the default
     * argument separator of " ".
     *
     * @param completers the embedded argument completors
     */
    public ArgumentCompleter(final Completer... completers) {
        this(new WhitespaceArgumentDelimiter(), completers);
    }

    /**
     * Constuctor: create a new completor with the default
     * argument separator of " ".
     *
     * @param completors the List of completors to use
     */
    public ArgumentCompleter(final List<Completer> completors) {
        this(completors.toArray(new Completer[completors.size()]));
    }
    
    /**
     * If true, a completion at argument index N will only succeed
     * if all the completions from 0-(N-1) also succeed.
     */
    public void setStrict(final boolean strict) {
        this.strict = strict;
    }

    /**
     * Returns whether a completion at argument index N will succees
     * if all the completions from arguments 0-(N-1) also succeed.
     */
    public boolean getStrict() {
        return this.strict;
    }

    public int complete(final String buffer, final int cursor, final List<String> candidates) {
        ArgumentList list = delim.delimit(buffer, cursor);
        int argpos = list.getArgumentPosition();
        int argIndex = list.getCursorArgumentIndex();

        if (argIndex < 0) {
            return -1;
        }

        final Completer comp;

        // if we are beyond the end of the completers, just use the last one
        if (argIndex >= completers.length) {
            comp = completers[completers.length - 1];
        }
        else {
            comp = completers[argIndex];
        }

        // ensure that all the previous completers are successful before
        // allowing this completor to pass (only if strict is true).
        for (int i = 0; getStrict() && (i < argIndex); i++) {
            Completer sub = completers[(i >= completers.length) ? (completers.length - 1) : i];
            String[] args = list.getArguments();
            String arg = ((args == null) || (i >= args.length)) ? "" : args[i];

            List<String> subCandidates = new LinkedList<String>();

            if (sub.complete(arg, arg.length(), subCandidates) == -1) {
                return -1;
            }

            if (subCandidates.size() == 0) {
                return -1;
            }
        }

        int ret = comp.complete(list.getCursorArgument(), argpos, candidates);

        if (ret == -1) {
            return -1;
        }

        int pos = ret + (list.getBufferPosition() - argpos);

        /**
         *  Special case: when completing in the middle of a line, and the
         *  area under the cursor is a delimiter, then trim any delimiters
         *  from the candidates, since we do not need to have an extra
         *  delimiter.
         *
         *  E.g., if we have a completion for "foo", and we
         *  enter "f bar" into the buffer, and move to after the "f"
         *  and hit TAB, we want "foo bar" instead of "foo  bar".
         */
        if ((cursor != buffer.length()) && delim.isDelimiter(buffer, cursor)) {
            for (int i = 0; i < candidates.size(); i++) {
                String val = candidates.get(i);

                while ((val.length() > 0)
                    && delim.isDelimiter(val, val.length() - 1)) {
                    val = val.substring(0, val.length() - 1);
                }

                candidates.set(i, val);
            }
        }

        // FIXME: Get ride of this string concat
        ConsoleReader.debug("Completing " + buffer + "(pos=" + cursor + ") " + "with: " + candidates + ": offset=" + pos);

        return pos;
    }

    /**
     * The {@link ArgumentCompleter.ArgumentDelimiter} allows custom
     * breaking up of a {@link String} into individual arguments in
     * order to dispatch the arguments to the nested {@link Completer}.
     *
     * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
     */
    public static interface ArgumentDelimiter
    {
        /**
         * Break the specified buffer into individual tokens
         * that can be completed on their own.
         *
         * @param buffer           the buffer to split
         * @param argumentPosition the current position of the
         *                         cursor in the buffer
         * @return the tokens
         */
        ArgumentList delimit(String buffer, int argumentPosition);

        /**
         * Returns true if the specified character is a whitespace
         * parameter.
         *
         * @param buffer the complete command buffer
         * @param pos    the index of the character in the buffer
         * @return true if the character should be a delimiter
         */
        boolean isDelimiter(String buffer, int pos);
    }

    /**
     * Abstract implementation of a delimiter that uses the
     * {@link #isDelimiter} method to determine if a particular
     * character should be used as a delimiter.
     *
     * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
     */
    public abstract static class AbstractArgumentDelimiter
        implements ArgumentDelimiter
    {
        private char[] quoteChars = { '\'', '"' };

        private char[] escapeChars = { '\\' };

        public void setQuoteChars(final char[] quoteChars) {
            this.quoteChars = quoteChars;
        }

        public char[] getQuoteChars() {
            return this.quoteChars;
        }

        public void setEscapeChars(final char[] escapeChars) {
            this.escapeChars = escapeChars;
        }

        public char[] getEscapeChars() {
            return this.escapeChars;
        }

        public ArgumentList delimit(final String buffer, final int cursor) {
            List<String> args = new LinkedList<String>();
            StringBuilder arg = new StringBuilder();
            int argpos = -1;
            int bindex = -1;

            for (int i = 0; (buffer != null) && (i <= buffer.length()); i++) {
                // once we reach the cursor, set the
                // position of the selected index
                if (i == cursor) {
                    bindex = args.size();
                    // the position in the current argument is just the
                    // length of the current argument
                    argpos = arg.length();
                }

                if ((i == buffer.length()) || isDelimiter(buffer, i)) {
                    if (arg.length() > 0) {
                        args.add(arg.toString());
                        arg.setLength(0); // reset the arg
                    }
                }
                else {
                    arg.append(buffer.charAt(i));
                }
            }

            return new ArgumentList(args.toArray(new String[args.size()]), bindex, argpos, cursor);
        }

        /**
         * Returns true if the specified character is a whitespace
         * parameter. Check to ensure that the character is not
         * escaped by any of
         * {@link #getQuoteChars}, and is not escaped by ant of the
         * {@link #getEscapeChars}, and returns true from
         * {@link #isDelimiterChar}.
         *
         * @param buffer the complete command buffer
         * @param pos    the index of the character in the buffer
         * @return true if the character should be a delimiter
         */
        public boolean isDelimiter(final String buffer, final int pos) {
            return !isQuoted(buffer, pos) && !isEscaped(buffer, pos) && isDelimiterChar(buffer, pos);

        }

        public boolean isQuoted(final String buffer, final int pos) {
            return false;
        }

        public boolean isEscaped(final String buffer, final int pos) {
            if (pos <= 0) {
                return false;
            }

            for (int i = 0; (escapeChars != null) && (i < escapeChars.length);
                 i++) {
                if (buffer.charAt(pos) == escapeChars[i]) {
                    return !isEscaped(buffer, pos - 1); // escape escape
                }
            }

            return false;
        }

        /**
         * Returns true if the character at the specified position
         * if a delimiter. This method will only be called if the
         * character is not enclosed in any of the
         * {@link #getQuoteChars}, and is not escaped by ant of the
         * {@link #getEscapeChars}. To perform escaping manually,
         * override {@link #isDelimiter} instead.
         */
        public abstract boolean isDelimiterChar(String buffer, int pos);
    }

    /**
     * {@link ArgumentCompleter.ArgumentDelimiter}
     * implementation that counts all
     * whitespace (as reported by {@link Character#isWhitespace})
     * as being a delimiter.
     *
     * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
     */
    public static class WhitespaceArgumentDelimiter
        extends AbstractArgumentDelimiter
    {
        /**
         * The character is a delimiter if it is whitespace, and the
         * preceeding character is not an escape character.
         */
        public boolean isDelimiterChar(String buffer, int pos) {
            return Character.isWhitespace(buffer.charAt(pos));
        }
    }

    /**
     * The result of a delimited buffer.
     *
     * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
     */
    public static class ArgumentList
    {
        private String[] arguments;

        private int cursorArgumentIndex;

        private int argumentPosition;

        private int bufferPosition;

        /**
         * @param arguments           the array of tokens
         * @param cursorArgumentIndex the token index of the cursor
         * @param argumentPosition    the position of the cursor in the
         *                            current token
         * @param bufferPosition      the position of the cursor in
         *                            the whole buffer
         */
        public ArgumentList(final String[] arguments, final int cursorArgumentIndex, final int argumentPosition, final int bufferPosition) {
            this.arguments = arguments;
            this.cursorArgumentIndex = cursorArgumentIndex;
            this.argumentPosition = argumentPosition;
            this.bufferPosition = bufferPosition;
        }

        public void setCursorArgumentIndex(final int cursorArgumentIndex) {
            this.cursorArgumentIndex = cursorArgumentIndex;
        }

        public int getCursorArgumentIndex() {
            return this.cursorArgumentIndex;
        }

        public String getCursorArgument() {
            if ((cursorArgumentIndex < 0) || (cursorArgumentIndex >= arguments.length)) {
                return null;
            }

            return arguments[cursorArgumentIndex];
        }

        public void setArgumentPosition(final int argumentPosition) {
            this.argumentPosition = argumentPosition;
        }

        public int getArgumentPosition() {
            return this.argumentPosition;
        }

        public void setArguments(final String[] arguments) {
            this.arguments = arguments;
        }

        public String[] getArguments() {
            return this.arguments;
        }

        public void setBufferPosition(final int bufferPosition) {
            this.bufferPosition = bufferPosition;
        }

        public int getBufferPosition() {
            return this.bufferPosition;
        }
    }
}

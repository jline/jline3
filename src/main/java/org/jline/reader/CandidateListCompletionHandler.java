/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.io.IOException;
import java.util.List;

import org.jline.utils.AnsiHelper;

/**
 * A {@link CompletionHandler} that deals with multiple distinct completions
 * by outputting the complete list of possibilities to the console. This
 * mimics the behavior of the
 * <a href="http://www.gnu.org/directory/readline.html">readline</a> library.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class CandidateListCompletionHandler
    implements CompletionHandler
{
    private boolean printSpaceAfterFullCompletion = true;
    private boolean stripAnsi;

    public boolean getPrintSpaceAfterFullCompletion() {
        return printSpaceAfterFullCompletion;
    }

    public void setPrintSpaceAfterFullCompletion(boolean printSpaceAfterFullCompletion) {
        this.printSpaceAfterFullCompletion = printSpaceAfterFullCompletion;
    }

    public boolean isStripAnsi() {
        return stripAnsi;
    }

    public void setStripAnsi(boolean stripAnsi) {
        this.stripAnsi = stripAnsi;
    }

    // TODO: handle quotes and escaped quotes && enable automatic escaping of whitespace

    public boolean complete(final ConsoleReaderImpl reader, final List<String> candidates, final int pos) throws
        IOException
    {
        CursorBuffer buf = reader.getCursorBuffer();

        // if there is only one completion, then fill in the buffer
        if (candidates.size() == 1) {
            String value = AnsiHelper.strip(candidates.get(0));

            if (buf.cursor == buf.buffer.length()
                    && printSpaceAfterFullCompletion
                    && !value.endsWith(" ")) {
                value += " ";
            }

            // fail if the only candidate is the same as the current buffer
            if (value.equals(buf.toString())) {
                return false;
            }

            setBuffer(reader, value, pos);

            return true;
        }
        else if (candidates.size() > 1) {
            String value = getUnambiguousCompletions(candidates);
            if (!buf.buffer.substring(pos, buf.cursor).equals(value)) {
                setBuffer(reader, value, pos);
            } else {
                reader.printCandidates(candidates);
            }
            return true;
        }
        else {
            return false;
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public static void setBuffer(final ConsoleReaderImpl reader, final CharSequence value, final int offset) throws
        IOException
    {
        while ((reader.getCursorBuffer().cursor > offset) && reader.backspace()) {
            // empty
        }

        reader.putString(value);
        reader.setCursorPosition(offset + value.length());
    }

    /**
     * Returns a root that matches all the {@link String} elements of the specified {@link List},
     * or null if there are no commonalities. For example, if the list contains
     * <i>foobar</i>, <i>foobaz</i>, <i>foobuz</i>, the method will return <i>foob</i>.
     */
    private String getUnambiguousCompletions(final List<String> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // convert to an array for speed
        String first = null;
        String[] strings = new String[candidates.size() - 1];
        for (int i = 0; i < candidates.size(); i++) {
            String str = candidates.get(i);
            if (stripAnsi) {
                str = AnsiHelper.strip(str);
            }
            if (first == null) {
                first = str;
            } else {
                strings[i - 1] = str;
            }
        }

        StringBuilder candidate = new StringBuilder();

        for (int i = 0; i < first.length(); i++) {
            if (startsWith(first.substring(0, i + 1), strings)) {
                candidate.append(first.charAt(i));
            }
            else {
                break;
            }
        }

        return candidate.toString();
    }

    /**
     * @return true is all the elements of <i>candidates</i> start with <i>starts</i>
     */
    private boolean startsWith(final String starts, final String[] candidates) {
        for (String candidate : candidates) {
            if (!candidate.startsWith(starts)) {
                return false;
            }
        }

        return true;
    }

}

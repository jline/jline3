/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.console;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * A {@link CompletionHandler} that deals with multiple distinct completions
 * by outputting the complete list of possibilities to the console. This
 * mimics the behavior of the
 * <a href="http://www.gnu.org/directory/readline.html">readline</a>
 * library.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 *
 * @since 2.0
 */
public class CandidateListCompletionHandler
    implements CompletionHandler
{
    // TODO: handle quotes and escaped quotes
    //       enable automatic escaping of whitespace

    private static ResourceBundle loc = ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName());

    private boolean eagerNewlines = true;

    public void setAlwaysIncludeNewline(boolean eagerNewlines) {
        this.eagerNewlines = eagerNewlines;
    }

    public boolean complete(final ConsoleReader reader, final List<String> candidates, final int pos) throws IOException {
        CursorBuffer buf = reader.getCursorBuffer();

        // if there is only one completion, then fill in the buffer
        if (candidates.size() == 1) {
            String value = candidates.get(0);

            // fail if the only candidate is the same as the current buffer
            if (value.equals(buf.toString())) {
                return false;
            }

            setBuffer(reader, value, pos);

            return true;
        }
        else if (candidates.size() > 1) {
            String value = getUnambiguousCompletions(candidates);
            String bufString = buf.toString();
            setBuffer(reader, value, pos);
        }

        if (eagerNewlines) {
            reader.printNewline();
        }
        printCandidates(reader, candidates, eagerNewlines);

        // redraw the current console buffer
        reader.drawLine();

        return true;
    }

    public static void setBuffer(ConsoleReader reader, String value, int offset) throws IOException {
        while ((reader.getCursorBuffer().cursor > offset) && reader.backspace()) {
            // empty
        }

        reader.putString(value);
        reader.setCursorPosition(offset + value.length());
    }

    /**
     * Print out the candidates. If the size of the candidates
     * is greated than the {@link getAutoprintThreshhold},
     * they prompt with aq warning.
     *
     * @param candidates the list of candidates to print
     */
    public static void printCandidates(ConsoleReader reader, Collection<String> candidates, boolean eagerNewlines) throws IOException {
        Set distinct = new HashSet<String>(candidates);

        if (distinct.size() > reader.getAutoprintThreshhold()) {
            if (!eagerNewlines) {
                reader.printNewline();
            }
            reader.printString(MessageFormat.format(loc.getString("display-candidates"), candidates.size()) + " ");
            reader.flushConsole();

            int c;

            String noOpt = loc.getString("display-candidates-no");
            String yesOpt = loc.getString("display-candidates-yes");

            while ((c = reader.readCharacter(new char[]{
                yesOpt.charAt(0), noOpt.charAt(0)})) != -1) {
                if (noOpt.startsWith
                    (new String(new char[]{(char) c}))) {
                    reader.printNewline();
                    return;
                }
                else if (yesOpt.startsWith
                    (new String(new char[]{(char) c}))) {
                    break;
                }
                else {
                    reader.beep();
                }
            }
        }

        // copy the values and make them distinct, without otherwise
        // affecting the ordering. Only do it if the sizes differ.
        if (distinct.size() != candidates.size()) {
            Collection<String> copy = new ArrayList<String>();

            for (String next : candidates) {
                if (!(copy.contains(next))) {
                    copy.add(next);
                }
            }

            candidates = copy;
        }

        reader.printNewline();
        reader.printColumns(candidates);
    }

    /**
     * Returns a root that matches all the {@link String} elements
     * of the specified {@link List}, or null if there are
     * no commalities. For example, if the list contains
     * <i>foobar</i>, <i>foobaz</i>, <i>foobuz</i>, the
     * method will return <i>foob</i>.
     */
    private String getUnambiguousCompletions(final List<String> candidates) {
        if ((candidates == null) || (candidates.size() == 0)) {
            return null;
        }

        // convert to an array for speed
        String[] strings = candidates.toArray(new String[candidates.size()]);

        String first = strings[0];
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
     * @return true is all the elements of <i>candidates</i>
     *         start with <i>starts</i>
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

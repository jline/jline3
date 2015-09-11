/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.completer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jline.Candidate;
import org.jline.Completer;
import org.jline.reader.ParsedLine;

import static org.jline.utils.Preconditions.checkNotNull;

/**
 * A {@link Completer} implementation that invokes a child completer using the appropriate <i>separator</i> argument.
 * This can be used instead of the individual completers having to know about argument parsing semantics.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class ArgumentCompleter
    implements Completer
{
    private final List<Completer> completers = new ArrayList<>();

    private boolean strict = true;

    /**
     * Create a new completer.
     *
     * @param completers    The embedded completers
     */
    public ArgumentCompleter(final Collection<Completer> completers) {
        checkNotNull(completers);
        this.completers.addAll(completers);
    }

    /**
     * Create a new completer.
     *
     * @param completers    The embedded completers
     */
    public ArgumentCompleter(final Completer... completers) {
        this(Arrays.asList(completers));
    }

    /**
     * If true, a completion at argument index N will only succeed
     * if all the completions from 0-(N-1) also succeed.
     */
    public void setStrict(final boolean strict) {
        this.strict = strict;
    }

    /**
     * Returns whether a completion at argument index N will success
     * if all the completions from arguments 0-(N-1) also succeed.
     *
     * @return  True if strict.
     * @since 2.3
     */
    public boolean isStrict() {
        return this.strict;
    }

    /**
     * @since 2.3
     */
    public List<Completer> getCompleters() {
        return completers;
    }

    public int complete(ParsedLine line, final List<Candidate> candidates) {
        checkNotNull(line);
        checkNotNull(candidates);

        if (line.wordIndex() < 0) {
            return -1;
        }

        List<Completer> completers = getCompleters();
        Completer completer;

        // if we are beyond the end of the completers, just use the last one
        if (line.wordIndex() >= completers.size()) {
            completer = completers.get(completers.size() - 1);
        }
        else {
            completer = completers.get(line.wordIndex());
        }

        // ensure that all the previous completers are successful before allowing this completer to pass (only if strict).
        for (int i = 0; isStrict() && (i < line.wordIndex()); i++) {
            Completer sub = completers.get(i >= completers.size() ? (completers.size() - 1) : i);
            List<String> args = line.words();
            String arg = (args == null || i >= args.size()) ? "" : args.get(i);

            List<Candidate> subCandidates = new LinkedList<>();

            if (sub.complete(new ArgumentLine(arg, arg.length()), subCandidates) == -1) {
                return -1;
            }

            boolean found = false;
            for (Candidate cand : subCandidates) {
                if (cand.value().equals(arg)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return -1;
            }
        }

        int ret = completer.complete(line, candidates);

        if (ret == -1) {
            return -1;
        }

        int pos = ret + line.cursor() - line.wordCursor();

        // Special case: when completing in the middle of a line, and the area under the cursor is a delimiter,
        // then trim any delimiters from the candidates, since we do not need to have an extra delimiter.
        //
        // E.g., if we have a completion for "foo", and we enter "f bar" into the buffer, and move to after the "f"
        // and hit TAB, we want "foo bar" instead of "foo  bar".

//        if ((cursor != buffer.length()) && delim.isDelimiter(buffer, cursor)) {
//            for (int i = 0; i < candidates.size(); i++) {
//                Candidate cand = candidates.get(i);
//                String val = cand.value();
//
//                while (val.length() > 0 && delim.isDelimiter(val, val.length() - 1)) {
//                    val = val.substring(0, val.length() - 1);
//                }
//
//                candidates.set(i, cand);
//            }
//        }
//
//        Log.trace("Completing ", buffer, " (pos=", cursor, ") with: ", candidates, ": offset=", pos);

        return pos;
    }

    public static class ArgumentLine implements ParsedLine {
        private final String word;
        private final int cursor;

        public ArgumentLine(String word, int cursor) {
            this.word = word;
            this.cursor = cursor;
        }

        @Override
        public String word() {
            return word;
        }

        @Override
        public int wordCursor() {
            return cursor;
        }

        @Override
        public int wordIndex() {
            return 0;
        }

        @Override
        public List<String> words() {
            return Collections.singletonList(word);
        }

        @Override
        public String line() {
            return word;
        }

        @Override
        public int cursor() {
            return cursor;
        }

        @Override
        public boolean complete() {
            return true;
        }

        @Override
        public String missingPrompt() {
            return null;
        }
    }
}

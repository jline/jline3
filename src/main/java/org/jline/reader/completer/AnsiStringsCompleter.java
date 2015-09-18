/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.completer;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jline.Candidate;
import org.jline.Completer;
import org.jline.reader.ParsedLine;
import org.jline.utils.AnsiHelper;

import static org.jline.utils.Preconditions.checkNotNull;

/**
 * Completer for a set of strings.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class AnsiStringsCompleter
    implements Completer
{
    private final SortedMap<String, String> strings = new TreeMap<String, String>();

    public AnsiStringsCompleter() {
        // empty
    }

    public AnsiStringsCompleter(final Collection<String> strings) {
        checkNotNull(strings);
        for (String str : strings) {
            this.strings.put(AnsiHelper.strip(str), str);
        }
    }

    public AnsiStringsCompleter(final String... strings) {
        this(Arrays.asList(strings));
    }

    public Collection<String> getStrings() {
        return strings.values();
    }

    public int complete(ParsedLine line, final List<Candidate> candidates) {
        checkNotNull(line);
        checkNotNull(candidates);

        String buffer = line.word().substring(0, line.wordCursor());
        buffer = AnsiHelper.strip(buffer);
        for (Map.Entry<String, String> match : strings.tailMap(buffer).entrySet()) {
            if (!match.getKey().startsWith(buffer)) {
                break;
            }
            candidates.add(new Candidate(match.getValue()));
        }

        return candidates.isEmpty() ? -1 : line.cursor() - line.wordCursor();
    }
}
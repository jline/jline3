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
import java.util.SortedSet;
import java.util.TreeSet;

import org.jline.Candidate;
import org.jline.Completer;
import org.jline.reader.ParsedLine;

import static org.jline.utils.Preconditions.checkNotNull;

/**
 * Completer for a set of strings.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class StringsCompleter
    implements Completer
{
    private final SortedSet<String> strings = new TreeSet<String>();

    public StringsCompleter() {
        // empty
    }

    public StringsCompleter(final Collection<String> strings) {
        checkNotNull(strings);
        getStrings().addAll(strings);
    }

    public StringsCompleter(final String... strings) {
        this(Arrays.asList(strings));
    }

    public Collection<String> getStrings() {
        return strings;
    }

    public int complete(final ParsedLine line, final List<Candidate> candidates) {
        checkNotNull(line);
        checkNotNull(candidates);
        strings.forEach(s -> candidates.add(new Candidate(s)));
        return 0;
    }
}
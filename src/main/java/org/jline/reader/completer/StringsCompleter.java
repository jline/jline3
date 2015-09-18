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
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

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
public class StringsCompleter implements Completer
{
    protected final Collection<Candidate> candidates = new ArrayList<>();

    public StringsCompleter() {
    }

    public StringsCompleter(String... strings) {
        this(Arrays.asList(strings));
    }

    public StringsCompleter(Iterable<String> strings) {
        assert strings != null;
        for (String string : strings) {
            candidates.add(new Candidate(AnsiHelper.strip(string), string, null, null, true));
        }
    }

    public void complete(final ParsedLine commandLine, final List<Candidate> candidates) {
        assert commandLine != null;
        assert candidates != null;
        candidates.addAll(this.candidates);
    }

}
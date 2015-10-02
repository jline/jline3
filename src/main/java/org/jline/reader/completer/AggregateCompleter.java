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

import org.jline.Candidate;
import org.jline.Completer;
import org.jline.ConsoleReader;
import org.jline.reader.ParsedLine;

import static org.jline.utils.Preconditions.checkNotNull;

/**
 * Completer which contains multiple completers and aggregates them together.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public class AggregateCompleter
    implements Completer
{
    private final Collection<Completer> completers;

    /**
     * Construct an AggregateCompleter with the given completers.
     * The completers will be used in the order given.
     *
     * @param completers the completers
     */
    public AggregateCompleter(final Completer... completers) {
        this(Arrays.asList(completers));
    }

    /**
     * Construct an AggregateCompleter with the given completers.
     * The completers will be used in the order given.
     *
     * @param completers the completers
     */
    public AggregateCompleter(Collection<Completer> completers) {
        assert completers != null;
        this.completers = completers;
    }

    /**
     * Retrieve the collection of completers currently being aggregated.
     *
     * @return the aggregated completers
     */
    public Collection<Completer> getCompleters() {
        return completers;
    }

    /**
     * Perform a completion operation across all aggregated completers.
     *
     * @see Completer#complete(ConsoleReader, ParsedLine, List)
     */
    public void complete(ConsoleReader reader, final ParsedLine line, final List<Candidate> candidates) {
        checkNotNull(line);
        checkNotNull(candidates);

        for (Completer completer : completers) {
            completer.complete(reader, line, candidates);
        }
    }

    /**
     * @return a string representing the aggregated completers
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
            "completers=" + completers +
            '}';
    }

}

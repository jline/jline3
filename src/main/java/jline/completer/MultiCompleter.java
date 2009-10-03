/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.completer;

import jline.completer.Completer;

import java.util.*;

/**
 * <p>
 * A completor that contains multiple embedded completors. This differs
 * from the {@link ArgumentCompleter}, in that the nested completors
 * are dispatched individually, rather than delimited by arguments.
 * </p>
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class MultiCompleter implements Completer {
    Completer[] completers = new Completer[0];

    /**
     * Construct a MultiCompletor with no embedded completors.
     */
    public MultiCompleter() {
        this(new Completer[0]);
    }

    /**
     * Construct a MultiCompletor with the specified list of
     * {@link Completer} instances.
     */
    public MultiCompleter(final List completors) {
        this((Completer[]) completors.toArray(new Completer[completors.size()]));
    }

    /**
     * Construct a MultiCompletor with the specified
     * {@link Completer} instances.
     */
    public MultiCompleter(final Completer[] completers) {
        this.completers = completers;
    }

    public int complete(final String buffer, final int pos, final List cand) {
        int[] positions = new int[completers.length];
        List[] copies = new List[completers.length];

        for (int i = 0; i < completers.length; i++) {
            // clone and save the candidate list
            copies[i] = new LinkedList(cand);
            positions[i] = completers[i].complete(buffer, pos, copies[i]);
        }

        int maxposition = -1;

        for (int i = 0; i < positions.length; i++) {
            maxposition = Math.max(maxposition, positions[i]);
        }

        // now we have the max cursor value: build up all the
        // candidate lists that have the same cursor value
        for (int i = 0; i < copies.length; i++) {
            if (positions[i] == maxposition) {
                cand.addAll(copies[i]);
            }
        }

        return maxposition;
    }

    public void setCompletors(final Completer[] completers) {
        this.completers = completers;
    }

    public Completer[] getCompletors() {
        return this.completers;
    }
}

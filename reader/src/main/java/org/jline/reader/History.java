/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.io.IOException;
import java.time.Instant;
import java.util.ListIterator;

/**
 * Console history.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public interface History extends Iterable<History.Entry>
{

    /**
     * Initialize the history for the given reader.
     */
    void attach(LineReader reader);

    /**
     * Load history.
     */
    void load() throws IOException;

    /**
     * Save history.
     */
    void save() throws IOException;

    /**
     * Purge history.
     */
    void purge() throws IOException;


    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    int index();

    int first();

    int last();

    String get(int index);

    default void add(String line) {
        add(Instant.now(), line);
    }

    void add(Instant time, String line);

    //
    // Entries
    //
    
    interface Entry
    {
        int index();

        Instant time();

        String line();
    }

    ListIterator<Entry> iterator(int index);

    default ListIterator<Entry> iterator() {
        return iterator(first());
    }

    //
    // Navigation
    //

    /**
     * Return the content of the current buffer.
     */
    String current();

    /**
     * Move the pointer to the previous element in the buffer.
     *
     * @return true if we successfully went to the previous element
     */
    boolean previous();

    /**
     * Move the pointer to the next element in the buffer.
     *
     * @return true if we successfully went to the next element
     */
    boolean next();

    /**
     * Moves the history index to the first entry.
     *
     * @return Return false if there are no iterator in the history or if the
     * history is already at the beginning.
     */
    boolean moveToFirst();

    /**
     * This moves the history to the last entry. This entry is one position
     * before the moveToEnd() position.
     *
     * @return Returns false if there were no history iterator or the history
     * index was already at the last entry.
     */
    boolean moveToLast();

    /**
     * Move to the specified index in the history
     */
    boolean moveTo(int index);

    /**
     * Move to the end of the history buffer. This will be a blank entry, after
     * all of the other iterator.
     */
    void moveToEnd();
}

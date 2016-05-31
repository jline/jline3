/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.io.Flushable;
import java.util.Iterator;
import java.util.ListIterator;

/**
 * Console history.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.3
 */
public interface History
    extends Iterable<History.Entry>, Flushable
{
    int size();

    boolean isEmpty();

    int index();

    int first();

    int last();

    /**
     * Purge storage
     */
    void clear();

    String get(int index);

    void add(String line);

    /**
     * Set the history item at the given index to the given CharSequence.
     *
     * @param index the index of the history offset
     * @param item the new item
     * @since 2.7
     */
    void set(int index, String item);

    /**
     * Remove the history element at the given index.
     *
     * @param i the index of the element to remove
     * @return the removed element
     * @since 2.7
     */
    String remove(int i);

    /**
     * Remove the first element from history.
     *
     * @return the removed element
     * @since 2.7
     */
    String removeFirst();

    /**
     * Remove the last element from history
     *
     * @return the removed element
     * @since 2.7
     */
    String removeLast();

    void replace(String item);

    //
    // Entries
    //
    
    interface Entry
    {
        int index();

        String value();
    }

    ListIterator<Entry> entries(int index);

    ListIterator<Entry> entries();

    Iterator<Entry> iterator();

    //
    // Navigation
    //

    String current();

    boolean previous();

    boolean next();

    boolean moveToFirst();

    boolean moveToLast();

    boolean moveTo(int index);

    void moveToEnd();
}

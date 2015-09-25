/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.util.List;

public interface ParsedLine {

    /**
     * The current word being completed
     */
    CharSequence word();

    /**
     * The cursor position within the current word
     */
    int wordCursor();

    /**
     * The index of the current word in the list of words
     */
    int wordIndex();

    /**
     * The list of words
     */
    List<? extends CharSequence> words();

    /**
     * The unparsed line
     */
    CharSequence line();

    /**
     * The cursor position within the line
     */
    int cursor();

}

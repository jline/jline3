/*
 * Copyright (c) 2002-2016, the original author or authors.
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
     * The current word being completed.
     * If the cursor is after the last word, an empty string is returned.
     *
     * @return the word being completed or an empty string
     */
    String word();

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
    List<String> words();

    /**
     * The unparsed line
     */
    String line();

    /**
     * The cursor position within the line
     */
    int cursor();

}

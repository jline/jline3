/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline;

import java.io.InputStream;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;

public interface ConsoleReader {

    //
    // Variable names
    //

    String BIND_TTY_SPECIAL_CHARS = "bind-tty-special-chars";
    String COMMENT_BEGIN = "comment-begin";
    String BELL_STYLE = "bell-style";
    String PREFER_VISIBLE_BELL = "prefer-visible-bell";
    String COMPLETION_QUERY_ITEMS = "completion-query-items";
    String PAGE_COMPLETIONS = "page-completions";
    String DISABLE_HISTORY = "disable-history";
    String DISABLE_COMPLETION = "disable-completion";
    String EDITING_MODE = "editing-mode";
    String KEYMAP = "keymap";
    String BLINK_MATCHING_PAREN = "blink-matching-paren";
    String DISABLE_EVENT_EXPANSION = "disable-event-expansion";
    /**
     * Set to true if the reader should attempt to detect copy-n-paste. The
     * effect of this that an attempt is made to detect if tab is quickly
     * followed by another character, then it is assumed that the tab was
     * a literal tab as part of a copy-and-paste operation and is inserted as
     * such.
     */
    String COPY_PASTE_DETECTION = "copy-paste-detection";
    /**
     * Timeout for the ESCAPE key.
     * By default, when pressing the ESCAPE key, the next key press will
     * be waited for to find the sequence entered.  If there's no matching
     * sequence, the raw ESCAPE character will be used.
     * When the timeout is defined to a positive value, the raw ESCAPE key
     * will be sent after the timeout elapsed if there's no key pressed
     */
    String ESCAPE_TIMEOUT = "escape-timeout";

    /**
     * Read the next line and return the contents of the buffer.
     *
     * Equivalent to <code>readLine(null, null, null)</code>
     */
    String readLine() throws UserInterruptException, EndOfFileException;

    /**
     * Read the next line with the specified character mask. If null, then
     * characters will be echoed. If 0, then no characters will be echoed.
     *
     * Equivalent to <code>readLine(null, mask, null)</code>
     */
    String readLine(Character mask) throws UserInterruptException, EndOfFileException;

    /**
     * Read the next line with the specified prompt.
     * If null, then the default prompt will be used.
     *
     * Equivalent to <code>readLine(prompt, null, null)</code>
     */
    String readLine(String prompt) throws UserInterruptException, EndOfFileException;

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * Equivalent to <code>readLine(prompt, mask, null)</code>
     */
    String readLine(String prompt, Character mask) throws UserInterruptException, EndOfFileException;

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * @param prompt    The prompt to issue to the console, may be null.
     * @param mask      The character mask, may be null.
     * @param buffer    The default value presented to the user to edit, may be null.
     * @return          A line that is read from the console, can never be null.
     *
     * @throws UserInterruptException if readLine was interrupted (using Ctrl-C for example)
     * @throws EndOfFileException if an EOF has been found (using Ctrl-D for example)
     * @throws java.io.IOError in case of other i/o errors
     */
    String readLine(String prompt, Character mask, String buffer) throws UserInterruptException, EndOfFileException;
}

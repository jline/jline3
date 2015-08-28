/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline;

import java.io.Closeable;
import java.io.EOFException;
import java.io.Flushable;
import java.io.InputStream;
import java.io.PrintWriter;

import org.fusesource.jansi.Pty.Attributes;
import org.fusesource.jansi.Pty.Size;
import org.jline.console.NativeSignalHandler;
import org.jline.reader.UserInterruptException;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;

public interface Console extends Closeable, Flushable {

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

    //
    // Signal support
    //

    enum Signal {
        INT,
        QUIT,
        TSTP,
        CONT,
        INFO,
        WINCH
    }

    interface SignalHandler {

        SignalHandler SIG_DFL = NativeSignalHandler.SIG_DFL;
        SignalHandler SIG_IGN = NativeSignalHandler.SIG_IGN;

        void handle(Signal signal);
    }

    SignalHandler handle(Signal signal, SignalHandler handler);

    void raise(Signal signal);

    //
    // Input / output
    //

    NonBlockingReader reader();

    PrintWriter writer();

    //
    // Pty settings
    //

    Attributes enterRawMode();

    boolean echo();

    boolean echo(boolean echo);

    Attributes getAttributes();

    void setAttributes(Attributes attr);

    void setAttributes(Attributes attr, int actions);

    Size getSize();

    void setSize(Size size);

    //
    // Infocmp capabilities
    //

    String getType();

    boolean puts(Capability capability, Object... params);

    boolean getBooleanCapability(Capability capability);

    Integer getNumericCapability(Capability capability);

    String getStringCapability(Capability capability);

    //
    // Readline
    //

    /**
     * Read the next line and return the contents of the buffer.
     *
     * Equivalent to <code>readLine(null, null, null)</code>
     */
    String readLine() throws UserInterruptException, EOFException;

    /**
     * Read the next line with the specified character mask. If null, then
     * characters will be echoed. If 0, then no characters will be echoed.
     *
     * Equivalent to <code>readLine(null, mask, null)</code>
     */
    String readLine(Character mask) throws UserInterruptException, EOFException;

    /**
     * Read the next line with the specified prompt.
     * If null, then the default prompt will be used.
     *
     * Equivalent to <code>readLine(prompt, null, null)</code>
     */
    String readLine(String prompt) throws UserInterruptException, EOFException;

    /**
     * Read a line from the <i>in</i> {@link InputStream}, and return the line
     * (without any trailing newlines).
     *
     * Equivalent to <code>readLine(prompt, mask, null)</code>
     */
    String readLine(String prompt, Character mask) throws UserInterruptException, EOFException;

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
     * @throws EOFException if an EOF has been found (using Ctrl-D for example)
     * @throws java.io.IOError in case of other i/o errors
     */
    String readLine(String prompt, Character mask, String buffer) throws UserInterruptException, EOFException;

}

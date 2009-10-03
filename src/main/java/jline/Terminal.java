/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline;

import jline.console.ConsoleReader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Representation of the input terminal for a platform. Handles
 * any initialization that the platform may need to perform
 * in order to allow the {@link ConsoleReader} to correctly handle
 * input.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public interface Terminal
{
    /**
     * Returns true if the current console supports ANSI codes.
     */
    boolean isANSISupported();

    /**
     * Read a single character from the input stream. This might
     * enable a terminal implementation to better handle nuances of
     * the console.
     */
    int readCharacter(final InputStream in) throws IOException;


    /**
     * Reads a virtual key from the console. Typically, this will
     * just be the raw character that was entered, but in some cases,
     * multiple input keys will need to be translated into a single
     * virtual key.
     *
     * @param in the InputStream to read from
     * @return the virtual key
     */
    int readVirtualKey(InputStream in) throws IOException;

    /**
     * Initialize any system settings
     * that are required for the console to be able to handle
     * input correctly, such as setting tabtop, buffered input, and
     * character echo.
     */
    void initializeTerminal() throws Exception;

    /**
     * Restore the original terminal configuration, which can be used when
     * shutting down the console reader. The ConsoleReader cannot be
     * used after calling this method.
     */
    void restoreTerminal() throws Exception;

    /**
     * Returns the current width of the terminal (in characters)
     */
    int getTerminalWidth();

    /**
     * Returns the current height of the terminal (in lines)
     */
    int getTerminalHeight();

    /**
     * Returns true if this terminal is capable of initializing the
     * terminal to use jline.
     */
    boolean isSupported();

    /**
     * Returns true if the terminal will echo all characters type.
     */
    boolean getEcho();

    /**
     * Returns false if character echoing is disabled.
     */
    boolean isEchoEnabled();

    /**
     * Enable character echoing. This can be used to re-enable character
     * if the ConsoleReader is no longer being used.
     */
    void enableEcho();

    /**
     * Disable character echoing. This can be used to manually re-enable
     * character if the ConsoleReader has been disabled.
     */
    void disableEcho();

    /**
     * Invokes before the console reads a line with the prompt and mask.
     */
    void beforeReadLine(ConsoleReader reader, String prompt, Character mask);

    /**
     * Invokes after the console reads a line with the prompt and mask.
     */
    void afterReadLine(ConsoleReader reader, String prompt, Character mask);

    InputStream getDefaultBindings();
}

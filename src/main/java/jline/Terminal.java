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
    boolean isSupported();

    boolean isAnsiSupported();

    int readCharacter(final InputStream in) throws IOException;

    int readVirtualKey(InputStream in) throws IOException;

    void init() throws Exception;

    void restore() throws Exception;

    int getWidth();

    int getHeight();

    boolean getEcho();

    boolean isEchoEnabled();

    void enableEcho();

    void disableEcho();

    void beforeReadLine(ConsoleReader reader, String prompt, Character mask);

    void afterReadLine(ConsoleReader reader, String prompt, Character mask);

    InputStream getDefaultBindings();
}

/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline;

import jline.console.ConsoleOperations;
import jline.console.ConsoleReader;

import java.io.IOException;
import java.io.InputStream;

/**
 * ???
 */
public abstract class TerminalSupport
    implements Terminal, ConsoleOperations
{
    public int readCharacter(final InputStream in) throws IOException {
        return in.read();
    }

    public int readVirtualKey(InputStream in) throws IOException {
        return readCharacter(in);
    }

    public void beforeReadLine(ConsoleReader reader, String prompt, Character mask) {
    }

    public void afterReadLine(ConsoleReader reader, String prompt, Character mask) {
    }

    public InputStream getDefaultBindings() {
        return TerminalSupport.class.getResourceAsStream("keybindings.properties");
    }
}
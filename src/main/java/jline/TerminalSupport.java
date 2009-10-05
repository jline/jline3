/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline;

import jline.console.ConsoleReader;
import jline.internal.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides support for {@link Terminal} instances.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public abstract class TerminalSupport
    implements Terminal
{
    public static String DEFAULT_KEYBINDINGS_PROPERTIES = "keybindings.properties";

    public static final int DEFAULT_WIDTH = 80;

    public static final int DEFAULT_HEIGHT = 24;

    private Thread shutdownHook;

    public void init() throws Exception {
        // nothing
    }

    public void restore() throws Exception {
        // nothing
    }

    protected void installShutdownHook(final Thread hook) {
        assert hook != null;

        if (shutdownHook != null) {
            throw new IllegalStateException("Shutdown hook already installed");
        }
        
        try {
            Runtime.getRuntime().addShutdownHook(hook);
            shutdownHook = hook;
        }
        catch (AbstractMethodError e) {
            // JDK 1.3+ only method. Bummer.
            Log.trace("Failed to register shutdown hook: ", e);
        }
    }

    protected void removeShutdownHook() {
        if (shutdownHook != null) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
            }
            catch (AbstractMethodError e) {
                // JDK 1.3+ only method. Bummer.
                Log.trace("Failed to remove shutdown hook: ", e);
            }
            catch (IllegalStateException e) {
                // The VM is shutting down, not a big deal; ignore
            }
            shutdownHook = null;
        }
    }

    public int getWidth() {
        return DEFAULT_WIDTH;
    }

    public int getHeight() {
        return DEFAULT_HEIGHT;
    }

    public void enableEcho() {
        // nothing
    }

    public void disableEcho() {
        // nothing
    }

    public int readCharacter(final InputStream in) throws IOException {
        return in.read();
    }

    public int readVirtualKey(final InputStream in) throws IOException {
        return readCharacter(in);
    }

    public void beforeReadLine(final ConsoleReader reader, final String prompt, final Character mask) {
        // nothing
    }

    public void afterReadLine(final ConsoleReader reader, final String prompt, final Character mask) {
        // nothing
    }

    public InputStream getDefaultBindings() {
        return TerminalSupport.class.getResourceAsStream(DEFAULT_KEYBINDINGS_PROPERTIES);
    }

    //
    // RestoreHook
    //

    protected class RestoreHook
            extends Thread
    {
        public void start() {
            try {
                restore();
            }
            catch (Exception e) {
                Log.trace("Failed to restore: ", e);
            }
        }
    }
}
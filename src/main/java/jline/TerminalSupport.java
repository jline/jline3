/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jline.internal.Log;
import jline.internal.ShutdownHooks;

/**
 * Provides support for {@link Terminal} instances.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public abstract class TerminalSupport
    implements Terminal
{
    public static final int DEFAULT_WIDTH = 80;

    public static final int DEFAULT_HEIGHT = 24;

    private Runnable shutdownTask;

    private boolean supported;

    private boolean echoEnabled;

    private boolean ansiSupported;

    protected TerminalSupport(final boolean supported) {
        this.supported = supported;
    }

    public void init() throws Exception {
        this.shutdownTask = ShutdownHooks.add(new RestoreTask());
    }

    public void restore() throws Exception {
        TerminalFactory.resetIf(this);
        ShutdownHooks.remove(shutdownTask);
    }

    public void reset() throws Exception {
        restore();
        init();
    }

    public final boolean isSupported() {
        return supported;
    }

    public synchronized boolean isAnsiSupported() {
        return ansiSupported;
    }

    protected synchronized void setAnsiSupported(final boolean supported) {
        this.ansiSupported = supported;
        Log.debug("Ansi supported: ", supported);
    }

    /**
     * Subclass to change behavior if needed. 
     * @return the passed out
     */
    public OutputStream wrapOutIfNeeded(OutputStream out) {
        return out;
    }

    /**
     * Defaults to true which was the behaviour before this method was added.
     */
    public boolean hasWeirdWrap() {
        return true;
    }

    public int getWidth() {
        return DEFAULT_WIDTH;
    }

    public int getHeight() {
        return DEFAULT_HEIGHT;
    }

    public synchronized boolean isEchoEnabled() {
        return echoEnabled;
    }

    public synchronized void setEchoEnabled(final boolean enabled) {
        this.echoEnabled = enabled;
        Log.debug("Echo enabled: ", enabled);
    }

    public InputStream wrapInIfNeeded(InputStream in) throws IOException {
        return in;
    }

    //
    // RestoreHook
    //

    /**
     * Invoke {@link Terminal#restore} on shutdown.
     *
     * @see ShutdownHooks
     */
    protected class RestoreTask
        implements Runnable
    {
        public void run() {
            try {
                restore();
            }
            catch (Exception e) {
                Log.trace("Failed to restore: ", e);
            }
        }
    }
}
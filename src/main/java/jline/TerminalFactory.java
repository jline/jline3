/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import jline.internal.Configuration;
import jline.internal.Log;
import static jline.internal.Preconditions.checkNotNull;

/**
 * Creates terminal instances.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public class TerminalFactory
{
    public static final String JLINE_TERMINAL = "jline.terminal";

    public static final String AUTO = "auto";

    public static final String UNIX = "unix";

    public static final String OSV = "osv";

    public static final String WIN = "win";

    public static final String WINDOWS = "windows";

    public static final String NONE = "none";

    public static final String OFF = "off";

    public static final String FALSE = "false";

    private static Terminal term = null;

    public static synchronized Terminal create() {
    	return create(null);
    }
        
    public static synchronized Terminal create(String ttyDevice) {
        if (Log.TRACE) {
            //noinspection ThrowableInstanceNeverThrown
            Log.trace(new Throwable("CREATE MARKER"));
        }

        String type  = Configuration.getString(JLINE_TERMINAL);
        if (type == null) {
            type = AUTO;
            if ("dumb".equals(System.getenv("TERM"))) {
                // emacs communicates with shell through a 'dumb' terminal
                // but sets these env variables to let programs know
                // it is ok to send ANSI control sequences
                String emacs = System.getenv("EMACS");
                String insideEmacs = System.getenv("INSIDE_EMACS");
                if (emacs == null || insideEmacs == null) {
                    type = NONE;
                }
            }
        }

        Log.debug("Creating terminal; type=", type);

        Terminal t;
        try {
            String tmp = type.toLowerCase();

            if (tmp.equals(UNIX)) {
                t = getFlavor(Flavor.UNIX);
            }
            else if (tmp.equals(OSV)) {
                t = getFlavor(Flavor.OSV);
            }
            else if (tmp.equals(WIN) || tmp.equals(WINDOWS)) {
                t = getFlavor(Flavor.WINDOWS);
            }
            else if (tmp.equals(NONE) || tmp.equals(OFF) || tmp.equals(FALSE)) {
                t = new UnsupportedTerminal();
            }
            else {
                if (tmp.equals(AUTO)) {
                    String os = Configuration.getOsName();
                    Flavor flavor = Flavor.UNIX;
                    if (os.contains(WINDOWS)) {
                        flavor = Flavor.WINDOWS;
                    } else if (System.getenv("OSV_CPUS") != null) {
                        flavor = Flavor.OSV;
                    }
                    t = getFlavor(flavor, ttyDevice);
                }
                else {
                    try {
                        t = (Terminal) Thread.currentThread().getContextClassLoader().loadClass(type).newInstance();
                    }
                    catch (Exception e) {
                        throw new IllegalArgumentException(MessageFormat.format("Invalid terminal type: {0}", type), e);
                    }
                }
            }
        }
        catch (Exception e) {
            Log.error("Failed to construct terminal; falling back to unsupported", e);
            t = new UnsupportedTerminal();
        }

        Log.debug("Created Terminal: ", t);

        try {
            t.init();
        }
        catch (Throwable e) {
            Log.error("Terminal initialization failed; falling back to unsupported", e);
            return new UnsupportedTerminal();
        }

        return t;
    }

    public static synchronized void reset() {
        term = null;
    }

    public static synchronized void resetIf(final Terminal t) {
        if(t == term) {
            reset();
        }
    }

    public static enum Type
    {
        AUTO,
        WINDOWS,
        UNIX,
        OSV,
        NONE
    }

    public static synchronized void configure(final String type) {
        checkNotNull(type);
        System.setProperty(JLINE_TERMINAL, type);
    }

    public static synchronized void configure(final Type type) {
        checkNotNull(type);
        configure(type.name().toLowerCase());
    }

    //
    // Flavor Support
    //

    public static enum Flavor
    {
        WINDOWS,
        UNIX,
        OSV
    }

    private static final Map<Flavor, Class<? extends Terminal>> FLAVORS = new HashMap<Flavor, Class<? extends Terminal>>();

    static {
        registerFlavor(Flavor.WINDOWS, AnsiWindowsTerminal.class);
        registerFlavor(Flavor.UNIX, UnixTerminal.class);
        registerFlavor(Flavor.OSV, OSvTerminal.class);
    }

    public static synchronized Terminal get(String ttyDevice) {
    	// The code is assuming we've got only one terminal per process.
    	// Continuing this assumption, if this terminal is already initialized,
    	// we don't check if it's using the same tty line either. Both assumptions
    	// are a bit crude. TODO: check single terminal assumption.
        if (term == null) {
            term = create(ttyDevice);
        }
        return term;
    }
    
    public static synchronized Terminal get() {
        return get(null);
    }

    public static Terminal getFlavor(final Flavor flavor) throws Exception {
    	return getFlavor(flavor, null);
    }
    
    public static Terminal getFlavor(final Flavor flavor, String ttyDevice) throws Exception {
        Class<? extends Terminal> type = FLAVORS.get(flavor);
        Terminal result = null;
        if (type != null) {
        	if (ttyDevice != null) {
        		Constructor<?> ttyDeviceConstructor = type.getConstructor(String.class);
        		if (ttyDeviceConstructor != null) {
        			result = (Terminal) ttyDeviceConstructor.newInstance(ttyDevice);
        		} else {
        			result = type.newInstance();
        		}
        	} else {
                result = type.newInstance();
        	}
        } else {
            throw new InternalError();
        }
        return result;
    }

    public static void registerFlavor(final Flavor flavor, final Class<? extends Terminal> type) {
        FLAVORS.put(flavor, type);
    }

}

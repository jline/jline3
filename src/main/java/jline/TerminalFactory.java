/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline;

/**
 * Creates terminal instances.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public abstract class TerminalFactory
{
    public static final String JLINE_TERMINAL = "jline.terminal";

    public static final String AUTO = "auto";

    public static final String UNIX = "unix";

    public static final String WIN = "win";

    public static final String NONE = "none";

    public static final String OFF = "off";

    public static final String FALSE = "false";

    public static final String WINDOWS = "windows";

    private static Terminal terminal;

    public static Terminal getTerminal() {
        return create();
    }

    public static synchronized Terminal create() {
        if (terminal != null) {
            return terminal;
        }

        final Terminal t;

        String type = System.getProperty(JLINE_TERMINAL);

        if (type == null) {
            type = AUTO;
        }

        Log.debug("Creating terminal; type=", type);

        String tmp = type.toLowerCase();

        if (tmp.equals(UNIX)) {
            t = new UnixTerminal();
        }
        else if (tmp.equals(WIN) | tmp.equals(WINDOWS)) {
            t = new WindowsTerminal();
        }
        else if (tmp.equals(NONE) || tmp.equals(OFF) || tmp.equals(FALSE)) {
            t = new UnsupportedTerminal();
        }
        else {
            if (tmp.equals(AUTO)) {
                String os = System.getProperty("os.name").toLowerCase();

                if (os.contains(WINDOWS)) {
                    t = new WindowsTerminal();
                }
                else {
                    t = new UnixTerminal();
                }
            }
            else {
                try {
                    t = (Terminal)Thread.currentThread().getContextClassLoader().loadClass(type).newInstance();
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Invalid terminal type: " + type, e);
                }
            }
        }

        Log.debug("Created Terminal: ", t);

        try {
            t.init();
        }
        catch (Exception e) {
            Log.error("Terminal initialization failed; falling back to unsupported", e);
            return terminal = new UnsupportedTerminal();
        }

        return terminal = t;
    }

    public static void reset() {
        terminal = null;
    }

    public static void resetIf(final Terminal t) {
        if (terminal == t) {
            reset();
        }
    }

    public static enum Type {
        AUTO,
        WINDOWS,
        UNIX,
        NONE
    }

    public static void configure(String type) {
        assert type != null;
        System.setProperty(JLINE_TERMINAL, type);
    }

    public static void configure(final Type type) {
        assert type != null;
        configure(type.name().toLowerCase());
    }
}
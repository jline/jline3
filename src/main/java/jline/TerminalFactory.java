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

    private static Terminal term;

    public static Terminal getTerminal() {
        return setupTerminal();
    }

    public static void resetTerminal() {
        term = null;
    }

    public static synchronized Terminal setupTerminal() {
        if (term != null) {
            return term;
        }

        final Terminal t;

        String os = System.getProperty("os.name").toLowerCase();
        String termProp = System.getProperty(JLINE_TERMINAL);

        if ((termProp != null) && (termProp.length() > 0)) {
            try {
                t = (Terminal) Class.forName(termProp).newInstance();
            }
            catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        else if (os.contains("windows")) {
            t = new WindowsTerminal();
        }
        else {
            t = new UnixTerminal();
        }

        try {
            t.init();
        }
        catch (Exception e) {
            e.printStackTrace();

            return term = new UnsupportedTerminal();
        }

        return term = t;
    }

    public static void resetTerminal(Terminal t) {
        if (term == t) {
            resetTerminal();
        }
    }
}
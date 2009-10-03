/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline;

/**
 * ???
 */
public abstract class TerminalFactory
{
    private static Terminal term;

    /**
     * @see #setupTerminal
     */
    public static Terminal getTerminal() {
        return setupTerminal();
    }

    /**
     * Reset the current terminal to null.
     */
    public static void resetTerminal() {
        term = null;
    }

    /**
     * <p>Configure and return the {@link TerminalFactory} instance for the
     * current platform. This will initialize any system settings
     * that are required for the console to be able to handle
     * input correctly, such as setting tabtop, buffered input, and
     * character echo.</p>
     * <p/>
     * <p>This class will use the Terminal implementation specified in the
     * <em>jline.terminal</em> system property, or, if it is unset, by
     * detecting the operating system from the <em>os.name</em>
     * system property and instantiating either the
     * {@link jline.WindowsTerminal} or {@link jline.UnixTerminal}.
     */
    public static synchronized Terminal setupTerminal() {
        if (term != null) {
            return term;
        }

        final Terminal t;

        String os = System.getProperty("os.name").toLowerCase();
        String termProp = System.getProperty("jline.terminal");

        if ((termProp != null) && (termProp.length() > 0)) {
            try {
                t = (Terminal) Class.forName(termProp).newInstance();
            }
            catch (Exception e) {
                throw (IllegalArgumentException) new IllegalArgumentException(e
                    .toString()).fillInStackTrace();
            }
        }
        else if (os.indexOf("windows") != -1) {
            t = new WindowsTerminal();
        }
        else {
            t = new UnixTerminal();
        }

        try {
            t.initializeTerminal();
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
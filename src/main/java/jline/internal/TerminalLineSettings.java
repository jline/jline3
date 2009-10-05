/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.StringTokenizer;

/**
 * Provides access to <tt>stty</tt> information.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:dwkemp@gmail.com">Dale Kemp</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public class TerminalLineSettings
{
    public static final String JLINE_STTY_COMMAND = "jline.sttyCommand";

    public static final String DEFAULT_STTY = "stty";

    private static String command = System.getProperty(JLINE_STTY_COMMAND, DEFAULT_STTY);

    private String config;

    public TerminalLineSettings() throws IOException, InterruptedException {
        config = get("-g");

        Log.debug("Config: ", config);

        // sanity check
        if (config.length() == 0 || (!config.contains("=") && !config.contains(":"))) {
            throw new IOException(MessageFormat.format("Unrecognized stty code: {0}", config));
        }
    }

    public String getConfig() {
        return config;
    }

    public void restore() throws IOException, InterruptedException {
        set("sane");
    }

    /**
     * The command to use to set the terminal options. Defaults
     * to "stty", or the value of the system property "jline.sttyCommand".
     */
    public static void setCommand(final String cmd) {
        assert cmd != null;
        command = cmd;
    }

    /**
     * The command to use to set the terminal options. Defaults
     * to "stty", or the value of the system property "jline.sttyCommand".
     */
    public static String getCommand() {
        return command;
    }

    public String get(final String args) throws IOException, InterruptedException {
        return stty(args);
    }

    public void set(final String args) throws IOException, InterruptedException {
        stty(args);
    }

    public int getProperty(final String name) throws IOException, InterruptedException {
        assert name != null;

        // need to be able handle both output formats:
        // speed 9600 baud; 24 rows; 140 columns;
        // and:
        // speed 38400 baud; rows = 49; columns = 111; ypixels = 0; xpixels = 0;
        String props = get("-a");

        for (StringTokenizer tok = new StringTokenizer(props, ";\n"); tok.hasMoreTokens();) {
            String str = tok.nextToken().trim();

            if (str.startsWith(name)) {
                int index = str.lastIndexOf(" ");
                return Integer.parseInt(str.substring(index).trim());
            }
            else if (str.endsWith(name)) {
                int index = str.indexOf(" ");
                return Integer.parseInt(str.substring(0, index).trim());
            }
        }

        return -1;
    }

    private static String stty(final String args) throws IOException, InterruptedException {
        assert args != null;
        return exec(String.format("%s %s < /dev/tty", getCommand(), args));
    }

    private static String exec(final String cmd) throws IOException, InterruptedException {
        assert cmd != null;
        return exec("sh", "-c", cmd);
    }

    private static String exec(final String... cmd) throws IOException, InterruptedException {
        assert cmd != null;
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Log.trace("Running: ", cmd);
        
        Process p = Runtime.getRuntime().exec(cmd);
        int c;

        InputStream in = p.getInputStream();

        while ((c = in.read()) != -1) {
            out.write(c);
        }

        in = p.getErrorStream();

        while ((c = in.read()) != -1) {
            out.write(c);
        }

        p.waitFor();

        String result = new String(out.toByteArray());

        Log.trace("Result: ", result);

        return result;
    }
}
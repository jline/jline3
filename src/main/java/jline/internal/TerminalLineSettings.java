/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline.internal;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.StringTokenizer;

/**
 * Provides access to terminal line settings via <tt>stty</tt>.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:dwkemp@gmail.com">Dale Kemp</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public final class TerminalLineSettings
{
    public static final String JLINE_STTY = "jline.stty";

    public static final String DEFAULT_STTY = "stty";

    public static final String JLINE_SH = "jline.sh";

    public static final String DEFAULT_SH = "sh";

    private static String sttyCommand = System.getProperty(JLINE_STTY, DEFAULT_STTY);

    private static String shCommand = System.getProperty(JLINE_SH, DEFAULT_SH);

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

    public String get(final String args) throws IOException, InterruptedException {
        return stty(args);
    }

    public void set(final String args) throws IOException, InterruptedException {
        stty(args);
    }

    public int getProperty(final String name) {
        assert name != null;

        try {
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
        }
        catch (Exception e) {
            Log.warn("Failed to query stty ", name, e);
        }

        return -1;
    }

    private static String stty(final String args) throws IOException, InterruptedException {
        assert args != null;
        return exec(String.format("%s %s < /dev/tty", sttyCommand, args));
    }

    private static String exec(final String cmd) throws IOException, InterruptedException {
        assert cmd != null;
        return exec(shCommand, "-c", cmd);
    }

    private static String exec(final String... cmd) throws IOException, InterruptedException {
        assert cmd != null;

        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        Log.trace("Running: ", cmd);

        Process p = Runtime.getRuntime().exec(cmd);

        InputStream in = null;
        InputStream err = null;
        OutputStream out = null;
        try {
            int c;
            in = p.getInputStream();
            while ((c = in.read()) != -1) {
                bout.write(c);
            }
            err = p.getErrorStream();
            while ((c = err.read()) != -1) {
                bout.write(c);
            }
            out = p.getOutputStream();
            p.waitFor();
        }
        finally {
            close(in, out, err);
        }

        String result = bout.toString();

        Log.trace("Result: ", result);

        return result;
    }

    private static void close(final Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();
            }
            catch (Exception e) {
                // Ignore
            }
        }
    }
}
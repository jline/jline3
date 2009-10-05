/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline;

import jline.console.Key;
import static jline.console.Key.CTRL_A;
import static jline.console.Key.CTRL_B;
import static jline.console.Key.CTRL_E;
import static jline.console.Key.CTRL_F;
import static jline.console.Key.CTRL_N;
import static jline.console.Key.CTRL_P;
import static jline.console.Key.DELETE;
import jline.internal.Log;
import jline.internal.ReplayPrefixOneCharInputStream;
import jline.internal.TerminalLineSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * <p>
 * Terminal that is used for unix platforms. Terminal initialization
 * is handled by issuing the <em>stty</em> command against the
 * <em>/dev/tty</em> file to disable character echoing and enable
 * character input. All known unix systems (including
 * Linux and Macintosh OS X) support the <em>stty</em>), so this
 * implementation should work for an reasonable POSIX system.
 * </p>
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:dwkemp@gmail.com">Dale Kemp</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @aince 2.0
 */
public class UnixTerminal
    extends TerminalSupport
{
    public static final short ARROW_START = 27;

    public static final short ARROW_PREFIX = 91;

    public static final short ARROW_LEFT = 68;

    public static final short ARROW_RIGHT = 67;

    public static final short ARROW_UP = 65;

    public static final short ARROW_DOWN = 66;

    public static final short O_PREFIX = 79;

    public static final short HOME_CODE = 72;

    public static final short END_CODE = 70;

    public static final short DEL_THIRD = 51;

    public static final short DEL_SECOND = 126;

    private final TerminalLineSettings settings = new TerminalLineSettings();

    private final ReplayPrefixOneCharInputStream replayStream = new ReplayPrefixOneCharInputStream(System.getProperty("input.encoding", "UTF-8"));

    private final InputStreamReader replayReader;

    private boolean echoEnabled;

    private boolean backspaceDeleteSwitched = false;

    public UnixTerminal() throws Exception {
        replayReader = new InputStreamReader(replayStream, replayStream.getEncoding());
    }

    protected TerminalLineSettings getSettings() {
        return settings;
    }

    /**
     * Remove line-buffered input by invoking "stty -icanon min 1"
     * against the current terminal.
     */
    public void init() throws IOException, InterruptedException {
        checkBackspace();

        // set the console to be character-buffered instead of line-buffered
        settings.set("-icanon min 1");

        // disable character echoing
        settings.set("-echo");
        echoEnabled = false;

        installShutdownHook(new RestoreHook());
    }

    /**
     * Restore the original terminal configuration, which can be used when
     * shutting down the console reader. The ConsoleReader cannot be
     * used after calling this method.
     */
    public void restore() throws Exception {
        settings.restore();
        TerminalFactory.resetIf(this);
        removeShutdownHook();
    }

    public boolean isSupported() {
        return true;
    }

    public boolean getEcho() {
        return false;
    }

       /**
     * Returns the value of "stty columns" width param.
     *
     * <strong>Note</strong>: this method caches the value from the
     * first time it is called in order to increase speed, which means
     * that changing to size of the terminal will not be reflected
     * in the console.
     */
    public int getWidth() {
        int val = -1;

        try {
            val = settings.getProperty("columns");
        }
        catch (Exception e) {
            // ignore
        }

        if (val == -1) {
            val = DEFAULT_WIDTH;
        }

        return val;
    }

    /**
     * Returns the value of "stty rows" height param.
     *
     * <strong>Note</strong>: this method caches the value from the
     * first time it is called in order to increase speed, which means
     * that changing to size of the terminal will not be reflected
     * in the console.
     */
    public int getHeight() {
        int val = -1;

        try {
            val = settings.getProperty("rows");
        }
        catch (Exception e) {
            // ignore
        }

        if (val == -1) {
            val = DEFAULT_HEIGHT;
        }

        return val;
    }

    protected void checkBackspace() {
        String[] config = settings.getConfig().split(":|=");

        if (config.length < 7) {
            return;
        }

        if (config[6] == null) {
            return;
        }

        backspaceDeleteSwitched = config[6].equals("7f");
    }

    public boolean isAnsiSupported() {
        return true;
    }

    public synchronized boolean isEchoEnabled() {
        return echoEnabled;
    }

    public synchronized void enableEcho() {
        try {
            settings.set("echo");
            echoEnabled = true;
        }
        catch (Exception e) {
            Log.error("Failed to enable echo: ", e);
        }
    }

    public synchronized void disableEcho() {
        try {
            settings.set("-echo");
            echoEnabled = false;
        }
        catch (Exception e) {
            Log.error("Failed to disable echo: ", e);
        }
    }

    public int readVirtualKey(final InputStream in) throws IOException {
        int c = readCharacter(in);
        
        if (backspaceDeleteSwitched) {
            if (Key.valueOf(c) == DELETE) {
                c = '\b';
            }
            else if (c == '\b') {
                c = DELETE.code;
            }
        }

        // in Unix terminals, arrow keys are represented by a sequence of 3 characters. E.g., the up arrow key yields 27, 91, 68
        if (c == ARROW_START) {
            // also the escape key is 27 thats why we read until we have something different than 27
            // this is a bugfix, because otherwise pressing escape and than an arrow key was an undefined state
            while (c == ARROW_START) {
                c = readCharacter(in);
            }

            if (c == ARROW_PREFIX || c == O_PREFIX) {
                c = readCharacter(in);
                if (c == ARROW_UP) {
                    return CTRL_P.code;
                }
                else if (c == ARROW_DOWN) {
                    return CTRL_N.code;
                }
                else if (c == ARROW_LEFT) {
                    return CTRL_B.code;
                }
                else if (c == ARROW_RIGHT) {
                    return CTRL_F.code;
                }
                else if (c == HOME_CODE) {
                    return CTRL_A.code;
                }
                else if (c == END_CODE) {
                    return CTRL_E.code;
                }
                else if (c == DEL_THIRD) {
                    readCharacter(in); // read 4th & ignore
                    return DELETE.code;
                }
            }
        }

        // handle unicode characters, thanks for a patch from amyi@inf.ed.ac.uk
        if (c > 128) {
            // handle unicode characters longer than 2 bytes,
            // thanks to Marc.Herbert@continuent.com
            replayStream.setInput(c, in);
            // replayReader = new InputStreamReader(replayStream, encoding);
            c = replayReader.read();
        }

        return c;
    }
}

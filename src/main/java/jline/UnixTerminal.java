/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline;

import static jline.UnixTerminal.UnixKey.*;
import jline.console.Key;
import static jline.console.Key.*;
import jline.internal.Log;
import jline.internal.ReplayPrefixOneCharInputStream;
import jline.internal.TerminalLineSettings;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Terminal that is used for unix platforms. Terminal initialization
 * is handled by issuing the <em>stty</em> command against the
 * <em>/dev/tty</em> file to disable character echoing and enable
 * character input. All known unix systems (including
 * Linux and Macintosh OS X) support the <em>stty</em>), so this
 * implementation should work for an reasonable POSIX system.
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
     * Returns the value of <tt>stty columns</tt> param.
     */
    public int getWidth() {
        int val = -1;

        try {
            val = settings.getProperty("columns");
        }
        catch (Exception e) {
            Log.warn("Failed to query stty colums", e);
        }

        if (val == -1) {
            val = DEFAULT_WIDTH;
        }

        return val;
    }

    /**
     * Returns the value of <tt>stty rows>/tt> param.
     */
    public int getHeight() {
        int val = -1;

        try {
            val = settings.getProperty("rows");
        }
        catch (Exception e) {
            Log.warn("Failed to query stty rows", e);
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

        UnixKey key = UnixKey.valueOf(c);

        // in Unix terminals, arrow keys are represented by a sequence of 3 characters. E.g., the up arrow key yields 27, 91, 68
        if (key == ARROW_START) {
            // also the escape key is 27 thats why we read until we have something different than 27
            // this is a bugfix, because otherwise pressing escape and than an arrow key was an undefined state
            while (key == ARROW_START) {
                c = readCharacter(in);
                key = UnixKey.valueOf(c);
            }

            if (key == ARROW_PREFIX || key == O_PREFIX) {
                c = readCharacter(in);
                key = UnixKey.valueOf(c);

                if (key == ARROW_UP) {
                    return CTRL_P.code;
                }
                else if (key == ARROW_DOWN) {
                    return CTRL_N.code;
                }
                else if (key == ARROW_LEFT) {
                    return CTRL_B.code;
                }
                else if (key == ARROW_RIGHT) {
                    return CTRL_F.code;
                }
                else if (key == HOME_CODE) {
                    return CTRL_A.code;
                }
                else if (key == END_CODE) {
                    return CTRL_E.code;
                }
                else if (key == DEL_THIRD) {
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

    //
    // UnixKey
    //

    /**
     * Unix keys.
     */
    public static enum UnixKey
    {
        ARROW_START(27),

        ARROW_PREFIX(91),

        ARROW_LEFT(68),

        ARROW_RIGHT(67),

        ARROW_UP(65),

        ARROW_DOWN(66),

        O_PREFIX(79),

        HOME_CODE(72),

        END_CODE(70),

        DEL_THIRD(51),

        DEL_SECOND(126),
        ;

        public final short code;

        UnixKey(final int code) {
            this.code = (short)code;
        }

        private static final Map<Short, UnixKey> codes;

        static {
            Map<Short, UnixKey> map = new HashMap<Short, UnixKey>();

            for (UnixKey key : UnixKey.values()) {
                map.put(key.code, key);
            }

            codes = map;
        }

        public static UnixKey valueOf(final int code) {
            return codes.get((short)code);
        }
    }
}

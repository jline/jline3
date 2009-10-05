/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */

package jline;

import jline.console.ConsoleReader;

import java.io.IOException;
import java.io.Writer;

/**
 * An unsupported terminal.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 *
 * @since 2.0
 */
public class UnsupportedTerminal
    extends TerminalSupport
{
    private Thread maskThread;

    public boolean isAnsiSupported() {
        return false;
    }

    public boolean getEcho() {
        return true;
    }

    public boolean isEchoEnabled() {
        return true;
    }

    public boolean isSupported() {
        return false;
    }

    public void beforeReadLine(final ConsoleReader reader, final String prompt, final Character mask) {
        if (mask != null && maskThread == null) {
            final String fullPrompt = "\r" + prompt
                + "                 "
                + "                 "
                + "                 "
                + "\r" + prompt;

            maskThread = new Thread() {
                public void run() {
                    while (!interrupted()) {
                        try {
                            Writer out = reader.getOutput();
                            out.write(fullPrompt);
                            out.flush();
                            sleep(3);
                        }
                        catch (IOException ioe) {
                            return;
                        }
                        catch (InterruptedException ie) {
                            return;
                        }
                    }
                }
            };

            maskThread.setPriority(Thread.MAX_PRIORITY);
            maskThread.setDaemon(true);
            maskThread.start();
        }
    }

    public void afterReadLine(final ConsoleReader reader, final String prompt, final Character mask) {
        if ((maskThread != null) && maskThread.isAlive()) {
            maskThread.interrupt();
        }

        maskThread = null;
    }
}

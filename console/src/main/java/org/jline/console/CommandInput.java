/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.io.InputStream;
import java.io.PrintStream;

import org.jline.terminal.Terminal;

public class CommandInput {
    String command;
    String[] args;
    Object[] xargs;
    Terminal terminal;
    InputStream in;
    PrintStream out;
    PrintStream err;

    public CommandInput(String command, Object[] xargs, CommandRegistry.CommandSession session) {
        if (xargs != null) {
            this.xargs = xargs;
            this.args = new String[xargs.length];
            for (int i = 0; i < xargs.length; i++) {
                this.args[i] = xargs[i] != null ? xargs[i].toString() : null;
            }
        }
        this.command = command;
        this.terminal = session.terminal();
        this.in = session.in();
        this.out = session.out();
        this.err = session.err();
    }

    public CommandInput(
            String command, Object[] args, Terminal terminal, InputStream in, PrintStream out, PrintStream err) {
        this(command, args, new CommandRegistry.CommandSession(terminal, in, out, err));
    }

    public String command() {
        return command;
    }

    public String[] args() {
        return args;
    }

    public Object[] xargs() {
        return xargs;
    }

    public Terminal terminal() {
        return terminal;
    }

    public InputStream in() {
        return in;
    }

    public PrintStream out() {
        return out;
    }

    public PrintStream err() {
        return err;
    }

    public CommandRegistry.CommandSession session() {
        return new CommandRegistry.CommandSession(terminal, in, out, err);
    }
}

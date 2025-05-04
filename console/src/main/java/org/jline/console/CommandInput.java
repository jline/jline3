/*
 * Copyright (c) 2002-2025, the original author(s).
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

/**
 * Encapsulates the input and output streams for a command execution.
 * This class provides access to the command name, arguments, terminal, and I/O streams
 * needed for command execution in the console environment.
 */
public class CommandInput {
    /** The command name */
    String command;
    /** String representation of command arguments */
    String[] args;
    /** Original object arguments */
    Object[] xargs;
    /** Terminal instance for the command */
    Terminal terminal;
    /** Input stream for the command */
    InputStream in;
    /** Output stream for the command */
    PrintStream out;
    /** Error stream for the command */
    PrintStream err;

    /**
     * Creates a new CommandInput with the specified command, arguments, and session.
     *
     * @param command the command name
     * @param xargs the command arguments as objects
     * @param session the command session containing terminal and I/O streams
     */
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

    /**
     * Creates a new CommandInput with the specified command, arguments, terminal, and I/O streams.
     *
     * @param command the command name
     * @param args the command arguments as objects
     * @param terminal the terminal instance
     * @param in the input stream
     * @param out the output stream
     * @param err the error stream
     */
    public CommandInput(
            String command, Object[] args, Terminal terminal, InputStream in, PrintStream out, PrintStream err) {
        this(command, args, new CommandRegistry.CommandSession(terminal, in, out, err));
    }

    /**
     * Returns the command name.
     *
     * @return the command name
     */
    public String command() {
        return command;
    }

    /**
     * Returns the command arguments as strings.
     *
     * @return the command arguments as strings
     */
    public String[] args() {
        return args;
    }

    /**
     * Returns the original command arguments as objects.
     *
     * @return the command arguments as objects
     */
    public Object[] xargs() {
        return xargs;
    }

    /**
     * Returns the terminal instance for this command.
     *
     * @return the terminal instance
     */
    public Terminal terminal() {
        return terminal;
    }

    /**
     * Returns the input stream for this command.
     *
     * @return the input stream
     */
    public InputStream in() {
        return in;
    }

    /**
     * Returns the output stream for this command.
     *
     * @return the output stream
     */
    public PrintStream out() {
        return out;
    }

    /**
     * Returns the error stream for this command.
     *
     * @return the error stream
     */
    public PrintStream err() {
        return err;
    }

    /**
     * Creates and returns a new CommandSession using this command's terminal and I/O streams.
     *
     * @return a new command session
     */
    public CommandRegistry.CommandSession session() {
        return new CommandRegistry.CommandSession(terminal, in, out, err);
    }
}

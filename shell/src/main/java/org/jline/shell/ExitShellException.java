/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

/**
 * This exception is thrown by commands (e.g. an exit command)
 * to indicate that the shell should exit.
 * <p>
 * If a message is provided it will be printed to the terminal when the shell exits.
 */
public class ExitShellException extends RuntimeException {

    private static final long serialVersionUID = 9147467891877904413L;

    private final int exitCode;

    public ExitShellException() {
        this(0);
    }

    public ExitShellException(int exitCode) {
        super();
        this.exitCode = exitCode;
    }

    public ExitShellException(String message) {
        this(message, 0);
    }

    public ExitShellException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}

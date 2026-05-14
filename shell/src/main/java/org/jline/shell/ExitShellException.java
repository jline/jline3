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

    public ExitShellException() {
        super();
    }

    public ExitShellException(String message) {
        super(message);
    }
}

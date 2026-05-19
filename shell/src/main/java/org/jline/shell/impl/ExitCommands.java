/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.List;

import org.jline.shell.CommandSession;
import org.jline.shell.ExitShellException;

/**
 * Built-in exit command.
 * <p>
 * Provides the exit command used for exiting the current shell.
 *
 * @since 4.1
 */
public class ExitCommands extends SimpleCommandGroup {

    public ExitCommands() {
        this(null);
    }

    public ExitCommands(String message) {
        super("exit", List.of(new ExitCommand(message)));
    }

    private static class ExitCommand extends AbstractCommand {

        private ExitCommand(String message) {
            super("exit", "quit");
            this.message = message;
        }

        private final String message;

        @Override
        public String description() {
            return "Exit the current shell";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            throw new ExitShellException(this.message);
        }
    }
}

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

import org.jline.reader.EndOfFileException;
import org.jline.shell.CommandSession;

/**
 * Built-in exit command.
 * <p>
 * Provides the exit command used for exiting the current shell.
 *
 * @since 4.1
 */
public class ExitCommands extends SimpleCommandGroup {

    public ExitCommands() {
        super("exit", List.of(new ExitCommand()));
    }

    private static class ExitCommand extends AbstractCommand {

        private ExitCommand() {
            super("exit", "quit");
        }

        @Override
        public String description() {
            return "Exit the current shell";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            throw new EndOfFileException();
        }
    }
}

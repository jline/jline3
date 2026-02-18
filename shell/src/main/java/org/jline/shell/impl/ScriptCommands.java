/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.jline.shell.*;

/**
 * Built-in commands for script execution.
 * <p>
 * Provides:
 * <ul>
 *   <li>{@code source} (alias {@code .}) — executes a script file in the current session</li>
 * </ul>
 *
 * @see ScriptRunner
 * @since 4.0
 */
public class ScriptCommands implements CommandGroup {

    private final ScriptRunner scriptRunner;
    private final CommandDispatcher dispatcher;
    private final List<Command> commands;

    /**
     * Creates the script commands group.
     *
     * @param scriptRunner the script runner to use
     * @param dispatcher the command dispatcher for executing script lines
     */
    public ScriptCommands(ScriptRunner scriptRunner, CommandDispatcher dispatcher) {
        this.scriptRunner = scriptRunner;
        this.dispatcher = dispatcher;
        this.commands = List.of(new SourceCommand());
    }

    @Override
    public String name() {
        return "Script";
    }

    @Override
    public Collection<Command> commands() {
        return commands;
    }

    private class SourceCommand extends AbstractCommand {
        SourceCommand() {
            super("source", ".");
        }

        @Override
        public String description() {
            return "Execute a script file in the current session";
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            if (args.length < 1) {
                throw new IllegalArgumentException("Usage: source <file>");
            }
            Path scriptPath = Paths.get(args[0]);
            if (!scriptPath.isAbsolute() && session.workingDirectory() != null) {
                scriptPath = session.workingDirectory().resolve(scriptPath);
            }
            scriptRunner.execute(scriptPath, session, dispatcher);
            return null;
        }
    }
}

/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.shell.*;

/**
 * Built-in help command group.
 * <p>
 * Provides the {@code help} command:
 * <ul>
 *   <li>{@code help} — list all commands grouped by {@link CommandGroup} name</li>
 *   <li>{@code help <command>} — show detailed help for a specific command</li>
 * </ul>
 *
 * @since 4.0
 */
public class HelpCommands extends SimpleCommandGroup {

    /**
     * Creates help commands using the given dispatcher for command discovery.
     *
     * @param dispatcher the command dispatcher
     */
    public HelpCommands(CommandDispatcher dispatcher) {
        super("help", createCommands(dispatcher));
    }

    private static List<Command> createCommands(CommandDispatcher dispatcher) {
        return List.of(new HelpCommand(dispatcher));
    }

    private static class HelpCommand extends AbstractCommand {
        private final CommandDispatcher dispatcher;

        HelpCommand(CommandDispatcher dispatcher) {
            super("help", "?");
            this.dispatcher = dispatcher;
        }

        @Override
        public String description() {
            return "Display help for commands";
        }

        @Override
        public List<Completer> completers() {
            return List.of(new CommandNameCompleter(dispatcher));
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            if (args.length == 0) {
                // List all commands grouped
                for (CommandGroup group : dispatcher.groups()) {
                    session.out().println(group.name() + ":");
                    for (Command cmd : group.commands()) {
                        String desc = cmd.description();
                        if (desc == null || desc.isEmpty()) {
                            session.out().printf("  %-20s%n", cmd.name());
                        } else {
                            session.out().printf("  %-20s %s%n", cmd.name(), desc);
                        }
                    }
                }
            } else {
                String cmdName = args[0];
                Command cmd = dispatcher.findCommand(cmdName);
                if (cmd == null) {
                    session.err().println("help: unknown command: " + cmdName);
                    return null;
                }

                session.out().println(cmd.name());
                if (!cmd.description().isEmpty()) {
                    session.out().println("  " + cmd.description());
                }
                if (!cmd.aliases().isEmpty()) {
                    session.out().println("  Aliases: " + String.join(", ", cmd.aliases()));
                }

                // Try to get detailed description
                CommandDescription desc = cmd.describe(List.of(cmdName));
                if (desc != null) {
                    List<ArgumentDescription> argDescs = desc.arguments();
                    if (argDescs != null && !argDescs.isEmpty()) {
                        session.out().println("  Arguments:");
                        for (ArgumentDescription ad : argDescs) {
                            String argDesc = "";
                            if (ad.description() != null && !ad.description().isEmpty()) {
                                argDesc = " - " + ad.description().get(0).toString();
                            }
                            session.out().println("    " + ad.name() + argDesc);
                        }
                    }
                }
            }
            return null;
        }
    }

    private static class CommandNameCompleter implements Completer {
        private final CommandDispatcher dispatcher;

        CommandNameCompleter(CommandDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            for (CommandGroup group : dispatcher.groups()) {
                for (Command cmd : group.commands()) {
                    candidates.add(
                            new Candidate(cmd.name(), cmd.name(), group.name(), cmd.description(), null, null, true));
                }
            }
        }
    }
}

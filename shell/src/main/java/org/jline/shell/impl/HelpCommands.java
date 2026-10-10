/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.text.MessageFormat;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.shell.*;
import org.jline.utils.AttributedString;

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
                    // Print main description if present
                    List<AttributedString> mainDesc = desc.mainDescription();
                    if (mainDesc != null && !mainDesc.isEmpty()) {
                        for (AttributedString as : mainDesc) {
                            session.out().println(as);
                        }
                    }
                    List<ArgumentDescription> argDescs = desc.arguments();
                    if (argDescs != null && !argDescs.isEmpty()) {
                        session.out().println("  Arguments:");
                        for (ArgumentDescription ad : argDescs) {
                            printArgumentLines(session, ad);
                        }
                    }
                }
            }
            return null;
        }

        private void printArgumentLines(final CommandSession session, final ArgumentDescription argumentDescription) {
            final List<AttributedString> argumentLines = argumentDescription.description();
            if ((argumentLines == null) || (argumentLines.isEmpty())) {
                return;
            }

            // Print first line
            final AttributedString firstLine = argumentLines.get(0);
            final String firstPrintLine =
                    MessageFormat.format("    {0} - {1}", argumentDescription.name(), firstLine.toString());
            session.out().println(firstPrintLine);

            // Print following lines, if any
            for (int idx = 1; idx < argumentLines.size(); idx++) {
                final int indentation = 4 + argumentDescription.name().length() + 2;

                final AttributedString argumentLine = argumentLines.get(idx);
                final String padding = " ".repeat(indentation);

                final String argumentPrintLine = MessageFormat.format("{0} {1}", padding, argumentLine);

                session.out().println(argumentPrintLine);
            }
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

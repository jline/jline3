/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.shell.Command;
import org.jline.shell.CommandSession;

/**
 * Built-in option management commands: {@code setopt}, {@code unsetopt}, and {@code setvar}.
 * <p>
 * These commands control the {@link LineReader}'s options and variables at runtime:
 * <ul>
 *   <li>{@code setopt OPTION} — enable a LineReader option</li>
 *   <li>{@code setopt} — list all enabled options</li>
 *   <li>{@code unsetopt OPTION} — disable a LineReader option</li>
 *   <li>{@code setvar NAME VALUE} — set a LineReader variable</li>
 *   <li>{@code setvar} — list all variables</li>
 * </ul>
 *
 * @since 4.0
 */
public class OptionCommands extends SimpleCommandGroup {

    /**
     * Creates option commands for the given line reader.
     *
     * @param reader the line reader
     */
    public OptionCommands(LineReader reader) {
        super("options", createCommands(reader));
    }

    private static List<Command> createCommands(LineReader reader) {
        return List.of(new SetOptCommand(reader), new UnsetOptCommand(reader), new SetVarCommand(reader));
    }

    private static List<String> optionNames() {
        return Arrays.stream(Option.values()).map(Option::name).collect(Collectors.toList());
    }

    private static class SetOptCommand extends AbstractCommand {
        private final LineReader reader;

        SetOptCommand(LineReader reader) {
            super("setopt");
            this.reader = reader;
        }

        @Override
        public String description() {
            return "Enable a LineReader option";
        }

        @Override
        public List<Completer> completers() {
            return List.of(new StringsCompleter(optionNames()));
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            if (args.length == 0) {
                // List enabled options
                for (Option opt : Option.values()) {
                    if (reader.isSet(opt)) {
                        session.out().println(opt.name());
                    }
                }
                return null;
            }

            for (String arg : args) {
                try {
                    Option opt = Option.valueOf(arg);
                    reader.setOpt(opt);
                } catch (IllegalArgumentException e) {
                    session.err().println("setopt: unknown option: " + arg);
                }
            }
            return null;
        }
    }

    private static class UnsetOptCommand extends AbstractCommand {
        private final LineReader reader;

        UnsetOptCommand(LineReader reader) {
            super("unsetopt");
            this.reader = reader;
        }

        @Override
        public String description() {
            return "Disable a LineReader option";
        }

        @Override
        public List<Completer> completers() {
            return List.of(new StringsCompleter(optionNames()));
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            if (args.length == 0) {
                // List disabled options
                for (Option opt : Option.values()) {
                    if (!reader.isSet(opt)) {
                        session.out().println(opt.name());
                    }
                }
                return null;
            }

            for (String arg : args) {
                try {
                    Option opt = Option.valueOf(arg);
                    reader.unsetOpt(opt);
                } catch (IllegalArgumentException e) {
                    session.err().println("unsetopt: unknown option: " + arg);
                }
            }
            return null;
        }
    }

    private static class SetVarCommand extends AbstractCommand {
        private final LineReader reader;

        SetVarCommand(LineReader reader) {
            super("setvar");
            this.reader = reader;
        }

        @Override
        public String description() {
            return "Set or list LineReader variables";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            if (args.length == 0) {
                // List all variables
                for (var entry : reader.getVariables().entrySet()) {
                    session.out().printf("%-30s %s%n", entry.getKey(), entry.getValue());
                }
                return null;
            }

            if (args.length < 2) {
                // Show single variable
                Object val = reader.getVariable(args[0]);
                if (val != null) {
                    session.out().println(args[0] + "=" + val);
                } else {
                    session.err().println("setvar: variable not set: " + args[0]);
                }
                return null;
            }

            // Set variable
            String name = args[0];
            String value = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

            // Try to parse as integer
            try {
                reader.setVariable(name, Integer.parseInt(value));
            } catch (NumberFormatException e) {
                // Try boolean
                if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    reader.setVariable(name, Boolean.parseBoolean(value));
                } else {
                    reader.setVariable(name, value);
                }
            }
            return null;
        }
    }
}

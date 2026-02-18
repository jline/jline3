/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.jline.shell.*;

/**
 * Built-in commands for managing session variables: {@code set}, {@code unset}, and {@code export}.
 * <p>
 * <ul>
 *   <li>{@code set} — list all session variables, or set one ({@code set NAME=VALUE} or {@code set NAME VALUE})</li>
 *   <li>{@code unset NAME} — remove a session variable</li>
 *   <li>{@code export NAME=VALUE} — same as {@code set NAME=VALUE}</li>
 * </ul>
 *
 * @since 4.0
 */
public class VariableCommands implements CommandGroup {

    /**
     * Creates a new VariableCommands group.
     */
    public VariableCommands() {}

    private final List<Command> commands = List.of(new SetCommand(), new UnsetCommand(), new ExportCommand());

    @Override
    public String name() {
        return "Variables";
    }

    @Override
    public List<Command> commands() {
        return commands;
    }

    @Override
    public Command command(String name) {
        for (Command cmd : commands) {
            if (cmd.name().equals(name)) {
                return cmd;
            }
        }
        return null;
    }

    /**
     * The {@code set} command: lists all variables or sets a variable.
     * <p>
     * Usage:
     * <ul>
     *   <li>{@code set} — list all session variables</li>
     *   <li>{@code set NAME=VALUE} — set a variable</li>
     *   <li>{@code set NAME VALUE} — set a variable (alternative form)</li>
     * </ul>
     */
    static class SetCommand extends AbstractCommand {

        SetCommand() {
            super("set");
        }

        @Override
        public String description() {
            return "List or set session variables";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            if (args.length == 0) {
                // List all variables sorted by name
                Map<String, Object> sorted = new TreeMap<>(session.variables());
                for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                    session.out().println(entry.getKey() + "=" + entry.getValue());
                }
                return null;
            }
            // set NAME=VALUE or set NAME VALUE
            String first = args[0];
            int eq = first.indexOf('=');
            if (eq >= 0) {
                String name = first.substring(0, eq);
                String value = first.substring(eq + 1);
                if (value.isEmpty() && args.length > 1) {
                    value = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                }
                session.put(name, value);
            } else if (args.length >= 2) {
                String name = first;
                String value = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                session.put(name, value);
            } else {
                // set NAME with no value — show the variable
                Object value = session.get(first);
                if (value != null) {
                    session.out().println(first + "=" + value);
                } else {
                    session.out().println(first + ": not set");
                }
            }
            return null;
        }
    }

    /**
     * The {@code unset} command: removes a session variable.
     * <p>
     * Usage: {@code unset NAME [NAME...]}
     */
    static class UnsetCommand extends AbstractCommand {

        UnsetCommand() {
            super("unset");
        }

        @Override
        public String description() {
            return "Remove session variables";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            if (args.length == 0) {
                session.out().println("usage: unset NAME [NAME...]");
                return null;
            }
            for (String name : args) {
                session.remove(name);
            }
            return null;
        }
    }

    /**
     * The {@code export} command: sets a session variable (alias for {@code set}).
     * <p>
     * Usage: {@code export NAME=VALUE} or {@code export NAME VALUE}
     */
    static class ExportCommand extends AbstractCommand {

        ExportCommand() {
            super("export");
        }

        @Override
        public String description() {
            return "Set session variables";
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            if (args.length == 0) {
                // List all variables
                Map<String, Object> sorted = new TreeMap<>(session.variables());
                for (Map.Entry<String, Object> entry : sorted.entrySet()) {
                    session.out().println("export " + entry.getKey() + "=" + entry.getValue());
                }
                return null;
            }
            String first = args[0];
            int eq = first.indexOf('=');
            if (eq >= 0) {
                String name = first.substring(0, eq);
                String value = first.substring(eq + 1);
                if (value.isEmpty() && args.length > 1) {
                    value = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                }
                session.put(name, value);
            } else if (args.length >= 2) {
                session.put(first, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            } else {
                Object value = session.get(first);
                if (value != null) {
                    session.out().println("export " + first + "=" + value);
                } else {
                    session.out().println(first + ": not set");
                }
            }
            return null;
        }
    }
}

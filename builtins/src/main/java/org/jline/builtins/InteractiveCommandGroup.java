/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.*;

import org.jline.shell.Command;
import org.jline.shell.CommandGroup;
import org.jline.shell.CommandSession;

/**
 * A {@link CommandGroup} that wraps interactive builtins ({@code nano}, {@code less},
 * {@code tmux}, {@code ttop}) as shell {@link Command}s.
 * <p>
 * These commands require a terminal and are typically used in interactive sessions.
 * <p>
 * Example:
 * <pre>
 * dispatcher.addGroup(new InteractiveCommandGroup());
 * dispatcher.execute("nano file.txt");
 * </pre>
 *
 * @since 4.0
 */
public class InteractiveCommandGroup implements CommandGroup {

    private final List<Command> commands;

    /**
     * Creates a new InteractiveCommandGroup with all available interactive commands.
     */
    public InteractiveCommandGroup() {
        List<Command> cmds = new ArrayList<>();
        cmds.add(interactiveCommand("nano", "Text editor", (session, args) -> {
            PosixCommands.Context ctx = createContext(session);
            String[] argv = prependName("nano", args);
            PosixCommands.nano(ctx, argv);
        }));
        cmds.add(interactiveCommand("less", "File pager", (session, args) -> {
            PosixCommands.Context ctx = createContext(session);
            String[] argv = prependName("less", args);
            PosixCommands.less(ctx, argv);
        }));
        cmds.add(interactiveCommand("ttop", "Terminal top", (session, args) -> {
            PosixCommands.Context ctx = createContext(session);
            String[] argv = prependName("ttop", args);
            PosixCommands.ttop(ctx, argv);
        }));
        this.commands = Collections.unmodifiableList(cmds);
    }

    @Override
    public String name() {
        return "Interactive";
    }

    @Override
    public Collection<Command> commands() {
        return commands;
    }

    private static PosixCommands.Context createContext(CommandSession session) {
        return new PosixCommands.Context(
                session.in(),
                session.out(),
                session.err(),
                session.workingDirectory(),
                session.terminal(),
                session::get);
    }

    private static String[] prependName(String name, String[] args) {
        String[] argv = new String[args.length + 1];
        argv[0] = name;
        System.arraycopy(args, 0, argv, 1, args.length);
        return argv;
    }

    private static Command interactiveCommand(String name, String description, InteractiveExecutor executor) {
        return new Command() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String description() {
                return description;
            }

            @Override
            public Object execute(CommandSession session, String[] args) throws Exception {
                if (session.terminal() == null) {
                    throw new IllegalStateException(name + " requires an interactive terminal");
                }
                executor.execute(session, args);
                return null;
            }
        };
    }

    @FunctionalInterface
    private interface InteractiveExecutor {
        void execute(CommandSession session, String[] args) throws Exception;
    }
}

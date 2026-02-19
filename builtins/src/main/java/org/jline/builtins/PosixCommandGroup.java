/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import org.jline.shell.Command;
import org.jline.shell.CommandGroup;
import org.jline.shell.CommandSession;

/**
 * A {@link CommandGroup} that wraps {@link PosixCommands} static methods as shell {@link Command}s.
 * <p>
 * This provides a bridge between the builtins module's POSIX command implementations
 * and the shell module's command API. Each command creates a {@link PosixCommands.Context}
 * from the current {@link CommandSession}.
 * <p>
 * Special handling:
 * <ul>
 *   <li>{@code cd} — uses {@link CommandSession#setWorkingDirectory(Path)} to update the session</li>
 * </ul>
 * <p>
 * Example:
 * <pre>
 * dispatcher.addGroup(new PosixCommandGroup());
 * dispatcher.execute("echo hello");
 * dispatcher.execute("ls -la");
 * </pre>
 *
 * @since 4.0
 */
public class PosixCommandGroup implements CommandGroup {

    private final List<Command> commands;

    /**
     * Creates a new PosixCommandGroup with all available POSIX commands.
     */
    public PosixCommandGroup() {
        List<Command> cmds = new ArrayList<>();
        cmds.add(posixCommand("cd", "Change working directory", (ctx, args, session) -> {
            Consumer<Path> dirChanger = session::setWorkingDirectory;
            PosixCommands.cd(ctx, args, dirChanger);
        }));
        cmds.add(posixCommand("pwd", "Print working directory", (ctx, args, session) -> PosixCommands.pwd(ctx, args)));
        cmds.add(posixCommand("echo", "Display a line of text", (ctx, args, session) -> PosixCommands.echo(ctx, args)));
        cmds.add(posixCommand("cat", "Concatenate files", (ctx, args, session) -> PosixCommands.cat(ctx, args)));
        cmds.add(posixCommand("ls", "List directory contents", (ctx, args, session) -> PosixCommands.ls(ctx, args)));
        cmds.add(posixCommand("grep", "Search text patterns", (ctx, args, session) -> PosixCommands.grep(ctx, args)));
        cmds.add(posixCommand(
                "head", "Output the first part of files", (ctx, args, session) -> PosixCommands.head(ctx, args)));
        cmds.add(posixCommand(
                "tail", "Output the last part of files", (ctx, args, session) -> PosixCommands.tail(ctx, args)));
        cmds.add(posixCommand("wc", "Word, line, and byte count", (ctx, args, session) -> PosixCommands.wc(ctx, args)));
        cmds.add(posixCommand(
                "sort", "Sort lines of text files", (ctx, args, session) -> PosixCommands.sort(ctx, args)));
        cmds.add(posixCommand("date", "Display date and time", (ctx, args, session) -> PosixCommands.date(ctx, args)));
        cmds.add(posixCommand(
                "sleep", "Delay for a specified time", (ctx, args, session) -> PosixCommands.sleep(ctx, args)));
        cmds.add(posixCommand(
                "clear", "Clear the terminal screen", (ctx, args, session) -> PosixCommands.clear(ctx, args)));
        this.commands = Collections.unmodifiableList(cmds);
    }

    @Override
    public String name() {
        return "POSIX";
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

    private static Command posixCommand(String name, String description, PosixCommandExecutor executor) {
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
                PosixCommands.Context ctx = createContext(session);
                // Prepend command name to args for options parsing
                String[] argv = new String[args.length + 1];
                argv[0] = name;
                System.arraycopy(args, 0, argv, 1, args.length);
                executor.execute(ctx, argv, session);
                return null;
            }
        };
    }

    @FunctionalInterface
    private interface PosixCommandExecutor {
        void execute(PosixCommands.Context ctx, String[] args, CommandSession session) throws Exception;
    }
}

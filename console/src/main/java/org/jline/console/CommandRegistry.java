/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

import org.jline.reader.Candidate;
import org.jline.reader.LineReader;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.terminal.Terminal;

/**
 * Interface for registering, describing, and executing commands in a console application.
 * <p>
 * The CommandRegistry provides methods for managing a set of commands, including:
 * <ul>
 *   <li>Registering commands and their aliases</li>
 *   <li>Providing command descriptions and usage information</li>
 *   <li>Executing commands with arguments</li>
 *   <li>Creating command completers for tab completion</li>
 * </ul>
 * <p>
 * Implementations of this interface can be used to create custom command registries
 * for specific domains or applications.
 *
 */
@Deprecated(since = "4.0")
public interface CommandRegistry {

    /**
     * Aggregates SystemCompleters from multiple command registries into a single completer.
     * <p>
     * This method combines the completers from all provided command registries without compiling them.
     * The resulting completer can be used for tab completion across all commands from the provided registries.
     *
     * @param commandRegistries the command registries whose completers are to be aggregated
     * @return an uncompiled SystemCompleter containing all completers from the provided registries
     */
    static SystemCompleter aggregateCompleters(CommandRegistry... commandRegistries) {
        SystemCompleter out = new SystemCompleter();
        for (CommandRegistry r : commandRegistries) {
            out.add(r.compileCompleters());
        }
        return out;
    }

    /**
     * Aggregates and compiles SystemCompleters from multiple command registries into a single completer.
     * <p>
     * This method combines the completers from all provided command registries and compiles them into
     * a single completer. The resulting completer can be used for tab completion across all commands
     * from the provided registries.
     *
     * @param commandRegistries the command registries whose completers are to be aggregated and compiled
     * @return a compiled SystemCompleter containing all completers from the provided registries
     */
    static SystemCompleter compileCompleters(CommandRegistry... commandRegistries) {
        SystemCompleter out = aggregateCompleters(commandRegistries);
        out.compile(s -> createCandidate(commandRegistries, s));
        return out;
    }

    /**
     * Creates a completion candidate for the specified command.
     * <p>
     * This method searches for the command in the provided registries and creates a completion
     * candidate with the command's name, group, and description.
     *
     * @param commandRegistries the command registries to search for the command
     * @param command the command name
     * @return a completion candidate for the command
     */
    static Candidate createCandidate(CommandRegistry[] commandRegistries, String command) {
        String group = null, desc = null;
        for (CommandRegistry registry : commandRegistries) {
            if (registry.hasCommand(command)) {
                group = registry.name();
                desc = registry.commandInfo(command).stream().findFirst().orElse(null);
                break;
            }
        }
        return new Candidate(command, command, group, desc, null, null, true);
    }

    /**
     * Returns the name of this registry.
     * @return the name of the registry
     */
    default String name() {
        return this.getClass().getSimpleName();
    }
    /**
     * Returns the command names known by this registry.
     * @return the set of known command names, excluding aliases
     */
    Set<String> commandNames();

    /**
     * Returns a map of alias-to-command names known by this registry.
     * @return a map with alias keys and command name values
     */
    Map<String, String> commandAliases();

    /**
     * Returns a short info about command known by this registry.
     * @param command the command name
     * @return a short info about command
     */
    List<String> commandInfo(String command);

    /**
     * Returns whether a command with the specified name is known to this registry.
     * @param command the command name to test
     * @return true if the specified command is registered
     */
    boolean hasCommand(String command);

    /**
     * Returns a {@code SystemCompleter} that can provide detailed completion
     * information for all registered commands.
     * @return a SystemCompleter that can provide command completion for all registered commands
     */
    SystemCompleter compileCompleters();

    /**
     * Returns a command description for use in the JLine Widgets framework.
     * Default method must be overridden to return sub command descriptions.
     * @param args command (args[0]) and its arguments
     * @return command description for JLine TailTipWidgets to be displayed
     *         in the terminal status bar.
     */
    CmdDesc commandDescription(List<String> args);

    /**
     * Sets the line reader for this command registry.
     * <p>
     * This method allows the registry to access the line reader for terminal interaction.
     * Default implementation does nothing. Implementations that need access to the line
     * reader should override this method.
     *
     * @param reader the line reader to set
     */
    default void setLineReader(LineReader reader) {}

    /**
     * Execute a command.
     * @param session the data of the current command session
     * @param command the name of the command
     * @param args arguments of the command
     * @return result of the command execution
     * @throws Exception in case of error
     */
    default Object invoke(CommandSession session, String command, Object... args) throws Exception {
        throw new IllegalStateException(
                "CommandRegistry method invoke(session, command, ... args) is not implemented!");
    }

    /**
     * Class representing a command execution session.
     * <p>
     * A CommandSession encapsulates the terminal and I/O streams
     * used during command execution.
     *
     * @deprecated Use {@link org.jline.shell.CommandSession} instead, which provides
     *             the enriched API with variables, working directory, exit code, and job support.
     */
    @Deprecated(since = "4.0")
    class CommandSession {
        private final Terminal terminal;
        private final InputStream in;
        private final PrintStream out;
        private final PrintStream err;

        /**
         * Creates a new command session with the system's standard I/O streams.
         */
        public CommandSession() {
            this.in = System.in;
            this.out = System.out;
            this.err = System.err;
            this.terminal = null;
        }

        /**
         * Creates a new command session with the specified terminal.
         *
         * @param terminal the terminal
         */
        public CommandSession(Terminal terminal) {
            this(terminal, terminal.input(), new PrintStream(terminal.output()), new PrintStream(terminal.output()));
        }

        /**
         * Creates a new command session with the specified terminal and I/O streams.
         *
         * @param terminal the terminal
         * @param in the input stream
         * @param out the output stream
         * @param err the error stream
         */
        public CommandSession(Terminal terminal, InputStream in, PrintStream out, PrintStream err) {
            this.terminal = terminal;
            this.in = in;
            this.out = out;
            this.err = err;
        }

        /**
         * Returns the terminal.
         *
         * @return the terminal, or null
         */
        public Terminal terminal() {
            return terminal;
        }

        /**
         * Returns the input stream.
         *
         * @return the input stream
         */
        public InputStream in() {
            return in;
        }

        /**
         * Returns the output stream.
         *
         * @return the output stream
         */
        public PrintStream out() {
            return out;
        }

        /**
         * Returns the error stream.
         *
         * @return the error stream
         */
        public PrintStream err() {
            return err;
        }
    }
}

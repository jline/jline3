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
import java.nio.file.Path;
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
     * A CommandSession encapsulates the terminal, I/O streams, environment variables,
     * working directory, and other context used during command execution.
     */
    class CommandSession {
        /** The terminal for the command session */
        private final Terminal terminal;
        /** The input stream for the command session */
        private final InputStream in;
        /** The output stream for the command session */
        private final PrintStream out;
        /** The error stream for the command session */
        private final PrintStream err;
        /** Session variables / environment */
        private final Map<String, Object> variables;
        /** Working directory */
        private Path workingDirectory;
        /** Exit code of the last executed command */
        private int lastExitCode;
        /** The current foreground job */
        private Job foregroundJob;
        /** Reference to the system registry */
        private SystemRegistry systemRegistry;

        /**
         * Creates a new command session with the system's standard I/O streams.
         * The terminal will be null in this case.
         */
        public CommandSession() {
            this.in = System.in;
            this.out = System.out;
            this.err = System.err;
            this.terminal = null;
            this.variables = new LinkedHashMap<>();
        }

        /**
         * Creates a new command session with the specified terminal.
         * The I/O streams will be derived from the terminal.
         *
         * @param terminal the terminal for the command session
         */
        public CommandSession(Terminal terminal) {
            this(terminal, terminal.input(), new PrintStream(terminal.output()), new PrintStream(terminal.output()));
        }

        /**
         * Creates a new command session with the specified terminal and I/O streams.
         *
         * @param terminal the terminal for the command session
         * @param in the input stream for the command session
         * @param out the output stream for the command session
         * @param err the error stream for the command session
         */
        public CommandSession(Terminal terminal, InputStream in, PrintStream out, PrintStream err) {
            this.terminal = terminal;
            this.in = in;
            this.out = out;
            this.err = err;
            this.variables = new LinkedHashMap<>();
        }

        /**
         * Returns the terminal for the command session.
         *
         * @return the terminal, or null if no terminal is associated with this session
         */
        public Terminal terminal() {
            return terminal;
        }

        /**
         * Returns the input stream for the command session.
         *
         * @return the input stream
         */
        public InputStream in() {
            return in;
        }

        /**
         * Returns the output stream for the command session.
         *
         * @return the output stream
         */
        public PrintStream out() {
            return out;
        }

        /**
         * Returns the error stream for the command session.
         *
         * @return the error stream
         */
        public PrintStream err() {
            return err;
        }

        /**
         * Returns the value of a session variable, or {@code null} if not set.
         *
         * @param name the variable name
         * @return the variable value, or null
         */
        public Object get(String name) {
            return variables.get(name);
        }

        /**
         * Sets a session variable.
         *
         * @param name the variable name
         * @param value the variable value
         */
        public void put(String name, Object value) {
            variables.put(name, value);
        }

        /**
         * Returns all session variables.
         *
         * @return unmodifiable view of the variables map
         */
        public Map<String, Object> variables() {
            return Collections.unmodifiableMap(variables);
        }

        /**
         * Returns the working directory for this session.
         *
         * @return the working directory, or null if not set
         */
        public Path workingDirectory() {
            return workingDirectory;
        }

        /**
         * Sets the working directory for this session.
         *
         * @param workingDirectory the working directory
         */
        public void setWorkingDirectory(Path workingDirectory) {
            this.workingDirectory = workingDirectory;
        }

        /**
         * Returns the exit code of the last executed command.
         *
         * @return the last exit code (0 for success)
         */
        public int lastExitCode() {
            return lastExitCode;
        }

        /**
         * Sets the exit code of the last executed command.
         *
         * @param lastExitCode the exit code
         */
        public void setLastExitCode(int lastExitCode) {
            this.lastExitCode = lastExitCode;
        }

        /**
         * Returns the current foreground job, or {@code null} if none.
         *
         * @return the foreground job, or null
         */
        public Job foregroundJob() {
            return foregroundJob;
        }

        /**
         * Sets the current foreground job.
         *
         * @param foregroundJob the foreground job
         */
        public void setForegroundJob(Job foregroundJob) {
            this.foregroundJob = foregroundJob;
        }

        /**
         * Returns the system registry for this session.
         *
         * @return the system registry, or null if not set
         */
        public SystemRegistry systemRegistry() {
            return systemRegistry;
        }

        /**
         * Sets the system registry for this session.
         *
         * @param systemRegistry the system registry
         */
        public void setSystemRegistry(SystemRegistry systemRegistry) {
            this.systemRegistry = systemRegistry;
        }
    }
}

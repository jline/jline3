/*
 * Copyright (c) 2025, the original author(s).
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
import java.util.Map;
import java.util.function.Function;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

/**
 * Execution context for commands, providing access to I/O streams, terminal, and environment.
 * <p>
 * This interface provides a unified way for commands to access their execution environment,
 * including input/output streams, current directory, terminal capabilities, and variables.
 * It serves as a bridge between different command frameworks and JLine's interactive features.
 * <p>
 * This interface is inspired by and compatible with {@link org.jline.builtins.PosixCommands.Context}
 * but designed to be framework-agnostic and extensible.
 *
 * @see org.jline.builtins.PosixCommands.Context
 */
public interface CommandContext {

    /**
     * Returns the input stream for the command.
     * @return the input stream
     */
    InputStream in();

    /**
     * Returns the output stream for the command.
     * @return the output stream
     */
    PrintStream out();

    /**
     * Returns the error stream for the command.
     * @return the error stream
     */
    PrintStream err();

    /**
     * Returns the current working directory.
     * @return the current directory path
     */
    Path currentDir();

    /**
     * Returns the terminal instance, if available.
     * @return the terminal, or null if not available
     */
    Terminal terminal();

    /**
     * Returns whether this context is connected to a TTY.
     * @return true if connected to a TTY, false otherwise
     */
    default boolean isTty() {
        return terminal() != null;
    }

    /**
     * Gets a variable value by name.
     * @param name the variable name
     * @return the variable value, or null if not found
     */
    Object get(String name);

    /**
     * Sets a variable value.
     * @param name the variable name
     * @param value the variable value
     */
    void set(String name, Object value);

    /**
     * Returns all environment variables as a map.
     * @return the environment variables
     */
    Map<String, Object> getEnvironment();

    /**
     * Returns the line reader instance, if available.
     * This provides access to JLine's interactive features like completion and history.
     * @return the line reader, or null if not available
     */
    LineReader lineReader();

    /**
     * Returns the system registry, if available.
     * This provides access to other registered commands and system-level functionality.
     * @return the system registry, or null if not available
     */
    SystemRegistry systemRegistry();

    /**
     * Creates a new context with a different current directory.
     * @param newCurrentDir the new current directory
     * @return a new context with the updated directory
     */
    CommandContext withCurrentDirectory(Path newCurrentDir);

    /**
     * Creates a new context with additional variables.
     * @param variables the variables to add
     * @return a new context with the additional variables
     */
    CommandContext withVariables(Map<String, Object> variables);

    /**
     * Builder for creating CommandContext instances.
     */
    interface Builder {
        Builder terminal(Terminal terminal);
        Builder currentDir(Path currentDir);
        Builder environment(Map<String, Object> environment);
        Builder lineReader(LineReader lineReader);
        Builder systemRegistry(SystemRegistry systemRegistry);
        Builder variableProvider(Function<String, Object> variableProvider);
        CommandContext build();
    }

    /**
     * Creates a new builder for CommandContext.
     * @return a new builder instance
     */
    static Builder builder() {
        return new DefaultCommandContext.Builder();
    }

    /**
     * Creates a CommandContext from a PosixCommands.Context.
     * @param posixContext the POSIX context to wrap
     * @return a new CommandContext
     */
    static CommandContext from(org.jline.builtins.PosixCommands.Context posixContext) {
        return builder()
                .terminal(posixContext.terminal())
                .currentDir(posixContext.currentDir())
                .variableProvider(posixContext::get)
                .build();
    }

    /**
     * Converts this CommandContext to a PosixCommands.Context.
     * @return a new PosixCommands.Context
     */
    default org.jline.builtins.PosixCommands.Context toPosixContext() {
        return new org.jline.builtins.PosixCommands.Context(
                in(), out(), err(), currentDir(), terminal(), this::get);
    }
}

/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.jline.reader.Completer;

/**
 * Class that encapsulates the execution and completion methods for a command.
 * <p>
 * CommandMethods provides a way to associate a command execution function with
 * a completer compilation function. This allows commands to be registered with
 * both their execution logic and their completion logic in a single object.
 */
public class CommandMethods {
    /** The function that executes the command */
    Function<CommandInput, ?> execute;
    /** The function that compiles completers for the command */
    Function<String, List<Completer>> compileCompleter;

    /**
     * Creates a new CommandMethods with the specified execution and completer compilation functions.
     * <p>
     * This constructor takes a function that returns a result when executing the command.
     *
     * @param execute the function that executes the command and returns a result
     * @param compileCompleter the function that compiles completers for the command
     */
    public CommandMethods(Function<CommandInput, ?> execute, Function<String, List<Completer>> compileCompleter) {
        this.execute = execute;
        this.compileCompleter = compileCompleter;
    }

    /**
     * Creates a new CommandMethods with the specified execution and completer compilation functions.
     * <p>
     * This constructor takes a consumer that doesn't return a result when executing the command.
     * The execution function is wrapped to return null after executing the consumer.
     *
     * @param execute the consumer that executes the command without returning a result
     * @param compileCompleter the function that compiles completers for the command
     */
    public CommandMethods(Consumer<CommandInput> execute, Function<String, List<Completer>> compileCompleter) {
        this.execute = (CommandInput i) -> {
            execute.accept(i);
            return null;
        };
        this.compileCompleter = compileCompleter;
    }

    /**
     * Returns the function that executes the command.
     *
     * @return the function that executes the command
     */
    public Function<CommandInput, ?> execute() {
        return execute;
    }

    /**
     * Returns the function that compiles completers for the command.
     *
     * @return the function that compiles completers for the command
     */
    public Function<String, List<Completer>> compileCompleter() {
        return compileCompleter;
    }
}

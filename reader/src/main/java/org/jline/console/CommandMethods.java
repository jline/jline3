/*
 * Copyright (c) 2002-2020, the original author or authors.
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

public class CommandMethods {
    Consumer<CommandInput> execute;
    Function<CommandInput, Object> executeFunction;
    Function<String, List<Completer>> compileCompleter;

    public CommandMethods(Function<CommandInput, Object> execute,  Function<String, List<Completer>> compileCompleter) {
        this.executeFunction = execute;
        this.compileCompleter = compileCompleter;
    }

    public CommandMethods(Consumer<CommandInput> execute,  Function<String, List<Completer>> compileCompleter) {
        this.execute = execute;
        this.compileCompleter = compileCompleter;
    }

    public Consumer<CommandInput> execute() {
        return execute;
    }

    public Function<CommandInput, Object> executeFunction() {
        return executeFunction;
    }

    public Function<String, List<Completer>> compileCompleter() {
        return compileCompleter;
    }

    public boolean isConsumer() {
        return execute != null;
    }

}

/*
 * Copyright (c) 2002-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.Widget;

/**
 * Manage console variables, commands and script executions.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public interface ConsoleEngine extends CommandRegistry {

    /**
     * Console string variable of nanorc file full path
     */
    String VAR_NANORC = "NANORC";

    /**
     * Removes the command name first character if it is colon
     * @param command the name of the command to complete
     * @return command name without starting colon
     */
    static String plainCommand(String command) {
        return command.startsWith(":") ? command.substring(1) : command;
    }

    /**
     * Sets lineReader
     * @param reader LineReader
     */
    void setLineReader(LineReader reader);

    /**
     * Sets systemRegistry
     * @param systemRegistry SystemRegistry
     */
    void setSystemRegistry(SystemRegistry systemRegistry);

    /**
     * Substituting args references with their values.
     * @param args the arguments to be expanded
     * @return expanded arguments
     * @throws Exception in case of error
     */
    Object[] expandParameters(String[] args) throws Exception;

    /**
     * Substitutes command line with system registry invoke method call.
     * @param line command line to be expanded
     * @return expanded command line
     */
    String expandCommandLine(String line);

    /**
     * Expands parameter list to string
     * @param params list of script parameters
     * @return expanded parameters list
     */
    String expandToList(List<String> params);

    /**
     * Returns all scripts found from PATH
     * @return map keys have script file names and value is true if it is console script
     */
    Map<String, Boolean> scripts();

    /**
     * Sets file name extension used by console scripts
     * @param extension console script file extension
     */
    void setScriptExtension(String extension);

    /**
     * Returns true if alias 'name' exists
     * @param name alias name
     * @return true if alias exists
     */
    boolean hasAlias(String name);

    /**
     * Returns alias 'name' value
     * @param name alias name
     * @return value of alias
     */
    String getAlias(String name);

    /**
     * Returns defined pipes
     * @return map of defined pipes
     */
    Map<String, List<String>> getPipes();

    /**
     * Returns named pipe names
     * @return list of named pipe names
     */
    List<String> getNamedPipes();

    /**
     * Returns script and variable completers
     * @return script and variable completers
     */
    List<Completer> scriptCompleters();

    /**
     * Persist object to file
     * @param file file where object should be written
     * @param object object to persist
     */
    void persist(Path file, Object object);

    /**
     * Read object from file
     * @param file file from where object should be read
     * @return object
     * @throws IOException in case of error
     */
    Object slurp(Path file) throws IOException;

    /**
     * Read console option value
     * @param <T> option type
     * @param option option name
     * @param defval default value
     * @return option value
     */
    <T> T consoleOption(String option, T defval);

    /**
     * Set console option value
     * @param name the option name
     * @param value value to assign console option
     */
    void setConsoleOption(String name, Object value);

    /**
     * Executes command line that does not contain known command by the system registry.
     * If the line is neither JLine or ScriptEngine script it will be evaluated
     * as ScriptEngine statement.
     * @param name parsed command/script name
     * @param rawLine raw command line
     * @param args parsed arguments of the command
     * @return command line execution result
     * @throws Exception in case of error
     */
    Object execute(String name, String rawLine, String[] args) throws Exception;

    /**
     * Executes either JLine or ScriptEngine script.
     * @param script script file
     * @return script execution result
     * @throws Exception in case of error
     */
    default Object execute(File script) throws Exception {
        return execute(script, "", new String[0]);
    }

    /**
     * Executes either JLine or ScriptEngine script.
     * @param script script file
     * @param rawLine raw command line
     * @param args script arguments
     * @return script execution result
     * @throws Exception in case of error
     */
    Object execute(File script, String rawLine, String[] args) throws Exception;

    /**
     * Post processes execution result. If result is to be assigned to the console variable
     * then method will return null.
     * @param line command line
     * @param result command result to process
     * @param output command redirected output
     * @return processed result
     */
    ExecutionResult postProcess(String line, Object result, String output);

    /**
     * Post processes execution result.
     * @param result command result to process
     * @return processed result
     */
    ExecutionResult postProcess(Object result);

    /**
     * Print object if trace is enabled
     * @param object object to print
     */
    void trace(Object object);

    /**
     * Print object.
     * @param object object to print
     */
    void println(Object object);

    /**
     * Create console variable
     * @param name name of the variable
     * @param value value of the variable
     */
    void putVariable(String name, Object value);

    /**
     * Get variable value
     * @param name name of the variable
     * @return variable value
     */
    Object getVariable(String name);

    /**
     * Test if variable with name exists
     * @param name name of the variable
     * @return true if variable with name exists
     */
    boolean hasVariable(String name);

    /**
     * Delete temporary console variables
     */
    void purge();

    /**
     * Execute widget function
     * @param function to execute
     * @return true on success
     */
    boolean executeWidget(Object function);

    /**
     * Checks if consoleEngine is executing script
     * @return true when executing script
     */
    boolean isExecuting();

    class ExecutionResult {
        final int status;
        final Object result;

        public ExecutionResult(int status, Object result) {
            this.status = status;
            this.result = result;
        }

        public int status() {
            return status;
        }

        public Object result() {
            return result;
        }
    }

    class WidgetCreator implements Widget {
        private final ConsoleEngine consoleEngine;
        private final Object function;
        private final String name;

        public WidgetCreator(ConsoleEngine consoleEngine, String function) {
            this.consoleEngine = consoleEngine;
            this.name = function;
            this.function = consoleEngine.getVariable(function);
        }

        @Override
        public boolean apply() {
            return consoleEngine.executeWidget(function);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

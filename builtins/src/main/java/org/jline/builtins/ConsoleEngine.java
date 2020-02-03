/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.jline.builtins.CommandRegistry;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.Widget;

/**
 * Manage console variables, commands and script executions.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public interface ConsoleEngine extends CommandRegistry {

    /**
     * Removes command first character if it is colon
     * @param command name to complete
     * @return command name without colon
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
     * @param systemRegistry
     */
    void setSystemRegistry(SystemRegistry systemRegistry);

    /**
     * Substituting args references with their values.
     * @param args
     * @return Substituted args
     * @throws Exception in case of error
     */
    Object[] expandParameters(String[] args) throws Exception;

    /**
     * Returns all scripts found from PATH
     * @return script names
     */
    List<String> scripts();

    /**
     * Sets file name extension used by console scripts
     * @param console script file extension
     */
    public void setScriptExtension(String extension);

    /**
     * Returns true if alias 'name' exists
     * @param alias name
     * @return true if alias exists
     */
    boolean hasAlias(String name);

    /**
     * Returns alias 'name' value
     * @param alias name
     * @return value of alias
     */
    String getAlias(String name);

    /**
     * Returns script and variable completers
     * @return script completers
     */
    List<Completer> scriptCompleters();

    /**
     * Executes parsed line that does not contain known command by the system registry.
     * If parsed line is neither JLine or ScriptEngine script it will be evaluated
     * as ScriptEngine statement.
     * @param parsed command line
     * @return command line execution result
     * @throws Exception in case of error
     */
    Object execute(ParsedLine parsedLine) throws Exception;

    /**
     * Executes either JLine or ScriptEngine script.
     * @param script file
     * @return script execution result
     * @throws Exception in case of error
     */
    default Object execute(File script) throws Exception {
        return execute(script, "", new String[0]);
    }

    /**
     * Executes either JLine or ScriptEngine script.
     * @param script file
     * @param cmdLine complete command line
     * @param script arguments
     * @return script execution result
     * @throws Exception in case of error
     */
    Object execute(File script, String cmdLine, String[] args) throws Exception;

    /**
     * Post processes execution result. If result is to be assigned to the console variable
     * then method will return null.
     * @param command line
     * @param result to process
     * @return processed result
     */
    Object postProcess(String line, Object result, String output);

    /**
     * Displays object.
     * @param object to print
     */
    void println(Object object);

    /**
     * Displays object.
     * @param options println options
     * @param object to print
     */
    void println(Map<String, Object> options, Object object);

    /**
     * Get variable value
     * @param name of variable
     * @return variable value
     */
    Object getVariable(String name);

    /**
     * Execute widget function
     * @param function to execute
     * @return true on success
     */
    boolean executeWidget(Object function);

    boolean isExecuting();
    
    static class WidgetCreator implements Widget {
        private ConsoleEngine consoleEngine;
        private Object function;
        private String name;

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

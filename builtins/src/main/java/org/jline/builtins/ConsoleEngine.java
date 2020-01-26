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
import org.jline.reader.ParsedLine;

/**
 * Manage console variables, commands and script executions. 
 * 
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public interface ConsoleEngine extends CommandRegistry {

    /**
     * Removes command first character if it is colon
     * @param command
     * @return
     */
    static String plainCommand(String command) {
        return command.startsWith(":") ? command.substring(1) : command; 
    }

    /**
     * Sets systemRegistry
     * @param systemRegistry
     */
    void setSystemRegistry(SystemRegistry systemRegistry);

    /**
     * Substituting args references with their values.
     * @param args
     * @return Substituted args
     * @throws Exception
     */
    Object[] expandParameters(String[] args) throws Exception;
  
    /**
     * Returns all scripts found from PATH
     * @return script names
     */
    List<String> scripts();
    
    /**
     * Returns true if alias 'name' exists
     * @param alias name
     * @return
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
     * @return completers
     */
    List<Completer> scriptCompleters();
    
    /**
     * Executes parsed line that does not contain known command by the system registry.
     * If parsed line is neither JLine or ScriptEngine script it will be evaluated
     * as ScriptEngine statement.
     * @param parsedLine
     * @return result
     * @throws Exception
     */
    Object execute(ParsedLine parsedLine) throws Exception;

    /**
     * Executes either JLine or ScriptEngine script.
     * @param script
     * @return result
     * @throws Exception
     */
    default Object execute(File script) throws Exception {
        return execute(script, "", new String[0]);
    }

    /**
     * Executes either JLine or ScriptEngine script.
     * @param script
     * @param cmdLine
     * @param args
     * @return result
     * @throws Exception
     */
    Object execute(File script, String cmdLine, String[] args) throws Exception;

    /**
     * Post processes execution result. If result is to be assigned to the console variable
     * then method will return null.
     * @param line
     * @param result
     * @return result
     */
    Object postProcess(String line, Object result);

    /**
     * Displays object.
     * @param object
     */
    void println(Object object);

    /**
     * Displays object.
     * @param options
     * @param object
     */
    void println(Map<String, Object> options, Object object);

}

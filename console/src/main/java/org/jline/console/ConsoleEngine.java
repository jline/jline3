/*
 * Copyright (c) 2002-2025, the original author(s).
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
 * Interface for managing console variables, commands, and script execution in a console application.
 * <p>
 * The ConsoleEngine extends CommandRegistry to provide additional functionality for:
 * <ul>
 *   <li>Managing console variables and their values</li>
 *   <li>Executing scripts and commands</li>
 *   <li>Handling command line expansion and parameter substitution</li>
 *   <li>Managing aliases and pipes</li>
 *   <li>Post-processing command execution results</li>
 * </ul>
 * <p>
 * Implementations of this interface can be used to create custom console engines
 * for specific scripting languages or execution environments.
 *
 */
public interface ConsoleEngine extends CommandRegistry {

    /**
     * Console string variable containing the full path to the nanorc configuration file.
     * This variable can be used to customize the nano editor's behavior when used within the console.
     */
    String VAR_NANORC = "NANORC";

    /**
     * Removes the first character of the command name if it is a colon.
     * <p>
     * This method is used to normalize command names that may be prefixed with a colon,
     * which is a common convention in some command-line interfaces.
     *
     * @param command the name of the command to normalize
     * @return the command name without the starting colon, or the original command name if it doesn't start with a colon
     */
    static String plainCommand(String command) {
        return command.startsWith(":") ? command.substring(1) : command;
    }

    /**
     * Sets the LineReader instance to be used by this console engine.
     * <p>
     * The LineReader is used for reading input from the user and providing features
     * like command history, tab completion, and line editing.
     *
     * @param reader the LineReader instance to use
     */
    void setLineReader(LineReader reader);

    /**
     * Sets the SystemRegistry instance to be used by this console engine.
     * <p>
     * The SystemRegistry is used for executing commands and managing the console environment.
     * It provides access to registered commands and handles command execution.
     *
     * @param systemRegistry the SystemRegistry instance to use
     */
    void setSystemRegistry(SystemRegistry systemRegistry);

    /**
     * Substitutes argument references with their values.
     * <p>
     * This method expands arguments that reference variables or other values,
     * replacing them with their actual values. For example, a reference like "$VAR"
     * might be replaced with the value of the variable "VAR".
     *
     * @param args the arguments to be expanded
     * @return the expanded arguments with references replaced by their values
     * @throws Exception if an error occurs during expansion
     */
    Object[] expandParameters(String[] args) throws Exception;

    /**
     * Substitutes a command line with a system registry invoke method call.
     * <p>
     * This method expands a command line by replacing it with a call to the system registry's
     * invoke method. This is used to handle command execution through the system registry.
     *
     * @param line the command line to be expanded
     * @return the expanded command line with the system registry invoke method call
     */
    String expandCommandLine(String line);

    /**
     * Expands a list of script parameters to a string representation.
     * <p>
     * This method converts a list of script parameters into a string that can be used
     * in script execution. The parameters may be expanded or formatted according to
     * the console engine's rules.
     *
     * @param params the list of script parameters to expand
     * @return a string representation of the expanded parameters list
     */
    String expandToList(List<String> params);

    /**
     * Returns all scripts found in the PATH environment variable.
     * <p>
     * This method searches for scripts in the directories specified by the PATH environment
     * variable and returns a map of script names to a boolean indicating whether each script
     * is a console script.
     *
     * @return a map where keys are script file names and values are true if the script is a console script
     */
    Map<String, Boolean> scripts();

    /**
     * Sets the file name extension used by console scripts.
     * <p>
     * This method configures the file extension that the console engine will recognize
     * as indicating a console script. Files with this extension will be treated as
     * console scripts when executing scripts.
     *
     * @param extension the file extension to use for console scripts (e.g., ".jsh")
     */
    void setScriptExtension(String extension);

    /**
     * Checks if an alias with the specified name exists.
     * <p>
     * This method determines whether an alias has been defined for the specified name.
     * Aliases can be used to create shortcuts for commands or command sequences.
     *
     * @param name the alias name to check
     * @return true if an alias with the specified name exists, false otherwise
     */
    boolean hasAlias(String name);

    /**
     * Returns the value of the alias with the specified name.
     * <p>
     * This method retrieves the command or command sequence that the specified alias
     * is defined to represent. Aliases can be used to create shortcuts for commands
     * or command sequences.
     *
     * @param name the alias name
     * @return the value of the alias, or null if no alias with the specified name exists
     */
    String getAlias(String name);

    /**
     * Returns all defined pipes.
     * <p>
     * This method retrieves a map of all pipes defined in the console engine.
     * Pipes are used to connect the output of one command to the input of another.
     *
     * @return a map of defined pipes, where keys are pipe names and values are pipe definitions
     */
    Map<String, List<String>> getPipes();

    /**
     * Returns the names of all named pipes.
     * <p>
     * This method retrieves a list of all named pipes defined in the console engine.
     * Named pipes are pipes that have been given a specific name for easier reference.
     *
     * @return a list of named pipe names
     */
    List<String> getNamedPipes();

    /**
     * Returns completers for scripts and variables.
     * <p>
     * This method retrieves a list of completers that can be used for tab completion
     * of script names and variable names in the console.
     *
     * @return a list of completers for scripts and variables
     */
    List<Completer> scriptCompleters();

    /**
     * Persists an object to a file.
     * <p>
     * This method serializes the specified object and writes it to the specified file.
     * The object can later be read back using the {@link #slurp(Path)} method.
     *
     * @param file the file to write the object to
     * @param object the object to persist
     */
    void persist(Path file, Object object);

    /**
     * Reads an object from a file.
     * <p>
     * This method reads and deserializes an object from the specified file.
     * The object should have been written using the {@link #persist(Path, Object)} method.
     *
     * @param file the file to read the object from
     * @return the deserialized object
     * @throws IOException if an I/O error occurs while reading the file
     */
    Object slurp(Path file) throws IOException;

    /**
     * Reads a console option value with a default value if the option doesn't exist.
     * <p>
     * This method retrieves the value of a console option, returning a default value
     * if the option doesn't exist. Console options are used to configure the behavior
     * of the console engine and its components.
     *
     * @param <T> the type of the option value
     * @param option the name of the option to read
     * @param defval the default value to return if the option doesn't exist
     * @return the value of the option, or the default value if the option doesn't exist
     */
    <T> T consoleOption(String option, T defval);

    /**
     * Sets a console option value.
     * <p>
     * This method sets the value of a console option. Console options are used to
     * configure the behavior of the console engine and its components.
     *
     * @param name the name of the option to set
     * @param value the value to assign to the option
     */
    void setConsoleOption(String name, Object value);

    /**
     * Executes a command line that does not contain a command known by the system registry.
     * <p>
     * This method handles the execution of command lines that are not recognized as commands
     * by the system registry. If the line is neither a JLine script nor a ScriptEngine script,
     * it will be evaluated as a ScriptEngine statement.
     *
     * @param name the parsed command or script name
     * @param rawLine the raw command line as entered by the user
     * @param args the parsed arguments of the command
     * @return the result of executing the command line
     * @throws Exception if an error occurs during execution
     */
    Object execute(String name, String rawLine, String[] args) throws Exception;

    /**
     * Executes either a JLine script or a ScriptEngine script.
     * <p>
     * This method executes the specified script file, determining whether it is a JLine script
     * or a ScriptEngine script based on its extension or content. This is a convenience method
     * that calls {@link #execute(Path, String, String[])} with an empty raw line and no arguments.
     *
     * @param script the script file to execute
     * @return the result of executing the script
     * @throws Exception if an error occurs during execution
     */
    default Object execute(File script) throws Exception {
        return execute(script, "", new String[0]);
    }

    /**
     * Executes either a JLine script or a ScriptEngine script with the specified arguments.
     * <p>
     * This method executes the specified script file with the specified arguments, determining
     * whether it is a JLine script or a ScriptEngine script based on its extension or content.
     * This is a convenience method that calls {@link #execute(Path, String, String[])} with the
     * script file converted to a Path.
     *
     * @param script the script file to execute
     * @param rawLine the raw command line as entered by the user
     * @param args the arguments to pass to the script
     * @return the result of executing the script
     * @throws Exception if an error occurs during execution
     */
    default Object execute(File script, String rawLine, String[] args) throws Exception {
        return execute(script != null ? script.toPath() : null, rawLine, args);
    }

    /**
     * Executes either a JLine script or a ScriptEngine script with the specified arguments.
     * <p>
     * This method executes the specified script file with the specified arguments, determining
     * whether it is a JLine script or a ScriptEngine script based on its extension or content.
     *
     * @param script the script file to execute
     * @param rawLine the raw command line as entered by the user
     * @param args the arguments to pass to the script
     * @return the result of executing the script
     * @throws Exception if an error occurs during execution
     */
    Object execute(Path script, String rawLine, String[] args) throws Exception;

    /**
     * Post-processes the result of executing a command.
     * <p>
     * This method processes the result of executing a command, handling any special cases
     * such as assigning the result to a console variable. If the result is to be assigned
     * to a console variable, this method will return null.
     *
     * @param line the command line that was executed
     * @param result the result of executing the command
     * @param output the redirected output of the command, if any
     * @return the processed result, or null if the result was assigned to a console variable
     */
    ExecutionResult postProcess(String line, Object result, String output);

    /**
     * Post-processes the result of executing a command.
     * <p>
     * This method processes the result of executing a command, handling any special cases.
     * This is a convenience method that calls {@link #postProcess(String, Object, String)}
     * with a null command line and output.
     *
     * @param result the result of executing the command
     * @return the processed result
     */
    ExecutionResult postProcess(Object result);

    /**
     * Prints an object if tracing is enabled.
     * <p>
     * This method prints the specified object to the console if tracing is enabled.
     * Tracing can be used for debugging or logging purposes.
     *
     * @param object the object to print
     */
    void trace(Object object);

    /**
     * Prints an object to the console.
     * <p>
     * This method prints the specified object to the console, regardless of whether
     * tracing is enabled.
     *
     * @param object the object to print
     */
    void println(Object object);

    /**
     * Creates or updates a console variable.
     * <p>
     * This method creates a new console variable with the specified name and value,
     * or updates an existing variable if one with the specified name already exists.
     *
     * @param name the name of the variable to create or update
     * @param value the value to assign to the variable
     */
    void putVariable(String name, Object value);

    /**
     * Gets the value of a console variable.
     * <p>
     * This method retrieves the value of the console variable with the specified name.
     *
     * @param name the name of the variable to get
     * @return the value of the variable, or null if no variable with the specified name exists
     */
    Object getVariable(String name);

    /**
     * Tests if a variable with the specified name exists.
     * <p>
     * This method determines whether a console variable with the specified name exists.
     *
     * @param name the name of the variable to check
     * @return true if a variable with the specified name exists, false otherwise
     */
    boolean hasVariable(String name);

    /**
     * Deletes temporary console variables.
     * <p>
     * This method removes all temporary console variables, which are typically
     * created during command execution and are not meant to persist between commands.
     */
    void purge();

    /**
     * Executes a widget function.
     * <p>
     * This method executes the specified widget function, which can be used to
     * perform custom actions in the console.
     *
     * @param function the widget function to execute
     * @return true if the function was executed successfully, false otherwise
     */
    boolean executeWidget(Object function);

    /**
     * Checks if the console engine is currently executing a script.
     * <p>
     * This method determines whether the console engine is in the process of
     * executing a script, as opposed to processing interactive commands.
     *
     * @return true if the console engine is executing a script, false otherwise
     */
    boolean isExecuting();

    /**
     * Class representing the result of executing a command.
     * <p>
     * An ExecutionResult encapsulates the status code and result value of a command execution.
     */
    class ExecutionResult {
        /** The status code of the command execution */
        final int status;
        /** The result value of the command execution */
        final Object result;

        /**
         * Creates a new execution result with the specified status code and result value.
         *
         * @param status the status code of the command execution
         * @param result the result value of the command execution
         */
        public ExecutionResult(int status, Object result) {
            this.status = status;
            this.result = result;
        }

        /**
         * Returns the status code of the command execution.
         *
         * @return the status code
         */
        public int status() {
            return status;
        }

        /**
         * Returns the result value of the command execution.
         *
         * @return the result value
         */
        public Object result() {
            return result;
        }
    }

    /**
     * Class for creating widgets from console functions.
     * <p>
     * A WidgetCreator creates a widget that executes a function defined in the console.
     * This allows console functions to be bound to key sequences and used as widgets.
     */
    class WidgetCreator implements Widget {
        /** The console engine that will execute the function */
        private final ConsoleEngine consoleEngine;
        /** The function to execute */
        private final Object function;
        /** The name of the function */
        private final String name;

        /**
         * Creates a new widget creator for the specified function.
         *
         * @param consoleEngine the console engine that will execute the function
         * @param function the name of the function to execute
         */
        public WidgetCreator(ConsoleEngine consoleEngine, String function) {
            this.consoleEngine = consoleEngine;
            this.name = function;
            this.function = consoleEngine.getVariable(function);
        }

        /**
         * Executes the function when the widget is applied.
         *
         * @return true if the function was executed successfully, false otherwise
         */
        @Override
        public boolean apply() {
            return consoleEngine.executeWidget(function);
        }

        /**
         * Returns the name of the function.
         *
         * @return the name of the function
         */
        @Override
        public String toString() {
            return name;
        }
    }
}

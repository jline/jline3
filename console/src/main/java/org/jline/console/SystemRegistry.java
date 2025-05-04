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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jline.builtins.ConsoleOptionGetter;
import org.jline.reader.Completer;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;

/**
 * Interface for aggregating command registries and dispatching command executions in a console application.
 * <p>
 * The SystemRegistry serves as the central registry for commands in a console application.
 * It aggregates multiple command registries, handles command execution, and provides
 * facilities for command completion, description, and error handling.
 * <p>
 * The SystemRegistry is responsible for:
 * <ul>
 *   <li>Aggregating multiple command registries</li>
 *   <li>Dispatching command executions to the appropriate registry</li>
 *   <li>Providing command completion and description</li>
 *   <li>Handling command execution errors</li>
 *   <li>Managing console options and variables</li>
 * </ul>
 */
public interface SystemRegistry extends CommandRegistry, ConsoleOptionGetter {

    /**
     * Sets the command registries to be used by this system registry.
     * <p>
     * This method configures the command registries that will be aggregated by this
     * system registry. Commands from all of these registries will be available for
     * execution through this system registry.
     *
     * @param commandRegistries the command registries to be used by the application
     */
    void setCommandRegistries(CommandRegistry... commandRegistries);

    /**
     * Registers a subcommand registry for a main command.
     * <p>
     * This method associates a subcommand registry with a main command, allowing
     * the system registry to delegate subcommand execution to the appropriate registry.
     * This is useful for implementing command hierarchies.
     *
     * @param command the main command name
     * @param subcommandRegistry the registry containing the subcommands for the main command
     */
    void register(String command, CommandRegistry subcommandRegistry);

    /**
     * Initializes the console engine environment by executing a console script.
     * <p>
     * This method executes the specified script to initialize the console engine
     * environment. The script can set up variables, aliases, and other configuration
     * needed for the console application.
     *
     * @param script the initialization script to execute
     */
    void initialize(File script);

    /**
     * Returns the names of all pipes defined in this system registry.
     * <p>
     * This method retrieves the names of all pipes that have been defined in this
     * system registry. Pipes are used to connect the output of one command to the
     * input of another.
     *
     * @return a collection of pipe names defined in this system registry
     */
    Collection<String> getPipeNames();

    /**
     * Returns a command completer that includes console variable and script completion.
     * <p>
     * This method creates a completer that can provide completion for commands,
     * console variables, and scripts. The completer can be used for tab completion
     * in the console.
     *
     * @return a completer for commands, console variables, and scripts
     */
    Completer completer();

    /**
     * Returns a description for a command, method, or syntax for use in the JLine Widgets framework.
     * <p>
     * This method generates a description for the specified command line, which can be
     * displayed in the terminal status bar by JLine TailTipWidgets. The description
     * includes information about the command's arguments, options, and usage.
     *
     * @param line the command line whose description to return
     * @return a command description for JLine TailTipWidgets to be displayed in the terminal status bar
     */
    CmdDesc commandDescription(CmdLine line);

    /**
     * Executes a command, script, or evaluates a script engine statement.
     * <p>
     * This method parses and executes the specified command line. If the line contains
     * a known command, it will be executed. If it contains a script name, the script
     * will be executed. Otherwise, the line will be evaluated as a script engine statement.
     *
     * @param line the command line to be executed
     * @return the result of executing the command line
     * @throws Exception if an error occurs during execution
     */
    Object execute(String line) throws Exception;

    /**
     * Deletes temporary console variables and resets output streams.
     * <p>
     * This method cleans up temporary console variables and resets output streams
     * to their default state. It should be called after command execution to ensure
     * that temporary variables and redirected output streams don't affect subsequent
     * commands.
     */
    void cleanUp();

    /**
     * Prints an exception on the terminal.
     * <p>
     * This method prints the specified exception on the terminal, including its
     * message and stack trace. This is a convenience method that calls
     * {@link #trace(boolean, Throwable)} with stack=true.
     *
     * @param exception the exception to print on the terminal
     */
    void trace(Throwable exception);

    /**
     * Prints an exception on the terminal with control over stack trace display.
     * <p>
     * This method prints the specified exception on the terminal. If stack is true,
     * the full stack trace will be printed. Otherwise, only the exception message
     * will be printed.
     *
     * @param stack whether to print the full stack trace (true) or just the message (false)
     * @param exception the exception to be printed
     */
    void trace(boolean stack, Throwable exception);

    /**
     * Returns the value of a console option.
     * <p>
     * This method retrieves the value of the console option with the specified name.
     * Console options are used to configure the behavior of the console and its components.
     *
     * @param name the name of the option to retrieve
     * @return the value of the option, or null if the option doesn't exist
     */
    Object consoleOption(String name);

    /**
     * Returns the value of a console option with a default value if the option doesn't exist.
     * <p>
     * This method retrieves the value of the console option with the specified name,
     * returning a default value if the option doesn't exist. Console options are used
     * to configure the behavior of the console and its components.
     *
     * @param <T> the type of the option value
     * @param name the name of the option to retrieve
     * @param defVal the default value to return if the option doesn't exist
     * @return the value of the option, or the default value if the option doesn't exist
     */
    <T> T consoleOption(String name, T defVal);

    /**
     * Sets the value of a console option.
     * <p>
     * This method sets the value of the console option with the specified name.
     * Console options are used to configure the behavior of the console and its components.
     *
     * @param name the name of the option to set
     * @param value the value to assign to the option
     */
    void setConsoleOption(String name, Object value);

    /**
     * Returns the terminal associated with this system registry.
     * <p>
     * This method retrieves the terminal that is used by this system registry for
     * input and output operations.
     *
     * @return the terminal associated with this system registry
     */
    Terminal terminal();

    /**
     * Executes a command with the specified arguments.
     * <p>
     * This method executes the specified command with the specified arguments.
     * The command is looked up in the command registries associated with this system registry.
     *
     * @param command the command to be executed
     * @param args the arguments to pass to the command
     * @return the result of executing the command
     * @throws Exception if an error occurs during execution
     */
    Object invoke(String command, Object... args) throws Exception;

    /**
     * Checks if a parsed line contains a command or script that is known to this registry.
     * <p>
     * This method determines whether the specified parsed command line contains a command
     * or script that is known to this registry. This can be used to determine whether
     * the line can be executed by this registry.
     *
     * @param line the parsed command line to test
     * @return true if the specified line contains a command or script that is known to this registry, false otherwise
     */
    boolean isCommandOrScript(ParsedLine line);

    /**
     * Checks if a command or script is known to this registry.
     * <p>
     * This method determines whether the specified command or script is known to this registry.
     * This can be used to determine whether the command or script can be executed by this registry.
     *
     * @param command the command or script name to test
     * @return true if the specified command or script is known to this registry, false otherwise
     */
    boolean isCommandOrScript(String command);

    /**
     * Checks if an alias is a known command alias.
     * <p>
     * This method determines whether the specified alias is a known command alias.
     * Command aliases are alternative names for commands that can be used to invoke them.
     *
     * @param alias the alias to test
     * @return true if the specified alias is a known command alias, false otherwise
     */
    boolean isCommandAlias(String alias);

    /**
     * Orderly closes this system registry.
     * <p>
     * This method performs an orderly shutdown of this system registry, releasing
     * any resources it holds and performing any necessary cleanup operations.
     * It should be called when the system registry is no longer needed.
     */
    void close();
    /**
     * Returns the system registry associated with the current thread.
     * <p>
     * This method retrieves the system registry that has been associated with the
     * current thread using the {@link #add(SystemRegistry)} method. This can be used
     * to access the system registry from code that doesn't have a direct reference to it.
     *
     * @return the system registry associated with the current thread, or null if none is associated
     */
    static SystemRegistry get() {
        return Registeries.getInstance().getSystemRegistry();
    }

    /**
     * Associates a system registry with the current thread.
     * <p>
     * This method associates the specified system registry with the current thread,
     * making it accessible via the {@link #get()} method. This can be used to make
     * the system registry available to code that doesn't have a direct reference to it.
     *
     * @param systemRegistry the system registry to associate with the current thread
     */
    static void add(SystemRegistry systemRegistry) {
        Registeries.getInstance().addRegistry(systemRegistry);
    }

    /**
     * Removes the system registry association from the current thread.
     * <p>
     * This method removes the association between the current thread and its system registry,
     * making the system registry no longer accessible via the {@link #get()} method.
     * This should be called when the thread is done using the system registry.
     */
    static void remove() {
        Registeries.getInstance().removeRegistry();
    }

    /**
     * Class for managing the system registry store.
     * <p>
     * This class provides a thread-local store for system registries, allowing each thread
     * to have its own associated system registry. It is used internally by the
     * {@link #get()}, {@link #add(SystemRegistry)}, and {@link #remove()} methods.
     */
    class Registeries {
        private static final Registeries instance = new Registeries();
        private final Map<Long, SystemRegistry> systemRegisteries = new HashMap<>();

        private Registeries() {}

        protected static Registeries getInstance() {
            return instance;
        }

        protected void addRegistry(SystemRegistry systemRegistry) {
            systemRegisteries.put(getThreadId(), systemRegistry);
        }

        protected SystemRegistry getSystemRegistry() {
            return systemRegisteries.getOrDefault(getThreadId(), null);
        }

        protected void removeRegistry() {
            systemRegisteries.remove(getThreadId());
        }

        // TODO: Thread.getId() should be replaced with Thread.threadId() when minimum is JDK >= 19
        @SuppressWarnings("deprecation")
        private static long getThreadId() {
            return Thread.currentThread().getId();
        }
    }
}

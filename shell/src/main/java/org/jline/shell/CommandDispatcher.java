/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.io.File;
import java.util.List;

import org.jline.reader.Completer;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Central dispatcher that aggregates {@link CommandGroup}s and handles
 * command resolution, execution, and completion.
 * <p>
 * {@code CommandDispatcher} separates the aggregation and dispatch role
 * from individual command semantics defined in {@link Command} and
 * {@link CommandGroup}.
 * <p>
 * Example:
 * <pre>
 * CommandDispatcher dispatcher = new DefaultCommandDispatcher(terminal);
 * dispatcher.addGroup(myGroup);
 * Object result = dispatcher.execute("echo hello | grep h");
 * </pre>
 *
 * @see CommandGroup
 * @see Command
 * @see org.jline.shell.impl.DefaultCommandDispatcher
 * @since 4.0
 */
public interface CommandDispatcher extends AutoCloseable {

    /**
     * Adds a command group to this dispatcher.
     *
     * @param group the command group to add
     */
    void addGroup(CommandGroup group);

    /**
     * Returns all registered command groups.
     *
     * @return the list of command groups
     */
    List<CommandGroup> groups();

    /**
     * Finds a command by name or alias across all registered groups.
     *
     * @param name the command name or alias
     * @return the command, or null if not found
     */
    default Command findCommand(String name) {
        for (CommandGroup group : groups()) {
            Command cmd = group.command(name);
            if (cmd != null) {
                return cmd;
            }
        }
        return null;
    }

    /**
     * Executes a command line string.
     * <p>
     * The dispatcher parses the line (potentially into a pipeline) and
     * executes the commands.
     *
     * @param line the command line to execute
     * @return the result of execution
     * @throws Exception if execution fails
     */
    Object execute(String line) throws Exception;

    /**
     * Executes a pre-parsed pipeline.
     *
     * @param pipeline the pipeline to execute
     * @return the result of execution
     * @throws Exception if execution fails
     */
    Object execute(Pipeline pipeline) throws Exception;

    /**
     * Returns a completer that provides tab completion across all registered commands.
     *
     * @return the aggregated completer
     */
    Completer completer();

    /**
     * Returns a description for the command at the current command line position.
     * <p>
     * Used by widgets for context-sensitive help.
     *
     * @param commandLine the parsed command line
     * @return the command description, or null
     */
    CommandDescription describe(CommandLine commandLine);

    /**
     * Returns the terminal associated with this dispatcher.
     *
     * @return the terminal
     */
    Terminal terminal();

    /**
     * Initializes the dispatcher, optionally executing a startup script.
     *
     * @param script the initialization script, or null
     * @throws Exception if initialization fails
     */
    default void initialize(File script) throws Exception {}

    /**
     * Performs cleanup between command executions (e.g., resetting state).
     */
    default void cleanUp() {}

    /**
     * @return A {@link AttributedStyle} to be used when printing exception messages.
     */
    default AttributedStyle errorStyle() {
        return AttributedStyle.DEFAULT.foreground(AttributedStyle.RED);
    }

    /**
     * Traces an exception (e.g., prints it to the terminal).
     *
     * @param exception the exception to trace
     */
    default void trace(Throwable exception) {
        if (exception instanceof UnknownCommandException) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.styled(errorStyle(), exception.getMessage());
            asb.toAttributedString().println(terminal());
            return;
        }
        exception.printStackTrace(terminal().writer());
    }

    /**
     * Closes this dispatcher and releases resources.
     */
    @Override
    void close();
}

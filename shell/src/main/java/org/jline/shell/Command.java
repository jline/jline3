/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.util.List;

import org.jline.reader.Completer;

/**
 * Represents a single executable command in the shell.
 * <p>
 * A {@code Command} encapsulates everything about a command in one object:
 * its name, aliases, description, execution logic, and completion support.
 * <p>
 * Example:
 * <pre>
 * Command echo = new AbstractCommand("echo") {
 *     &#64;Override
 *     public Object execute(CommandSession session, String[] args) {
 *         session.out().println(String.join(" ", args));
 *         return null;
 *     }
 * };
 * </pre>
 *
 * @see CommandGroup
 * @see CommandDispatcher
 * @see CommandSession
 * @since 4.0
 */
public interface Command {

    /**
     * Returns the primary name of this command.
     *
     * @return the command name, never null
     */
    String name();

    /**
     * Returns alternative names for this command.
     * <p>
     * The default implementation returns an empty list.
     *
     * @return the list of aliases, never null
     */
    default List<String> aliases() {
        return List.of();
    }

    /**
     * Returns a short, one-line description of this command.
     * <p>
     * Used for help listings and completion candidates.
     *
     * @return the description, or empty string if none
     */
    default String description() {
        return "";
    }

    /**
     * Returns a detailed description of this command for the given arguments.
     * <p>
     * This is used by widgets to display context-sensitive help in the
     * terminal status bar.
     *
     * @param args the command arguments (args[0] is typically the command name)
     * @return the command description, or null if not available
     */
    default CommandDescription describe(List<String> args) {
        return null;
    }

    /**
     * Executes this command with the given session and arguments.
     *
     * @param session the current command session
     * @param args the command arguments (does not include the command name)
     * @return the result of the command execution, or null
     * @throws Exception if command execution fails
     */
    Object execute(CommandSession session, String[] args) throws Exception;

    /**
     * Returns the completers for this command's arguments.
     * <p>
     * The default implementation returns an empty list, meaning no custom completion.
     *
     * @return the list of completers
     */
    default List<Completer> completers() {
        return List.of();
    }
}

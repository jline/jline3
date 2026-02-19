/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.util.Collection;

/**
 * A named group of {@link Command}s for organization and discovery.
 * <p>
 * {@code CommandGroup} provides a pure discovery and organizational role.
 * Execution and completion concerns belong to {@link CommandDispatcher}.
 * <p>
 * Example:
 * <pre>
 * CommandGroup group = new SimpleCommandGroup("myapp", echoCmd, greetCmd);
 * dispatcher.addGroup(group);
 * </pre>
 *
 * @see Command
 * @see CommandDispatcher
 * @since 4.0
 */
public interface CommandGroup {

    /**
     * Returns the name of this command group.
     * <p>
     * The default implementation returns the simple class name.
     *
     * @return the group name
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Returns all commands in this group.
     *
     * @return the collection of commands, never null
     */
    Collection<Command> commands();

    /**
     * Finds a command by name or alias.
     * <p>
     * The default implementation scans {@link #commands()} for a match.
     *
     * @param name the command name or alias
     * @return the command, or null if not found
     */
    default Command command(String name) {
        for (Command cmd : commands()) {
            if (cmd.name().equals(name)) {
                return cmd;
            }
            if (cmd.aliases().contains(name)) {
                return cmd;
            }
        }
        return null;
    }

    /**
     * Returns whether this group contains a command with the given name or alias.
     *
     * @param name the command name or alias
     * @return true if a matching command exists
     */
    default boolean hasCommand(String name) {
        return command(name) != null;
    }
}

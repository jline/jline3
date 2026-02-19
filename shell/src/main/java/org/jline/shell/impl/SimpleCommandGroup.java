/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.util.*;

import org.jline.shell.Command;
import org.jline.shell.CommandGroup;

/**
 * Default implementation of {@link CommandGroup} backed by a list of commands.
 *
 * @see CommandGroup
 * @since 4.0
 */
public class SimpleCommandGroup implements CommandGroup {

    private final String name;
    private final List<Command> commands;

    /**
     * Creates a new command group with the given name and commands.
     *
     * @param name the group name
     * @param commands the commands in this group
     */
    public SimpleCommandGroup(String name, Command... commands) {
        this.name = name;
        this.commands = Collections.unmodifiableList(Arrays.asList(commands));
    }

    /**
     * Creates a new command group with the given name and commands.
     *
     * @param name the group name
     * @param commands the commands in this group
     */
    public SimpleCommandGroup(String name, Collection<Command> commands) {
        this.name = name;
        this.commands = Collections.unmodifiableList(new ArrayList<>(commands));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Collection<Command> commands() {
        return commands;
    }
}

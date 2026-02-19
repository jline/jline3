/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.util.*;

import org.jline.console.CommandRegistry;
import org.jline.shell.Command;
import org.jline.shell.CommandGroup;
import org.jline.shell.CommandSession;

/**
 * A {@link CommandGroup} that wraps a legacy {@link CommandRegistry}'s commands
 * as shell {@link Command}s.
 * <p>
 * This provides a migration path from the console module's {@link CommandRegistry} API
 * to the shell module's {@link CommandGroup} API. Each command in the registry
 * is wrapped as a shell {@code Command} that delegates to the registry's
 * {@link CommandRegistry#invoke(CommandRegistry.CommandSession, String, Object...)} method.
 * <p>
 * Example:
 * <pre>
 * CommandRegistry registry = ...; // existing Gogo/console registry
 * dispatcher.addGroup(new GogoCommandGroup("Gogo", registry));
 * </pre>
 *
 * @since 4.0
 */
public class GogoCommandGroup implements CommandGroup {

    private final String groupName;
    private final CommandRegistry registry;
    private final List<Command> commands;

    /**
     * Creates a new GogoCommandGroup wrapping the given registry.
     *
     * @param name the group name
     * @param registry the command registry to wrap
     */
    public GogoCommandGroup(String name, CommandRegistry registry) {
        this.groupName = name;
        this.registry = registry;
        List<Command> cmds = new ArrayList<>();
        for (String cmdName : registry.commandNames()) {
            cmds.add(new RegistryCommand(cmdName));
        }
        this.commands = Collections.unmodifiableList(cmds);
    }

    @Override
    public String name() {
        return groupName;
    }

    @Override
    public Collection<Command> commands() {
        return commands;
    }

    private class RegistryCommand implements Command {
        private final String name;

        RegistryCommand(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            List<String> infos = registry.commandInfo(name);
            if (infos != null && !infos.isEmpty()) {
                return infos.get(0);
            }
            return "";
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            CommandRegistry.CommandSession regSession =
                    new CommandRegistry.CommandSession(session.terminal(), session.in(), session.out(), session.err());
            Object[] objArgs = new Object[args.length];
            System.arraycopy(args, 0, objArgs, 0, args.length);
            return registry.invoke(regSession, name, objArgs);
        }
    }
}

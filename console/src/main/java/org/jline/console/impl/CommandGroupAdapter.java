/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.util.*;

import org.jline.console.CommandRegistry;
import org.jline.reader.Completer;
import org.jline.shell.*;
import org.jline.shell.Command;

/**
 * Adapts an old {@link CommandRegistry} to the new {@link CommandGroup} interface.
 * <p>
 * This bridge allows existing command registries (e.g., {@link Builtins},
 * {@code GroovyCommand}) to be used with the new {@link Shell} and
 * {@link CommandDispatcher} APIs.
 * <p>
 * Each command in the registry is wrapped as a {@link Command} that delegates
 * execution back to {@link CommandRegistry#invoke(CommandRegistry.CommandSession, String, Object...)}.
 *
 * @see CommandRegistryAdapter
 * @since 4.0
 */
public class CommandGroupAdapter implements CommandGroup {

    private final CommandRegistry registry;
    private final List<Command> commands;

    /**
     * Creates an adapter wrapping the given command registry.
     *
     * @param registry the old command registry to adapt
     */
    public CommandGroupAdapter(CommandRegistry registry) {
        this.registry = registry;
        this.commands = buildCommands();
    }

    @Override
    public String name() {
        return registry.name();
    }

    @Override
    public Collection<Command> commands() {
        return commands;
    }

    private List<Command> buildCommands() {
        List<Command> cmds = new ArrayList<>();
        Map<String, String> aliases = registry.commandAliases();
        // Invert alias map: command name -> list of aliases
        Map<String, List<String>> aliasesByCmd = new HashMap<>();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            aliasesByCmd
                    .computeIfAbsent(entry.getValue(), k -> new ArrayList<>())
                    .add(entry.getKey());
        }

        for (String name : registry.commandNames()) {
            List<String> cmdAliases = aliasesByCmd.getOrDefault(name, List.of());
            List<String> info = registry.commandInfo(name);
            String desc = info.isEmpty() ? "" : info.get(0);
            cmds.add(new RegistryCommand(name, cmdAliases, desc));
        }
        return Collections.unmodifiableList(cmds);
    }

    private class RegistryCommand implements Command {
        private final String name;
        private final List<String> aliases;
        private final String description;

        RegistryCommand(String name, List<String> aliases, String description) {
            this.name = name;
            this.aliases = aliases;
            this.description = description;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<String> aliases() {
            return aliases;
        }

        @Override
        public String description() {
            return description;
        }

        @Override
        public CommandDescription describe(List<String> args) {
            return DescriptionAdapter.toCommandDescription(registry.commandDescription(args));
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            CommandRegistry.CommandSession oldSession =
                    new CommandRegistry.CommandSession(session.terminal(), session.in(), session.out(), session.err());
            Object[] objArgs = args;
            return registry.invoke(oldSession, name, objArgs);
        }

        @Override
        public List<Completer> completers() {
            return List.of();
        }
    }
}

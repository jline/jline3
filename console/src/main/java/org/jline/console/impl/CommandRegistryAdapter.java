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

import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.shell.Command;
import org.jline.shell.CommandGroup;

/**
 * Adapts a new {@link CommandGroup} to the old {@link CommandRegistry} interface.
 * <p>
 * This bridge allows new-style command groups to be used with the legacy
 * {@link org.jline.console.impl.SystemRegistryImpl} and other old API consumers.
 *
 * @see CommandGroupAdapter
 * @since 4.0
 */
public class CommandRegistryAdapter implements CommandRegistry {

    private final CommandGroup group;

    /**
     * Creates an adapter wrapping the given command group.
     *
     * @param group the new command group to adapt
     */
    public CommandRegistryAdapter(CommandGroup group) {
        this.group = group;
    }

    @Override
    public String name() {
        return group.name();
    }

    @Override
    public Set<String> commandNames() {
        Set<String> names = new LinkedHashSet<>();
        for (Command cmd : group.commands()) {
            names.add(cmd.name());
        }
        return names;
    }

    @Override
    public Map<String, String> commandAliases() {
        Map<String, String> aliases = new LinkedHashMap<>();
        for (Command cmd : group.commands()) {
            for (String alias : cmd.aliases()) {
                aliases.put(alias, cmd.name());
            }
        }
        return aliases;
    }

    @Override
    public List<String> commandInfo(String command) {
        Command cmd = group.command(command);
        if (cmd != null) {
            String desc = cmd.description();
            return desc.isEmpty() ? List.of() : List.of(desc);
        }
        return List.of();
    }

    @Override
    public boolean hasCommand(String command) {
        return group.hasCommand(command);
    }

    @Override
    public SystemCompleter compileCompleters() {
        SystemCompleter completer = new SystemCompleter();
        for (Command cmd : group.commands()) {
            List<Completer> cmdCompleters = cmd.completers();
            if (!cmdCompleters.isEmpty()) {
                List<Completer> all = new ArrayList<>(cmdCompleters);
                all.add(NullCompleter.INSTANCE);
                completer.add(cmd.name(), new ArgumentCompleter(all));
                for (String alias : cmd.aliases()) {
                    completer.add(alias, new ArgumentCompleter(all));
                }
            } else {
                completer.add(cmd.name(), NullCompleter.INSTANCE);
                for (String alias : cmd.aliases()) {
                    completer.add(alias, NullCompleter.INSTANCE);
                }
            }
        }
        return completer;
    }

    @Override
    public CmdDesc commandDescription(List<String> args) {
        Command cmd = group.command(args.isEmpty() ? "" : args.get(0));
        if (cmd != null) {
            return DescriptionAdapter.toCmdDesc(cmd.describe(args));
        }
        return null;
    }

    @Override
    public Object invoke(CommandSession session, String command, Object... args) throws Exception {
        Command cmd = group.command(command);
        if (cmd == null) {
            throw new IllegalArgumentException("Unknown command: " + command);
        }
        String[] strArgs;
        if (args != null) {
            strArgs = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                strArgs[i] = args[i] != null ? args[i].toString() : null;
            }
        } else {
            strArgs = new String[0];
        }
        return cmd.execute(
                new org.jline.shell.CommandSession(session.terminal(), session.in(), session.out(), session.err()),
                strArgs);
    }
}

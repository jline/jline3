/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

/**
 * Abstract base class implementing common methods for command registries.
 * <p>
 * AbstractCommandRegistry provides a base implementation of the CommandRegistry interface,
 * with common methods for registering commands, generating command descriptions,
 * and handling command execution. Concrete implementations can extend this class
 * to create specific command registry types.
 *
 */
public abstract class AbstractCommandRegistry implements CommandRegistry {
    /** The internal registry of commands */
    private CmdRegistry cmdRegistry;
    /** The last exception that occurred during command execution */
    private Exception exception;

    /**
     * Creates a new AbstractCommandRegistry.
     * The command registry is initialized lazily when commands are registered.
     */
    public AbstractCommandRegistry() {}

    /**
     * Creates a command description for a help command.
     * <p>
     * This method combines the command information with the command description
     * to create a comprehensive help description for the command.
     *
     * @param command the command name
     * @param info the command information as a list of strings
     * @param cmdDesc the command description
     * @return a command description for the help command
     */
    public CmdDesc doHelpDesc(String command, List<String> info, CmdDesc cmdDesc) {
        List<AttributedString> mainDesc = new ArrayList<>();
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(command.toLowerCase()).append(" -  ");
        for (String s : info) {
            if (asb.length() == 0) {
                asb.append("\t");
            }
            asb.append(s);
            mainDesc.add(asb.toAttributedString());
            asb = new AttributedStringBuilder();
            asb.tabs(2);
        }
        asb = new AttributedStringBuilder();
        asb.tabs(7);
        asb.append("Usage:");
        for (AttributedString as : cmdDesc.getMainDesc()) {
            asb.append("\t");
            asb.append(as);
            mainDesc.add(asb.toAttributedString());
            asb = new AttributedStringBuilder();
            asb.tabs(7);
        }
        return new CmdDesc(mainDesc, new ArrayList<>(), cmdDesc.getOptsDesc());
    }

    public <T extends Enum<T>> void registerCommands(
            Map<T, String> commandName, Map<T, CommandMethods> commandExecute) {
        cmdRegistry = new EnumCmdRegistry<>(commandName, commandExecute);
    }

    public void registerCommands(Map<String, CommandMethods> commandExecute) {
        cmdRegistry = new NameCmdRegistry(commandExecute);
    }

    @Override
    public Object invoke(CommandSession session, String command, Object... args) throws Exception {
        exception = null;
        CommandMethods methods = getCommandMethods(command);
        Object out = methods.execute().apply(new CommandInput(command, args, session));
        if (exception != null) {
            throw exception;
        }
        return out;
    }

    public void saveException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public boolean hasCommand(String command) {
        return cmdRegistry.hasCommand(command);
    }

    @Override
    public Set<String> commandNames() {
        return cmdRegistry.commandNames();
    }

    @Override
    public Map<String, String> commandAliases() {
        return cmdRegistry.commandAliases();
    }

    public <V extends Enum<V>> void rename(V command, String newName) {
        cmdRegistry.rename(command, newName);
    }

    public void alias(String alias, String command) {
        cmdRegistry.alias(alias, command);
    }

    @Override
    public SystemCompleter compileCompleters() {
        return cmdRegistry.compileCompleters();
    }

    public CommandMethods getCommandMethods(String command) {
        return cmdRegistry.getCommandMethods(command);
    }

    public Object registeredCommand(String command) {
        return cmdRegistry.command(command);
    }

    private interface CmdRegistry {
        boolean hasCommand(String command);

        Set<String> commandNames();

        Map<String, String> commandAliases();

        Object command(String command);

        <V extends Enum<V>> void rename(V command, String newName);

        void alias(String alias, String command);

        SystemCompleter compileCompleters();

        CommandMethods getCommandMethods(String command);
    }

    private static class EnumCmdRegistry<T extends Enum<T>> implements CmdRegistry {
        private final Map<T, String> commandName;
        private Map<String, T> nameCommand = new HashMap<>();
        private final Map<T, CommandMethods> commandExecute;
        private final Map<String, String> aliasCommand = new HashMap<>();

        public EnumCmdRegistry(Map<T, String> commandName, Map<T, CommandMethods> commandExecute) {
            this.commandName = commandName;
            this.commandExecute = commandExecute;
            doNameCommand();
        }

        private void doNameCommand() {
            nameCommand =
                    commandName.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        }

        public Set<String> commandNames() {
            return nameCommand.keySet();
        }

        public Map<String, String> commandAliases() {
            return aliasCommand;
        }

        @SuppressWarnings("unchecked")
        public <V extends Enum<V>> void rename(V command, String newName) {
            if (nameCommand.containsKey(newName)) {
                throw new IllegalArgumentException("Duplicate command name '" + command + "'!");
            } else if (!commandName.containsKey(command)) {
                throw new IllegalArgumentException("Command '" + command + "' does not exists!");
            }
            commandName.put((T) command, newName);
            doNameCommand();
        }

        public void alias(String alias, String command) {
            if (!nameCommand.containsKey(command)) {
                throw new IllegalArgumentException("Command '" + command + "' does not exists!");
            }
            aliasCommand.put(alias, command);
        }

        public boolean hasCommand(String name) {
            return nameCommand.containsKey(name) || aliasCommand.containsKey(name);
        }

        public SystemCompleter compileCompleters() {
            SystemCompleter out = new SystemCompleter();
            for (Map.Entry<T, String> entry : commandName.entrySet()) {
                out.add(
                        entry.getValue(),
                        commandExecute.get(entry.getKey()).compileCompleter().apply(entry.getValue()));
            }
            out.addAliases(aliasCommand);
            return out;
        }

        public T command(String name) {
            T out;
            name = aliasCommand.getOrDefault(name, name);
            if (nameCommand.containsKey(name)) {
                out = nameCommand.get(name);
            } else {
                throw new IllegalArgumentException("Command '" + name + "' does not exists!");
            }
            return out;
        }

        public CommandMethods getCommandMethods(String command) {
            return commandExecute.get(command(command));
        }
    }

    private static class NameCmdRegistry implements CmdRegistry {
        private final Map<String, CommandMethods> commandExecute;
        private final Map<String, String> aliasCommand = new HashMap<>();

        public NameCmdRegistry(Map<String, CommandMethods> commandExecute) {
            this.commandExecute = commandExecute;
        }

        public Set<String> commandNames() {
            return commandExecute.keySet();
        }

        public Map<String, String> commandAliases() {
            return aliasCommand;
        }

        public <V extends Enum<V>> void rename(V command, String newName) {
            throw new IllegalArgumentException();
        }

        public void alias(String alias, String command) {
            if (!commandExecute.containsKey(command)) {
                throw new IllegalArgumentException("Command '" + command + "' does not exists!");
            }
            aliasCommand.put(alias, command);
        }

        public boolean hasCommand(String name) {
            return commandExecute.containsKey(name) || aliasCommand.containsKey(name);
        }

        public SystemCompleter compileCompleters() {
            SystemCompleter out = new SystemCompleter();
            for (String c : commandExecute.keySet()) {
                out.add(c, commandExecute.get(c).compileCompleter().apply(c));
            }
            out.addAliases(aliasCommand);
            return out;
        }

        public String command(String name) {
            if (commandExecute.containsKey(name)) {
                return name;
            }
            return aliasCommand.get(name);
        }

        public CommandMethods getCommandMethods(String command) {
            return commandExecute.get(command(command));
        }
    }
}

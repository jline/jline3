/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.util.*;
import java.util.stream.Collectors;

import org.jline.console.CommandRegistry.CommandSession;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

/**
 * CommandRegistry common methods.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public abstract class AbstractCommandRegistry {
    private CmdRegistry cmdRegistry;
    private Exception exception;

    public AbstractCommandRegistry() {}

    public CmdDesc doHelpDesc(String command, List<String> info, CmdDesc cmdDesc) {
        List<AttributedString> mainDesc = new ArrayList<>();
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(command.toString().toLowerCase()).append(" -  ");
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
        for(AttributedString as : cmdDesc.getMainDesc()) {
            asb.append("\t");
            asb.append(as);
            mainDesc.add(asb.toAttributedString());
            asb = new AttributedStringBuilder();
            asb.tabs(7);
        }
        return new CmdDesc(mainDesc, new ArrayList<>(), cmdDesc.getOptsDesc());
    }

    public <T extends Enum<T>>  void registerCommands(Map<T,String> commandName, Map<T,CommandMethods> commandExecute) {
        cmdRegistry = new EnumCmdRegistry<T>(commandName, commandExecute);
    }

    public void registerCommands(Map<String,CommandMethods> commandExecute) {
        cmdRegistry = new NameCmdRegistry(commandExecute);
    }

    public Object execute(CommandRegistry.CommandSession session, String command, String[] args) throws Exception {
        exception = null;
        getCommandMethods(command).execute().accept(new CommandInput(command, args, session));
        if (exception != null) {
            throw exception;
        }
        return null;
    }

    public Object invoke(CommandSession session, String command, Object... args) throws Exception {
        Object out = null;
        exception = null;
        CommandMethods methods = getCommandMethods(command);
        if (methods.isConsumer()) {
            String[] _args = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                if (!(args[i] instanceof String)) {
                    throw new IllegalArgumentException();
                }
                _args[i] = args[i].toString();
            }
            methods.execute().accept(new CommandInput(command, _args, session));
        } else {
            out = methods.executeFunction().apply(new CommandInput(command, args, session));
        }
        if (exception != null) {
            throw exception;
        }
        return out;
    }

    public void saveException(Exception exception) {
        this.exception = exception;
    }

    public boolean hasCommand(String command) {
        return cmdRegistry.hasCommand(command);
    }

    public Set<String> commandNames() {
        return cmdRegistry.commandNames();
    }

    public Map<String, String> commandAliases() {
        return cmdRegistry.commandAliases();
    }

    public <V extends Enum<V>> void rename(V command, String newName) {
        cmdRegistry.rename(command, newName);
    }

    public void alias(String alias, String command) {
        cmdRegistry.alias(alias, command);
    }

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

    private static class  EnumCmdRegistry<T extends Enum<T>> implements CmdRegistry {
        private Map<T,String> commandName;
        private Map<String,T> nameCommand = new HashMap<>();
        private Map<T,CommandMethods> commandExecute;
        private Map<String,String> aliasCommand = new HashMap<>();

        public EnumCmdRegistry(Map<T,String> commandName, Map<T,CommandMethods> commandExecute) {
            this.commandName = commandName;
            this.commandExecute = commandExecute;
            doNameCommand();
        }

        private void doNameCommand() {
            nameCommand = commandName.entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
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
                throw new IllegalArgumentException("Duplicate command name!");
            } else if (!commandName.containsKey((T)command)) {
                throw new IllegalArgumentException("Command does not exists!");
            }
            commandName.put((T)command, newName);
            doNameCommand();
        }

        public void alias(String alias, String command) {
            if (!nameCommand.keySet().contains(command)) {
                throw new IllegalArgumentException("Command does not exists!");
            }
            aliasCommand.put(alias, command);
        }

        public boolean hasCommand(String name) {
            if (nameCommand.containsKey(name) || aliasCommand.containsKey(name)) {
                return true;
            }
            return false;
        }

        public SystemCompleter compileCompleters() {
            SystemCompleter out = new SystemCompleter();
            for (Map.Entry<T, String> entry: commandName.entrySet()) {
                out.add(entry.getValue(), commandExecute.get(entry.getKey()).compileCompleter().apply(entry.getValue()));
            }
            out.addAliases(aliasCommand);
            return out;
        }

        public T command(String name) {
            T out = null;
            if (!hasCommand(name)) {
                throw new IllegalArgumentException("Command does not exists!");
            }
            if (aliasCommand.containsKey(name)) {
                name = aliasCommand.get(name);
            }
            if (nameCommand.containsKey(name)) {
                out = nameCommand.get(name);
            } else {
                throw new IllegalArgumentException("Command does not exists!");
            }
            return out;
        }

        public CommandMethods getCommandMethods(String command) {
            return commandExecute.get(command(command));
        }

    }

    private static class NameCmdRegistry implements CmdRegistry {
        private Map<String,CommandMethods> commandExecute;
        private Map<String,String> aliasCommand = new HashMap<>();

        public NameCmdRegistry(Map<String,CommandMethods> commandExecute) {
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
                throw new IllegalArgumentException("Command does not exists!");
            }
            aliasCommand.put(alias, command);
        }

        public boolean hasCommand(String name) {
            if (commandExecute.containsKey(name) || aliasCommand.containsKey(name)) {
                return true;
            }
            return false;
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
            } else if (aliasCommand.containsKey(name)) {
                return aliasCommand.get(name);
            }
            return null;
        }

        public CommandMethods getCommandMethods(String command) {
            return commandExecute.get(command(command));
        }

    }

}

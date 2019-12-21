package org.jline.builtins;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jline.builtins.Builtins;
import org.jline.builtins.Builtins.Command;
import org.jline.builtins.Builtins.CommandMethods;
import org.jline.reader.Completer;
import org.jline.reader.ConfigurationPath;
import org.jline.reader.LineReader;
import org.jline.reader.Widget;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.script.JLineEngine;

public class ScriptCommands implements CommandRegistry {
    public enum Command {SET
                       , DEL
                       , ENGINES};
    private final JLineEngine engine;
    private Map<Command,String> commandName = new HashMap<>();
    private Map<String,Command> nameCommand = new HashMap<>();
    private Map<String,String> aliasCommand = new HashMap<>();
    private final Map<Command,CommandMethods> commandExecute = new HashMap<>();
    private Map<Command,List<String>> commandInfo = new HashMap<>();
    private Exception exception;

    public ScriptCommands(JLineEngine engine) {
        this.engine = engine;
        Set<Command> cmds = new HashSet<>(EnumSet.allOf(Command.class));
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        doNameCommand();
        commandExecute.put(Command.ENGINES, new CommandMethods(this::engines, this::defaultCompleter));
        commandExecute.put(Command.SET, new CommandMethods(this::set, this::defaultCompleter));
    }
    
    public Set<String> commandNames() {
        return nameCommand.keySet();
    }

    public Map<String, String> commandAliases() {
        return aliasCommand;
    }
    
    public boolean hasCommand(String name) {
        if (nameCommand.containsKey(name) || aliasCommand.containsKey(name)) {
            return true;
        }
        return false;
    }

    private void doNameCommand() {
        nameCommand = commandName.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    private Command command(String name) {
        Command out = null;
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

    public void rename(Command command, String newName) {
        if (nameCommand.containsKey(newName)) {
            throw new IllegalArgumentException("Duplicate command name!");
        } else if (!commandName.containsKey(command)) {
            throw new IllegalArgumentException("Command does not exists!");
        }
        commandName.put(command, newName);
        doNameCommand();
    }

    public void alias(String alias, String command) {
        if (!nameCommand.keySet().contains(command)) {
            throw new IllegalArgumentException("Command does not exists!");
        }
        aliasCommand.put(alias, command);
    }

    public List<String> commandInfo(String command) {
        return null;
    }

    public Completers.SystemCompleter compileCompleters() {
        return null;
    }

    public Widgets.CmdDesc commandDescription(String command) {
        return null;
    }
  
    public Object execute(String statement) throws Exception {
        return engine.execute(statement);    
    }
    
    public Object execute(String command, String[] args) throws Exception {
        exception = null;
        Object out = commandExecute.get(command(command)).executeFunction().apply(new Builtins.CommandInput(args));
        if (exception != null) {
            throw exception;
        }
        return out;
    }

    public Object engines(Builtins.CommandInput input) {
        return JLineEngine.listEngines();
    }

    public Object set(Builtins.CommandInput input) {
        return engine.get();
    }

    private List<Completer> defaultCompleter(String command) {
        return Arrays.asList(NullCompleter.INSTANCE);
    }

}

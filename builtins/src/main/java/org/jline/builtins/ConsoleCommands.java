package org.jline.builtins;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jline.builtins.Builtins;
import org.jline.builtins.Builtins.Command;
import org.jline.builtins.Builtins.CommandInput;
import org.jline.builtins.Builtins.CommandMethods;
import org.jline.builtins.Completers.SystemCompleter;
import org.jline.reader.Completer;
import org.jline.reader.ConfigurationPath;
import org.jline.reader.LineReader;
import org.jline.reader.Widget;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.ScriptEngine;
    
public class ConsoleCommands implements CommandRegistry {
    public enum Command {SHOW
                       , DEL
                       , ENGINES};
    private final ScriptEngine engine;
    private Map<Command,String> commandName = new HashMap<>();
    private Map<String,Command> nameCommand = new HashMap<>();
    private Map<String,String> aliasCommand = new HashMap<>();
    private final Map<Command,CommandMethods> commandExecute = new HashMap<>();
    private Map<Command,List<String>> commandInfo = new HashMap<>();
    private Exception exception;
    private Consumer<String> execute;

    public ConsoleCommands(ScriptEngine engine) {
        this.engine = engine;
//        this.commandExecute = execute;
        Set<Command> cmds = new HashSet<>(EnumSet.allOf(Command.class));
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        doNameCommand();
        commandExecute.put(Command.ENGINES, new CommandMethods(this::engines, this::defaultCompleter));
        commandExecute.put(Command.DEL, new CommandMethods(this::del, this::defaultCompleter));
        commandExecute.put(Command.SHOW, new CommandMethods(this::show, this::defaultCompleter));
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
        SystemCompleter out = new SystemCompleter();
        for (Map.Entry<Command, String> entry: commandName.entrySet()) {
            out.add(entry.getValue(), commandExecute.get(entry.getKey()).compileCompleter().apply(entry.getValue()));
        }
        out.addAliases(aliasCommand);
        return out;
    }

    public Widgets.CmdDesc commandDescription(String command) {
        return null;
    }
  
    public Object execute(String cmd, String[] args, String statement) throws Exception {
        Object out = null;
        if (new File(cmd).exists()) {
            File file = new File(cmd);
            String name = file.getName();
            String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "";
            if(engine.getExtensions().contains(ext)) {
                out = engine.execute(file, args);            
            } else {
                throw new IllegalArgumentException("Command not found: " + cmd);
            }
//          execute.accept(statement);

        } else {
            out = engine.execute(statement);
        }
        return out;    
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
        return ScriptEngine.listEngines();
    }

    public Object show(Builtins.CommandInput input) {
        return engine.get();
    }

    public Object del(Builtins.CommandInput input) {
        engine.del(input.args());
        return null;
    }

    private List<Completer> defaultCompleter(String command) {
        return Arrays.asList(NullCompleter.INSTANCE);
    }
}
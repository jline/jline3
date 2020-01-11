/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jline.builtins.CommandRegistry;
import org.jline.builtins.Builtins;
import org.jline.builtins.Builtins.Command;
import org.jline.builtins.Builtins.CommandInput;
import org.jline.builtins.Builtins.CommandMethods;
import org.jline.builtins.Completers.SystemCompleter;
import org.jline.reader.*;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
    
/**
 * Console commands and script execution.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class ConsoleEngineImpl implements ConsoleEngine {
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
    private SystemRegistry masterRegistry;
    private String scriptExtension = "jline";
    private Parser parser;

    public ConsoleEngineImpl(ScriptEngine engine, Parser parser) {
        this.engine = engine;
        this.parser = parser;
        Set<Command> cmds = new HashSet<>(EnumSet.allOf(Command.class));
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        doNameCommand();
        commandExecute.put(Command.ENGINES, new CommandMethods(this::engines, this::defaultCompleter));
        commandExecute.put(Command.DEL, new CommandMethods(this::del, this::defaultCompleter));
        commandExecute.put(Command.SHOW, new CommandMethods(this::show, this::defaultCompleter));
    }
        
    public void setMasterRegistry(SystemRegistry masterRegistry) {
        this.masterRegistry = masterRegistry;
    }
    
    public ConsoleEngineImpl scriptExtension(String extension) {
        this.scriptExtension = extension;
        return this;
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
        return new ArrayList<>();
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

    private List<String> slurp(String path) throws IOException{
        List<String> out = new ArrayList<>();
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        out.addAll(Arrays.asList(new String(encoded, StandardCharsets.UTF_8).split("\n|\n\r")));
        return out;
    }
  
    public Object[] expandVariables(String[] args) throws Exception {
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("$")) {
                out[i] = engine.get(expandName(args[i]));
            } else {
                out[i] = args[i];
            }
        }
        return out;
    }
    
    private String expandName(String name) {
        String regexVar = "[a-zA-Z]{1,}[a-zA-Z0-9_-]*";
        String out = name;
        if (name.matches("^\\$" + regexVar)) {
            out = name.substring(1);
        } else if (name.matches("^\\$\\{" + regexVar + "\\}.*")) {
            Matcher matcher = Pattern.compile("^\\$\\{(" + regexVar + ")\\}(.*)").matcher(name);
            if (matcher.find()) {
                out = matcher.group(1) + matcher.group(2);
            } else {
                throw new IllegalArgumentException();    
            }
        }
        return out;
    }
    
    private String quote(String var) {
        if ((var.startsWith("\\\"") && var.endsWith("\\\""))
                || (var.startsWith("'") && var.endsWith("'"))) {
            return var;
        }
        if (var.contains("\\\"")) {
            return "'" + var + "'";   
        } 
        return "\\\"" + var + "\\\"";
    }
    
    public Object execute(ParsedLine pl) throws Exception {
        String[] args = pl.words().subList(1, pl.words().size()).toArray(new String[0]);
        String cmd = Parser.getCommand(pl.word());
        Object out = null;
        if (new File(cmd).exists()) {
            File file = new File(cmd);
            String name = file.getName();
            String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "";
            if(engine.getExtensions().contains(ext)) {
                out = engine.execute(file, expandVariables(args));
            } else if (scriptExtension.equals(ext)) {
                List<String> commandsBuffer = slurp(cmd);
                String line = "";
                boolean done = false;
                while (!commandsBuffer.isEmpty()) {
                    try {
                        line += commandsBuffer.remove(0);
                        parser.parse(line, line.length() + 1, ParseContext.ACCEPT_LINE);
                        for (int i = 0; i < args.length; i++) {
                            line = line.replaceAll("\\$\\d", args[i].startsWith("$") ? expandName(args[i]) : quote(args[i]));
                        }
                        masterRegistry.execute(parser.parse(line, 0, ParseContext.ACCEPT_LINE));
                        line = "";
                    } catch (EOFError e) {
                        if (commandsBuffer.isEmpty()) {
                            throw new IllegalArgumentException("Incompleted command: \n" + line);
                        }
                        line += "\n";
                    } catch (SyntaxError e) {
                        commandsBuffer.clear();
                        throw e;
                    } catch (Exception e) {
                        commandsBuffer.clear();
                        throw new IllegalArgumentException(e.getMessage());
                    }
                }
            } else {
                throw new IllegalArgumentException("Command not found: " + cmd);
            }
        } else {
            System.out.println(pl.line());
            out = engine.execute(pl.line());
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
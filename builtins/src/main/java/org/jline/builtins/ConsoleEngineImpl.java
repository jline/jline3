/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
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
    private SystemRegistry systemRegistry;
    private String scriptExtension = "jline";
    private Parser parser;
    private Terminal terminal;

    public ConsoleEngineImpl(ScriptEngine engine, Parser parser, Terminal terminal) {
        this.engine = engine;
        this.parser = parser;
        this.terminal = terminal;
        Set<Command> cmds = new HashSet<>(EnumSet.allOf(Command.class));
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        doNameCommand();
        commandExecute.put(Command.ENGINES, new CommandMethods(this::engines, this::defaultCompleter));
        commandExecute.put(Command.DEL, new CommandMethods(this::del, this::defaultCompleter));
        commandExecute.put(Command.SHOW, new CommandMethods(this::show, this::defaultCompleter));
    }
        
    public void setSystemRegistry(SystemRegistry systemRegistry) {
        this.systemRegistry = systemRegistry;
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

    private Object[] expandVariables(String[] args) throws Exception {
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
    
    private boolean isCodeBlock(String line) {
        return line.contains("\n") && line.trim().endsWith("}");
    }
    
    private boolean isCommandLine(String line) {
        String command = Parser.getCommand(line);
        return command.startsWith(":") && systemRegistry.hasCommand(command.substring(1));
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
        if (pl.line().trim().startsWith("#")) {
            return null;            
        }
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
                boolean done = false;
                String line = "";
                try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                    for(String l; (l = br.readLine()) != null; ) {
                        try {
                            line += l;
                            parser.parse(line, line.length() + 1, ParseContext.ACCEPT_LINE);
                            done = true;
                            for (int i = 0; i < args.length; i++) {
                                line = line.replaceAll("\\$\\d", args[i].startsWith("$") ? expandName(args[i]) : quote(args[i]));
                            }
                            systemRegistry.execute(parser.parse(line, 0, ParseContext.COMPLETE));
                            line = "";
                        } catch (EOFError e) {
                            done = false;
                            line += "\n";
                        } catch (SyntaxError e) {
                            throw e;
                        } catch (Exception e) {
                            throw new IllegalArgumentException(line + "\n" + e.getMessage());
                        }
                    }
                    if (!done) {
                        throw new IllegalArgumentException("Incompleted command: \n" + line);                        
                    }
                }
            } else {
                throw new IllegalArgumentException("Command not found: " + cmd);
            }
            if (pl.word().contains("=")) {
                engine.put(pl.word().substring(0, pl.word().indexOf('=')), out);
                out = null;
            }
        } else {
            String line = pl.line();
            if (isCodeBlock(line)) {
                StringBuilder sb = new StringBuilder();
                boolean copyRegistry = false;
                String registry = "_systemRegistry";
                for (String s: line.split("\n|\n\r")) {
                    if (isCommandLine(s)) {
                        copyRegistry = true;
                        List<String> ws = parser.parse(s, 0, ParseContext.COMPLETE).words();
                        int idx = ws.get(0).lastIndexOf(":");
                        if (idx > 0) {
                            sb.append(ws.get(0).substring(0, idx - 1));   
                        } 
                        sb.append(registry + ".invoke('" + ws.get(0).substring(idx + 1) + "'");
                        for (int i = 1; i < ws.size(); i++) {
                            sb.append(", ");
                            sb.append(ws.get(i));
                        }
                        sb.append(")");
                    } else {
                        sb.append(s);
                    }
                    sb.append("\n");
                }
                line = sb.toString();
                if (copyRegistry && !engine.hasVariable(registry)) {
                    engine.put(registry, systemRegistry);                    
                }
            }
            if (engine.hasVariable(line)) {
                out = engine.get(line);
            } else if (Parser.getVariable(line) == null) {
                out = engine.execute(line);
            } else {
                engine.execute(line);
            }
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
    
    public void println(Map<String, Object> options, Object object) {
        options.putIfAbsent("width", terminal.getSize().getColumns());
        for (AttributedString as : engine.format(options, object)) {
            as.println(terminal);
        }
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
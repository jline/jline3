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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jline.builtins.Builtins;
import org.jline.builtins.Builtins.CommandMethods;
import org.jline.builtins.Completers.SystemCompleter;
import org.jline.builtins.Nano.SyntaxHighlighter;
import org.jline.builtins.Options.HelpException;
import org.jline.reader.*;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

/**
 * Manage console variables, commands and script execution.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class ConsoleEngineImpl implements ConsoleEngine {
    public enum Command {SHOW
                       , DEL
                       , ENGINES
                       , PRNT
                       , ECHO};
    private final ScriptEngine engine;
    private Map<Command,String> commandName = new HashMap<>();
    private Map<String,Command> nameCommand = new HashMap<>();
    private Map<String,String> aliasCommand = new HashMap<>();
    private final Map<Command,CommandMethods> commandExecute = new HashMap<>();
    private Exception exception;
    private SystemRegistry systemRegistry;
    private String scriptExtension = "jline";
    private Parser parser;
    private Terminal terminal;
    private ConfigurationPath configPath;

    public ConsoleEngineImpl(ScriptEngine engine, Parser parser, Terminal terminal, ConfigurationPath configPath) {
        this.engine = engine;
        this.parser = parser;
        this.terminal = terminal;
        this.configPath = configPath;
        Set<Command> cmds = new HashSet<>(EnumSet.allOf(Command.class));
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        doNameCommand();
        commandExecute.put(Command.ENGINES, new CommandMethods(this::engines, this::defaultCompleter));
        commandExecute.put(Command.DEL, new CommandMethods(this::del, this::defaultCompleter));
        commandExecute.put(Command.SHOW, new CommandMethods(this::show, this::defaultCompleter));
        commandExecute.put(Command.PRNT, new CommandMethods(this::prnt, this::defaultCompleter));
        commandExecute.put(Command.ECHO, new CommandMethods(this::echo, this::defaultCompleter));
    }

    @Override
    public void setSystemRegistry(SystemRegistry systemRegistry) {
        this.systemRegistry = systemRegistry;
    }

    public ConsoleEngineImpl scriptExtension(String extension) {
        this.scriptExtension = extension;
        return this;
    }

    @Override
    public Set<String> commandNames() {
        return nameCommand.keySet();
    }

    public Map<String, String> commandAliases() {
        return aliasCommand;
    }

    @Override
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

    @Override
    public List<String> commandInfo(String command) {
        return new ArrayList<>();
    }

    @Override
    public Completers.SystemCompleter compileCompleters() {
        SystemCompleter out = new SystemCompleter();
        for (Map.Entry<Command, String> entry: commandName.entrySet()) {
            out.add(entry.getValue(), commandExecute.get(entry.getKey()).compileCompleter().apply(entry.getValue()));
        }
        out.addAliases(aliasCommand);
        return out;
    }

    @Override
    public Widgets.CmdDesc commandDescription(String command) {
        return null;
    }

    private Object[] expandParameters(String[] args) throws Exception {
        Object[] out = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("${")) {
                out[i] = engine.execute(expandName(args[i]));
            } else if (args[i].startsWith("$")) {
                out[i] = engine.get(expandName(args[i]));
            } else {
                out[i] = engine.expandParameter(args[i]);
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
        if (!var.contains("\\\"")) {
            return "'" + var + "'";
        }
        return "\\\"" + var + "\\\"";
    }

    private class ScriptFile {
        private File script;
        private String extension = "";

        @SuppressWarnings("unchecked")
        public ScriptFile(String command) {
            this.script = new File(command);
            if (script.exists()) {
                scriptExtension(command);
            } else if (engine.hasVariable("PATH")) {
                List<String>  extensions = new ArrayList<>();
                extensions.addAll(engine.getExtensions());
                extensions.add(scriptExtension);
                boolean found = false;
                for (String p: (List<String>)engine.get("PATH")) {
                    for (String e : extensions) {
                        String file = command + "." + e;
                        Path path = Paths.get(p, file);
                        if (path.toFile().exists()) {
                            script = path.toFile();
                            scriptExtension(command);
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
            }
        }

        private void scriptExtension(String command) {
            String name = script.getName();
            this.extension = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "";
            if (!isEngineScript() && !isConsoleScript()) {
                throw new IllegalArgumentException("Command not found: " + command);
            }
        }

        public File getScript() {
            return script;
        }

        public boolean isEngineScript() {
            return engine.getExtensions().contains(extension);
        }

        public boolean isConsoleScript() {
            return scriptExtension.equals(extension);
        }

    }

    @Override
    public Object execute(ParsedLine pl) throws Exception {
        if (pl.line().trim().startsWith("#")) {
            return null;
        }
        String[] args = pl.words().subList(1, pl.words().size()).toArray(new String[0]);
        String cmd = Parser.getCommand(pl.word());
        if (cmd.startsWith(":")) {
            cmd = cmd.substring(1);
        }
        Object out = null;
        ScriptFile file = new ScriptFile(cmd);
        if(file.isEngineScript()) {
            out = engine.execute(file.getScript(), expandParameters(args));
            out = postProcess(pl.line(), out);
        } else if (file.isConsoleScript()) {
            boolean done = false;
            String line = "";
            try(BufferedReader br = new BufferedReader(new FileReader(file.getScript()))) {
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
                out = engine.get("_return");
                out = postProcess(pl.line(), out);
            }
        } else {
            String line = pl.line();
            if (isCodeBlock(line)) {
                StringBuilder sb = new StringBuilder();
                for (String s: line.split("\n|\n\r")) {
                    if (isCommandLine(s)) {
                        List<String> ws = parser.parse(s, 0, ParseContext.COMPLETE).words();
                        int idx = ws.get(0).lastIndexOf(":");
                        if (idx > 0) {
                            sb.append(ws.get(0).substring(0, idx));
                        }
                        String[] argv = new String[ws.size()];
                        for (int i = 1; i < ws.size(); i++) {
                            argv[i] = ws.get(i);
                            if (argv[i].startsWith("${")) {
                                Matcher argvMatcher = Pattern.compile("\\$\\{(.*)}").matcher(argv[i]);
                                if (argvMatcher.find()) {
                                    argv[i] = argv[i].replace(argv[i], argvMatcher.group(1));
                                }
                            } else if (argv[i].startsWith("$")) {
                                argv[i] = argv[i].substring(1);
                            } else {
                                argv[i] = quote(argv[i]);
                            }
                        }
                        sb.append("org.jline.builtins.SystemRegistry.get().invoke('" + ws.get(0).substring(idx + 1) + "'");
                        for (int i = 1; i < argv.length; i++) {
                            sb.append(", ");
                            sb.append(argv[i]);
                        }
                        sb.append(")");
                    } else {
                        sb.append(s);
                    }
                    sb.append("\n");
                }
                line = sb.toString();
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

    @Override
    public Object postProcess(String line, Object result) {
        Object out = result;
        if (Parser.getVariable(line) != null) {
            engine.put(Parser.getVariable(line), result);
            out = null;
        }
        return out;
    }

    @Override
    public Object execute(String command, String[] args) throws Exception {
        exception = null;
        Object out = commandExecute.get(command(command)).executeFunction().apply(new Builtins.CommandInput(args));
        if (exception != null) {
            throw exception;
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> defaultPrntOptions() {
        Map<String, Object> out = new HashMap<>();
        if (engine.hasVariable("PRNT_OPTIONS")) {
            out.putAll((Map<String,Object>)engine.get("PRNT_OPTIONS"));
        }
        return out;
    }

    @Override
    public void println(Object object) {
        println(defaultPrntOptions(), object);
    }

    @Override
    public void println(Map<String, Object> options, Object object) {
        if (object == null) {
            return;
        }
        options.putIfAbsent("width", terminal.getSize().getColumns());
        String style = (String)options.getOrDefault("style", "");
        if (style.equals("JSON")) {
            highlight((int)options.get("width"), style, engine.format(options, object));
        } else if (!style.isEmpty() && object instanceof String) {
            highlight((int)options.get("width"), style, (String)object);
        } else {
            for (AttributedString as : engine.highlight(options, object)) {
                as.println(terminal);
            }
        }
    }

    private void highlight(int width, String style, String object) {
        SyntaxHighlighter highlighter = configPath != null ? SyntaxHighlighter.build(configPath.getConfig("jnanorc"), style)
                                                           : null;
        for (String s: object.split("\n")) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.append(s).setLength(width);
            if (highlighter != null) {
                highlighter.highlight(asb).println(terminal);
            } else {
                asb.toAttributedString().println(terminal);
            }
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

    public Object echo(Builtins.CommandInput input) {
        final String[] usage = {
                "echo -  print object",
                "Usage: echo object",
                "  -? --help                       Displays command help",
        };
        Options opt = Options.compile(usage).parse(input.args());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }
        try {
            Object[] args = expandParameters(opt.args().stream().toArray(String[]::new));
            if (args.length > 0) {
                println(defaultPrntOptions(), args[0]);
            }
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    public Object prnt(Builtins.CommandInput input) {
        try {
            invokePrnt(expandParameters(input.args()));
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    public Object invokePrnt(Object[] argv) throws Exception {
        final String[] usage = {
                "prnt -  print object",
                "Usage: prnt [OPTIONS] object",
                "  -? --help                       Displays command help",
                "  -s --style=STYLE                Use nanorc STYLE",
                "  -w --width=WIDTH                Display width (default terminal width)"
        };
        Options opt = Options.compile(usage).parse(argv);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        Map<String, Object> options = defaultPrntOptions();
        if (opt.isSet("style")) {
            options.put("style", opt.get("style"));
        }
        if (opt.isSet("width")) {
            options.put("width", opt.getNumber("width"));
        }
        List<Object> args = opt.argObjects();
        if (args.size() > 0) {
            println(options, args.get(0));
        }
        return null;
    }

    private List<Completer> defaultCompleter(String command) {
        return Arrays.asList(NullCompleter.INSTANCE);
    }

}
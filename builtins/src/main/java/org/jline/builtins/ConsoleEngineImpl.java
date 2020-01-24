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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jline.builtins.Builtins;
import org.jline.builtins.Builtins.CommandMethods;
import org.jline.builtins.Completers.FilesCompleter;
import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.Completers.SystemCompleter;
import org.jline.builtins.Nano.SyntaxHighlighter;
import org.jline.builtins.Options.HelpException;
import org.jline.reader.*;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Manage console variables, commands and script execution.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class ConsoleEngineImpl implements ConsoleEngine {
    public enum Command {SHOW
                       , DEL
                       , PRNT
                       , ECHO
                       , SLURP};
    private static final String VAR_PRNT_OPTIONS = "PRNT_OPTIONS";
    private static final String VAR_PATH = "PATH";
    private static final String VAR_NANORC = "NANORC";
    private static final String OPTION_HELP = "-?";
    private static final String OPTION_VERBOSE = "-v";
    private static final String HELP_END = "HELP_END";
    private static final int HELP_MAX_SIZE = 30;
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
    private final Supplier<Path> workDir;
    private ConfigurationPath configPath;

    public ConsoleEngineImpl(ScriptEngine engine, Parser parser, Terminal terminal, Supplier<Path> workDir, ConfigurationPath configPath) {
        this.engine = engine;
        this.parser = parser;
        this.terminal = terminal;
        this.workDir = workDir;
        this.configPath = configPath;
        Set<Command> cmds = new HashSet<>(EnumSet.allOf(Command.class));
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        doNameCommand();
        commandExecute.put(Command.DEL, new CommandMethods(this::del, this::variableCompleter));
        commandExecute.put(Command.SHOW, new CommandMethods(this::show, this::variableCompleter));
        commandExecute.put(Command.PRNT, new CommandMethods(this::prnt, this::prntCompleter));
        commandExecute.put(Command.ECHO, new CommandMethods(this::echo, this::echoCompleter));
        commandExecute.put(Command.SLURP, new CommandMethods(this::slurp, this::slurpCompleter));
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
    public Completers.SystemCompleter compileCompleters() {
        SystemCompleter out = new SystemCompleter();
        for (Map.Entry<Command, String> entry: commandName.entrySet()) {
            out.add(entry.getValue(), commandExecute.get(entry.getKey()).compileCompleter().apply(entry.getValue()));
        }
        out.addAliases(aliasCommand);
        return out;
    }

    @Override
    public Object[] expandParameters(String[] args) throws Exception {
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
        boolean out = false;
        if (command.startsWith(":")) {
            if (systemRegistry.hasCommand(command.substring(1))) {
                out = true;
            } else {
                ScriptFile sf = new ScriptFile(command.substring(1), "", new String[0]);
                if (sf.isScript()) {
                    out = true;
                }
            }
        }
        return out;
    }

    private String quote(String var) {
        if ((var.startsWith("\"") && var.endsWith("\""))
                || (var.startsWith("'") && var.endsWith("'"))) {
            return var;
        }
        if (var.contains("\\\"")) {
            return "'" + var + "'";
        }
        return "\"" + var + "\"";
    }

    private class ScriptFile {
        private File script;
        private String extension = "";
        private String cmdLine;
        private String[] args;
        private boolean verbose;
        private Object result;

        @SuppressWarnings("unchecked")
        public ScriptFile(String command, String cmdLine, String[] args) {
            this.script = new File(command);
            this.cmdLine = cmdLine;
            if (script.exists()) {
                scriptExtension(command);
            } else if (engine.hasVariable(VAR_PATH)) {
                List<String>  extensions = new ArrayList<>();
                extensions.addAll(engine.getExtensions());
                extensions.add(scriptExtension);
                boolean found = false;
                for (String p: (List<String>)engine.get(VAR_PATH)) {
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
            doArgs(args);
        }

        public ScriptFile(File script, String cmdLine, String[] args) {
            if (!script.exists()) {
                throw new IllegalArgumentException();
            }
            this.script = script;
            this.cmdLine = cmdLine;
            scriptExtension(script.getName());
            doArgs(args);
        }

        private void scriptExtension(String command) {
            String name = script.getName();
            this.extension = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "";
            if (!isEngineScript() && !isConsoleScript()) {
                throw new IllegalArgumentException("Command not found: " + command);
            }
        }

        private void doArgs(String[] args) {
            List<String> _args = new ArrayList<>();
            if (isConsoleScript()) {
                _args.add(script.getAbsolutePath());
            }
            for (String a : args) {
                if (isConsoleScript()) {
                    if (!a.equals(OPTION_VERBOSE)) {
                        _args.add(a);
                    } else {
                        this.verbose = true;
                    }
                } else {
                    _args.add(a);
                }
            }
            this.args = _args.toArray(new String[0]);
        }

        private boolean isEngineScript() {
            return engine.getExtensions().contains(extension);
        }

        private boolean isConsoleScript() {
            return scriptExtension.equals(extension);
        }

        private boolean isScript() {
            return engine.getExtensions().contains(extension) || scriptExtension.equals(extension);
        }

        public boolean execute() throws Exception {
            if (!isScript()) {
                return false;
            }
            result = null;
            if (Arrays.asList(args).contains(OPTION_HELP)) {
                try(BufferedReader br = new BufferedReader(new FileReader(script))) {
                    int size = 0;
                    StringBuilder usage = new StringBuilder();
                    for(String l; (l = br.readLine()) != null; ) {
                        size++;
                        l = l.replaceAll("\\s+$", "");
                        String line = l;
                        if (size > HELP_MAX_SIZE || line.endsWith(HELP_END)) {
                            break;
                        }
                        if (l.trim().startsWith("*") || l.trim().startsWith("#")) {
                            line = l.trim().substring(1);
                        } else if (l.trim().startsWith("/*") || l.trim().startsWith("//")) {
                            line = l.trim().substring(2);
                        }
                        usage.append(line).append('\n');
                    }
                    if (usage.length() > 0) {
                        usage.append("\n");
                        throw new HelpException(usage.toString());
                    } else {
                        internalExecute();
                    }
                }
            } else if (isScript()) {
                internalExecute();
            }
            return true;
        }

        private void internalExecute() throws Exception {
            if (isEngineScript()) {
                result = engine.execute(script, expandParameters(args));
                result = postProcess(cmdLine, result);
            } else if (isConsoleScript()) {
                boolean done = false;
                String line = "";
                try (BufferedReader br = new BufferedReader(new FileReader(script))) {
                    for (String l; (l = br.readLine()) != null;) {
                        try {
                            line += l;
                            parser.parse(line, line.length() + 1, ParseContext.ACCEPT_LINE);
                            done = true;
                            for (int i = 1; i < args.length; i++) {
                                line = line.replaceAll("\\s\\$" + i + "\\b",
                                        args[i].startsWith("$") ? (" " + expandName(args[i]) + " ")
                                                : (" " + quote(args[i]) + " "));
                                line = line.replaceAll("\\$\\{" + i + "(|:-.*)\\}",
                                        args[i].startsWith("$") ? expandName(args[i]) : quote(args[i]));
                            }
                            line = line.replaceAll("\\s\\$\\d\\b", "");
                            line = line.replaceAll("\\$\\{\\d+\\}", "");
                            Matcher matcher=Pattern.compile("\\$\\{\\d+:-(.*?)\\}").matcher(line);
                            if (matcher.find()) {
                                  line = matcher.replaceAll("'$1'");
                            }
                            if (verbose) {
                                AttributedStringBuilder asb = new AttributedStringBuilder();
                                asb.append(line, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                                asb.toAttributedString().println(terminal);
                                terminal.flush();
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
                    result = engine.get("_return");
                    result = postProcess(cmdLine, result);
                }
            }
        }

        public Object getResult() {
            return result;
        }

    }

    public Object execute(File script, String cmdLine, String[] args) throws Exception {
        ScriptFile file = new ScriptFile(script, cmdLine, args);
        file.execute();
        return file.getResult();
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
        ScriptFile file = new ScriptFile(cmd, pl.line(), args);
        if (file.execute()) {
            out = file.getResult();
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
                engine.put("_", out);
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
        } else if (!Parser.getCommand(line).equals("show") && systemRegistry.hasCommand(Parser.getCommand(line))
                && result != null) {
            engine.put("_", result);
        }
        return out;
    }

    @Override
    public Object invoke(String command, Object... args) throws Exception {
        exception = null;
        Object out = null;
        if (hasCommand(command)) {
            out = commandExecute.get(command(command)).executeFunction().apply(new Builtins.CommandInput(args, true));
        } else {
            String[] _args = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                if (!(args[i] instanceof String)) {
                    throw new IllegalArgumentException();
                }
                _args[i] = args[i].toString();
            }
            ScriptFile sf = new ScriptFile(command, "", _args);
            if (sf.execute()) {
                out = sf.getResult();
            }
        }
        if (exception != null) {
            throw exception;
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> defaultPrntOptions() {
        Map<String, Object> out = new HashMap<>();
        if (engine.hasVariable(VAR_PRNT_OPTIONS)) {
            out.putAll((Map<String,Object>)engine.get(VAR_PRNT_OPTIONS));
        }
        return out;
    }

    @Override
    public void println(Object object) {
        Map<String,Object> options = defaultPrntOptions();
        options.put("exception", "message");
        println(options, object);
    }

    @Override
    public void println(Map<String, Object> options, Object object) {
        if (object == null) {
            return;
        }
        options.putIfAbsent("width", terminal.getSize().getColumns());
        String style = (String) options.getOrDefault("style", "");
        int width = (int) options.get("width");
        if (style.equalsIgnoreCase("JSON")) {
            highlight(width, style, engine.format(options, object));
        } else if (!style.isEmpty() && object instanceof String) {
            highlight(width, style, (String) object);
        } else if (object instanceof Exception) {
            if (object instanceof Options.HelpException) {
                Options.HelpException.highlight(((Exception)object).getMessage(), Options.HelpException.defaultStyle()).print(terminal);
            } else if (options.getOrDefault("exception", "stack").equals("stack")) {
                ((Exception) object).printStackTrace();
            } else {
                String message = ((Exception) object).getMessage();
                AttributedStringBuilder asb = new AttributedStringBuilder();
                if (message != null) {
                    asb.append(message, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                } else {
                    asb.append("Caught exception: ", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                    asb.append(object.getClass().getCanonicalName(), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                }
                asb.toAttributedString().println(terminal);
            }
        } else if (object instanceof String) {
            highlight(AttributedStyle.YELLOW + AttributedStyle.BRIGHT, object);
        } else if (object instanceof Number) {
            highlight(AttributedStyle.BLUE + AttributedStyle.BRIGHT, object);
        } else {
            for (AttributedString as : engine.highlight(options, object)) {
                as.println(terminal);
            }
        }
        terminal.flush();
    }

    private void highlight(int attrStyle, Object obj) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(obj.toString(), AttributedStyle.DEFAULT.foreground(attrStyle));
        asb.toAttributedString().println(terminal);
    }

    private void highlight(int width, String style, String object) {
        Path nanorc = configPath != null ? configPath.getConfig("jnanorc") : null;
        if (engine.hasVariable(VAR_NANORC)) {
            nanorc = Paths.get((String)engine.get(VAR_NANORC));
        }
        if (nanorc == null) {
            nanorc = Paths.get("/etc/nanorc");
        }
        SyntaxHighlighter highlighter = nanorc != null ? SyntaxHighlighter.build(nanorc, style)
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

    private Object show(Builtins.CommandInput input) {
        final String[] usage = {
                "show -  list console variables",
                "Usage: show [VARIABLE]",
                "  -? --help                       Displays command help",
        };
        Options opt = Options.compile(usage).parse(input.args());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }
        return engine.find(input.args().length > 0 ? input.args()[0] : null);
    }

    private Object del(Builtins.CommandInput input) {
        final String[] usage = {
                "del -  delete console variables",
                "Usage: del [var1] ...",
                "  -? --help                       Displays command help",
        };
        Options opt = Options.compile(usage).parse(input.xargs());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }
        engine.del(input.args());
        return null;
    }

    private Object echo(Builtins.CommandInput input) {
        final String[] usage = {
                "echo -  print object",
                "Usage: echo object",
                "  -? --help                       Displays command help",
        };
        Options opt = Options.compile(usage).parse(input.xargs());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }
        List<Object> args = opt.argObjects();
        if (args.size() > 0) {
            println(defaultPrntOptions(), args.get(0));
        }
        return null;
    }

    private Object prnt(Builtins.CommandInput input) {
        final String[] usage = {
                "prnt -  print object",
                "Usage: prnt [OPTIONS] object",
                "  -? --help                       Displays command help",
                "  -r --rownum                     Display table row numbers",
                "  -s --style=STYLE                Use nanorc STYLE",
                "  -w --width=WIDTH                Display width (default terminal width)"
        };
        Options opt = Options.compile(usage).parse(input.xargs());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }
        Map<String, Object> options = defaultPrntOptions();
        if (opt.isSet("style")) {
            options.put("style", opt.get("style"));
        }
        if (opt.isSet("width")) {
            options.put("width", opt.getNumber("width"));
        }
        if (opt.isSet("rownum")) {
            options.put("rownum", true);
        }
        options.put("exception", "stack");
        List<Object> args = opt.argObjects();
        if (args.size() > 0) {
            println(options, args.get(0));
        }
        return null;
    }

    private Object slurp(Builtins.CommandInput input) {
        final String[] usage = {
                "slurp -  slurp file context to string/object",
                "Usage: slurp [OPTIONS] file",
                "  -? --help                       Displays command help",
                "  -e --encoding=ENCODING          Encoding (default UTF-8)",
                "  -f --format=FORMAT              Serialization format"
        };
        Options opt = Options.compile(usage).parse(input.args());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }
        Object out = null;
        try  {
            if (!opt.args().isEmpty()){
                Charset encoding = opt.isSet("encoding") ? Charset.forName(opt.get("encoding")): StandardCharsets.UTF_8;
                byte[] encoded = Files.readAllBytes(Paths.get(opt.args().get(0)));
                String format = opt.isSet("format") ? opt.get("format") : "";
                if (format.equalsIgnoreCase("TXT")) {
                    out = new String(encoded, encoding);
                } else {
                    out = engine.expandParameter(new String(encoded, encoding), format);
                }
            }
        } catch (Exception e) {
            exception = e;
        }
        return out;
    }

    private List<OptDesc> commandOptions(String command) {
        try {
            invoke(command, "--help");
        } catch (HelpException e) {
            return Builtins.compileCommandOptions(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Completer> slurpCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                           , new OptionCompleter(new FilesCompleter(workDir)
                                                               , this::commandOptions
                                                               , 1)
                                            ));
        return completers;
    }

    private List<Completer> variableCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new StringsCompleter(() -> engine.find().keySet()));
        return completers;
    }

    private List<String> variableReferences() {
        List<String> out = new ArrayList<>();
        for (String v : engine.find().keySet()) {
            out.add("$" + v);
        }
        return out;
    }

    private List<Completer> echoCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new StringsCompleter(this::variableReferences));
        return completers;
    }

    private List<Completer> prntCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                           , new OptionCompleter(new StringsCompleter(this::variableReferences)
                                                               , this::commandOptions
                                                               , 1)
                                            ));
        return completers;
    }

}
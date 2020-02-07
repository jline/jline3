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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
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
                       , ALIAS
                       , UNALIAS
                       , SLURP};
    private static final String VAR_CONSOLE_OPTIONS = "CONSOLE_OPTIONS";
    private static final String VAR_PRNT_OPTIONS = "PRNT_OPTIONS";
    private static final String VAR_PATH = "PATH";
    private static final String VAR_NANORC = "NANORC";
    private static final String[] OPTION_HELP = {"-?", "--help"};
    private static final String OPTION_VERBOSE = "-v";
    private static final String END_HELP = "END_HELP";
    private static final int HELP_MAX_SIZE = 30;
    private final ScriptEngine engine;
    private Map<Command,String> commandName = new HashMap<>();
    private Map<String,Command> nameCommand = new HashMap<>();
    private Map<String,String> aliasCommand = new HashMap<>();
    private final Map<Command,CommandMethods> commandExecute = new HashMap<>();
    private Exception exception;
    private SystemRegistry systemRegistry;
    private String scriptExtension = "jline";
    private final Supplier<Path> workDir;
    private final ConfigurationPath configPath;
    private final Map<String, String> aliases = new HashMap<>();
    private Path aliasFile;
    private LineReader reader;
    private boolean executing = false;

    @SuppressWarnings("unchecked")
    public ConsoleEngineImpl(ScriptEngine engine
                           , Supplier<Path> workDir, ConfigurationPath configPath) throws IOException {
        this.engine = engine;
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
        commandExecute.put(Command.SLURP, new CommandMethods(this::slurpcmd, this::slurpCompleter));
        commandExecute.put(Command.ALIAS, new CommandMethods(this::aliascmd, this::aliasCompleter));
        commandExecute.put(Command.UNALIAS, new CommandMethods(this::unalias, this::unaliasCompleter));
        aliasFile = configPath.getUserConfig("aliases.json");
        if (aliasFile == null) {
            aliasFile = configPath.getUserConfig("aliases.json", true);
            engine.persist(aliasFile, aliases);
        } else {
            aliases.putAll((Map<String,String>)slurp(aliasFile));
        }
    }

    @Override
    public void setLineReader(LineReader reader) {
        this.reader = reader;
    }

    private Parser parser() {
        return reader.getParser();
    }

    private Terminal terminal() {
        return systemRegistry.terminal();
    }

    public boolean isExecuting() {
        return executing;
    }

    @Override
    public void setSystemRegistry(SystemRegistry systemRegistry) {
        this.systemRegistry = systemRegistry;
    }

    @Override
    public void setScriptExtension(String extension) {
        this.scriptExtension = extension;
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

    @Override
    public boolean hasAlias(String name) {
        return aliases.containsKey(name);
    }

    @Override
    public String getAlias(String name) {
        return aliases.getOrDefault(name, null);
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

    private Set<String> variables() {
        return engine.find().keySet();
    }

    @Override
    public List<Completer> scriptCompleters() {
        List<Completer> out = new ArrayList<>();
        out.add(new ArgumentCompleter(new StringsCompleter(this::variables), NullCompleter.INSTANCE));
        out.add(new ArgumentCompleter(new StringsCompleter(this::scripts)
                                    , new OptionCompleter(NullCompleter.INSTANCE
                                                        , this::commandOptions
                                                        , 1)
                                       ));
        out.add(new ArgumentCompleter(new StringsCompleter(aliases::keySet), NullCompleter.INSTANCE));
        return out;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> scripts() {
        List<String> out = new ArrayList<>();
        try {
            List<Path> scripts = new ArrayList<>();
            if (engine.hasVariable(VAR_PATH)) {
                for (String pp : (List<String>) engine.get(VAR_PATH)) {
                    for (String e : scriptExtensions()) {
                        String regex = pp + "/*." + e;
                        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + regex);
                        Files.find(Paths.get(new File(regex).getParent()), Integer.MAX_VALUE,
                                (path, f) -> pathMatcher.matches(path)).forEach(p -> scripts.add(p));
                    }
                }
            }
            for (Path p : scripts) {
                String name = p.toFile().getName();
                out.add(name.substring(0, name.lastIndexOf(".")));
            }
        } catch (Exception e) {
            trace(e);
        }
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
        String regexVar = "[a-zA-Z_]{1,}[a-zA-Z0-9_-]*";
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

    private List<String> scriptExtensions() {
        List<String>  extensions = new ArrayList<>();
        extensions.addAll(engine.getExtensions());
        extensions.add(scriptExtension);
        return extensions;
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
            if (!command.matches("[a-zA-Z0-9_-]+")) {
                return;
            }
            try {
                this.script = new File(command);
                this.cmdLine = cmdLine;
                if (script.exists()) {
                    scriptExtension(command);
                } else if (engine.hasVariable(VAR_PATH)) {
                    boolean found = false;
                    for (String p : (List<String>) engine.get(VAR_PATH)) {
                        for (String e : scriptExtensions()) {
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
            } catch (Exception e) {

            }
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
            if (Arrays.asList(args).contains(OPTION_HELP[0]) || Arrays.asList(args).contains(OPTION_HELP[1])) {
                try(BufferedReader br = new BufferedReader(new FileReader(script))) {
                    int size = 0;
                    StringBuilder usage = new StringBuilder();
                    boolean helpEnd = false;
                    boolean headComment = false;
                    for(String l; (l = br.readLine()) != null; ) {
                        size++;
                        l = l.replaceAll("\\s+$", "");
                        String line = l;
                        if (size > HELP_MAX_SIZE || line.endsWith(END_HELP)) {
                            helpEnd = line.endsWith(END_HELP);
                            break;
                        }
                        if (headComment || size < 3) {
                            String ltr = l.trim();
                            if (ltr.startsWith("*") || ltr.startsWith("#")) {
                                headComment = true;
                                line = ltr.length() > 1 ? ltr.substring(2) : "";
                            } else if (ltr.startsWith("/*") || ltr.startsWith("//")) {
                                headComment = true;
                                line = ltr.length() > 2 ? ltr.substring(3) : "";
                            }
                        }
                        usage.append(line).append('\n');
                    }
                    if (usage.length() > 0) {
                        usage.append("\n");
                        if (!helpEnd) {
                            usage.insert(0, "\n");
                        }
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
                postProcess(cmdLine, result);
            } else if (isConsoleScript()) {
                executing = true;
                boolean done = false;
                String line = "";
                try (BufferedReader br = new BufferedReader(new FileReader(script))) {
                    for (String l; (l = br.readLine()) != null;) {
                        try {
                            line += l;
                            parser().parse(line, line.length() + 1, ParseContext.ACCEPT_LINE);
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
                                asb.toAttributedString().println(terminal());
                                terminal().flush();
                            }
                            systemRegistry.execute(line);
                            line = "";
                        } catch (EOFError e) {
                            done = false;
                            line += "\n";
                        } catch (SyntaxError e) {
                            throw e;
                        } catch (EndOfFileException e) {
                            done = true;
                            executing = false;
                            break;
                        } catch (Exception e) {
                            executing = false;
                            throw new IllegalArgumentException(line + "\n" + e.getMessage());
                        }
                    }
                    if (!done) {
                        executing = false;
                        throw new IllegalArgumentException("Incompleted command: \n" + line);
                    }
                    executing = false;
                    result = engine.get("_return");
                    postProcess(cmdLine, result);
                }
            }
        }

        public Object getResult() {
            return result;
        }

    }

    @Override
    public Object execute(File script, String cmdLine, String[] args) throws Exception {
        ScriptFile file = new ScriptFile(script, cmdLine, args);
        file.execute();
        return file.getResult();
    }

    @Override
    public Object execute(String cmd, String line, String[] args) throws Exception {
        if (line.trim().startsWith("#")) {
            return null;
        }
        Object out = null;
        ScriptFile file = new ScriptFile(cmd, line, args);
        if (file.execute()) {
            out = file.getResult();
        } else {
            line = line.trim();
            if (isCodeBlock(line)) {
                StringBuilder sb = new StringBuilder();
                for (String s: line.split("\n|\n\r")) {
                    if (isCommandLine(s)) {
                        List<String> ws = parser().parse(s, 0, ParseContext.COMPLETE).words();
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
    public void purge() {
        engine.del("_*");
    }

    @Override
    public void putVariable(String name, Object value) {
        engine.put(name, value);
    }

    @Override
    public Object getVariable(String name) {
        if (!engine.hasVariable(name)) {
            throw new IllegalArgumentException("Variable " + name + " does not exists!");
        }
        return engine.get(name);
    }

    @Override
    public boolean executeWidget(Object function) {
        engine.put("_reader", reader);
        engine.put("_widgetFunction", function);
        try {
            if (engine.getEngineName().equals("GroovyEngine")) {
                engine.execute("def _buffer() {_reader.getBuffer()}");
                engine.execute("def _widget(w) {_reader.callWidget(w)}");
            }
            engine.execute("_widgetFunction()");
        } catch (Exception e) {
            trace(e);
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean splitCommandOutput() {
        boolean out = true;
        try {
            if (engine.hasVariable(VAR_CONSOLE_OPTIONS)) {
                out = (boolean) ((Map<String, Object>) engine.get(VAR_CONSOLE_OPTIONS)).getOrDefault("splitOutput", true);
            }
        } catch (Exception e) {
            trace(new Exception("Bad CONSOLE_OPTION value: " + e.getMessage()));
        }
        return out;
    }


    @Override
    public Object postProcess(String line, Object result, String output) {
        Object out = result;
        Object _output = output != null && splitCommandOutput() ? output.split("\n") : output;
        if (Parser.getVariable(line) != null && result != null) {
            engine.put("output", _output);
        }
        if (systemRegistry.hasCommand(Parser.getCommand(line))) {
            out = postProcess(line, Parser.getVariable(line) != null && result == null ? _output : result);
        } else if (Parser.getVariable(line) != null) {
            if (result == null) {
                engine.put(Parser.getVariable(line), _output);
            }
            out = null;
        }
        return out;
    }

    private Object postProcess(String line, Object result) {
        Object out = result instanceof String && ((String)result).trim().length() == 0 ? null : result;
        if (Parser.getVariable(line) != null) {
            engine.put(Parser.getVariable(line), result);
            out = null;
        } else if (!Parser.getCommand(line).equals("show") && result != null) {
            engine.put("_", result);
        }
        return out;
    }

    @Override
    public Object invoke(CommandRegistry.CommandSession session, String command, Object... args) throws Exception {
        exception = null;
        Object out = null;
        if (hasCommand(command)) {
            out = commandExecute.get(command(command)).executeFunction().apply(new Builtins.CommandInput(null, args, session));
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

    @SuppressWarnings("unchecked")
    @Override
    public void trace(Object object) {
        boolean on = false;
        if (engine.hasVariable(VAR_CONSOLE_OPTIONS)) {
            on = (boolean) ((Map<String, Object>) engine.get(VAR_CONSOLE_OPTIONS)).getOrDefault("trace", false);
        }
        Map<String, Object> options = new HashMap<>();
        options.put("exception", "message");
        if (!on && object instanceof Exception) {
            println(options, object);
        } else if (on) {
            options.put("exception", "stack");
            println(options, object);
        }
    }

    @Override
    public void println(Object object) {
        Map<String,Object> options = defaultPrntOptions();
        println(options, object);
    }

    @Override
    public void println(Map<String, Object> options, Object object) {
        if (object == null) {
            return;
        }
        options.putIfAbsent("width", terminal().getSize().getColumns());
        String style = (String) options.getOrDefault("style", "");
        int width = (int) options.get("width");
        if (style.equalsIgnoreCase("JSON")) {
            highlight(width, style, engine.format(options, object));
        } else if (!style.isEmpty() && object instanceof String) {
            highlight(width, style, (String) object);
        } else if (object instanceof Exception) {
            SystemRegistry.println(options.getOrDefault("exception", "stack").equals("stack"), terminal(), (Exception)object);
        } else if (object instanceof String) {
            highlight(AttributedStyle.YELLOW + AttributedStyle.BRIGHT, object);
        } else if (object instanceof Number) {
            highlight(AttributedStyle.BLUE + AttributedStyle.BRIGHT, object);
        } else {
            for (AttributedString as : engine.highlight(options, object)) {
                as.println(terminal());
            }
        }
        terminal().flush();
    }

    private void highlight(int attrStyle, Object obj) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(obj.toString(), AttributedStyle.DEFAULT.foreground(attrStyle));
        asb.toAttributedString().println(terminal());
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
        for (String s: object.split("\\r?\\n")) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.append(s).subSequence(0, width);   // setLength(width) fill nul-chars at the end of line
            if (highlighter != null) {
                highlighter.highlight(asb).println(terminal());
            } else {
                asb.toAttributedString().println(terminal());
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

    private Object slurpcmd(Builtins.CommandInput input) {
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
                String format = opt.isSet("format") ? opt.get("format") : "";
                out = slurp(Paths.get(opt.args().get(0)), encoding, format);
            }
        } catch (Exception e) {
            exception = e;
        }
        return out;
    }

    private Object slurp(Path file) throws IOException {
        return slurp(file, StandardCharsets.UTF_8, "JSON");
    }

    private Object slurp(Path file, Charset encoding, String format) throws IOException {
        Object out = null;
        byte[] encoded = Files.readAllBytes(file);
        if (format.equalsIgnoreCase("TXT")) {
            out = new String(encoded, encoding);
        } else {
            out = engine.expandParameter(new String(encoded, encoding), format);
        }
        return out;
    }

    private Object aliascmd(Builtins.CommandInput input) {
        final String[] usage = {
                "alias -  create command alias",
                "Usage: alias [ALIAS] [COMMANDLINE]",
                "  -? --help                       Displays command help"
        };
        Options opt = Options.compile(usage).parse(input.args());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }
        Object out = null;
        try  {
            List<String> args = opt.args();
            if (args.isEmpty()) {
                out = aliases;
            } else if (args.size() == 1) {
                out = aliases.getOrDefault(args.get(0), null);
            } else {
                aliases.put(args.get(0), String.join(" ", args.subList(1, args.size())));
                engine.persist(aliasFile, aliases);
            }
        } catch (Exception e) {
            exception = e;
        }
        return out;
    }

    private Object unalias(Builtins.CommandInput input) {
        final String[] usage = {
                "unalias -  remove command alias",
                "Usage: unalias [ALIAS...]",
                "  -? --help                       Displays command help"
        };
        Options opt = Options.compile(usage).parse(input.args());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }
        Object out = null;
        try  {
            for (String a : opt.args()) {
                if (aliases.containsKey(a)) {
                    aliases.remove(a);
                }
            }
        } catch (Exception e) {
            exception = e;
        } finally {
            engine.persist(aliasFile, aliases);
        }
        return out;
    }

    private List<OptDesc> commandOptions(String command) {
        try {
            invoke(new CommandSession(), command, "--help");
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

    private List<Completer> prntCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                       , new OptionCompleter(new StringsCompleter(this::variableReferences)
                                           , this::commandOptions
                                           , 1)
                                    ));
        return completers;
    }

    private static class AliasValueCompleter implements Completer {
        private final Map<String,String> aliases;

        public AliasValueCompleter(Map<String,String> aliases) {
            this.aliases = aliases;
        }

        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            List<String> words = commandLine.words();
            if (words.size() > 1) {
                String h = words.get(words.size()-2);
                if (h != null && h.length() > 0) {
                     if(aliases.containsKey(h)){
                          String v = aliases.get(h);
                          candidates.add(new Candidate(AttributedString.stripAnsi(v)
                                           , v, null, null, null, null, true));
                     }
                }
            }
        }
    }

    private List<Completer> aliasCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                , new StringsCompleter(aliases::keySet), new AliasValueCompleter(aliases), NullCompleter.INSTANCE));
        return completers;
    }

    private List<Completer> unaliasCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE, new StringsCompleter(aliases::keySet)));
        return completers;
    }

}
/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jline.builtins.Completers.FilesCompleter;
import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.ConfigurationPath;
import org.jline.builtins.Options;
import org.jline.builtins.Options.HelpException;
import org.jline.builtins.Styles;
import org.jline.console.*;
import org.jline.reader.*;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.Log;
import org.jline.utils.OSUtils;

/**
 * Manage console variables, commands and script execution.
 *
 */
public class ConsoleEngineImpl extends JlineCommandRegistry implements ConsoleEngine {
    public enum Command {
        SHOW,
        DEL,
        PRNT,
        ALIAS,
        PIPE,
        UNALIAS,
        DOC,
        SLURP
    }

    private static final String VAR_CONSOLE_OPTIONS = "CONSOLE_OPTIONS";
    private static final String VAR_PATH = "PATH";
    private static final String[] OPTION_HELP = {"-?", "--help"};
    private static final String OPTION_VERBOSE = "-v";
    private static final String SLURP_FORMAT_TEXT = "TEXT";
    private static final String END_HELP = "END_HELP";
    private static final int HELP_MAX_SIZE = 30;
    private final ScriptEngine engine;
    private Exception exception;
    private SystemRegistry systemRegistry;
    private String scriptExtension = "jline";
    private final Supplier<Path> workDir;
    private final Map<String, String> aliases = new HashMap<>();
    private final Map<String, List<String>> pipes = new HashMap<>();
    private Path aliasFile;
    private LineReader reader;
    private boolean executing = false;
    private final Printer printer;

    public ConsoleEngineImpl(ScriptEngine engine, Printer printer, Supplier<Path> workDir, ConfigurationPath configPath)
            throws IOException {
        this(null, engine, printer, workDir, configPath);
    }

    @SuppressWarnings({"unchecked", "this-escape"})
    public ConsoleEngineImpl(
            Set<Command> commands,
            ScriptEngine engine,
            Printer printer,
            Supplier<Path> workDir,
            ConfigurationPath configPath)
            throws IOException {
        super();
        this.engine = engine;
        this.workDir = workDir;
        this.printer = printer;
        Map<Command, String> commandName = new HashMap<>();
        Map<Command, CommandMethods> commandExecute = new HashMap<>();
        Set<Command> cmds;
        if (commands == null) {
            cmds = new HashSet<>(EnumSet.allOf(Command.class));
        } else {
            cmds = new HashSet<>(commands);
        }
        for (Command c : cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        commandExecute.put(Command.DEL, new CommandMethods(this::del, this::variableCompleter));
        commandExecute.put(Command.SHOW, new CommandMethods(this::show, this::variableCompleter));
        commandExecute.put(Command.PRNT, new CommandMethods(this::prnt, this::prntCompleter));
        commandExecute.put(Command.SLURP, new CommandMethods(this::slurpcmd, this::slurpCompleter));
        commandExecute.put(Command.ALIAS, new CommandMethods(this::aliascmd, this::aliasCompleter));
        commandExecute.put(Command.UNALIAS, new CommandMethods(this::unalias, this::unaliasCompleter));
        commandExecute.put(Command.DOC, new CommandMethods(this::doc, this::docCompleter));
        commandExecute.put(Command.PIPE, new CommandMethods(this::pipe, this::defaultCompleter));
        aliasFile = configPath.getUserConfig("aliases.json");
        if (aliasFile == null) {
            aliasFile = configPath.getUserConfig("aliases.json", true);
            if (aliasFile == null) {
                Log.warn("Failed to write in user config path!");
                aliasFile = OSUtils.IS_WINDOWS ? Paths.get("NUL") : Paths.get("/dev/null");
            }
            persist(aliasFile, aliases);
        } else {
            aliases.putAll((Map<String, String>) slurp(aliasFile));
        }
        registerCommands(commandName, commandExecute);
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
    public boolean hasAlias(String name) {
        return aliases.containsKey(name);
    }

    @Override
    public String getAlias(String name) {
        return aliases.getOrDefault(name, null);
    }

    @Override
    public Map<String, List<String>> getPipes() {
        return pipes;
    }

    @Override
    public List<String> getNamedPipes() {
        List<String> out = new ArrayList<>();
        List<String> opers = new ArrayList<>();
        for (String p : pipes.keySet()) {
            if (p.matches("[a-zA-Z0-9]+")) {
                out.add(p);
            } else {
                opers.add(p);
            }
        }
        opers.addAll(systemRegistry.getPipeNames());
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            if (opers.contains(entry.getValue().split(" ")[0])) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    @Override
    public List<Completer> scriptCompleters() {
        List<Completer> out = new ArrayList<>();
        out.add(new ArgumentCompleter(
                new StringsCompleter(this::scriptNames),
                new OptionCompleter(NullCompleter.INSTANCE, this::commandOptions, 1)));
        out.add(new ArgumentCompleter(new StringsCompleter(this::commandAliasNames), NullCompleter.INSTANCE));
        return out;
    }

    private Set<String> commandAliasNames() {
        Set<String> opers =
                pipes.keySet().stream().filter(p -> !p.matches("\\w+")).collect(Collectors.toSet());
        opers.addAll(systemRegistry.getPipeNames());
        return aliases.entrySet().stream()
                .filter(e -> !opers.contains(e.getValue().split(" ")[0]))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Set<String> scriptNames() {
        return scripts().keySet();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Boolean> scripts() {
        Map<String, Boolean> out = new HashMap<>();
        try {
            List<Path> scripts = new ArrayList<>();
            if (engine.hasVariable(VAR_PATH)) {
                List<String> dirs = new ArrayList<>();
                for (String file : (List<String>) engine.get(VAR_PATH)) {
                    file = file.startsWith("~") ? file.replace("~", System.getProperty("user.home")) : file;
                    File dir = new File(file);
                    if (dir.exists() && dir.isDirectory()) {
                        dirs.add(file);
                    }
                }
                for (String pp : dirs) {
                    for (String e : scriptExtensions()) {
                        String regex = pp + "/*." + e;
                        PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + regex);
                        try (Stream<Path> pathStream =
                                Files.walk(new File(regex).getParentFile().toPath())) {
                            pathStream.filter(pathMatcher::matches).forEach(scripts::add);
                        }
                    }
                }
            }
            for (Path p : scripts) {
                String name = p.getFileName().toString();
                int idx = name.lastIndexOf(".");
                out.put(name.substring(0, idx), name.substring(idx + 1).equals(scriptExtension));
            }
        } catch (NoSuchFileException e) {
            error("Failed reading PATH. No file found: " + e.getMessage());
        } catch (InvalidPathException e) {
            error("Failed reading PATH. Invalid path:");
            error(e.toString());
        } catch (Exception e) {
            error("Failed reading PATH:");
            trace(e);
            engine.put("exception", e);
        }
        return out;
    }

    @Override
    public Object[] expandParameters(String[] args) throws Exception {
        Object[] out = new Object[args.length];
        String regexPath = "(.*)\\$\\{(.*?)}(/.*)";
        for (int i = 0; i < args.length; i++) {
            if (args[i].matches(regexPath)) {
                Matcher matcher = Pattern.compile(regexPath).matcher(args[i]);
                if (matcher.find()) {
                    out[i] = matcher.group(1) + engine.get(matcher.group(2)) + matcher.group(3);
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (args[i].startsWith("${")) {
                out[i] = engine.execute(expandName(args[i]));
            } else if (args[i].startsWith("$")) {
                out[i] = engine.get(expandName(args[i]));
            } else {
                out[i] = engine.deserialize(args[i]);
            }
        }
        return out;
    }

    private String expandToList(String[] args) {
        return expandToList(Arrays.asList(args));
    }

    @Override
    public String expandToList(List<String> params) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String param : params) {
            if (!first) {
                sb.append(",");
            }
            if (param.equalsIgnoreCase("true") || param.equalsIgnoreCase("false") || param.equalsIgnoreCase("null")) {
                sb.append(param.toLowerCase());
            } else if (isNumber(param)) {
                sb.append(param);
            } else {
                sb.append(param.startsWith("$") ? param.substring(1) : quote(param));
            }
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }

    private String expandName(String name) {
        String regexVar = "[a-zA-Z_]+[a-zA-Z0-9_-]*";
        String out = name;
        if (name.matches("^\\$" + regexVar)) {
            out = name.substring(1);
        } else if (name.matches("^\\$\\{" + regexVar + "}.*")) {
            Matcher matcher = Pattern.compile("^\\$\\{(" + regexVar + ")}(.*)").matcher(name);
            if (matcher.find()) {
                out = matcher.group(1) + matcher.group(2);
            } else {
                throw new IllegalArgumentException();
            }
        }
        return out;
    }

    private boolean isNumber(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private boolean isCodeBlock(String line) {
        return line.contains("\n") && line.trim().endsWith("}");
    }

    private boolean isCommandLine(String line) {
        String command = parser().getCommand(line);
        boolean out = false;
        if (command != null && command.startsWith(":")) {
            command = command.substring(1);
            if (hasAlias(command)) {
                command = getAlias(command);
            }
            if (systemRegistry.hasCommand(command)) {
                out = true;
            } else {
                ScriptFile sf = new ScriptFile(command, "", new String[0]);
                if (sf.isScript()) {
                    out = true;
                }
            }
        }
        return out;
    }

    private String quote(String var) {
        if ((var.startsWith("\"") && var.endsWith("\"")) || (var.startsWith("'") && var.endsWith("'"))) {
            return var;
        }
        if (var.contains("\\\"")) {
            return "'" + var + "'";
        }
        return "\"" + var + "\"";
    }

    private List<String> scriptExtensions() {
        List<String> extensions = new ArrayList<>(engine.getExtensions());
        extensions.add(scriptExtension);
        return extensions;
    }

    private class ScriptFile {
        private Path script;
        private String extension = "";
        private String cmdLine;
        private String[] args;
        private boolean verbose;
        private Object result;

        @SuppressWarnings("unchecked")
        public ScriptFile(String command, String cmdLine, String[] args) {
            this.cmdLine = cmdLine;
            try {
                if (!parser().validCommandName(command)) {
                    // DefaultParser not necessarily parse script file from command line.
                    // As an example for Groovy REPL demo '/tmp/script.jline' is not a valid command i.e.
                    // prompt> /tmp/script.jline arg1 arg2
                    command = cmdLine.split("\\s+")[0];
                    this.extension = fileExtension(command);
                    if (isScript()) {
                        this.extension = "";
                        this.script = Paths.get(command);
                        if (Files.exists(script)) {
                            scriptExtension(command);
                        }
                    }
                } else {
                    this.script = Paths.get(command);
                    if (Files.exists(script)) {
                        scriptExtension(command);
                    } else if (engine.hasVariable(VAR_PATH)) {
                        boolean found = false;
                        for (String p : (List<String>) engine.get(VAR_PATH)) {
                            for (String e : scriptExtensions()) {
                                String file = command + "." + e;
                                Path path = Paths.get(p, file);
                                if (Files.exists(path)) {
                                    script = path;
                                    this.extension = e;
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
                doArgs(args);
            } catch (Exception e) {
                Log.trace("Not a script file: " + command);
            }
        }

        public ScriptFile(Path script, String cmdLine, String[] args) {
            if (!Files.exists(script)) {
                throw new IllegalArgumentException("Script file not found!");
            }
            this.script = script;
            this.cmdLine = cmdLine;
            scriptExtension(script.getFileName().toString());
            doArgs(args);
        }

        private String fileExtension(String fileName) {
            return fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1) : "";
        }

        private void scriptExtension(String command) {
            this.extension = fileExtension(script.getFileName().toString());
            if (!isEngineScript() && !isConsoleScript()) {
                throw new IllegalArgumentException("Command not found: " + command);
            }
        }

        private void doArgs(String[] args) {
            List<String> _args = new ArrayList<>();
            if (isConsoleScript()) {
                _args.add(script.toAbsolutePath().toString());
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
            if (Arrays.asList(args).contains(OPTION_HELP[0])
                    || Arrays.asList(args).contains(OPTION_HELP[1])) {
                try (BufferedReader br = Files.newBufferedReader(script)) {
                    int size = 0;
                    StringBuilder usage = new StringBuilder();
                    boolean helpEnd = false;
                    boolean headComment = false;
                    for (String l; (l = br.readLine()) != null; ) {
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
            } else {
                internalExecute();
            }
            return true;
        }

        private String expandParameterName(String parameter) {
            if (parameter.startsWith("$")) {
                return expandName(parameter);
            } else if (isNumber(parameter)) {
                return parameter;
            }
            return quote(parameter);
        }

        private void internalExecute() throws Exception {
            if (isEngineScript()) {
                result = engine.execute(script, expandParameters(args));
            } else if (isConsoleScript()) {
                executing = true;
                boolean done = true;
                String line = "";
                try (BufferedReader br = Files.newBufferedReader(script)) {
                    for (String l; (l = br.readLine()) != null; ) {
                        if (l.trim().isEmpty() || l.trim().startsWith("#")) {
                            done = true;
                            continue;
                        }
                        try {
                            line += l;
                            parser().parse(line, line.length() + 1, ParseContext.ACCEPT_LINE);
                            done = true;
                            for (int i = 1; i < args.length; i++) {
                                line = line.replaceAll(
                                        "\\s\\$" + i + "\\b", (" " + expandParameterName(args[i]) + " "));
                                line = line.replaceAll("\\$\\{" + i + "(|:-.*)}", expandParameterName(args[i]));
                            }
                            line = line.replaceAll("\\$\\{@}", expandToList(args));
                            line = line.replaceAll("\\$@", expandToList(args));
                            line = line.replaceAll("\\s\\$\\d\\b", "");
                            line = line.replaceAll("\\$\\{\\d+}", "");
                            Matcher matcher =
                                    Pattern.compile("\\$\\{\\d+:-(.*?)}").matcher(line);
                            if (matcher.find()) {
                                line = matcher.replaceAll(expandParameterName(matcher.group(1)));
                            }
                            if (verbose) {
                                AttributedStringBuilder asb = new AttributedStringBuilder();
                                asb.styled(Styles.prntStyle().resolve(".vs"), line);
                                asb.toAttributedString().println(terminal());
                                terminal().flush();
                            }
                            println(systemRegistry.execute(line));
                            line = "";
                        } catch (EOFError e) {
                            done = false;
                            line += "\n";
                        } catch (SyntaxError e) {
                            throw e;
                        } catch (EndOfFileException e) {
                            done = true;
                            result = engine.get("_return");
                            postProcess(cmdLine, result);
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
                }
            }
        }

        public Object getResult() {
            return result;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            try {
                sb.append("script:").append(script.normalize());
            } catch (Exception e) {
                sb.append(e.getMessage());
            }
            sb.append(", ");
            sb.append("extension:").append(extension);
            sb.append(", ");
            sb.append("cmdLine:").append(cmdLine);
            sb.append(", ");
            sb.append("args:").append(Arrays.asList(args));
            sb.append(", ");
            sb.append("verbose:").append(verbose);
            sb.append(", ");
            sb.append("result:").append(result);
            sb.append("]");
            return sb.toString();
        }
    }

    @Override
    public Object execute(Path script, String cmdLine, String[] args) throws Exception {
        ScriptFile file = new ScriptFile(script, cmdLine, args);
        file.execute();
        return file.getResult();
    }

    @Override
    public String expandCommandLine(String line) {
        String out;
        if (isCommandLine(line)) {
            StringBuilder sb = new StringBuilder();
            List<String> ws = parser().parse(line, 0, ParseContext.COMPLETE).words();
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
            String cmd = hasAlias(ws.get(0).substring(idx + 1))
                    ? getAlias(ws.get(0).substring(idx + 1))
                    : ws.get(0).substring(idx + 1);
            sb.append(SystemRegistry.class.getCanonicalName())
                    .append(".get().invoke('")
                    .append(cmd)
                    .append("'");
            for (int i = 1; i < argv.length; i++) {
                sb.append(", ");
                sb.append(argv[i]);
            }
            sb.append(")");
            out = sb.toString();
        } else {
            out = line;
        }
        return out;
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
                for (String s : line.split("\\r?\\n")) {
                    sb.append(expandCommandLine(s));
                    sb.append("\n");
                }
                line = sb.toString();
            }
            if (engine.hasVariable(line)) {
                out = engine.get(line);
            } else if (parser().getVariable(line) == null) {
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
    public boolean hasVariable(String name) {
        return engine.hasVariable(name);
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
        } finally {
            purge();
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> consoleOptions() {
        return engine.hasVariable(VAR_CONSOLE_OPTIONS)
                ? (Map<String, Object>) engine.get(VAR_CONSOLE_OPTIONS)
                : new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T consoleOption(String option, T defval) {
        T out = defval;
        try {
            out = (T) consoleOptions().getOrDefault(option, defval);
        } catch (Exception e) {
            trace(new Exception("Bad CONSOLE_OPTION value: " + e.getMessage()));
        }
        return out;
    }

    @Override
    public void setConsoleOption(String name, Object value) {
        consoleOptions().put(name, value);
    }

    private boolean consoleOption(String option) {
        boolean out = false;
        try {
            out = consoleOptions().containsKey(option);
        } catch (Exception e) {
            trace(new Exception("Bad CONSOLE_OPTION value: " + e.getMessage()));
        }
        return out;
    }

    @Override
    public ExecutionResult postProcess(String line, Object result, String output) {
        ExecutionResult out;
        Object _output = output != null && !output.trim().isEmpty() && !consoleOption("no-splittedOutput")
                ? output.split("\\r?\\n")
                : output;
        String consoleVar = parser().getVariable(line);
        if (consoleVar != null && result != null) {
            engine.put("output", _output);
        }
        if (systemRegistry.hasCommand(parser().getCommand(line))) {
            out = postProcess(line, consoleVar != null && result == null ? _output : result);
        } else {
            Object _result = result == null ? _output : result;
            int status = saveResult(consoleVar, _result);
            out = new ExecutionResult(status, consoleVar != null && !consoleVar.startsWith("_") ? null : _result);
        }
        return out;
    }

    private ExecutionResult postProcess(String line, Object result) {
        int status = 0;
        Object out = result instanceof String && ((String) result).trim().isEmpty() ? null : result;
        String consoleVar = parser().getVariable(line);
        if (consoleVar != null) {
            status = saveResult(consoleVar, result);
            out = null;
        } else if (!parser().getCommand(line).equals("show")) {
            if (result != null) {
                status = saveResult("_", result);
            } else {
                status = 1;
            }
        }
        return new ExecutionResult(status, out);
    }

    @Override
    public ExecutionResult postProcess(Object result) {
        return new ExecutionResult(saveResult(null, result), result);
    }

    private int saveResult(String var, Object result) {
        int out;
        try {
            engine.put("_executionResult", result);
            if (var != null) {
                if (var.contains(".") || var.contains("[")) {
                    engine.execute(var + " = _executionResult");
                } else {
                    engine.put(var, result);
                }
            }
            out = (int) engine.execute("_executionResult ? 0 : 1");
        } catch (Exception e) {
            trace(e);
            out = 1;
        }
        return out;
    }

    @Override
    public Object invoke(CommandRegistry.CommandSession session, String command, Object... args) throws Exception {
        exception = null;
        Object out = null;
        if (hasCommand(command)) {
            out = getCommandMethods(command).execute().apply(new CommandInput(command, args, session));
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

    public void trace(final Object object) {
        Object toPrint = object;
        int level = consoleOption("trace", 0);
        Map<String, Object> options = new HashMap<>();
        if (level < 2) {
            options.put("exception", "message");
        }
        if (level == 0) {
            if (!(object instanceof Throwable)) {
                toPrint = null;
            }
        } else if (level == 1) {
            if (object instanceof SystemRegistryImpl.CommandData) {
                toPrint = ((SystemRegistryImpl.CommandData) object).rawLine();
            }
        } else if (level > 1) {
            if (object instanceof SystemRegistryImpl.CommandData) {
                toPrint = object.toString();
            }
        }
        printer.println(options, toPrint);
    }

    private void error(String message) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.styled(Styles.prntStyle().resolve(".em"), message);
        asb.println(terminal());
    }

    @Override
    public void println(Object object) {
        printer.println(object);
    }

    private Object show(CommandInput input) {
        final String[] usage = {
            "show -  list console variables",
            "Usage: show [VARIABLE]",
            "  -? --help                       Displays command help",
        };
        try {
            parseOptions(usage, input.args());
            Map<String, Object> options = new HashMap<>();
            options.put(Printer.MAX_DEPTH, 0);
            printer.println(options, engine.find(input.args().length > 0 ? input.args()[0] : null));
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    private Object del(CommandInput input) {
        final String[] usage = {
            "del -  delete console variables, methods, classes and imports",
            "Usage: del [var1] ...",
            "  -? --help                       Displays command help",
        };
        try {
            parseOptions(usage, input.args());
            engine.del(input.args());
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    private Object prnt(CommandInput input) {
        Exception result = printer.prntCommand(input);
        if (result != null) {
            exception = result;
        }
        return null;
    }

    private Object slurpcmd(CommandInput input) {
        final String[] usage = {
            "slurp -  slurp file or string variable context to object",
            "Usage: slurp [OPTIONS] file|variable",
            "  -? --help                       Displays command help",
            "  -e --encoding=ENCODING          Encoding (default UTF-8)",
            "  -f --format=FORMAT              Serialization format"
        };
        Object out = null;
        try {
            Options opt = parseOptions(usage, input.xargs());
            if (!opt.args().isEmpty()) {
                Object _arg = opt.argObjects().get(0);
                if (!(_arg instanceof String)) {
                    throw new IllegalArgumentException(
                            "Invalid parameter type: " + _arg.getClass().getSimpleName());
                }
                String arg = (String) _arg;
                Charset encoding =
                        opt.isSet("encoding") ? Charset.forName(opt.get("encoding")) : StandardCharsets.UTF_8;
                String format = opt.isSet("format")
                        ? opt.get("format")
                        : engine.getSerializationFormats().get(0);
                try {
                    Path path = Paths.get(arg);
                    if (Files.exists(path)) {
                        if (!format.equals(SLURP_FORMAT_TEXT)) {
                            out = slurp(path, encoding, format);
                        } else {
                            out = Files.readAllLines(Paths.get(arg), encoding);
                        }
                    } else {
                        if (!format.equals(SLURP_FORMAT_TEXT)) {
                            out = engine.deserialize(arg, format);
                        } else {
                            out = arg.split("\n");
                        }
                    }
                } catch (Exception e) {
                    out = engine.deserialize(arg, format);
                }
            }
        } catch (Exception e) {
            exception = e;
        }
        return out;
    }

    @Override
    public void persist(Path file, Object object) {
        engine.persist(file, object);
    }

    @Override
    public Object slurp(Path file) throws IOException {
        return slurp(
                file, StandardCharsets.UTF_8, engine.getSerializationFormats().get(0));
    }

    private Object slurp(Path file, Charset encoding, String format) throws IOException {
        byte[] encoded = Files.readAllBytes(file);
        return engine.deserialize(new String(encoded, encoding), format);
    }

    private Object aliascmd(CommandInput input) {
        final String[] usage = {
            "alias -  create command alias",
            "Usage: alias [ALIAS] [COMMANDLINE]",
            "  -? --help                       Displays command help"
        };
        Object out = null;
        try {
            Options opt = parseOptions(usage, input.args());
            List<String> args = opt.args();
            if (args.isEmpty()) {
                out = aliases;
            } else if (args.size() == 1) {
                out = aliases.getOrDefault(args.get(0), null);
            } else {
                String alias = String.join(" ", args.subList(1, args.size()));
                for (int j = 0; j < 10; j++) {
                    alias = alias.replaceAll("%" + j, "\\$" + j);
                    alias = alias.replaceAll("%\\{" + j + "}", "\\$\\{" + j + "\\}");
                    alias = alias.replaceAll("%\\{" + j + ":-", "\\$\\{" + j + ":-");
                }
                alias = alias.replaceAll("%@", "\\$@");
                alias = alias.replaceAll("%\\{@}", "\\${@}");
                aliases.put(args.get(0), alias);
                persist(aliasFile, aliases);
            }
        } catch (Exception e) {
            exception = e;
        }
        return out;
    }

    private Object unalias(CommandInput input) {
        final String[] usage = {
            "unalias -  remove command alias",
            "Usage: unalias [ALIAS...]",
            "  -? --help                       Displays command help"
        };
        try {
            Options opt = parseOptions(usage, input.args());
            for (String a : opt.args()) {
                aliases.remove(a);
            }
            persist(aliasFile, aliases);
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    private Object pipe(CommandInput input) {
        final String[] usage = {
            "pipe -  create/delete pipe operator",
            "Usage: pipe [OPERATOR] [PREFIX] [POSTFIX]",
            "       pipe --list",
            "       pipe --delete [OPERATOR...]",
            "  -? --help                       Displays command help",
            "  -d --delete                     Delete pipe operators",
            "  -l --list                       List pipe operators",
        };
        try {
            Options opt = parseOptions(usage, input.args());
            Map<String, Object> options = new HashMap<>();
            if (opt.isSet("delete")) {
                if (opt.args().size() == 1 && opt.args().get(0).equals("*")) {
                    pipes.clear();
                } else {
                    for (String p : opt.args()) {
                        pipes.remove(p.trim());
                    }
                }
            } else if (opt.isSet("list") || opt.args().isEmpty()) {
                options.put(Printer.MAX_DEPTH, 0);
                printer.println(options, pipes);
            } else if (opt.args().size() != 3) {
                exception = new IllegalArgumentException("Bad number of arguments!");
            } else if (systemRegistry.getPipeNames().contains(opt.args().get(0))) {
                exception = new IllegalArgumentException("Reserved pipe operator");
            } else {
                List<String> fixes = new ArrayList<>();
                fixes.add(opt.args().get(1));
                fixes.add(opt.args().get(2));
                pipes.put(opt.args().get(0), fixes);
            }
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    private Object doc(CommandInput input) {
        final String[] usage = {
            "doc -  open document on browser",
            "Usage: doc [OBJECT]",
            "  -? --help                       Displays command help"
        };
        try {
            parseOptions(usage, input.xargs());
            if (input.xargs().length == 0) {
                return null;
            }
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("Desktop is not supported!");
            }
            Map<String, Object> docs;
            try {
                docs = consoleOption("docs", null);
            } catch (Exception e) {
                Exception exception = new IllegalStateException("Bad documents configuration!");
                exception.addSuppressed(e);
                throw exception;
            }
            if (docs == null) {
                throw new IllegalStateException("No documents configuration!");
            }
            boolean done = false;
            Object arg = input.xargs()[0];
            if (arg instanceof String) {
                String address = (String) docs.get(input.args()[0]);
                if (address != null) {
                    done = true;
                    if (urlExists(address)) {
                        Desktop.getDesktop().browse(new URI(address));
                    } else {
                        throw new IllegalArgumentException("Document not found: " + address);
                    }
                }
            }
            if (!done) {
                String name;
                if (arg instanceof String && ((String) arg).matches("([a-z]+\\.)+[A-Z][a-zA-Z]+")) {
                    name = (String) arg;
                } else {
                    name = arg.getClass().getCanonicalName();
                }
                name = name.replaceAll("\\.", "/") + ".html";
                Object doc = null;
                for (Map.Entry<String, Object> entry : docs.entrySet()) {
                    if (name.matches(entry.getKey())) {
                        doc = entry.getValue();
                        break;
                    }
                }
                if (doc == null) {
                    throw new IllegalArgumentException("No document configuration for " + name);
                }
                String url = name;
                if (doc instanceof Collection) {
                    for (Object o : (Collection<?>) doc) {
                        url = o + name;
                        if (urlExists(url)) {
                            Desktop.getDesktop().browse(new URI(url));
                            done = true;
                        }
                    }
                } else {
                    url = doc + name;
                    if (urlExists(url)) {
                        Desktop.getDesktop().browse(new URI(url));
                        done = true;
                    }
                }
                if (!done) {
                    throw new IllegalArgumentException("Document not found: " + url);
                }
            }
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    private boolean urlExists(String weburl) {
        try {
            URL url = URI.create(weburl).toURL();
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setRequestMethod("HEAD");
            return huc.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        }
    }

    private List<Completer> slurpCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        List<OptDesc> optDescs = commandOptions("slurp");
        for (OptDesc o : optDescs) {
            if (o.shortOption() != null && o.shortOption().equals("-f")) {
                List<String> formats = new ArrayList<>(engine.getDeserializationFormats());
                formats.add(SLURP_FORMAT_TEXT);
                o.setValueCompleter(new StringsCompleter(formats));
                break;
            }
        }
        AggregateCompleter argCompleter =
                new AggregateCompleter(new FilesCompleter(workDir), new VariableReferenceCompleter(engine));
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE,
                new OptionCompleter(Arrays.asList(argCompleter, NullCompleter.INSTANCE), optDescs, 1)));
        return completers;
    }

    private List<Completer> variableCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new StringsCompleter(() -> engine.find().keySet()));
        return completers;
    }

    private List<Completer> prntCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE,
                new OptionCompleter(
                        Arrays.asList(new VariableReferenceCompleter(engine), NullCompleter.INSTANCE),
                        this::commandOptions,
                        1)));
        return completers;
    }

    private static class VariableReferenceCompleter implements Completer {
        private final ScriptEngine engine;

        public VariableReferenceCompleter(ScriptEngine engine) {
            this.engine = engine;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            String word = commandLine.word();
            try {
                if (!word.contains(".") && !word.contains("}")) {
                    for (String v : engine.find().keySet()) {
                        String c = "${" + v + "}";
                        candidates.add(new Candidate(AttributedString.stripAnsi(c), c, null, null, null, null, false));
                    }
                } else if (word.startsWith("${") && word.contains("}") && word.contains(".")) {
                    String var = word.substring(2, word.indexOf('}'));
                    if (engine.hasVariable(var)) {
                        String curBuf = word.substring(0, word.lastIndexOf("."));
                        String objStatement = curBuf.replace("${", "").replace("}", "");
                        Object obj = curBuf.contains(".") ? engine.execute(objStatement) : engine.get(var);
                        Map<?, ?> map = obj instanceof Map ? (Map<?, ?>) obj : null;
                        Set<String> identifiers = new HashSet<>();
                        if (map != null
                                && !map.isEmpty()
                                && map.keySet().iterator().next() instanceof String) {
                            identifiers = (Set<String>) map.keySet();
                        } else if (map == null && obj != null) {
                            identifiers = getClassMethodIdentifiers(obj.getClass());
                        }
                        for (String key : identifiers) {
                            candidates.add(new Candidate(
                                    AttributedString.stripAnsi(curBuf + "." + key),
                                    key,
                                    null,
                                    null,
                                    null,
                                    null,
                                    false));
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }

        private Set<String> getClassMethodIdentifiers(Class<?> clazz) {
            Set<String> out = new HashSet<>();
            do {
                for (Method m : clazz.getMethods()) {
                    if (!m.isSynthetic() && m.getParameterCount() == 0) {
                        String name = m.getName();
                        if (name.matches("get[A-Z].*")) {
                            out.add(convertGetMethod2identifier(name));
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            } while (clazz != null);
            return out;
        }

        private String convertGetMethod2identifier(String name) {
            char[] c = name.substring(3).toCharArray();
            c[0] = Character.toLowerCase(c[0]);
            return new String(c);
        }
    }

    private static class AliasValueCompleter implements Completer {
        private final Map<String, String> aliases;

        public AliasValueCompleter(Map<String, String> aliases) {
            this.aliases = aliases;
        }

        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            List<String> words = commandLine.words();
            if (words.size() > 1) {
                String h = words.get(words.size() - 2);
                if (h != null && !h.isEmpty()) {
                    String v = aliases.get(h);
                    if (v != null) {
                        candidates.add(new Candidate(AttributedString.stripAnsi(v), v, null, null, null, null, true));
                    }
                }
            }
        }
    }

    private List<Completer> aliasCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        List<Completer> params = new ArrayList<>();
        params.add(new StringsCompleter(aliases::keySet));
        params.add(new AliasValueCompleter(aliases));
        completers.add(
                new ArgumentCompleter(NullCompleter.INSTANCE, new OptionCompleter(params, this::commandOptions, 1)));
        return completers;
    }

    private List<Completer> unaliasCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE,
                new OptionCompleter(new StringsCompleter(aliases::keySet), this::commandOptions, 1)));
        return completers;
    }

    private List<String> docs() {
        List<String> out = new ArrayList<>();
        Map<String, String> docs = consoleOption("docs", null);
        if (docs == null) {
            return out;
        }
        for (String v : engine.find().keySet()) {
            out.add("$" + v);
        }
        if (!docs.isEmpty()) {
            for (String d : docs.keySet()) {
                if (d.matches("\\w+")) {
                    out.add(d);
                }
            }
        }
        return out;
    }

    private List<Completer> docCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(
                NullCompleter.INSTANCE,
                new OptionCompleter(
                        Arrays.asList(new StringsCompleter(this::docs), NullCompleter.INSTANCE),
                        this::commandOptions,
                        1)));
        return completers;
    }
}

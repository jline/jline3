/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import static org.jline.keymap.KeyMap.ctrl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jline.builtins.Completers.FilesCompleter;
import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.ConsoleEngine.ExecutionResult;
import org.jline.builtins.Widgets;
import org.jline.builtins.Options.HelpException;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.ConfigurationPath;
import org.jline.reader.*;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.utils.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Aggregate command registeries.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class SystemRegistryImpl implements SystemRegistry {

    public enum Pipe {
        FLIP, NAMED, AND, OR
    }

    private static final Class<?>[] BUILTIN_REGISTERIES = { Builtins.class, ConsoleEngineImpl.class };
    private CommandRegistry[] commandRegistries;
    private Integer consoleId;
    private Parser parser;
    private ConfigurationPath configPath;
    private Map<String,CommandRegistry> subcommands = new HashMap<>();
    private Map<Pipe, String> pipeName = new HashMap<>();
    private final Map<String, CommandMethods> commandExecute = new HashMap<>();
    private Map<String, List<String>> commandInfos = new HashMap<>();
    private Exception exception;
    private CommandOutputStream outputStream;
    private ScriptStore scriptStore = new ScriptStore();
    private NamesAndValues names = new NamesAndValues();
    private Supplier<Path> workDir;
    private SystemCompleter customSystemCompleter = new SystemCompleter();
    private AggregateCompleter customAggregateCompleter = new AggregateCompleter(new ArrayList<>());

    public SystemRegistryImpl(Parser parser, Terminal terminal, Supplier<Path> workDir, ConfigurationPath configPath) {
        this.parser = parser;
        this.workDir = workDir;
        this.configPath = configPath;
        outputStream = new CommandOutputStream(terminal);
        pipeName.put(Pipe.FLIP, "|;");
        pipeName.put(Pipe.NAMED, "|");
        pipeName.put(Pipe.AND, "&&");
        pipeName.put(Pipe.OR, "||");
        commandExecute.put("exit", new CommandMethods(this::exit, this::exitCompleter));
        commandExecute.put("help", new CommandMethods(this::help, this::helpCompleter));
    }

    public void rename(Pipe pipe, String name) {
        if (name.matches("/w+") || pipeName.containsValue(name)) {
            throw new IllegalArgumentException();
        }
        pipeName.put(pipe, name);
    }

    @Override
    public Collection<String> getPipeNames() {
        return pipeName.values();
    }

    @Override
    public void setCommandRegistries(CommandRegistry... commandRegistries) {
        this.commandRegistries = commandRegistries;
        for (int i = 0; i < commandRegistries.length; i++) {
            if (commandRegistries[i] instanceof ConsoleEngine) {
                if (consoleId != null) {
                    throw new IllegalArgumentException();
                } else {
                    this.consoleId = i;
                    ((ConsoleEngine) commandRegistries[i]).setSystemRegistry(this);
                    this.scriptStore = new ScriptStore((ConsoleEngine)commandRegistries[i]);
                    this.names = new NamesAndValues(configPath);
                }
            } else if (commandRegistries[i] instanceof SystemRegistry) {
                throw new IllegalArgumentException();
            }
        }
        SystemRegistry.add(this);
    }

    @Override
    public void initialize(File script) {
        if (consoleId != null) {
            try {
                consoleEngine().execute(script);
            } catch (Exception e) {
                trace(e);
            }
        }
    }

    @Override
    public Set<String> commandNames() {
        Set<String> out = new HashSet<>();
        for (CommandRegistry r : commandRegistries) {
            out.addAll(r.commandNames());
        }
        out.addAll(localCommandNames());
        return out;
    }

    private Set<String> localCommandNames() {
        return commandExecute.keySet();
    }

    @Override
    public Map<String, String> commandAliases() {
        Map<String, String> out = new HashMap<>();
        for (CommandRegistry r : commandRegistries) {
            out.putAll(r.commandAliases());
        }
        return out;
    }

    @Override
    public Object consoleOption(String name) {
        Object out = null;
        if (consoleId != null) {
            out = consoleEngine().consoleOption(name, null);
        }
        return out;
    }

    /**
     * Register subcommand registry
     * @param command main command
     * @param subcommandRegistry subcommand registry
     */
    @Override
    public void register(String command, CommandRegistry subcommandRegistry) {
        subcommands.put(command, subcommandRegistry);
        commandExecute.put(command, new CommandMethods(this::subcommand, this::emptyCompleter));
    }

    private List<String> localCommandInfo(String command) {
        try {
            if (subcommands.containsKey(command)) {
                registryHelp(subcommands.get(command));
            } else {
                localExecute(command, new String[] { "--help" });
            }
        } catch (HelpException e) {
            exception = null;
            return JlineCommandRegistry.compileCommandInfo(e.getMessage());
        } catch (Exception e) {
            trace(e);
        }
        return new ArrayList<>();
    }

    @Override
    public List<String> commandInfo(String command) {
        int id = registryId(command);
        List<String> out = new ArrayList<>();
        if (id > -1) {
            if (!commandInfos.containsKey(command)) {
                commandInfos.put(command, commandRegistries[id].commandInfo(command));
            }
            out = commandInfos.get(command);
        } else if (scriptStore.hasScript(command)) {
            out = consoleEngine().commandInfo(command);
        } else if (isLocalCommand(command)) {
            out = localCommandInfo(command);
        }
        return out;
    }

    @Override
    public boolean hasCommand(String command) {
        return registryId(command) > -1 || isLocalCommand(command);
    }

    private boolean isLocalCommand(String command) {
        return commandExecute.containsKey(command);
    }

    private boolean isCommandOrScript(String command) {
        if (hasCommand(command)) {
            return true;
        }
        return scriptStore.hasScript(command);
    }

    public void addCompleter(Completer completer) {
        if (completer instanceof SystemCompleter) {
            SystemCompleter sc = (SystemCompleter)completer;
            if (sc.isCompiled()) {
                customAggregateCompleter.getCompleters().add(sc);
            } else {
                customSystemCompleter.add(sc);
            }
        } else {
            customAggregateCompleter.getCompleters().add(completer);
        }
    }

    @Override
    public SystemCompleter compileCompleters() {
        throw new IllegalStateException("Use method completer() to retrieve Completer!");
    }

    private SystemCompleter _compileCompleters() {
        SystemCompleter out = CommandRegistry.aggregateCompleters(commandRegistries);
        SystemCompleter local = new SystemCompleter();
        for (String command : commandExecute.keySet()) {
            if (subcommands.containsKey(command)) {
                for(Map.Entry<String,List<Completer>> entry : subcommands.get(command).compileCompleters().getCompleters().entrySet()) {
                    for (Completer cc : entry.getValue()) {
                        if (!(cc instanceof ArgumentCompleter)) {
                            throw new IllegalArgumentException();
                        }
                        List<Completer> cmps = ((ArgumentCompleter)cc).getCompleters();
                        cmps.add(0, NullCompleter.INSTANCE);
                        cmps.set(1, new StringsCompleter(entry.getKey()));
                        Completer last = cmps.get(cmps.size() - 1);
                        if (last instanceof OptionCompleter) {
                            ((OptionCompleter)last).setStartPos(cmps.size() - 1);
                            cmps.set(cmps.size() - 1, last);
                        }
                        local.add(command, new ArgumentCompleter(cmps));
                    }
                }
            } else {
                local.add(command, commandExecute.get(command).compileCompleter().apply(command));
            }
        }
        local.add(customSystemCompleter);
        out.add(local);
        out.compile();
        return out;
    }

    @Override
    public Completer completer() {
        List<Completer> completers = new ArrayList<>();
        completers.add(_compileCompleters());
        completers.add(customAggregateCompleter);
        if (consoleId != null) {
            completers.addAll(consoleEngine().scriptCompleters());
            completers.add(new PipelineCompleter().doCompleter());
        }
        return new AggregateCompleter(completers);
    }

    private CmdDesc localCommandDescription(String command) {
        if (!isLocalCommand(command)) {
            throw new IllegalArgumentException();
        }
        try {
            localExecute(command, new String[] { "--help" });
        } catch (HelpException e) {
            exception = null;
            return JlineCommandRegistry.compileCommandDescription(e.getMessage());
        } catch (Exception e) {
            trace(e);
        }
        return null;
    }

    @Override
    public CmdDesc commandDescription(String command) {
        return commandDescription(Arrays.asList(command));
    }

    @Override
    public CmdDesc commandDescription(List<String> args) {
        CmdDesc out = new CmdDesc(false);
        String command = args.get(0);
        int id = registryId(command);
        if (id > -1) {
            out = commandRegistries[id].commandDescription(args);
        } else if (scriptStore.hasScript(command)) {
            out = consoleEngine().commandDescription(command);
        } else if (isLocalCommand(command)) {
            out = localCommandDescription(command);
        }
        return out;
    }

    private CmdDesc commandDescription(CommandRegistry subreg) {
        List<AttributedString> main = new ArrayList<>();
        Map<String, List<AttributedString>> options = new HashMap<>();
        StyleResolver helpStyle = Styles.helpStyle();
        for (String sc : new TreeSet<String>(subreg.commandNames())) {
            for (String info : subreg.commandInfo(sc)) {
                main.add(HelpException.highlightSyntax(sc + " -  " + info, helpStyle, true));
                break;
            }
        }
        return new CmdDesc(main, ArgDesc.doArgNames(Arrays.asList("")), options);
    }

    @Override
    public CmdDesc commandDescription(Widgets.CmdLine line) {
        CmdDesc out = null;
        switch (line.getDescriptionType()) {
        case COMMAND:
            String cmd = parser.getCommand(line.getArgs().get(0));
            if (isCommandOrScript(cmd) && !names.hasPipes(line.getArgs())) {
                List<String> args = line.getArgs();
                if (subcommands.containsKey(cmd)) {
                    String c = args.size() > 1 ? args.get(1) : null;
                    if (c == null || subcommands.get(cmd).hasCommand(c)) {
                        if (c != null && c.equals("help")) {
                            out = null;
                        } else if (c != null) {
                            out = subcommands.get(cmd).commandDescription(c);
                        } else {
                            out = commandDescription(subcommands.get(cmd));
                        }
                    } else {
                        out = commandDescription(subcommands.get(cmd));
                    }
                    if (out != null) {
                        out.setSubcommand(true);
                    }
                } else {
                    args.set(0, cmd);
                    out = commandDescription(args);
                }
            }
            break;
        case METHOD:
        case SYNTAX:
            // TODO
            break;
        }
        return out;
    }

    @Override
    public Object invoke(String command, Object... args) throws Exception {
        Object out = null;
        command = ConsoleEngine.plainCommand(command);
        args = args == null ? new Object[] {null} : args;
        int id = registryId(command);
        if (id > -1) {
            out = commandRegistries[id].invoke(commandSession(), command, args);
        } else if (isLocalCommand(command)) {
            out = localExecute(command, args);
        } else if (consoleId != null) {
            out = consoleEngine().invoke(commandSession(), command, args);
        }
        return out;
    }

    @Override
    public Object execute(String command, String[] args) throws Exception {
        Object out = null;
        int id = registryId(command);
        if (id > -1) {
            out = commandRegistries[id].execute(commandSession(), command, args);
        } else if (isLocalCommand(command)) {
            out = localExecute(command, args);
        }
        return out;
    }

    public Object localExecute(String command, Object[] args) throws Exception {
        if (!isLocalCommand(command)) {
            throw new IllegalArgumentException();
        }
        Object out = commandExecute.get(command).executeFunction()
                          .apply(new CommandInput(command, args, commandSession()));
        if (exception != null) {
            throw exception;
        }
        return out;
    }

    public Terminal terminal() {
        return commandSession().terminal();
    }

    private CommandSession commandSession() {
        return outputStream.getCommandSession();
    }

    private static class CommandOutputStream {
        private PrintStream origOut;
        private PrintStream origErr;
        private Terminal origTerminal;
        private ByteArrayOutputStream byteOutputStream;
        private FileOutputStream fileOutputStream;
        private PrintStream out;
        private InputStream in;
        private Terminal terminal;
        private String output;
        private CommandRegistry.CommandSession commandSession;
        private boolean redirecting = false;

        public CommandOutputStream(Terminal terminal) {
            this.origOut = System.out;
            this.origErr = System.err;
            this.origTerminal = terminal;
            this.terminal = terminal;
            PrintStream ps = new PrintStream(terminal.output());
            this.commandSession = new CommandRegistry.CommandSession(terminal, terminal.input(), ps, ps);
        }

        public void redirect() throws IOException {
            byteOutputStream = new ByteArrayOutputStream();
        }

        public void redirect(File file, boolean append) throws IOException {
            if (!file.exists()){
                try {
                    file.createNewFile();
                } catch(IOException e){
                    (new File(file.getParent())).mkdirs();
                    file.createNewFile();
                }
            }
            fileOutputStream = new FileOutputStream(file, append);
        }

        public void open() throws IOException {
            if (redirecting || (byteOutputStream == null && fileOutputStream == null)) {
                return;
            }
            OutputStream outputStream = byteOutputStream != null ? byteOutputStream : fileOutputStream;
            out = new PrintStream(outputStream);
            System.setOut(out);
            System.setErr(out);
            String input = ctrl('X') + "q";
            in = new ByteArrayInputStream( input.getBytes() );
            Attributes attrs = new Attributes();
            if (OSUtils.IS_WINDOWS) {
                attrs.setInputFlag(InputFlag.IGNCR, true);
            }
            terminal = TerminalBuilder.builder()
                                      .streams(in, outputStream)
                                      .attributes(attrs)
                                      .jna(false)
                                      .jansi(false)
                                      .type(Terminal.TYPE_DUMB).build();
            this.commandSession = new CommandRegistry.CommandSession(terminal, terminal.input(), out, out);
            redirecting = true;
        }

        public void flush() {
            if (out == null) {
                return;
            }
            try {
                out.flush();
                if (byteOutputStream != null) {
                    byteOutputStream.flush();
                    output = byteOutputStream.toString();
                } else if (fileOutputStream != null) {
                    fileOutputStream.flush();
                }
            } catch (Exception e) {

            }
        }

        public void close() {
            if (out == null) {
                return;
            }
            try {
                in.close();
                flush();
                if (byteOutputStream != null) {
                    byteOutputStream.close();
                    byteOutputStream = null;
                } else if (fileOutputStream != null) {
                    fileOutputStream.close();
                    fileOutputStream = null;
                }
                out.close();
                out = null;
            } catch (Exception e) {

            }
        }

        public CommandRegistry.CommandSession getCommandSession() {
            return commandSession;
        }

        public String getOutput() {
            return output;
        }

        public boolean isRedirecting() {
            return redirecting;
        }

        public boolean isByteStream() {
            return redirecting && byteOutputStream != null;
        }

        public void reset() {
            if (redirecting) {
                out = null;
                byteOutputStream = null;
                fileOutputStream = null;
                output = null;
                System.setOut(origOut);
                System.setErr(origErr);
                terminal = null;
                terminal = origTerminal;
                PrintStream ps = new PrintStream(terminal.output());
                this.commandSession = new CommandRegistry.CommandSession(terminal, terminal.input(), ps, ps);
                redirecting = false;
            }
        }

        public void closeAndReset() {
            close();
            reset();
        }
    }

    private boolean isCommandAlias(String command) {
        if (consoleId == null || !parser.validCommandName(command) || !consoleEngine().hasAlias(command)) {
            return false;
        }
        String value = consoleEngine().getAlias(command).split("\\s+")[0];
        return !names.isPipe(value);
    }

    private String replaceCommandAlias(String variable, String command, String rawLine) {
        return variable == null ? rawLine.replaceFirst(command + "(\\b|$)", consoleEngine().getAlias(command))
                                : rawLine.replaceFirst("=" + command + "(\\b|$)", "=" + consoleEngine().getAlias(command));
    }

    private List<CommandData> compileCommandLine(String commandLine) {
        List<CommandData> out = new ArrayList<>();
        ArgsParser ap = new ArgsParser(parser);
        ap.parse(commandLine);
        //
        // manage pipe aliases
        //
        List<String> ws = ap.args();
        Map<String,List<String>> customPipes = consoleId != null ? consoleEngine().getPipes() : new HashMap<>();
        if (consoleId != null && ws.contains(pipeName.get(Pipe.NAMED))) {
            StringBuilder sb = new StringBuilder();
            boolean trace = false;
            for (int i = 0 ; i < ws.size(); i++) {
                if (ws.get(i).equals(pipeName.get(Pipe.NAMED))) {
                    if (i + 1 < ws.size() && consoleEngine().hasAlias(ws.get(i + 1))) {
                        trace = true;
                        List<String> args = new ArrayList<>();
                        String pipeAlias = consoleEngine().getAlias(ws.get(++i));
                        while (i < ws.size() - 1 && !names.isPipe(ws.get(i + 1), customPipes.keySet())) {
                            args.add(ws.get(++i));
                        }
                        for (int j = 0; j < args.size(); j++) {
                            pipeAlias = pipeAlias.replaceAll("\\s\\$" + j + "\\b", " " + args.get(j));
                            pipeAlias = pipeAlias.replaceAll("\\$\\{" + j + "(|:-.*)\\}", args.get(j));
                        }
                        pipeAlias = pipeAlias.replaceAll("\\$\\{@\\}", consoleEngine().expandToList(args));
                        pipeAlias = pipeAlias.replaceAll("\\$@", consoleEngine().expandToList(args));
                        pipeAlias = pipeAlias.replaceAll("\\s+\\$\\d\\b", "");
                        pipeAlias = pipeAlias.replaceAll("\\s+\\$\\{\\d+\\}", "");
                        pipeAlias = pipeAlias.replaceAll("\\$\\{\\d+\\}", "");
                        Matcher matcher = Pattern.compile("\\$\\{\\d+:-(.*?)\\}").matcher(pipeAlias);
                        if (matcher.find()) {
                            pipeAlias = matcher.replaceAll("$1");
                        }
                        sb.append(pipeAlias);
                    } else {
                        sb.append(ws.get(i));
                    }
                } else {
                    sb.append(ws.get(i));
                }
                sb.append(" ");
            }
            ap.parse(sb.toString());
            if (trace) {
                consoleEngine().trace(ap.line());
            }
        }
        List<String> words = ap.args();
        String nextRawLine = ap.line();
        int first = 0;
        int last = words.size();
        List<String> pipes = new ArrayList<>();
        String pipeSource = null;
        String rawLine = null;
        String pipeResult = null;
        if (!names.hasPipes(words)) {
            if (isCommandAlias(ap.command())) {
                nextRawLine = replaceCommandAlias(ap.variable(), ap.command(), nextRawLine);
            }
            out.add(new CommandData(ap, false, nextRawLine, ap.variable(), null, false,""));
        } else {
            //
            // compile pipe line
            //
            do {
                String rawCommand = parser.getCommand(words.get(first));
                String command = ConsoleEngine.plainCommand(rawCommand);
                String variable = parser.getVariable(words.get(first));
                if (isCommandAlias(command)) {
                    ap.parse(replaceCommandAlias(variable, command, nextRawLine));
                    rawCommand = ap.rawCommand();
                    command = ap.command();
                    words = ap.args();
                    first = 0;
                }
                if (scriptStore.isConsoleScript(command) && !rawCommand.startsWith(":") ) {
                    throw new IllegalArgumentException("Commands must be used in pipes with colon prefix!");
                }
                last = words.size();
                File file = null;
                boolean append = false;
                boolean pipeStart = false;
                boolean skipPipe = false;
                List<String> _words = new ArrayList<>();
                //
                // find next pipe
                //
                for (int i = first; i < last; i++) {
                    if (words.get(i).equals(">") || words.get(i).equals(">>")) {
                        pipes.add(words.get(i));
                        append = words.get(i).equals(">>");
                        if (i + 1 >= last) {
                            throw new IllegalArgumentException();
                        }
                        file = redirectFile(words.get(i + 1));
                        last = i + 1;
                        break;
                    } else if (words.get(i).equals(pipeName.get(Pipe.FLIP))) {
                        if (variable != null || file != null || pipeResult != null || consoleId == null) {
                            throw new IllegalArgumentException();
                        }
                        pipes.add(words.get(i));
                        last = i;
                        variable = "_pipe" + (pipes.size() - 1);
                        break;
                    } else if (words.get(i).equals(pipeName.get(Pipe.NAMED))
                            || (words.get(i).matches("^.*[^a-zA-Z0-9 ].*$") && customPipes.containsKey(words.get(i)))) {
                        String pipe = words.get(i);
                        if (pipe.equals(pipeName.get(Pipe.NAMED))) {
                            if (i + 1 >= last) {
                                throw new IllegalArgumentException("Pipe is NULL!");
                            }
                            pipe = words.get(i + 1);
                            if (!pipe.matches("\\w+") || !customPipes.containsKey(pipe)) {
                                throw new IllegalArgumentException("Unknown or illegal pipe name: " + pipe);
                            }
                        }
                        pipes.add(pipe);
                        last = i;
                        if (pipeSource == null) {
                            pipeSource = "_pipe" + (pipes.size() - 1);
                            pipeResult = variable;
                            variable = pipeSource;
                            pipeStart = true;
                        }
                        break;
                    } else if (words.get(i).equals(pipeName.get(Pipe.OR))
                            || words.get(i).equals(pipeName.get(Pipe.AND))) {
                        if (variable != null || pipeSource != null) {
                            pipes.add(words.get(i));
                        } else if (pipes.size() > 0 && (pipes.get(pipes.size() - 1).equals(">")
                                || pipes.get(pipes.size() - 1).equals(">>"))) {
                            pipes.remove(pipes.size() - 1);
                            out.get(out.size() - 1).setPipe(words.get(i));
                            skipPipe = true;
                        } else {
                            pipes.add(words.get(i));
                            pipeSource = "_pipe" + (pipes.size() - 1);
                            pipeResult = variable;
                            variable = pipeSource;
                            pipeStart = true;
                        }
                        last = i;
                        break;
                    } else {
                        _words.add(words.get(i));
                    }
                }
                if (last == words.size()) {
                    pipes.add("END_PIPE");
                } else if (skipPipe) {
                    first = last + 1;
                    continue;
                }
                //
                // compose pipe command
                //
                String subLine = last < words.size() || first > 0 ? _words.stream().collect(Collectors.joining(" "))
                        : ap.line();
                if (last + 1 < words.size()) {
                    nextRawLine = words.subList(last + 1, words.size()).stream().collect(Collectors.joining(" "));
                }
                boolean done = true;
                boolean statement = false;
                List<String> arglist = new ArrayList<>();
                if (_words.size() > 0) {
                    arglist.addAll(_words.subList(1, _words.size()));
                }
                if (rawLine != null || (pipes.size() > 1 && customPipes.containsKey(pipes.get(pipes.size() - 2)))) {
                    done = false;
                    if (rawLine == null) {
                        rawLine = pipeSource;
                    }
                    if (customPipes.containsKey(pipes.get(pipes.size() - 2))) {
                        List<String> fixes = customPipes.get(pipes.get(pipes.size() - 2));
                        if (pipes.get(pipes.size() - 2).matches("\\w+")) {
                            int idx = subLine.indexOf(" ");
                            subLine = idx > 0 ? subLine.substring(idx + 1) : "";
                        }
                        rawLine += fixes.get(0)
                                     + (consoleId != null ? consoleEngine().expandCommandLine(subLine) : subLine)
                                 + fixes.get(1);
                        statement = true;
                    }
                    if (pipes.get(pipes.size() - 1).equals(pipeName.get(Pipe.FLIP))
                            || pipes.get(pipes.size() - 1).equals(pipeName.get(Pipe.AND))
                            || pipes.get(pipes.size() - 1).equals(pipeName.get(Pipe.OR))) {
                        done = true;
                        pipeSource = null;
                        if (variable != null) {
                            rawLine = variable + " = " + rawLine;
                        }
                    }
                    if (last + 1 >= words.size() || file != null) {
                        done = true;
                        pipeSource = null;
                        if (pipeResult != null) {
                            rawLine = pipeResult + " = " + rawLine;
                        }
                    }
                } else if (pipes.get(pipes.size() - 1).equals(pipeName.get(Pipe.FLIP)) || pipeStart) {
                    if (pipeStart && pipeResult != null) {
                        subLine = subLine.substring(subLine.indexOf("=") + 1);
                    }
                    rawLine = flipArgument(command, subLine, pipes, arglist);
                    rawLine = variable + "=" + rawLine;
                } else {
                    rawLine = flipArgument(command, subLine, pipes, arglist);
                }
                if (done) {
                    //
                    // add composed command to return list
                    //
                    out.add(new CommandData(ap, statement, rawLine, variable, file, append, pipes.get(pipes.size() - 1)));
                    if (pipes.get(pipes.size() - 1).equals(pipeName.get(Pipe.AND))
                            || pipes.get(pipes.size() - 1).equals(pipeName.get(Pipe.OR))) {
                        pipeSource = null;
                        pipeResult = null;
                    }
                    rawLine = null;
                }
                first = last + 1;
            } while (first < words.size());
        }
        return out;
    }

    private File redirectFile(String name) {
        File out = null;
        if (name.equals("null")) {
            out = OSUtils.IS_WINDOWS ? new File("NUL") : new File("/dev/null");
        } else {
            out = new File(name);
        }
        return out;
    }

    private static class ArgsParser {
        private int round = 0;
        private int curly = 0;
        private int square = 0;
        private boolean quoted;
        private boolean doubleQuoted;
        private String line;
        private String command;
        private String variable;
        private List<String> args;
        private Parser parser;

        public ArgsParser(Parser parser) {
            this.parser = parser;
        }

        private void reset() {
            round = 0;
            curly = 0;
            square = 0;
            quoted = false;
            doubleQuoted = false;
        }

        private void next(String arg) {
            char prevChar = ' ';
            for (int i = 0; i < arg.length(); i++) {
                char c = arg.charAt(i);
                if (!parser.isEscapeChar(prevChar)) {
                    if (!quoted && !doubleQuoted) {
                        if (c == '(') {
                            round++;
                        } else if (c == ')') {
                            round--;
                        } else if (c == '{') {
                            curly++;
                        } else if (c == '}') {
                            curly--;
                        } else if (c == '[') {
                            square++;
                        } else if (c == ']') {
                            square--;
                        } else if (c == '"') {
                            doubleQuoted = true;
                        } else if (c == '\'') {
                            quoted = true;
                        }
                    } else if (quoted && c == '\'') {
                        quoted = false;
                    } else if (doubleQuoted && c == '"') {
                        doubleQuoted = false;
                    }
                }
                prevChar = c;
            }
        }

        private boolean isEnclosed() {
            return round == 0 && curly == 0 && square == 0 && !quoted && !doubleQuoted;
        }

        private void enclosedArgs(List<String> words) {
            args = new ArrayList<>();
            reset();
            boolean first = true;
            StringBuilder sb = new StringBuilder();
            for (String a : words) {
                next(a);
                if (!first) {
                    sb.append(" ");
                }
                if (isEnclosed()) {
                    sb.append(a);
                    args.add(sb.toString());
                    sb = new StringBuilder();
                    first = true;
                } else {
                    sb.append(a);
                    first = false;
                }
            }
            if (!first) {
                args.add(sb.toString());
            }
        }

        public void parse(String line) {
            this.line = line;
            ParsedLine pl = parser.parse(line, 0, ParseContext.SPLIT_LINE);
            enclosedArgs(pl.words());
            this.command = parser.getCommand(args.get(0));
            if (!parser.validCommandName(command)) {
                this.command = "";
            }
            this.variable = parser.getVariable(args.get(0));
        }

        public String line() {
            return line;
        }

        public String command() {
            return ConsoleEngine.plainCommand(command);
        }

        public String rawCommand() {
            return command;
        }

        public String variable() {
            return variable;
        }

        public List<String> args() {
            return args;
        }

        private int closingQuote(String arg) {
            int out = -1;
            char prevChar = ' ';
            for (int i = 1; i < arg.length(); i++) {
                char c = arg.charAt(i);
                if (!parser.isEscapeChar(prevChar)) {
                    if (c == arg.charAt(0)) {
                        out = i;
                        break;
                    }
                }
                prevChar = c;
            }
            return out;
        }

        private String unquote(String arg) {
            if (arg.length() > 1 && (arg.startsWith("\"") && arg.endsWith("\""))
                    || (arg.startsWith("'") && arg.endsWith("'"))) {
                if (closingQuote(arg) == arg.length() - 1) {
                    return arg.substring(1, arg.length() -1);
                }
            }
            return arg;
        }

    }

    private String flipArgument(final String command, final String subLine, final List<String> pipes, List<String> arglist) {
        String out = null;
        if (pipes.size() > 1 && pipes.get(pipes.size() - 2).equals(pipeName.get(Pipe.FLIP))) {
            String s = isCommandOrScript(command) ? "$" : "";
            out = subLine + " " + s + "_pipe" + (pipes.size() - 2);
            if (!command.isEmpty()) {
                arglist.add(s + "_pipe" + (pipes.size() - 2));
            }
        } else {
            out = subLine;
        }
        return out;
    }

    protected static class CommandData {
        private String rawLine;
        private String command;
        private String[] args;
        private File file;
        private boolean append;
        private String variable;
        private String pipe;

        public CommandData(ArgsParser parser, boolean statement, String rawLine, String variable, File file, boolean append, String pipe) {
            this.rawLine = rawLine;
            this.variable = variable;
            this.file = file;
            this.append = append;
            this.pipe = pipe;
            this.args = new String[] {};
            this.command = "";
            if (!statement) {
                parser.parse(rawLine);
                this.command = parser.command();
                if (parser.args().size() > 1) {
                    this.args = new String[parser.args().size() - 1];
                    for (int i = 1; i < parser.args().size(); i++) {
                        args[i - 1] = parser.unquote(parser.args().get(i));
                    }
                }
            }
        }

        public void setPipe(String pipe) {
            this.pipe = pipe;
        }

        public File file() {
            return file;
        }

        public boolean append() {
            return append;
        }

        public String variable() {
            return variable;
        }

        public String command() {
            return command;
        }

        public String[] args() {
            return args;
        }

        public String rawLine() {
            return rawLine;
        }

        public String pipe() {
            return pipe;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            sb.append("rawLine:").append(rawLine);
            sb.append(", ");
            sb.append("command:").append(command);
            sb.append(", ");
            sb.append("args:").append(Arrays.asList(args));
            sb.append(", ");
            sb.append("variable:").append(variable);
            sb.append(", ");
            sb.append("file:").append(file);
            sb.append(", ");
            sb.append("append:").append(append);
            sb.append(", ");
            sb.append("pipe:").append(pipe);
            sb.append("]");
            return sb.toString();
        }

    }

    private static class ScriptStore {
        ConsoleEngine engine;
        Map<String, Boolean> scripts = new HashMap<>();

        public ScriptStore() {}

        public ScriptStore(ConsoleEngine engine) {
            this.engine = engine;
        }

        public void refresh() {
            if (engine != null) {
                scripts = engine.scripts();
            }
        }

        public boolean hasScript(String name) {
            return scripts.containsKey(name);
        }

        public boolean isConsoleScript(String name) {
            return scripts.getOrDefault(name, false);
        }

        public Set<String> getScripts() {
            return scripts.keySet();
        }

    }

    @SuppressWarnings("serial")
    private static class UnknownCommandException extends Exception {

    }

    private Object execute(String command, String rawLine, String[] args) throws Exception {
        if (!parser.validCommandName(command)) {
            throw new UnknownCommandException();
        }
        Object out = null;
        if (isLocalCommand(command)) {
            out = localExecute(command, consoleId != null ? consoleEngine().expandParameters(args) : args);
        } else {
            int id = registryId(command);
            if (id > -1) {
                Object[] _args = consoleId != null ? consoleEngine().expandParameters(args) : (Object[])args;
                out = commandRegistries[id].invoke(outputStream.getCommandSession(), command, _args);
            } else if (scriptStore.hasScript(command)) {
                out = consoleEngine().execute(command, rawLine, args);
            } else {
                throw new UnknownCommandException();
            }
        }
        return out;
    }

    @Override
    public Object execute(String line) throws Exception {
        if (line.isEmpty() || line.trim().startsWith("#")) {
            return null;
        }
        long start = new Date().getTime();
        Object out = null;
        boolean statement = false;
        boolean postProcessed = false;
        int errorCount = 0;
        scriptStore.refresh();
        List<CommandData> cmds = compileCommandLine(line);
        for (int i = 0; i < cmds.size(); i++) {
            CommandData cmd = cmds.get(i);
            if (cmd.file() != null && scriptStore.isConsoleScript(cmd.command())) {
                throw new IllegalArgumentException("Console script output cannot be redirected!");
            }
            try {
                outputStream.closeAndReset();
                if (consoleId != null && !consoleEngine().isExecuting()) {
                    trace(cmd);
                }
                exception = null;
                statement = false;
                postProcessed = false;
                if (cmd.variable() != null || cmd.file() != null) {
                    if (cmd.file() != null) {
                        outputStream.redirect(cmd.file(), cmd.append());
                    } else if (consoleId != null) {
                        outputStream.redirect();
                    }
                    outputStream.open();
                }
                boolean consoleScript = false;
                try {
                    out = execute(cmd.command(), cmd.rawLine(), cmd.args());
                } catch (UnknownCommandException e) {
                    consoleScript = true;
                }
                if (consoleId != null) {
                    if (consoleScript) {
                        statement = cmd.command().isEmpty() || !scriptStore.hasScript(cmd.command());
                        if (statement && outputStream.isByteStream()) {
                            outputStream.closeAndReset();
                        }
                        out = consoleEngine().execute(cmd.command(), cmd.rawLine(), cmd.args());
                    }
                    if (cmd.pipe().equals(pipeName.get(Pipe.OR)) || cmd.pipe().equals(pipeName.get(Pipe.AND))) {
                        ExecutionResult er = postProcess(cmd, statement, out);
                        postProcessed = true;
                        consoleEngine().println(er.result());
                        out = null;
                        boolean success = er.status() == 0 ? true : false;
                        if ((cmd.pipe().equals(pipeName.get(Pipe.OR)) && success)
                                || (cmd.pipe().equals(pipeName.get(Pipe.AND)) && !success)) {
                            break;
                        }
                    }
                }
            } catch (HelpException e) {
                trace(e);
            } catch (Exception e) {
                errorCount++;
                if (cmd.pipe().equals(pipeName.get(Pipe.OR))) {
                    trace(e);
                    postProcessed = true;
                } else {
                    throw e;
                }
            } finally {
                if (!postProcessed && consoleId != null) {
                    out = postProcess(cmd, statement, out).result();
                }
            }
        }
        if (errorCount == 0) {
            names.extractNames(line);
        }
        Log.debug("execute: ", new Date().getTime() - start, " msec");
        return out;
    }

    private ExecutionResult postProcess(CommandData cmd, boolean statement, Object result) {
        ExecutionResult out = new ExecutionResult(result != null ? 0 : 1, result);
        if (cmd.file() != null) {
            int status = 1;
            if (cmd.file().exists()) {
                long delta = new Date().getTime() - cmd.file().lastModified();
                status = delta < 100 ? 0 : 1;
            }
            out = new ExecutionResult(status, result);
        } else if (!statement) {
            outputStream.flush();
            outputStream.close();
            out = consoleEngine().postProcess(cmd.rawLine(), result, outputStream.getOutput());
            outputStream.reset();
        } else if (cmd.variable() != null) {
            if (consoleEngine().hasVariable(cmd.variable())) {
                out = consoleEngine().postProcess(consoleEngine().getVariable(cmd.variable()));
            } else {
                out = consoleEngine().postProcess(result);
            }
            if (!cmd.variable().startsWith("_")) {
                out = new ExecutionResult(out.status(), null);
            }
        } else {
            out = consoleEngine().postProcess(result);
        }
        return out;
    }

    public void cleanUp() {
        if (outputStream.isRedirecting()) {
            outputStream.closeAndReset();
        }
        if (consoleId != null) {
            consoleEngine().purge();
        }
    }

    private void trace(CommandData commandData) {
        if (consoleId != null) {
            consoleEngine().trace(commandData);
        } else {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.append(commandData.rawLine(), AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)).println(terminal());
        }
    }

    @Override
    public void trace(Exception exception) {
        if (outputStream.isRedirecting()) {
            outputStream.closeAndReset();
        }
        if (consoleId != null) {
            consoleEngine().putVariable("exception", exception);
            consoleEngine().trace(exception);
        } else {
            trace(false, exception);
        }
    }

    @Override
    public void trace(boolean stack, Exception exception) {
        if (exception instanceof Options.HelpException) {
            Options.HelpException.highlight((exception).getMessage(), Styles.helpStyle()).print(terminal());
        } else if (stack) {
            exception.printStackTrace();
        } else {
            String message = exception.getMessage();
            AttributedStringBuilder asb = new AttributedStringBuilder();
            if (message != null) {
                String m = exception.getClass().getSimpleName() + ": " + message;
                asb.append(m, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
            } else {
                asb.append("Caught exception: ", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
                asb.append(exception.getClass().getCanonicalName(), AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
            }
            asb.toAttributedString().println(terminal());
            Log.debug("Stack: ", exception);
        }
    }

    @Override
    public void close() {
        names.save();
    }

    private ConsoleEngine consoleEngine() {
        return consoleId != null ? (ConsoleEngine) commandRegistries[consoleId] : null;
    }

    private boolean isBuiltinRegistry(CommandRegistry registry) {
        for (Class<?> c : BUILTIN_REGISTERIES) {
            if (c == registry.getClass()) {
                return true;
            }
        }
        return false;
    }

    private void printHeader(String header) {
        AttributedStringBuilder asb = new AttributedStringBuilder().tabs(2);
        asb.append("\t");
        asb.append(header, HelpException.defaultStyle().resolve(".ti"));
        asb.append(":");
        asb.toAttributedString().println(terminal());
    }

    private void printCommandInfo(String command, String info, int max) {
        AttributedStringBuilder asb = new AttributedStringBuilder().tabs(Arrays.asList(4, max + 4));
        asb.append("\t");
        asb.append(command, HelpException.defaultStyle().resolve(".co"));
        asb.append("\t");
        asb.append(info);
        asb.setLength(terminal().getWidth());
        asb.toAttributedString().println(terminal());
    }

    private void printCommands(Collection<String> commands, int max) {
        AttributedStringBuilder asb = new AttributedStringBuilder().tabs(Arrays.asList(4, max + 4));
        int col = 0;
        asb.append("\t");
        col += 4;
        boolean done = false;
        for (String c : commands) {
            asb.append(c, HelpException.defaultStyle().resolve(".co"));
            asb.append("\t");
            col += max;
            if (col + max > terminal().getWidth()) {
                asb.toAttributedString().println(terminal());
                asb = new AttributedStringBuilder().tabs(Arrays.asList(4, max + 4));
                col = 0;
                asb.append("\t");
                col += 4;
                done = true;
            } else {
                done = false;
            }
        }
        if (!done) {
            asb.toAttributedString().println(terminal());
        }
        terminal().flush();
    }

    private String doCommandInfo(List<String> info) {
        return info.size() > 0 ? info.get(0) : " ";
    }

    private boolean isInTopics(List<String> args, String name) {
        return args.isEmpty() || args.contains(name);
    }

    private Options parseOptions(String[] usage, Object[] args) throws HelpException {
        Options opt = Options.compile(usage).parse(args);
        if (opt.isSet("help")) {
            throw new HelpException(opt.usage());
        }
        return opt;
    }

    private Object help(CommandInput input) {
        final String[] usage = { "help -  command help"
                               , "Usage: help [TOPIC...]",
                                 "  -? --help                       Displays command help", };
        try {
            Options opt = parseOptions(usage, input.args());
            boolean doTopic = false;
            if (!opt.args().isEmpty() && opt.args().size() == 1) {
                try {
                    String[] args = {"--help"};
                    String command = opt.args().get(0);
                    execute(command, command + " " + args[0], args);
                } catch (UnknownCommandException e) {
                    doTopic = true;
                } catch (Exception e) {
                    exception = e;
                }
            } else {
                doTopic = true;
            }
            if (doTopic) {
                helpTopic(opt.args());
            }
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    private void helpTopic(List<String> topics) {
        Set<String> commands = commandNames();
        commands.addAll(scriptStore.getScripts());
        boolean withInfo = commands.size() < terminal().getHeight() || !topics.isEmpty() ? true : false;
        int max = Collections.max(commands, Comparator.comparing(String::length)).length() + 1;
        TreeMap<String, String> builtinCommands = new TreeMap<>();
        TreeMap<String, String> systemCommands = new TreeMap<>();
        for (CommandRegistry r : commandRegistries) {
            if (isBuiltinRegistry(r)) {
                for (String c : r.commandNames()) {
                    builtinCommands.put(c, doCommandInfo(commandInfo(c)));
                }
            }
        }
        for (String c : localCommandNames()) {
            systemCommands.put(c, doCommandInfo(commandInfo(c)));
            exception = null;
        }
        if (isInTopics(topics, "System")) {
            printHeader("System");
            if (withInfo) {
                for (Map.Entry<String, String> entry : systemCommands.entrySet()) {
                    printCommandInfo(entry.getKey(), entry.getValue(), max);
                }
            } else {
                printCommands(systemCommands.keySet(), max);
            }
        }
        if (isInTopics(topics, "Builtins")) {
            printHeader("Builtins");
            if (withInfo) {
                for (Map.Entry<String, String> entry : builtinCommands.entrySet()) {
                    printCommandInfo(entry.getKey(), entry.getValue(), max);
                }
            } else {
                printCommands(builtinCommands.keySet(), max);
            }
        }
        for (CommandRegistry r : commandRegistries) {
            if (isBuiltinRegistry(r) || !isInTopics(topics, r.name())) {
                continue;
            }
            TreeSet<String> cmds = new TreeSet<>(r.commandNames());
            printHeader(r.name());
            if (withInfo) {
                for (String c : cmds) {
                    printCommandInfo(c, doCommandInfo(commandInfo(c)), max);
                }
            } else {
                printCommands(cmds, max);
            }
        }
        if (consoleId != null && isInTopics(topics, "Scripts")) {
            printHeader("Scripts");
            if (withInfo) {
                for (String c : scriptStore.getScripts()) {
                    printCommandInfo(c, doCommandInfo(commandInfo(c)), max);
                }
            } else {
                printCommands(scriptStore.getScripts(), max);
            }
        }
        terminal().flush();
    }

    private Object exit(CommandInput input) {
        final String[] usage = { "exit -  exit from app/script"
                               , "Usage: exit [OBJECT]"
                               , "  -? --help                       Displays command help"
                          };

        try {
            Options opt = parseOptions(usage, input.args());
            if (!opt.args().isEmpty() && consoleId != null) {
                try {
                    Object[] ret = consoleEngine().expandParameters(opt.args().toArray(new String[0]));
                    consoleEngine().putVariable("_return", ret.length == 1 ? ret[0] : ret);
                } catch (Exception e) {
                    trace(e);
                }
            }
            exception = new EndOfFileException();
        } catch (Exception e) {
            exception = e;
        }
        return null;
    }

    private void registryHelp(CommandRegistry registry) throws Exception {
        List<Integer> tabs = new ArrayList<>();
        tabs.add(0);
        tabs.add(9);
        int max = registry.commandNames().stream().map(String::length).max(Integer::compareTo).get();
        tabs.add(10 + max);
        AttributedStringBuilder sb = new AttributedStringBuilder().tabs(tabs);
        sb.append(" -  ");
        sb.append(registry.name());
        sb.append(" registry");
        sb.append("\n");
        boolean first = true;
        for (String c : new TreeSet<String>(registry.commandNames())) {
            if (first) {
                sb.append("Summary:");
                first = false;
            }
            sb.append("\t");
            sb.append(c);
            sb.append("\t");
            sb.append(registry.commandInfo(c).get(0));
            sb.append("\n");
        }
        throw new HelpException(sb.toString());
    }

    private Object subcommand(CommandInput input) {
        Object out = null;
        try {
            if (input.args().length > 0 && subcommands.get(input.command()).hasCommand(input.args()[0])) {
                out = subcommands.get(input.command()).invoke(input.session()
                                         , input.args()[0]
                                         , input.xargs().length > 1 ? Arrays.copyOfRange(input.xargs(), 1, input.xargs().length)
                                                                    : new Object[] {});
            } else {
                registryHelp(subcommands.get(input.command()));
            }
        } catch (Exception e) {
            exception = e;
        }
        return out;
    }

    private List<OptDesc> commandOptions(String command) {
        try {
            localExecute(command, new String[] { "--help" });
        } catch (HelpException e) {
            exception = null;
            return JlineCommandRegistry.compileCommandOptions(e.getMessage());
        } catch (Exception e) {
            trace(e);
        }
        return null;
    }

    private List<String> registryNames() {
        List<String> out = new ArrayList<>();
        out.add("System");
        out.add("Builtins");
        if (consoleId != null) {
            out.add("Scripts");
        }
        for (CommandRegistry r : commandRegistries) {
            if (!isBuiltinRegistry(r)) {
                out.add(r.name());
            }
        }
        out.addAll(commandNames());
        out.addAll(scriptStore.getScripts());
        return out;
    }

    private List<Completer> emptyCompleter(String command) {
        return new ArrayList<>();
    }

    private List<Completer> helpCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        List<Completer> params = new ArrayList<>();
        params.add(new StringsCompleter(this::registryNames));
        params.add(NullCompleter.INSTANCE);
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE,
                new OptionCompleter(params, this::commandOptions, 1)));
        return completers;
    }

    private List<Completer> exitCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE,
                new OptionCompleter(NullCompleter.INSTANCE, this::commandOptions, 1)));
        return completers;
    }

    private int registryId(String command) {
        for (int i = 0; i < commandRegistries.length; i++) {
            if (commandRegistries[i].hasCommand(command)) {
                return i;
            }
        }
        return -1;
    }

    private class PipelineCompleter implements Completer {

        public PipelineCompleter() {}

        public Completer doCompleter() {
            ArgumentCompleter out = new ArgumentCompleter(this);
            out.setStrict(false);
            return out;
        }

        @Override
        public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
            assert commandLine != null;
            assert candidates != null;
            if (commandLine.wordIndex() < 2 || !names.hasPipes(commandLine.words())) {
                return;
            }
            String pWord = commandLine.words().get(commandLine.wordIndex() - 1);
            if (pWord.equals(pipeName.get(Pipe.NAMED))) {
                for (String name : names.namedPipes()) {
                    candidates.add(new Candidate(name, name, null, null, null, null, true));
                }
            } else if (pWord.equals(">") || pWord.equals(">>")) {
                Completer c = new FilesCompleter(workDir);
                c.complete(reader, commandLine, candidates);
            } else {
                String buffer = commandLine.word().substring(0, commandLine.wordCursor());
                String param = buffer;
                String curBuf = "";
                int lastDelim = names.indexOfLastDelim(buffer);
                if (lastDelim > - 1) {
                    param = buffer.substring(lastDelim + 1);
                    curBuf = buffer.substring(0, lastDelim + 1);
                }
                if (curBuf.startsWith("--") && !curBuf.contains("=")) {
                    doCandidates(candidates, names.options(), curBuf, "", param);
                } else if (param.length() == 0) {
                    doCandidates(candidates, names.fieldsAndValues(), curBuf, "", "");
                } else if (param.contains(".")) {
                    int point = buffer.lastIndexOf(".");
                    param = buffer.substring(point + 1);
                    curBuf = buffer.substring(0, point + 1);
                    doCandidates(candidates, names.fields(), curBuf, "", param);
                } else if (names.encloseBy(param).length() == 1) {
                    lastDelim++;
                    String postFix = names.encloseBy(param);
                    param = buffer.substring(lastDelim + 1);
                    curBuf = buffer.substring(0, lastDelim + 1);
                    doCandidates(candidates, names.quoted(), curBuf, postFix, param);
                } else {
                    doCandidates(candidates, names.fieldsAndValues(), curBuf, "", param);
                }

            }
        }

        private void doCandidates(List<Candidate> candidates
                                , Collection<String> fields, String curBuf, String postFix, String hint) {
            if (fields == null) {
                return;
            }
            for (String s : fields) {
                if (s != null && s.startsWith(hint)) {
                    candidates.add(new Candidate(AttributedString.stripAnsi(curBuf + s + postFix), s, null, null, null,
                            null, false));
                }
            }
        }

    }

    private class NamesAndValues {
        private final String[] delims = {"&", "\\|", "\\{", "\\}", "\\[", "\\]", "\\(", "\\)"
                , "\\+", "-", "\\*", "=", ">", "<", "~", "!", ":", ",", ";"};

        private Path fileNames;
        private Map<String,List<String>> names = new HashMap<>();
        private List<String> namedPipes;

        public NamesAndValues() {}

        @SuppressWarnings("unchecked")
        public NamesAndValues(ConfigurationPath configPath) {
            names.put("fields", new ArrayList<>());
            names.put("values", new ArrayList<>());
            names.put("quoted", new ArrayList<>());
            names.put("options", new ArrayList<>());
            if (configPath != null) {
                try {
                    fileNames = configPath.getUserConfig("pipeline-names.json", true);
                    Map<String,List<String>> temp = (Map<String,List<String>>)consoleEngine().slurp(fileNames);
                    for (Entry<String, List<String>> entry : temp.entrySet()) {
                        names.get(entry.getKey()).addAll((Collection<? extends String>)entry.getValue());
                    }
                } catch (Exception e) {
                }
            }
        }

        public boolean isPipe(String arg) {
            Map<String,List<String>> customPipes = consoleId != null ? consoleEngine().getPipes() : new HashMap<>();
            return isPipe(arg, customPipes.keySet());
        }

        public boolean hasPipes(Collection<String> args) {
            Map<String,List<String>> customPipes = consoleId != null ? consoleEngine().getPipes() : new HashMap<>();
            for (String a : args) {
                if (isPipe(a, customPipes.keySet()) || a.contains(">") || a.contains(">>")) {
                    return true;
                }
            }
            return false;
        }

        private boolean isPipe(String arg, Set<String> pipes) {
            return pipeName.containsValue(arg) || pipes.contains(arg);
        }

        public void extractNames(String line) {
            if (parser.getCommand(line).equals("pipe")) {
                return;
            }
            ArgsParser ap = new ArgsParser(parser);
            ap.parse(line);
            List<String> args = ap.args();
            int pipeId = 0;
            for (String a : args) {
                if (isPipe(a)) {
                    break;
                }
                pipeId++;
            }
            if (pipeId  < args.size()) {
                StringBuilder sb = new StringBuilder();
                int redirectPipe = -1;
                for (int i = pipeId + 1; i < args.size(); i++) {
                    String arg = args.get(i);
                    if (!isPipe(arg) && !namedPipes().contains(arg)
                            && !arg.matches("\\d+") && redirectPipe != i - 1) {
                        if (arg.equals(">") || arg.equals(">>")) {
                            redirectPipe = i;
                        } else if (arg.matches("\\w+(\\(\\)){0,1}")) {
                            addValues(arg);
                        } else if (arg.matches("--\\w+(=.*|)$") && arg.length() > 4) {
                            int idx = arg.indexOf('=');
                            if (idx > 0) {
                                if (idx > 4) {
                                    addOptions(arg.substring(2, idx));
                                }
                                sb.append(arg.substring(idx + 1));
                                sb.append(" ");
                            } else if (idx == -1) {
                                addOptions(arg.substring(2));
                            }
                        } else {
                            sb.append(arg);
                            sb.append(" ");
                        }
                    } else {
                        redirectPipe = -1;
                    }
                }
                if (sb.length() > 0) {
                    String rest = sb.toString();
                    for (String d : delims) {
                        rest = rest.replaceAll(d, " ");
                    }
                    String[] words = rest.split("\\s+");
                    for (String w : words) {
                        if (w.length() < 3 || w.matches("\\d+")) {
                            continue;
                        }
                        if (isQuoted(w)) {
                            addQuoted(w.substring(1, w.length() - 1));
                        } else if (w.contains(".")) {
                            for (String f : w.split("\\.")) {
                                if (!f.matches("\\d+") && f.matches("\\w+")) {
                                    addFields(f);
                                }
                            }
                        } else if (w.matches("\\w+")) {
                            addValues(w);
                        }
                    }
                }
            }
            namedPipes = null;
        }

        public String encloseBy(String param) {
            boolean quoted = param.length() > 0 && (
                       param.startsWith("\"")
                    || param.startsWith("'")
                    || param.startsWith("/"));
            if (quoted && param.length() > 1) {
                quoted = !param.endsWith(Character.toString(param.charAt(0)));
            }
            return quoted ? Character.toString(param.charAt(0)) : "";
        }

        private boolean isQuoted(String word) {
            if (word.length() > 1 && ((word.startsWith("\"") && word.endsWith("\""))
                    || (word.startsWith("'") && word.endsWith("'"))
                    || (word.startsWith("/") && word.endsWith("/")))) {
                return true;
            }
            return false;
        }

        public int indexOfLastDelim(String word){
            int out = -1;
            for (String d: delims) {
                int x = word.lastIndexOf(d.replace("\\", ""));
                if (x > out) {
                    out = x;
                }
            }
            return out;
        }

        private void addFields(String field) {
            add("fields", field);
        }

        private void addValues(String arg) {
            add("values", arg);
        }

        private void addQuoted(String arg) {
            add("quoted", arg);
        }

        private void addOptions(String arg) {
            add("options", arg);
        }

        private void add(String where, String value) {
            if (value.length() < 3) {
                return;
            }
            names.get(where).remove(value);
            names.get(where).add(0, value);
        }

        public List<String> namedPipes() {
            if (namedPipes == null) {
                namedPipes = consoleId != null ? consoleEngine().getNamedPipes() : new ArrayList<>();
            }
            return namedPipes;
        }

        public List<String> values() {
            return names.get("values");
        }

        public List<String> fields() {
            return names.get("fields");
        }

        public List<String> quoted() {
            return names.get("quoted");
        }

        public List<String> options() {
            return names.get("options");
        }

        private Set<String> fieldsAndValues() {
            Set<String> out = new HashSet<>();
            out.addAll(fields());
            out.addAll(values());
            return out;
        }

        private void truncate(String where, int maxSize) {
            if (names.get(where).size() > maxSize) {
                names.put(where, names.get(where).subList(0, maxSize));
            }
        }

        public void save() {
            if (consoleEngine() != null && fileNames != null) {
                int maxSize = consoleEngine().consoleOption("maxValueNames", 100);
                truncate("fields", maxSize);
                truncate("values", maxSize);
                truncate("quoted", maxSize);
                truncate("options", maxSize);
                consoleEngine().persist(fileNames, names);
            }
        }
    }

}

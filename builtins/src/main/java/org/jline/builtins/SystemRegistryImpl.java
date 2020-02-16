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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.CommandRegistry;
import org.jline.builtins.ConsoleEngine.ExecutionResult;
import org.jline.builtins.Widgets;
import org.jline.builtins.Builtins.CommandMethods;
import org.jline.builtins.Options.HelpException;
import org.jline.reader.Completer;
import org.jline.reader.ConfigurationPath;
import org.jline.reader.EndOfFileException;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.OSUtils;

/**
 * Aggregate command registeries.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class SystemRegistryImpl implements SystemRegistry {
    public enum Command {
        EXIT, HELP
    };

    public enum Pipe {
        FLIP, NAMED, AND, OR
    }

    private static final Class<?>[] BUILTIN_REGISTERIES = { Builtins.class, ConsoleEngineImpl.class };
    private CommandRegistry[] commandRegistries;
    private Integer consoleId = null;
    private Parser parser;
    private ConfigurationPath configPath;
    private Map<Command, String> commandName = new HashMap<>();
    private Map<String, Command> nameCommand = new HashMap<>();
    private Map<String, String> aliasCommand = new HashMap<>();
    private Map<Pipe, String> pipeName = new HashMap<>();
    private final Map<Command, CommandMethods> commandExecute = new HashMap<>();
    private Map<String, List<String>> commandInfos = new HashMap<>();
    private Exception exception;
    private CommandOutputStream outputStream;

    public SystemRegistryImpl(Parser parser, Terminal terminal, ConfigurationPath configPath) {
        this.parser = parser;
        this.configPath = configPath;
        outputStream = new CommandOutputStream(terminal);
        Set<Command> cmds = new HashSet<>(EnumSet.allOf(Command.class));
        for (Command c : cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        doNameCommand();
        pipeName.put(Pipe.FLIP, "|;");
        pipeName.put(Pipe.NAMED, "|");
        pipeName.put(Pipe.AND, "&&");
        pipeName.put(Pipe.OR, "||");
        commandExecute.put(Command.EXIT, new CommandMethods(this::exit, this::exitCompleter));
        commandExecute.put(Command.HELP, new CommandMethods(this::help, this::helpCompleter));
    }

    private void doNameCommand() {
        nameCommand = commandName.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
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
        return nameCommand.keySet();
    }

    @Override
    public Map<String, String> commandAliases() {
        Map<String, String> out = new HashMap<>();
        for (CommandRegistry r : commandRegistries) {
            out.putAll(r.commandAliases());
        }
        out.putAll(aliasCommand);
        return out;
    }

    private Command command(String name) {
        Command out = null;
        if (!isLocalCommand(name)) {
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

    private List<String> localCommandInfo(String command) {
        try {
            localExecute(command, new String[] { "--help" });
        } catch (HelpException e) {
            exception = null;
            return Builtins.compileCommandInfo(e.getMessage());
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
        } else if (consoleId > -1 && consoleEngine().scripts().contains(command)) {
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
        return nameCommand.containsKey(command) || aliasCommand.containsKey(command);
    }

    private boolean isCommandOrScript(String command) {
        if (hasCommand(command)) {
            return true;
        }
        return consoleId > -1 ? consoleEngine().scripts().contains(command) : false;
    }

    @Override
    public Completers.SystemCompleter compileCompleters() {
        Completers.SystemCompleter out = CommandRegistry.aggregateCompleters(commandRegistries);
        Completers.SystemCompleter local = new Completers.SystemCompleter();
        for (Map.Entry<Command, String> entry : commandName.entrySet()) {
            local.add(entry.getValue(), commandExecute.get(entry.getKey()).compileCompleter().apply(entry.getValue()));
        }
        local.addAliases(aliasCommand);
        out.add(local);
        out.compile();
        return out;
    }

    @Override
    public Completer completer() {
        List<Completer> completers = new ArrayList<>();
        completers.add(compileCompleters());
        if (consoleId > -1) {
            completers.addAll(consoleEngine().scriptCompleters());
        }
        return new AggregateCompleter(completers);
    }

    private Widgets.CmdDesc localCommandDescription(String command) {
        if (!isLocalCommand(command)) {
            throw new IllegalArgumentException();
        }
        try {
            localExecute(command, new String[] { "--help" });
        } catch (HelpException e) {
            exception = null;
            return Builtins.compileCommandDescription(e.getMessage());
        } catch (Exception e) {
            trace(e);
        }
        return null;
    }

    @Override
    public Widgets.CmdDesc commandDescription(String command) {
        Widgets.CmdDesc out = new Widgets.CmdDesc(false);
        int id = registryId(command);
        if (id > -1) {
            out = commandRegistries[id].commandDescription(command);
        } else if (consoleId > -1 && consoleEngine().scripts().contains(command)) {
            out = consoleEngine().commandDescription(command);
        } else if (isLocalCommand(command)) {
            out = localCommandDescription(command);
        }
        return out;
    }

    @Override
    public Widgets.CmdDesc commandDescription(Widgets.CmdLine line) {
        Widgets.CmdDesc out = null;
        switch (line.getDescriptionType()) {
        case COMMAND:
            String cmd = parser.getCommand(line.getArgs().get(0));
            if (isCommandOrScript(cmd) && !hasPipes(line.getArgs())) {
                out = commandDescription(cmd);
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
        int id = registryId(command);
        if (id > -1) {
            out = commandRegistries[id].invoke(commandSession(), command, args);
        } else if (isLocalCommand(command)) {
            String[] _args = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                if (!(args[i] instanceof String)) {
                    throw new IllegalArgumentException();
                }
                _args[i] = args[i].toString();
            }
            out = localExecute(command, _args);
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

    public Object localExecute(String command, String[] args) throws Exception {
        if (!isLocalCommand(command)) {
            throw new IllegalArgumentException();
        }
        Object out = commandExecute.get(command(command)).executeFunction()
                .apply(new Builtins.CommandInput(args, commandSession()));
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

    private boolean hasPipes(Collection<String> args) {
        Map<String,List<String>> customPipes = consoleId != null ? consoleEngine().getPipes() : new HashMap<>();
        for (String a : args) {
            if (isPipe(a, customPipes.keySet())) {
                return true;
            }
        }
        return false;
    }

    private boolean isPipe(String arg, Set<String> pipes) {
        return pipeName.containsValue(arg) || pipes.contains(arg);
    }

    private List<CommandData> compileCommandLine(String commandLine) {
        List<CommandData> out = new ArrayList<>();
        ParsedLine pl = parser.parse(commandLine, 0, ParseContext.SPLIT_LINE);
        List<String> ws = pl.words();
        List<String> words = new ArrayList<>();
        String nextRawLine = "";
        Map<String,List<String>> customPipes = consoleId != null ? consoleEngine().getPipes() : new HashMap<>();
        if (consoleId != null && pl.words().contains(pipeName.get(Pipe.NAMED))) {
            StringBuilder sb = new StringBuilder();
            boolean trace = false;
            for (int i = 0 ; i < ws.size(); i++) {
                if (ws.get(i).equals(pipeName.get(Pipe.NAMED))) {
                    if (i + 1 >= ws.size()) {
                        throw new IllegalArgumentException();
                    }
                    if (consoleEngine().hasAlias(ws.get(i + 1))) {
                        trace = true;
                        List<String> args = new ArrayList<>();
                        String pipeAlias = consoleEngine().getAlias(ws.get(++i));
                        while (i < ws.size() - 1 && !isPipe(ws.get(i + 1), customPipes.keySet())) {
                            args.add(ws.get(++i));
                        }
                        for (int j = 0; j < args.size(); j++) {
                            pipeAlias = pipeAlias.replaceAll("\\s\\$" + j + "\\b", " " + args.get(j));
                            pipeAlias = pipeAlias.replaceAll("\\$\\{" + j + "(|:-.*)\\}", args.get(j));
                        }
                        pipeAlias = pipeAlias.replaceAll("\\s+\\$\\d\\b", "");
                        pipeAlias = pipeAlias.replaceAll("\\s+\\$\\{\\d+\\}", "");
                        pipeAlias = pipeAlias.replaceAll("\\$\\{\\d+\\}", "");
                        Matcher matcher=Pattern.compile("\\$\\{\\d+:-(.*?)\\}").matcher(pipeAlias);
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
            nextRawLine = sb.toString();
            pl = parser.parse(nextRawLine, 0, ParseContext.SPLIT_LINE);
            words = pl.words();
            if (trace) {
                consoleEngine().trace(nextRawLine);
            }
        } else {
            words = ws;
            nextRawLine = commandLine;
        }
        int first = 0;
        int last = words.size();
        List<String> pipes = new ArrayList<>();
        String pipeSource = null;
        String rawLine = null;
        String pipeResult = null;
        do {
            String variable = null;
            String command = ConsoleEngine.plainCommand(parser.getCommand(words.get(first)));
            if (parser.validCommandName(command) && consoleId != null) {
                variable = parser.getVariable(words.get(first));
                if (consoleEngine().hasAlias(command)) {
                    pl = parser.parse(nextRawLine.replaceFirst(command, consoleEngine().getAlias(command)), 0, ParseContext.SPLIT_LINE);
                    command = ConsoleEngine.plainCommand(parser.getCommand(pl.word()));
                    words = pl.words();
                    first = 0;
                }
            }
            last = words.size();
            File file = null;
            boolean append = false;
            boolean pipeStart = false;
            boolean skipPipe = false;
            ArgsParser argsParser = new ArgsParser();
            List<String> _words = new ArrayList<>();
            for (int i = first; i < last; i++) {
                argsParser.next(words.get(i));
                if (argsParser.isEnclosed()) {
                    _words.add(words.get(i));
                } else if (words.get(i).equals(">") || words.get(i).equals(">>")) {
                    pipes.add(words.get(i));
                    append = words.get(i).equals(">>");
                    if (i + 1 >= last) {
                        throw new IllegalArgumentException();
                    }
                    file = new File(words.get(i + 1));
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
                } else if (  words.get(i).equals(pipeName.get(Pipe.NAMED))
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
                } else if (words.get(i).equals(pipeName.get(Pipe.OR)) || words.get(i).equals(pipeName.get(Pipe.AND))) {
                    if (pipeResult != null) {
                        pipes.add(words.get(i));
                        variable = pipeResult;
                        pipeResult = null;
                    } else if (pipeSource != null
                            || variable != null
                            ) {
                        pipes.add(words.get(i));                                                
                    } else if (pipes.size() > 0 && (
                                   pipes.get(pipes.size() - 1).equals(">") 
                                || pipes.get(pipes.size() - 1).equals(">>"))) {
                        pipes.remove(pipes.size() - 1);
                        out.get(out.size() - 1).setPipe(words.get(i));
                        skipPipe = true;
                    } else {
                        pipes.add(pipeName.get(Pipe.FLIP));
                        pipeSource = "_pipe" + (pipes.size() - 1);
                        variable = pipeSource;
                        pipeStart = true;
                        i = i - 1;
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
            String subLine = last < words.size() || first > 0
                    ? _words.stream().collect(Collectors.joining(" "))
                    : pl.line();
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
                    rawLine += fixes.get(0) + subLine + fixes.get(1);
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
                String[] args = new String[] {};
                if (!statement) {
                    ParsedLine plf = parser.parse(rawLine, 0, ParseContext.ACCEPT_LINE);
                    if (plf.words().size() > 1) {
                        args = plf.words().subList(1, plf.words().size()).toArray(new String[0]);
                    }
                }
                out.add(new CommandData(rawLine, statement ? "" : command, args, variable, file, append, pipes.get(pipes.size() - 1)));
                rawLine = null;
            }
            first = last + 1;
        } while (first < words.size());
        return out;
    }

    private static class ArgsParser {
        private int round = 0;
        private int curly = 0;
        private int square = 0;
        private boolean quoted;
        private boolean doubleQuoted;

        public ArgsParser() {}

        public void next(String arg) {
            char prevChar = ' ';
            for (int i = 0; i < arg.length(); i++) {
                char c = arg.charAt(i);
                if (prevChar != '\\') {
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

        public boolean isEnclosed() {
            return round != 0 || curly != 0 || square != 0 || quoted || doubleQuoted;
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

        public CommandData(String rawLine, String command, String[] args, String variable, File file, boolean append, String pipe) {
            this.rawLine = rawLine;
            this.command = command;
            this.args = args;
            this.variable = variable;
            this.file = file;
            this.append = append;
            this.pipe = pipe;
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

    @Override
    public Object execute(String line) throws Exception {
        if (line.isEmpty() || line.trim().startsWith("#")) {
            return null;
        }
        Object out = null;
        boolean statement = false;
        boolean postProcessed = false;
        for (CommandData cmd : compileCommandLine(line)) {
            try {
                outputStream.closeAndReset();
                if (!consoleEngine().isExecuting()) {
                    trace(cmd);
                }
                exception = null;
                statement = false;
                postProcessed = false;
                if (cmd.variable() != null || cmd.file() != null) {
                    if (cmd.file() != null) {
                        outputStream.redirect(cmd.file(), cmd.append());
                    } else if (consoleId != null && !consoleEngine().isExecuting()) {
                        outputStream.redirect();
                    }
                    outputStream.open();
                }
                boolean consoleScript = false;
                if (parser.validCommandName(cmd.command())) {
                    if (isLocalCommand(cmd.command())) {
                        out = localExecute(cmd.command(), cmd.args());
                    } else {
                        int id = registryId(cmd.command());
                        if (id > -1) {
                            if (consoleId != null) {
                                out = commandRegistries[id].invoke(outputStream.getCommandSession(), cmd.command(),
                                        consoleEngine().expandParameters(cmd.args()));
                            } else {
                                out = commandRegistries[id].execute(outputStream.getCommandSession(), cmd.command(),
                                        cmd.args());
                            }
                        } else if (consoleId != null) {
                            consoleScript = true;
                        }
                    }
                } else if (consoleId != null) {
                    consoleScript = true;
                }
                if (consoleScript) {
                    if (cmd.command().isEmpty() || !consoleEngine().scripts().contains(cmd.command())) {
                        statement = true;
                    }
                    if (statement && outputStream.isByteStream()) {
                        outputStream.closeAndReset();
                    }
                    out = consoleEngine().execute(cmd.command(), cmd.rawLine(), cmd.args());
                }
                if (consoleId != null && cmd.pipe().equals(pipeName.get(Pipe.OR)) || cmd.pipe().equals(pipeName.get(Pipe.AND))) {
                    ExecutionResult er = postProcess(cmd, statement, out);
                    postProcessed = true;
                    if (!consoleEngine().isExecuting()) {
                        consoleEngine().println(er.result());
                    }
                    out = null;
                    boolean success = er.status() == 0 ? true : false;
                    if (   (cmd.pipe().equals(pipeName.get(Pipe.OR)) && success)
                        || (cmd.pipe().equals(pipeName.get(Pipe.AND)) && !success)) {
                        break;
                    }
                }
            } catch (HelpException e) {
                trace(e);
            } finally {
                if (!postProcessed && consoleId != null && !consoleEngine().isExecuting()) {
                    out = postProcess(cmd, statement, out).result();
                }
            }
        }
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
            out = new ExecutionResult(status, null);
        } else if (!statement) {
            outputStream.flush();
            out = consoleEngine().postProcess(cmd.rawLine(), result, outputStream.getOutput());
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
            SystemRegistry.println(false, terminal(), exception);
        }
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

    private boolean isInArgs(List<String> args, String name) {
        return args.isEmpty() || args.contains(name);
    }

    private Object help(Builtins.CommandInput input) {
        final String[] usage = { "help -  command help", "Usage: help [NAME...]",
                "  -? --help                       Displays command help", };
        Options opt = Options.compile(usage).parse(input.args());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }

        Set<String> commands = commandNames();
        if (consoleId > -1) {
            commands.addAll(consoleEngine().scripts());
        }
        boolean withInfo = commands.size() < terminal().getHeight() || !opt.args().isEmpty() ? true : false;
        int max = Collections.max(commands, Comparator.comparing(String::length)).length() + 1;
        TreeMap<String, String> builtinCommands = new TreeMap<>();
        for (CommandRegistry r : commandRegistries) {
            if (isBuiltinRegistry(r)) {
                for (String c : r.commandNames()) {
                    builtinCommands.put(c, doCommandInfo(commandInfo(c)));
                }
            }
        }
        for (String c : localCommandNames()) {
            builtinCommands.put(c, doCommandInfo(commandInfo(c)));
            exception = null;
        }
        if (isInArgs(opt.args(), "Builtins")) {
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
            if (isBuiltinRegistry(r) || !isInArgs(opt.args(), r.name())) {
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
        if (consoleId > -1 && isInArgs(opt.args(), "Scripts")) {
            printHeader("Scripts");
            if (withInfo) {
                for (String c : consoleEngine().scripts()) {
                    printCommandInfo(c, doCommandInfo(commandInfo(c)), max);
                }
            } else {
                printCommands(consoleEngine().scripts(), max);
            }
        }
        terminal().flush();
        return null;
    }

    private Object exit(Builtins.CommandInput input) {
        final String[] usage = { "exit -  exit from app/script", "Usage: exit",
                "  -? --help                       Displays command help", };
        Options opt = Options.compile(usage).parse(input.args());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
        } else {
            exception = new EndOfFileException();
        }
        return null;
    }

    private List<OptDesc> commandOptions(String command) {
        try {
            localExecute(command, new String[] { "--help" });
        } catch (HelpException e) {
            exception = null;
            return Builtins.compileCommandOptions(e.getMessage());
        } catch (Exception e) {
            trace(e);
        }
        return null;
    }

    private List<String> registryNames() {
        List<String> out = new ArrayList<>();
        out.add("Builtins");
        if (consoleId > -1) {
            out.add("Scripts");
        }
        for (CommandRegistry r : commandRegistries) {
            if (isBuiltinRegistry(r)) {
                continue;
            }
            out.add(r.name());
        }
        return out;
    }

    private List<Completer> helpCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE,
                new OptionCompleter(new StringsCompleter(this::registryNames), this::commandOptions, 1)));
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
}

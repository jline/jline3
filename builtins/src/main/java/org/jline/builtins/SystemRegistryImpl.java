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
import java.util.*;
import java.util.stream.Collectors;

import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.CommandRegistry;
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
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;

/**
 * Aggregate command registeries.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class SystemRegistryImpl implements SystemRegistry {
    public enum Command {EXIT
                       , HELP};
    private static final Class<?>[] BUILTIN_REGISTERIES = {Builtins.class, ConsoleEngineImpl.class};
    private CommandRegistry[] commandRegistries;
    private Integer consoleId = null;
    private Terminal terminal;
    private Parser parser;
    private ConfigurationPath configPath;
    private Map<Command,String> commandName = new HashMap<>();
    private Map<String,Command> nameCommand = new HashMap<>();
    private Map<String,String> aliasCommand = new HashMap<>();
    private final Map<Command,CommandMethods> commandExecute = new HashMap<>();
    private Map<String, List<String>> commandInfos = new HashMap<>();
    private Exception exception;

    public SystemRegistryImpl(Parser parser, Terminal terminal, ConfigurationPath configPath) {
        this.parser = parser;
        this.terminal = terminal;
        this.configPath = configPath;
        Set<Command> cmds = new HashSet<>(EnumSet.allOf(Command.class));
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        doNameCommand();
        commandExecute.put(Command.EXIT, new CommandMethods(this::exit, this::exitCompleter));
        commandExecute.put(Command.HELP, new CommandMethods(this::help, this::helpCompleter));
    }

    private void doNameCommand() {
        nameCommand = commandName.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
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
                consoleEngine().println(e);
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
            localExecute(command, new String[] {"--help"});
        } catch (HelpException e) {
            exception = null;
            return Builtins.compileCommandInfo(e.getMessage());
        } catch (Exception e) {
            consoleEngine().println(e);
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

    @Override
    public Completers.SystemCompleter compileCompleters() {
        Completers.SystemCompleter out = CommandRegistry.aggregateCompleters(commandRegistries);
        Completers.SystemCompleter local = new Completers.SystemCompleter();
        for (Map.Entry<Command, String> entry: commandName.entrySet()) {
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
            localExecute(command, new String[] {"--help"});
        } catch (HelpException e) {
            exception = null;
            return Builtins.compileCommandDescription(e.getMessage());
        } catch (Exception e) {
            consoleEngine().println(e);
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
            String cmd = Parser.getCommand(line.getArgs().get(0));
            out = commandDescription(cmd);
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
            out = commandRegistries[id].invoke(command, args);
        } else if (isLocalCommand(command)) {
            out = invoke(command, args);
        } else if (consoleId != null) {
            out = consoleEngine().invoke(command, args);
        }
        return out;
    }

    @Override
    public Object execute(String command, String[] args) throws Exception {
        Object out = null;
        int id = registryId(command);
        if (id > -1) {
            out = commandRegistries[id].execute(command, args);
        } else if (isLocalCommand(command)) {
            out = localExecute(command, args);
        }
        return out;
    }

    public Object localExecute(String command, String[] args) throws Exception {
        if (!isLocalCommand(command)) {
            throw new IllegalArgumentException();
        }
        Object out = commandExecute.get(command(command)).executeFunction().apply(new Builtins.CommandInput(args));
        if (exception != null) {
            throw exception;
        }
        return out;
    }

    @Override
    public Object execute(String line) throws Exception {
        ParsedLine pl = parser.parse(line, 0, ParseContext.ACCEPT_LINE);
        if (pl.line().isEmpty() || pl.line().trim().startsWith("#")) {
            return null;
        }
        String cmd = ConsoleEngine.plainCommand(Parser.getCommand(pl.word()));
        if (consoleId != null && consoleEngine().hasAlias(cmd)) {
            pl = parser.parse(line.replaceFirst(cmd, consoleEngine().getAlias(cmd)), 0, ParseContext.ACCEPT_LINE);
            cmd = ConsoleEngine.plainCommand(Parser.getCommand(pl.word()));
        }
        String[] argv = pl.words().subList(1, pl.words().size()).toArray(new String[0]);
        Object out = null;
        exception = null;
        if (isLocalCommand(cmd)) {
            out = localExecute(cmd, argv);
        } else {
            int id = registryId(cmd);
            try {
                if (id > -1) {
                    if (consoleId != null) {
                        out = commandRegistries[id].invoke(cmd, consoleEngine().expandParameters(argv));
                        out = consoleEngine().postProcess(pl.line(), out);
                    } else {
                        out = commandRegistries[id].execute(cmd, argv);
                    }
                } else if (consoleId != null) {
                    out = consoleEngine().execute(pl);
                }
            } catch (HelpException e) {
                consoleEngine().println(e);
            }
        }
        return out;
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
        asb.toAttributedString().println(terminal);
    }

    private void printCommandInfo(String command, String info, int max) {
        AttributedStringBuilder asb = new AttributedStringBuilder().tabs(Arrays.asList(4, max + 4));
        asb.append("\t");
        asb.append(command, HelpException.defaultStyle().resolve(".co"));
        asb.append("\t");
        asb.append(info);
        asb.setLength(terminal.getWidth());
        asb.toAttributedString().println(terminal);
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
            if (col + max > terminal.getWidth()) {
                asb.toAttributedString().println(terminal);
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
            asb.toAttributedString().println(terminal);
        }
        terminal.flush();
    }

    private String doCommandInfo(List<String> info) {
        return info.size() > 0 ? info.get(0) : " ";
    }

    private boolean isInArgs(List<String> args, String name) {
        return args.isEmpty() || args.contains(name);
    }

    private Object help(Builtins.CommandInput input) {
        final String[] usage = {
                "help -  command help",
                "Usage: help [NAME...]",
                "  -? --help                       Displays command help",
        };
        Options opt = Options.compile(usage).parse(input.args());
        if (opt.isSet("help")) {
            exception = new HelpException(opt.usage());
            return null;
        }

        Set<String> commands = commandNames();
        if (consoleId > -1) {
            commands.addAll(consoleEngine().scripts());
        }
        boolean withInfo = commands.size() < terminal.getHeight() || !opt.args().isEmpty() ? true : false;
        int max = Collections.max(commands, Comparator.comparing(String::length)).length() + 1;
        TreeMap<String,String> builtinCommands = new TreeMap<>();
        for (CommandRegistry r : commandRegistries) {
            if (isBuiltinRegistry(r)) {
                for (String c: r.commandNames()) {
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
                for (String c: consoleEngine().scripts()) {
                    printCommandInfo(c, doCommandInfo(commandInfo(c)), max);
                }
            } else {
                printCommands(consoleEngine().scripts(), max);
            }
        }
        return null;
    }

    private Object exit(Builtins.CommandInput input) {
        final String[] usage = {
                "exit -  exit from app/script",
                "Usage: exit",
                "  -? --help                       Displays command help",
        };
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
            localExecute(command, new String[] {"--help"});
        } catch (HelpException e) {
            exception = null;
            return Builtins.compileCommandOptions(e.getMessage());
        } catch (Exception e) {
            consoleEngine().println(e);
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
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                               , new OptionCompleter(new StringsCompleter(this::registryNames)
                                                   , this::commandOptions
                                                   , 1)
                                            ));
        return completers;
    }

    private List<Completer> exitCompleter(String command) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                , new OptionCompleter(NullCompleter.INSTANCE
                                                    , this::commandOptions
                                                    , 1)
                                             ));
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

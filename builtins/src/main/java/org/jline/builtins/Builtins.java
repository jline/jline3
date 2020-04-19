/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.builtins.Completers.FilesCompleter;
import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.Options.HelpException;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.console.ConfigurationPath;
import org.jline.reader.*;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

/**
 * Builtins: create tab completers, execute and create descriptions for builtins commands.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class Builtins extends AbstractCommandRegistry implements CommandRegistry {
    public enum Command {NANO
                       , LESS
                       , HISTORY
                       , WIDGET
                       , KEYMAP
                       , SETOPT
                       , SETVAR
                       , UNSETOPT
                       , TTOP};
    private ConfigurationPath configPath;
    private final Function<String, Widget> widgetCreator;
    private final Supplier<Path> workDir;
    private LineReader reader;

    public Builtins(Path workDir, ConfigurationPath configPath, Function<String, Widget> widgetCreator) {
        this(null, () -> workDir, configPath, widgetCreator);
    }

    public Builtins(Set<Command> commands, Path workDir, ConfigurationPath configpath, Function<String, Widget> widgetCreator) {
        this(commands, () -> workDir, configpath, widgetCreator);
    }

    public Builtins(Supplier<Path> workDir, ConfigurationPath configPath, Function<String, Widget> widgetCreator) {
        this(null, workDir, configPath, widgetCreator);
    }

    public Builtins(Set<Command> commands, Supplier<Path> workDir, ConfigurationPath configpath, Function<String, Widget> widgetCreator) {
        super();
        this.configPath = configpath;
        this.widgetCreator = widgetCreator;
        this.workDir = workDir;
        Set<Command> cmds = new HashSet<>();
        Map<Command,String> commandName = new HashMap<>();
        Map<Command,CommandMethods> commandExecute = new HashMap<>();
        if (commands == null) {
            cmds = new HashSet<>(EnumSet.allOf(Command.class));
        } else {
            cmds = new HashSet<>(commands);
        }
        for (Command c: cmds) {
            commandName.put(c, c.name().toLowerCase());
        }
        commandExecute.put(Command.NANO, new CommandMethods(this::nano, this::nanoCompleter));
        commandExecute.put(Command.LESS, new CommandMethods(this::less, this::lessCompleter));
        commandExecute.put(Command.HISTORY, new CommandMethods(this::history, this::historyCompleter));
        commandExecute.put(Command.WIDGET, new CommandMethods(this::widget, this::widgetCompleter));
        commandExecute.put(Command.KEYMAP, new CommandMethods(this::keymap, this::defaultCompleter));
        commandExecute.put(Command.SETOPT, new CommandMethods(this::setopt, this::setoptCompleter));
        commandExecute.put(Command.SETVAR, new CommandMethods(this::setvar, this::setvarCompleter));
        commandExecute.put(Command.UNSETOPT, new CommandMethods(this::unsetopt, this::unsetoptCompleter));
        commandExecute.put(Command.TTOP, new CommandMethods(this::ttop, this::defaultCompleter));
        registerCommands(commandName, commandExecute);
    }

    public void setLineReader(LineReader reader) {
        this.reader = reader;
    }

    private void less(CommandInput input) {
        try {
            Commands.less(input.terminal(), input.in(), input.out(), input.err(), workDir.get(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void nano(CommandInput input) {
        try {
            Commands.nano(input.terminal(), input.out(), input.err(), workDir.get(), input.args(), configPath);
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void history(CommandInput input) {
        try {
            Commands.history(reader, input.out(), input.err(), workDir.get(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void widget(CommandInput input) {
        try {
            Commands.widget(reader, input.out(), input.err(), widgetCreator, input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void keymap(CommandInput input) {
        try {
            Commands.keymap(reader, input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void setopt(CommandInput input) {
        try {
            Commands.setopt(reader, input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void setvar(CommandInput input) {
        try {
            Commands.setvar(reader, input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void unsetopt(CommandInput input) {
        try {
            Commands.unsetopt(reader, input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private void ttop(CommandInput input) {
        try {
            TTop.ttop(input.terminal(), input.out(), input.err(), input.args());
        } catch (Exception e) {
            saveException(e);
        }
    }

    private List<String> unsetOptions(boolean set) {
        List<String> out = new ArrayList<>();
        for (Option option : Option.values()) {
            if (set == (reader.isSet(option) == option.isDef())) {
                out.add((option.isDef() ? "no-" : "") + option.toString().toLowerCase().replace('_', '-'));
            }
        }
        return out;
    }

    private Set<String> allWidgets() {
        Set<String> out = new HashSet<>();
        for (String s: reader.getWidgets().keySet()) {
            out.add(s);
            out.add(reader.getWidgets().get(s).toString());
        }
        return out;
    }

    private List<Completer> nanoCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                           , new OptionCompleter(new FilesCompleter(workDir)
                                                               , this::commandOptions
                                                               , 1)
                                            ));
        return completers;
    }

    private List<Completer> lessCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                           , new OptionCompleter(new FilesCompleter(workDir)
                                                               , this::commandOptions
                                                               , 1)
                                       ));
        return completers;
    }

    private List<Completer> historyCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        List<OptDesc> optDescs = commandOptions(name);
        for (OptDesc o : optDescs) {
            if (o.shortOption() != null && (o.shortOption().equals("-A") || o.shortOption().equals("-W")
                                        || o.shortOption().equals("-R"))) {
                o.setValueCompleter(new FilesCompleter(workDir));
            }
        }
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                            , new OptionCompleter(NullCompleter.INSTANCE
                                                                , optDescs
                                                                , 1)
                                       ));
        return completers;
    }

    private List<Completer> widgetCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        List<OptDesc> optDescs = commandOptions(name);
        Candidate aliasOption = new Candidate("-A", "-A", null, null, null, null, true);
        Iterator<OptDesc> i = optDescs.iterator();
        while (i.hasNext()) {
            OptDesc o = i.next();
            if (o.shortOption() != null) {
                if (o.shortOption().equals("-D")) {
                    o.setValueCompleter(new StringsCompleter(() -> reader.getWidgets().keySet()));
                } else if (o.shortOption().equals("-A")) {
                    aliasOption = new Candidate(o.shortOption(), o.shortOption(), null, o.description(), null, null, true);
                    i.remove();
                }
            }
        }
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                            , new OptionCompleter(NullCompleter.INSTANCE
                                                                , optDescs
                                                                , 1)
                                       ));
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                     , new StringsCompleter(aliasOption), new StringsCompleter(() -> allWidgets())
                     , new StringsCompleter(() -> reader.getWidgets().keySet()), NullCompleter.INSTANCE));
        return completers;
    }

    private List<Completer> setvarCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                     , new StringsCompleter(() -> reader.getVariables().keySet()), NullCompleter.INSTANCE));
        return completers;
    }

    private List<Completer> setoptCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                     , new StringsCompleter(() -> unsetOptions(true))));
        return completers;
    }

    private List<Completer> unsetoptCompleter(String name) {
        List<Completer> completers = new ArrayList<>();
        completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                     , new StringsCompleter(() -> unsetOptions(false))));
        return completers;
    }

    //
    // Utils for helpMessage parsing
    //
    private static AttributedString highlightComment(String comment) {
        return HelpException.highlightComment(comment, HelpException.defaultStyle());
    }

    private static String[] helpLines(String helpMessage, boolean body) {
        return new HelpLines(helpMessage, body).lines();
    }

    private static class HelpLines {
        private String helpMessage;
        private boolean body;
        private boolean subcommands;

        public HelpLines(String helpMessage, boolean body) {
            this.helpMessage = helpMessage;
            this.body = body;
        }

        public String[] lines() {
            String out = "";
            Matcher tm = Pattern.compile("(^|\\n)(Usage|Summary)(:)").matcher(helpMessage);
            if (tm.find()) {
                subcommands = tm.group(2).matches("Summary");
                if (body) {
                    out = helpMessage.substring(tm.end(3));
                } else {
                    out = helpMessage.substring(0,tm.start(1));
                }
            } else if (!body) {
                out = helpMessage;
            }
            return out.split("\\r?\\n");
        }

        public boolean subcommands() {
            return subcommands;
        }
    }

    public static CmdDesc compileCommandDescription(String helpMessage) {
        List<AttributedString> main = new ArrayList<>();
        Map<String, List<AttributedString>> options = new HashMap<>();
        String prevOpt = null;
        boolean mainDone = false;
        HelpLines hl = new HelpLines(helpMessage, true);
        for (String s : hl.lines()) {
            if (s.matches("^\\s+-.*$")) {
                mainDone = true;
                int ind = s.lastIndexOf("  ");
                if (ind > 0) {
                    String o = s.substring(0, ind);
                    String d = s.substring(ind);
                    if (o.trim().length() > 0) {
                        prevOpt = o.trim();
                        options.put(prevOpt, new ArrayList<>(Arrays.asList(highlightComment(d.trim()))));
                    }
                }
            } else if (s.matches("^[\\s]{20}.*$") && prevOpt != null && options.containsKey(prevOpt)) {
                int ind = s.lastIndexOf("  ");
                if (ind > 0) {
                    options.get(prevOpt).add(highlightComment(s.substring(ind).trim()));
                }
            } else {
                prevOpt = null;
            }
            if (!mainDone) {
                main.add(HelpException.highlightSyntax(s.trim(), HelpException.defaultStyle(), hl.subcommands()));
            }
        }
        return new CmdDesc(main, ArgDesc.doArgNames(Arrays.asList("")), options);
    }

    public static List<OptDesc> compileCommandOptions(String helpMessage) {
        List<OptDesc> out = new ArrayList<>();
        for (String s : helpLines(helpMessage, true)) {
            if (s.matches("^\\s+-.*$")) {
                int ind = s.lastIndexOf("  ");
                if (ind > 0) {
                    String[] op = s.substring(0, ind).trim().split("\\s+");
                    String d = s.substring(ind).trim();
                    String so = null;
                    String lo = null;
                    if (op.length == 1) {
                        if (op[0].startsWith("--")) {
                            lo = op[0];
                        } else {
                            so = op[0];
                        }
                    } else {
                        so = op[0];
                        lo = op[1];
                    }
                    lo = lo == null ? lo : lo.split("=")[0];
                    out.add(new OptDesc(so, lo, d));
                }
            }
        }
        return out;
    }

    public static List<String> compileCommandInfo(String helpMessage) {
        List<String> out = new ArrayList<>();
        boolean first = true;
        for (String s  : helpLines(helpMessage, false)) {
            if (first && s.contains(" - ")) {
                out.add(s.substring(s.indexOf(" - ") + 3).trim());
            } else {
                out.add(s.trim());
            }
            first = false;
        }
        return out;
    }

    public static class CommandInput {
        String command;
        String[] args;
        Object[] xargs;
        Terminal terminal;
        InputStream in;
        PrintStream out;
        PrintStream err;

        public CommandInput(String command, Object[] xargs, CommandRegistry.CommandSession session) {
            if (xargs != null) {
                this.xargs = xargs;
                this.args = new String[xargs.length];
                for (int i = 0; i < xargs.length; i++) {
                    this.args[i] = xargs[i] != null ? xargs[i].toString() : null;
                }
            }
            this.command = command;
            this.terminal = session.terminal();
            this.in = session.in();
            this.out = session.out();
            this.err = session.err();
        }

        public CommandInput(String command, Object[] args, Terminal terminal, InputStream in, PrintStream out, PrintStream err) {
            this(command, args, new CommandRegistry.CommandSession(terminal, in, out, err));
        }

        public String command() {
            return command;
        }

        public String[] args() {
            return args;
        }

        public Object[] xargs() {
            return xargs;
        }

        public Terminal terminal() {
            return terminal;
        }

        public InputStream in() {
            return in;
        }

        public PrintStream out() {
            return out;
        }

        public PrintStream err() {
            return err;
        }

        public CommandRegistry.CommandSession session() {
            return new CommandRegistry.CommandSession(terminal, in, out, err);
        }

    }

    public static class CommandMethods {
        Consumer<CommandInput> execute;
        Function<CommandInput, Object> executeFunction;
        Function<String, List<Completer>> compileCompleter;

        public CommandMethods(Function<CommandInput, Object> execute,  Function<String, List<Completer>> compileCompleter) {
            this.executeFunction = execute;
            this.compileCompleter = compileCompleter;
        }

        public CommandMethods(Consumer<CommandInput> execute,  Function<String, List<Completer>> compileCompleter) {
            this.execute = execute;
            this.compileCompleter = compileCompleter;
        }

        public Consumer<CommandInput> execute() {
            return execute;
        }

        public Function<CommandInput, Object> executeFunction() {
            return executeFunction;
        }

        public Function<String, List<Completer>> compileCompleter() {
            return compileCompleter;
        }

        public boolean isConsumer() {
            return execute != null;
        }
    }

}

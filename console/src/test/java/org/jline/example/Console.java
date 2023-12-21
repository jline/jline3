/*
 * Copyright (c) 2002-2022, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.example;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

import org.jline.builtins.ConfigurationPath;
import org.jline.builtins.Styles;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.Printer;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.DefaultPrinter;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReader.SuggestionType;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.InfoCmp.Capability;
import org.jline.widget.AutopairWidgets;
import org.jline.widget.AutosuggestionWidgets;
import org.jline.widget.TailTipWidgets;
import org.jline.widget.TailTipWidgets.TipType;

public class Console {
    private static Map<String, CmdDesc> compileTailTips() {
        Map<String, CmdDesc> tailTips = new HashMap<>();
        Map<String, List<AttributedString>> optDesc = new HashMap<>();
        optDesc.put("--optionA", Arrays.asList(new AttributedString("optionA description...")));
        optDesc.put("--noitpoB", Arrays.asList(new AttributedString("noitpoB description...")));
        optDesc.put(
                "--optionC",
                Arrays.asList(new AttributedString("optionC description..."), new AttributedString("line2")));
        Map<String, List<AttributedString>> widgetOpts = new HashMap<>();
        List<AttributedString> mainDesc = Arrays.asList(
                new AttributedString("widget -N new-widget [function-name]"),
                new AttributedString("widget -D widget ..."),
                new AttributedString("widget -A old-widget new-widget"),
                new AttributedString("widget -U string ..."),
                new AttributedString("widget -l [options]"));
        widgetOpts.put("-N", Arrays.asList(new AttributedString("Create new widget")));
        widgetOpts.put("-D", Arrays.asList(new AttributedString("Delete widgets")));
        widgetOpts.put("-A", Arrays.asList(new AttributedString("Create alias to widget")));
        widgetOpts.put("-U", Arrays.asList(new AttributedString("Push characters to the stack")));
        widgetOpts.put("-l", Arrays.asList(new AttributedString("List user-defined widgets")));

        tailTips.put("widget", new CmdDesc(mainDesc, ArgDesc.doArgNames(Arrays.asList("[pN...]")), widgetOpts));
        tailTips.put("foo12", new CmdDesc(ArgDesc.doArgNames(Arrays.asList("param1", "param2", "[paramN...]"))));
        tailTips.put(
                "foo11",
                new CmdDesc(
                        Arrays.asList(
                                new ArgDesc(
                                        "param1",
                                        Arrays.asList(
                                                new AttributedString("Param1 description..."),
                                                new AttributedString(
                                                        "line 2: This is a very long line that does exceed the terminal width."
                                                                + " The line will be truncated automatically (by Status class) before printing out."),
                                                new AttributedString("line 3"),
                                                new AttributedString("line 4"),
                                                new AttributedString("line 5"),
                                                new AttributedString("line 6"))),
                                new ArgDesc(
                                        "param2",
                                        Arrays.asList(
                                                new AttributedString("Param2 description..."),
                                                new AttributedString("line 2"))),
                                new ArgDesc("param3", new ArrayList<>())),
                        optDesc));
        return tailTips;
    }

    private static class ExampleCommands implements CommandRegistry {
        private LineReader reader;
        private AutosuggestionWidgets autosuggestionWidgets;
        private TailTipWidgets tailtipWidgets;
        private AutopairWidgets autopairWidgets;
        private final Map<String, CommandMethods> commandExecute = new HashMap<>();
        private final Map<String, List<String>> commandInfo = new HashMap<>();
        private final Map<String, String> aliasCommand = new HashMap<>();
        private Exception exception;
        private final Printer printer;
        private final Printer myPrinter;

        public ExampleCommands(Printer printer, Printer myPrinter) {
            this.printer = printer;
            this.myPrinter = myPrinter;
            commandExecute.put("testprint", new CommandMethods(this::testprint, this::defaultCompleter));
            commandExecute.put("testkey", new CommandMethods(this::testkey, this::defaultCompleter));
            commandExecute.put("clear", new CommandMethods(this::clear, this::defaultCompleter));
            commandExecute.put("autopair", new CommandMethods(this::autopair, this::defaultCompleter));
            commandExecute.put(
                    "autosuggestion", new CommandMethods(this::autosuggestion, this::autosuggestionCompleter));

            commandInfo.put(
                    "testprint",
                    Arrays.asList("print table using DefaultPrinter (args.length=0) or MyPrinter (args.length>0)"));
            commandInfo.put("testkey", Arrays.asList("display key events"));
            commandInfo.put("clear", Arrays.asList("clear screen"));
            commandInfo.put("autopair", Arrays.asList("toggle brackets/quotes autopair key bindings"));
            commandInfo.put(
                    "autosuggestion",
                    Arrays.asList("set autosuggestion modality: history, completer, tailtip or none"));
        }

        public void setLineReader(LineReader reader) {
            this.reader = reader;
        }

        public void setAutosuggestionWidgets(AutosuggestionWidgets autosuggestionWidgets) {
            this.autosuggestionWidgets = autosuggestionWidgets;
        }

        public void setTailTipWidgets(TailTipWidgets tailtipWidgets) {
            this.tailtipWidgets = tailtipWidgets;
        }

        public void setAutopairWidgets(AutopairWidgets autopairWidgets) {
            this.autopairWidgets = autopairWidgets;
        }

        private Terminal terminal() {
            return reader.getTerminal();
        }

        public Set<String> commandNames() {
            return commandExecute.keySet();
        }

        public Map<String, String> commandAliases() {
            return aliasCommand;
        }

        public List<String> commandInfo(String command) {
            return commandInfo.get(command(command));
        }

        public boolean hasCommand(String command) {
            return commandExecute.containsKey(command) || aliasCommand.containsKey(command);
        }

        private String command(String name) {
            if (commandExecute.containsKey(name)) {
                return name;
            }
            return aliasCommand.get(name);
        }

        public SystemCompleter compileCompleters() {
            SystemCompleter out = new SystemCompleter();
            for (String c : commandExecute.keySet()) {
                out.add(c, commandExecute.get(c).compileCompleter().apply(c));
            }
            out.addAliases(aliasCommand);
            return out;
        }

        public Object invoke(CommandSession session, String command, Object... args) throws Exception {
            exception = null;
            Object out = commandExecute.get(command(command)).execute().apply(new CommandInput(command, args, session));
            if (exception != null) {
                throw exception;
            }
            return out;
        }

        public CmdDesc commandDescription(List<String> args) {
            // TODO
            return new CmdDesc(false);
        }

        private Map<String, Object> fillMap(String name, Integer age, String country, String town) {
            Map<String, Object> out = new HashMap<>();
            Map<String, String> address = new HashMap<>();
            address.put("country", country);
            address.put("town", town);
            out.put("name", name);
            out.put("age", age);
            out.put("address", address);
            return out;
        }

        private void testprint(CommandInput input) {
            Printer p = input.args().length > 0 ? myPrinter : printer;
            List<Map<String, Object>> data = new ArrayList<>();
            data.add(fillMap("heikki", 10, "finland", "helsinki"));
            data.add(fillMap("pietro", 11, "italy", "milano"));
            data.add(fillMap("john", 12, "england", "london"));
            p.println("Printing tables using: " + p.getClass().getName());
            p.println(data);
            Map<String, Object> options = new HashMap<>();
            options.put(Printer.STRUCT_ON_TABLE, true);
            options.put(Printer.VALUE_STYLE, "classpath:/org/jline/example/gron.nanorc");
            p.println(options, data);
            options.clear();
            options.put(Printer.COLUMNS, Arrays.asList("name", "age", "address.country", "address.town"));
            options.put(Printer.SHORT_NAMES, true);
            p.println(options, data);
        }

        private void testkey(CommandInput input) {
            try {
                terminal().writer().write("Input the key event(Enter to complete): ");
                terminal().writer().flush();
                StringBuilder sb = new StringBuilder();
                while (true) {
                    int c = ((LineReaderImpl) reader).readCharacter();
                    if (c == 10 || c == 13) break;
                    sb.append(new String(Character.toChars(c)));
                }
                terminal().writer().println(KeyMap.display(sb.toString()));
                terminal().writer().flush();
            } catch (Exception e) {
                exception = e;
            }
        }

        private void clear(CommandInput input) {
            try {
                terminal().puts(Capability.clear_screen);
                terminal().flush();
            } catch (Exception e) {
                exception = e;
            }
        }

        private void autopair(CommandInput input) {
            try {
                if (tailtipWidgets.isEnabled()) {
                    tailtipWidgets.disable();
                }
                terminal().writer().print("Autopair widgets are ");
                if (autopairWidgets.toggle()) {
                    terminal().writer().println("enabled.");
                } else {
                    terminal().writer().println("disabled.");
                }
            } catch (Exception e) {
                exception = e;
            }
        }

        private void autosuggestion(CommandInput input) {
            String[] argv = input.args();
            try {
                if (argv.length > 0) {
                    String type = argv[0].toLowerCase();
                    if (type.startsWith("his")) {
                        tailtipWidgets.disable();
                        autosuggestionWidgets.enable();
                    } else if (type.startsWith("tai")) {
                        autosuggestionWidgets.disable();
                        autopairWidgets.disable();
                        if (argv.length > 1) {
                            String mode = argv[1].toLowerCase();
                            if (mode.startsWith("tai")) {
                                tailtipWidgets.setTipType(TipType.TAIL_TIP);
                            } else if (mode.startsWith("comp")) {
                                tailtipWidgets.setTipType(TipType.COMPLETER);
                            } else if (mode.startsWith("comb")) {
                                tailtipWidgets.setTipType(TipType.COMBINED);
                            }
                        }
                        tailtipWidgets.enable();
                    } else if (type.startsWith("com")) {
                        autosuggestionWidgets.disable();
                        tailtipWidgets.disable();
                        reader.setAutosuggestion(SuggestionType.COMPLETER);
                    } else if (type.startsWith("non")) {
                        autosuggestionWidgets.disable();
                        tailtipWidgets.disable();
                        reader.setAutosuggestion(SuggestionType.NONE);
                    } else {
                        terminal().writer().println("Usage: autosuggestion history|completer|tailtip|none");
                    }
                } else {
                    if (tailtipWidgets.isEnabled()) {
                        terminal().writer().println("Autosuggestion: tailtip/" + tailtipWidgets.getTipType());
                    } else {
                        terminal().writer().println("Autosuggestion: " + reader.getAutosuggestion());
                    }
                }
            } catch (Exception e) {
                exception = e;
            }
        }

        private List<Completer> defaultCompleter(String command) {
            return Arrays.asList(NullCompleter.INSTANCE);
        }

        private List<Completer> autosuggestionCompleter(String command) {
            List<Completer> out = new ArrayList<>();
            out.add(new ArgumentCompleter(
                    NullCompleter.INSTANCE,
                    new StringsCompleter("history", "completer", "none"),
                    NullCompleter.INSTANCE));
            out.add(new ArgumentCompleter(
                    NullCompleter.INSTANCE,
                    new StringsCompleter("tailtip"),
                    new StringsCompleter("tailtip", "completer", "combined"),
                    NullCompleter.INSTANCE));
            return out;
        }
    }

    /**
     * If you are not using SystemRegistry in your REPL app and want to use DefaultPrinter
     * then you must override:
     * 1) method terminal()
     * 2) method highlightAndPrint(options, exception) if you are going to print exceptions
     * 3) method defaultPrntOptions(skipDefault) if you want to manage configurable printing options
     */
    private static class MyPrinter extends DefaultPrinter {
        private final Terminal terminal;

        public MyPrinter(ConfigurationPath configPath, Terminal terminal) {
            super(configPath);
            this.terminal = terminal;
        }

        @Override
        protected Terminal terminal() {
            return terminal;
        }

        @Override
        protected void highlightAndPrint(Map<String, Object> options, Throwable exception) {
            if (options.getOrDefault("exception", "stack").equals("stack")) {
                exception.printStackTrace();
            } else {
                AttributedStringBuilder asb = new AttributedStringBuilder();
                asb.append(exception.getMessage(), Styles.prntStyle().resolve(".em"));
                asb.toAttributedString().println(terminal());
            }
        }
    }

    public static void main(String[] args) {
        try {
            Completer completer = new ArgumentCompleter(
                    new Completer() {
                        @Override
                        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                            candidates.add(new Candidate("foo11", "foo11", null, "complete cmdDesc", null, null, true));
                            candidates.add(
                                    new Candidate("foo12", "foo12", null, "cmdDesc -names only", null, null, true));
                            candidates.add(new Candidate("foo13", "foo13", null, "-", null, null, true));
                            candidates.add(new Candidate(
                                    "widget", "widget", null, "cmdDesc with short options", null, null, true));
                        }
                    },
                    new StringsCompleter("foo21", "foo22", "foo23"),
                    new Completer() {
                        @Override
                        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                            candidates.add(new Candidate("", "", null, "frequency in MHz", null, null, false));
                        }
                    });

            Terminal terminal = TerminalBuilder.builder().build();
            Parser parser = new DefaultParser();
            //
            // Command registers
            //
            Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
            ConfigurationPath configPath = new ConfigurationPath(Paths.get("."), Paths.get("."));
            Builtins builtins = new Builtins(workDir, configPath, null);
            builtins.rename(Builtins.Command.TTOP, "top");
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");
            Printer printer = new DefaultPrinter(null);
            Printer myPrinter = new MyPrinter(null, terminal);
            ExampleCommands exampleCommands = new ExampleCommands(printer, myPrinter);
            SystemRegistryImpl masterRegistry = new SystemRegistryImpl(parser, terminal, workDir, configPath);
            masterRegistry.setCommandRegistries(exampleCommands, builtins);
            masterRegistry.addCompleter(completer);
            //
            // Terminal & LineReader
            //
            System.out.println(terminal.getName() + ": " + terminal.getType());
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(masterRegistry.completer())
                    .parser(parser)
                    .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                    .variable(LineReader.INDENTATION, 2)
                    .option(Option.INSERT_BRACKET, true)
                    .option(Option.EMPTY_WORD_OPTIONS, false)
                    .build();
            //
            // widgets
            //
            AutopairWidgets autopairWidgets = new AutopairWidgets(reader);
            AutosuggestionWidgets autosuggestionWidgets = new AutosuggestionWidgets(reader);
            TailTipWidgets tailtipWidgets;
            boolean argument = true;
            if (argument) {
                tailtipWidgets = new TailTipWidgets(reader, compileTailTips(), 5, TipType.COMPLETER);
            } else {
                tailtipWidgets = new TailTipWidgets(reader, masterRegistry::commandDescription, 5, TipType.COMPLETER);
            }
            //
            // complete command registers
            //
            builtins.setLineReader(reader);
            exampleCommands.setLineReader(reader);
            exampleCommands.setAutosuggestionWidgets(autosuggestionWidgets);
            exampleCommands.setTailTipWidgets(tailtipWidgets);
            exampleCommands.setAutopairWidgets(autopairWidgets);
            //
            // REPL-loop
            //
            while (true) {
                try {
                    masterRegistry.cleanUp();
                    String line = reader.readLine("prompt> ", null, (MaskingCallback) null, null);
                    masterRegistry.execute(line);
                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    break;
                } catch (Exception | Error e) {
                    masterRegistry.trace(true, e);
                }
            }
            masterRegistry.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.Widgets.AutopairWidgets;
import org.jline.console.Widgets.AutosuggestionWidgets;
import org.jline.console.Widgets.TailTipWidgets;
import org.jline.console.Widgets.TailTipWidgets.TipType;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReader.SuggestionType;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.InfoCmp;
import org.jline.utils.InfoCmp.Capability;

public class Console
{
    private static Map<String,CmdDesc> compileTailTips() {
        Map<String, CmdDesc> tailTips = new HashMap<>();
        Map<String, List<AttributedString>> optDesc = new HashMap<>();
        optDesc.put("--optionA", Arrays.asList(new AttributedString("optionA description...")));
        optDesc.put("--noitpoB", Arrays.asList(new AttributedString("noitpoB description...")));
        optDesc.put("--optionC", Arrays.asList(new AttributedString("optionC description...")
                                             , new AttributedString("line2")));
        Map<String, List<AttributedString>> widgetOpts = new HashMap<>();
        List<AttributedString> mainDesc = Arrays.asList(new AttributedString("widget -N new-widget [function-name]")
                                        , new AttributedString("widget -D widget ...")
                                        , new AttributedString("widget -A old-widget new-widget")
                                        , new AttributedString("widget -U string ...")
                                        , new AttributedString("widget -l [options]")
                       );
        widgetOpts.put("-N", Arrays.asList(new AttributedString("Create new widget")));
        widgetOpts.put("-D", Arrays.asList(new AttributedString("Delete widgets")));
        widgetOpts.put("-A", Arrays.asList(new AttributedString("Create alias to widget")));
        widgetOpts.put("-U", Arrays.asList(new AttributedString("Push characters to the stack")));
        widgetOpts.put("-l", Arrays.asList(new AttributedString("List user-defined widgets")));

        tailTips.put("widget", new CmdDesc(mainDesc, ArgDesc.doArgNames(Arrays.asList("[pN...]")), widgetOpts));
        tailTips.put("foo12", new CmdDesc(ArgDesc.doArgNames(Arrays.asList("param1", "param2", "[paramN...]"))));
        tailTips.put("foo11", new CmdDesc(Arrays.asList(
                new ArgDesc("param1",Arrays.asList(new AttributedString("Param1 description...")
                                                , new AttributedString("line 2: This is a very long line that does exceed the terminal width."
                                                      +" The line will be truncated automatically (by Status class) before printing out.")
                                                , new AttributedString("line 3")
                                                , new AttributedString("line 4")
                                                , new AttributedString("line 5")
                                                , new AttributedString("line 6")
                                                  ))
              , new ArgDesc("param2",Arrays.asList(new AttributedString("Param2 description...")
                                                , new AttributedString("line 2")
                                                  ))
              , new ArgDesc("param3", new ArrayList<>())
              ), optDesc));
        return tailTips;
    }


    private static class ExampleCommands implements CommandRegistry {
        private LineReader reader;
        private AutosuggestionWidgets autosuggestionWidgets;
        private TailTipWidgets tailtipWidgets;
        private AutopairWidgets autopairWidgets;
        private final Map<String,CommandMethods> commandExecute = new HashMap<>();
        private final Map<String,List<String>> commandInfo = new HashMap<>();
        private Map<String,String> aliasCommand = new HashMap<>();
        private Exception exception;

        public ExampleCommands() {
            commandExecute.put("tput", new CommandMethods(this::tput, this::tputCompleter));
            commandExecute.put("testkey", new CommandMethods(this::testkey, this::defaultCompleter));
            commandExecute.put("clear", new CommandMethods(this::clear, this::defaultCompleter));
            commandExecute.put("sleep", new CommandMethods(this::sleep, this::defaultCompleter));
            commandExecute.put("autopair", new CommandMethods(this::autopair, this::defaultCompleter));
            commandExecute.put("autosuggestion", new CommandMethods(this::autosuggestion, this::autosuggestionCompleter));

            commandInfo.put("tput", Arrays.asList("set terminal capability"));
            commandInfo.put("testkey", Arrays.asList("display key events"));
            commandInfo.put("clear", Arrays.asList("clear screen"));
            commandInfo.put("sleep", Arrays.asList("sleep 3 seconds"));
            commandInfo.put("autopair", Arrays.asList("toggle brackets/quotes autopair key bindings"));
            commandInfo.put("autosuggestion", Arrays.asList("set autosuggestion modality: history, completer, tailtip or none"));
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
            if (commandExecute.containsKey(command) || aliasCommand.containsKey(command)) {
                return true;
            }
            return false;
        }

        private String command(String name) {
            if (commandExecute.containsKey(name)) {
                return name;
            } else if (aliasCommand.containsKey(name)) {
                return aliasCommand.get(name);
            }
            return null;
        }

        public SystemCompleter compileCompleters() {
            SystemCompleter out = new SystemCompleter();
            for (String c : commandExecute.keySet()) {
                out.add(c, commandExecute.get(c).compileCompleter().apply(c));
            }
            out.addAliases(aliasCommand);
            return out;
        }

        public Object execute(CommandRegistry.CommandSession session, String command, String[] args) throws Exception {
            exception = null;
            commandExecute.get(command(command)).execute().accept(new CommandInput(command, args, session));
            if (exception != null) {
                throw exception;
            }
            return null;
        }

        public CmdDesc commandDescription(String command) {
            // TODO
            return new CmdDesc(false);
        }

        private void tput(CommandInput input) {
            String[] argv = input.args();
            try {
                if (argv.length == 1 && !argv[0].equals("--help") && !argv[0].equals("-?")) {
                    Capability vcap = Capability.byName(argv[0]);
                    if (vcap != null) {
                        terminal().puts(vcap);
                    } else {
                        terminal().writer().println("Unknown capability");
                    }
                } else {
                    terminal().writer().println("Usage: tput <capability>");
                }
            } catch (Exception e) {
                exception = e;
            }
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

        private void sleep(CommandInput input) {
            try {
                Thread.sleep(3000);
            } catch (Exception e) {
                exception = e;
            }
        }

        private void autopair(CommandInput input) {
            try {
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
                        tailtipWidgets.enable();
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

        private Set<String> capabilities() {
            return InfoCmp.getCapabilitiesByName().keySet();
        }

        private List<Completer> tputCompleter(String command) {
            return Arrays.asList(new ArgumentCompleter(NullCompleter.INSTANCE
                                                     , new StringsCompleter(this::capabilities)
                                                   ));
        }

        private List<Completer> autosuggestionCompleter(String command) {
            List<Completer> out = new ArrayList<>();
            out.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                        , new StringsCompleter("history", "completer", "none")
                                        , NullCompleter.INSTANCE));
            out.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                        , new StringsCompleter("tailtip")
                                        , new StringsCompleter("tailtip", "completer", "combined")
                                        , NullCompleter.INSTANCE));
            return out;
        }
    }

    private static Path workDir() {
        return Paths.get(System.getProperty("user.dir"));
    }

    public static void main(String[] args) throws IOException {
        try {
            String prompt = "prompt> ";
            String rightPrompt = null;

            boolean argument = true;
            Completer completer = new ArgumentCompleter(new Completer() {
                @Override
                public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                    candidates.add(new Candidate("foo11", "foo11", null, "complete cmdDesc", null, null, true));
                    candidates.add(new Candidate("foo12", "foo12", null, "cmdDesc -names only", null, null, true));
                    candidates.add(new Candidate("foo13", "foo13", null, "-", null, null, true));
                    candidates.add(
                            new Candidate("widget", "widget", null, "cmdDesc with short options", null, null, true));
                }
            }, new StringsCompleter("foo21", "foo22", "foo23"), new Completer() {
                @Override
                public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
                    candidates.add(new Candidate("", "", null, "frequency in MHz", null, null, false));
                }
            });

            Terminal terminal = TerminalBuilder.builder().build();
            Parser parser = new DefaultParser();
            //
            // Command registeries
            //
            Builtins builtins = new Builtins(Console::workDir, null, null);
            builtins.rename(Builtins.Command.TTOP, "top");
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");
            ExampleCommands exampleCommands = new ExampleCommands();
            SystemRegistryImpl masterRegistry = new SystemRegistryImpl(parser, terminal, Console::workDir, null);
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
            TailTipWidgets tailtipWidgets = null;
            if (argument) {
                tailtipWidgets = new TailTipWidgets(reader, compileTailTips(), 5, TipType.COMPLETER);
            } else {
                tailtipWidgets = new TailTipWidgets(reader, masterRegistry::commandDescription, 5, TipType.COMPLETER);
            }
            //
            // complete command registeries
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
                    String line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                    masterRegistry.execute(line);
                }
                catch (UserInterruptException e) {
                    // Ignore
                }
                catch (EndOfFileException e) {
                    break;
                }
                catch (Exception e) {
                    masterRegistry.trace(e);
                }
            }
            masterRegistry.close();
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
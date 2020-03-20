/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jline.builtins.Builtins;
import org.jline.builtins.CommandRegistry;
import org.jline.builtins.Completers;
import org.jline.builtins.ConsoleEngine;
import org.jline.builtins.ConsoleEngineImpl;
import org.jline.builtins.Options;
import org.jline.builtins.Completers.OptDesc;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.Completers.SystemCompleter;
import org.jline.builtins.Options.HelpException;
import org.jline.builtins.SystemRegistryImpl;
import org.jline.builtins.Widgets;
import org.jline.builtins.Widgets.TailTipWidgets;
import org.jline.builtins.Widgets.TailTipWidgets.TipType;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Completer;
import org.jline.reader.ConfigurationPath;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.DefaultParser.Bracket;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.script.GroovyEngine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.OSUtils;

/**
 * Demo how to create REPL app with JLine.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class Repl {

    private static class SubCommands implements CommandRegistry {
        private final Map<String,Builtins.CommandMethods> commandExecute = new HashMap<>();
        private Exception exception;

        public SubCommands() {
            commandExecute.put("cmd1", new Builtins.CommandMethods(this::cmd1, this::defaultCompleter));
            commandExecute.put("cmd2", new Builtins.CommandMethods(this::cmd2, this::defaultCompleter));
            commandExecute.put("cmd3", new Builtins.CommandMethods(this::cmd3, this::defaultCompleter));
            commandExecute.put("help", new Builtins.CommandMethods(this::help, this::defaultCompleter));
        }

        public Set<String> commandNames() {
            return commandExecute.keySet();
        }

        @Override
        public Map<String, String> commandAliases() {
            return new HashMap<>();
        }

        public boolean hasCommand(String command) {
            return commandExecute.containsKey(command);
        }

        private String command(String name) {
            String out = name;
            if (name.equals("-?") || name.equals("--help")) {
                out = "help";
            }
            if (!hasCommand(out)) {
                throw new IllegalArgumentException("Unknown command: " + name);
            }
            return out;
        }

        @Override
        public Widgets.CmdDesc commandDescription(String command) {
            Widgets.CmdDesc out = new Widgets.CmdDesc();
            if (!hasCommand(command)) {
                return out;
            }
            try {
                Object[] args = new Object[] {};
                if (!command.equals("help")) {
                    args = new Object[] {"--help"};
                }
                invoke(new CommandSession(), command, args);
            } catch (HelpException e) {
                out = Builtins.compileCommandDescription(e.getMessage());
            } catch (Exception e) {
            }
            return out;
        }

        private Object cmd1(Builtins.CommandInput input) {
            final String[] usage = {
                    "cmd1 -  cmd1 parse input.args, return opt.argObjects[0]",
                    "Usage: cmd1 [OBJECT]",
                    "  -? --help                       Displays command help"
            };
            Options opt = Options.compile(usage).parse(input.args());
            if (opt.isSet("help")) {
                exception = new HelpException(opt.usage());
                return null;
            }
            List<Object> xargs = opt.argObjects();
            return xargs.size() > 0 ? xargs.get(0) : null;
        }

        private Object cmd2(Builtins.CommandInput input) {
            final String[] usage = {
                    "cmd2 -  cmd2 parse input.xargs, return opt.args[0]",
                    "Usage: cmd2",
                    "  -? --help                       Displays command help"
            };
            Options opt = Options.compile(usage).parse(input.xargs());
            if (opt.isSet("help")) {
                exception = new HelpException(opt.usage());
                return null;
            }
            List<String> args = opt.args();
            return args.size() > 0 ? args.get(0) : null;
        }

        private Object cmd3(Builtins.CommandInput input) {
            final String[] usage = {
                    "cmd3 -  cmd3 parse input.xargs, return opt.argObjects[0]",
                    "Usage: cmd3 [OBJECT]",
                    "  -? --help                       Displays command help"
            };
            Options opt = Options.compile(usage).parse(input.xargs());
            if (opt.isSet("help")) {
                exception = new HelpException(opt.usage());
                return null;
            }
            List<Object> xargs = opt.argObjects();
            return xargs.size() > 0 ? xargs.get(0) : null;
        }

        private Object help(Builtins.CommandInput input) {
            final String[] usage = {
                    " -  demonstrates object parameter usages. ",
                    "    cmd3 manage correctly object parameters",
                    "    while cmd1 & cmd2 works only with string parameters",
                    "Summary: " + commandInfo("cmd1").get(0),
                    "         " + commandInfo("cmd2").get(0),
                    "         " + commandInfo("cmd3").get(0),
                    "         help show subcommands help"
            };
            Options opt = Options.compile(usage).parse(input.args());
            exception = new HelpException(opt.usage());
            return null;
        }

        @Override
        public Object invoke(CommandSession session, String command, Object... args) throws Exception {
            exception = null;
            Object out = commandExecute.get(command(command)).executeFunction().apply(new Builtins.CommandInput(command, null, args, session));
            if (exception != null) {
                throw exception;
            }
            return out;
        }

        public Completers.SystemCompleter compileCompleters() {
            SystemCompleter out = new SystemCompleter();
            for (String c : commandExecute.keySet()) {
                out.add(c, commandExecute.get(c).compileCompleter().apply(c));
            }
            return out;
        }

        private List<OptDesc> commandOptions(String command) {
            try {
                invoke(new CommandRegistry.CommandSession(), command, new Object[] {"--help"});
            } catch (HelpException e) {
                return Builtins.compileCommandOptions(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private List<Completer> defaultCompleter(String command) {
            List<Completer> completers = new ArrayList<>();
            completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                               , new OptionCompleter(NullCompleter.INSTANCE
                                                                   , this::commandOptions
                                                                   , 1)
                                                ));
            return completers;
        }

    }

    private static class MyCommands implements CommandRegistry {
        private LineReader reader;
        private final Map<String,Builtins.CommandMethods> commandExecute = new HashMap<>();
        private Map<String,String> aliasCommand = new HashMap<>();
        private Exception exception;
        private Supplier<Path> workDir;

        public MyCommands(Supplier<Path> workDir) {
            this.workDir = workDir;
            commandExecute.put("tput", new Builtins.CommandMethods(this::tput, this::tputCompleter));
            commandExecute.put("testkey", new Builtins.CommandMethods(this::testkey, this::defaultCompleter));
            commandExecute.put("clear", new Builtins.CommandMethods(this::clear, this::defaultCompleter));
            commandExecute.put("!", new Builtins.CommandMethods(this::shell, this::defaultCompleter));
        }

        public void setLineReader(LineReader reader) {
            this.reader = reader;
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

        public Completers.SystemCompleter compileCompleters() {
            SystemCompleter out = new SystemCompleter();
            for (String c : commandExecute.keySet()) {
                out.add(c, commandExecute.get(c).compileCompleter().apply(c));
            }
            out.addAliases(aliasCommand);
            return out;
        }

        public Object execute(CommandRegistry.CommandSession session, String command, String[] args) throws Exception {
            exception = null;
            commandExecute.get(command(command)).execute().accept(new Builtins.CommandInput(command, args, session));
            if (exception != null) {
                throw exception;
            }
            return null;
        }

        private void tput(Builtins.CommandInput input) {
            final String[] usage = {
                    "tput -  put terminal capability",
                    "Usage: tput [CAPABILITY]",
                    "  -? --help                       Displays command help"
            };
            Options opt = Options.compile(usage).parse(input.args());
            if (opt.isSet("help")) {
                exception = new HelpException(opt.usage());
                return;
            }

            List<String> argv = opt.args();
            try {
                if (argv.size() == 1) {
                    Capability vcap = Capability.byName(argv.get(0));
                    if (vcap != null) {
                        terminal().puts(vcap);
                    } else {
                        terminal().writer().println("Unknown capability");
                    }
                } else {
                    terminal().writer().println("Usage: tput [CAPABILITY]");
                }
            } catch (Exception e) {
                exception = e;
            }
        }

        private void testkey(Builtins.CommandInput input) {
            final String[] usage = {
                    "testkey -  display the key events",
                    "Usage: testkey",
                    "  -? --help                       Displays command help"
            };
            Options opt = Options.compile(usage).parse(input.args());
            if (opt.isSet("help")) {
                exception = new HelpException(opt.usage());
                return;
            }
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

        private void clear(Builtins.CommandInput input) {
            final String[] usage = {
                    "clear -  clear terminal",
                    "Usage: clear",
                    "  -? --help                       Displays command help"
            };
            Options opt = Options.compile(usage).parse(input.args());
            if (opt.isSet("help")) {
                exception = new HelpException(opt.usage());
                return;
            }
            try {
                terminal().puts(Capability.clear_screen);
                terminal().flush();
            } catch (Exception e) {
                exception = e;
            }
        }

        private void executeCmnd(List<String> args) throws Exception {
            ProcessBuilder builder = new ProcessBuilder();
            List<String> _args = new ArrayList<>();
            if (OSUtils.IS_WINDOWS) {
                _args.add("cmd.exe");
                _args.add("/c");
            } else {
                _args.add("sh");
                _args.add("-c");
            }
            _args.add(args.stream().collect(Collectors.joining(" ")));
            builder.command(_args);
            builder.directory(workDir.get().toFile());
            Process process = builder.start();
            StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
            new Thread(streamGobbler).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new Exception("Failed to execute: " + String.join(" ", args.subList(2, args.size())));
            }
        }

        private void shell(Builtins.CommandInput input) {
            final String[] usage = { "!<command> -  execute shell command"
                                   , "Usage: !<command>"
                                   , "  -? --help                       Displays command help" };
            try {
                Options opt = Options.compile(usage).parse(input.args());
                if (opt.isSet("help") && opt.args().isEmpty()) {
                    exception = new HelpException(opt.usage());
                    return;
                }
            } catch (Exception e) {
                // ignore
            }
            List<String> argv = new ArrayList<>();
            argv.addAll(Arrays.asList(input.args()));
            if (!argv.isEmpty()) {
                try {
                    executeCmnd(argv);
                } catch (Exception e) {
                    exception = e;
                }
            }
        }

        private List<OptDesc> commandOptions(String command) {
            try {
                execute(new CommandRegistry.CommandSession(), command, new String[] {"--help"});
            } catch (HelpException e) {
                return Builtins.compileCommandOptions(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private List<Completer> defaultCompleter(String command) {
            List<Completer> completers = new ArrayList<>();
            completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                               , new OptionCompleter(NullCompleter.INSTANCE
                                                                   , this::commandOptions
                                                                   , 1)
                                                ));
            return completers;
        }

        private Set<String> capabilities() {
            return InfoCmp.getCapabilitiesByName().keySet();
        }

        private List<Completer> tputCompleter(String command) {
            List<Completer> completers = new ArrayList<>();
            completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                               , new OptionCompleter(new StringsCompleter(this::capabilities)
                                                                   , this::commandOptions
                                                                   , 1)
                                                ));
            return completers;
        }

    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
              .forEach(consumer);
        }
    }

    private static Path workDir() {
        return Paths.get(System.getProperty("user.dir"));
    }

    public static void main(String[] args) {
        try {
            //
            // Parser & Terminal
            //
            DefaultParser parser = new DefaultParser();
            parser.setEofOnUnclosedBracket(Bracket.CURLY, Bracket.ROUND, Bracket.SQUARE);
            parser.setEofOnUnclosedQuote(true);
            parser.setEscapeChars(null);
            parser.setRegexCommand("[:]{0,1}[a-zA-Z!]{1,}\\S*");    // change default regex to support shell commands
            Terminal terminal = TerminalBuilder.builder().build();
            //
            // ScriptEngine and command registeries
            //
            File file = new File(Repl.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            String root = file.getCanonicalPath().replace("classes", "").replaceAll("\\\\", "/"); // forward slashes works better also in windows!
            GroovyEngine scriptEngine = new GroovyEngine();
            scriptEngine.put("ROOT", root);
            ConfigurationPath configPath = new ConfigurationPath(Paths.get(root), Paths.get(root));
            ConsoleEngine consoleEngine = new ConsoleEngineImpl(scriptEngine, Repl::workDir, configPath);
            Builtins builtins = new Builtins(Repl::workDir, configPath,  (String fun)-> {return new ConsoleEngine.WidgetCreator(consoleEngine, fun);});
            MyCommands myCommands = new MyCommands(Repl::workDir);
            SystemRegistryImpl systemRegistry = new SystemRegistryImpl(parser, terminal, configPath);
            systemRegistry.register("command", new SubCommands());
            systemRegistry.setCommandRegistries(consoleEngine, builtins, myCommands);
            //
            // LineReader
            //
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(systemRegistry.completer())
                    .parser(parser)
                    .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                    .variable(LineReader.INDENTATION, 2)
                    .variable(LineReader.LIST_MAX, 100)
                    .variable(LineReader.HISTORY_FILE, Paths.get(root, "history"))
                    .option(Option.INSERT_BRACKET, true)
                    .option(Option.EMPTY_WORD_OPTIONS, false)
                    .option(Option.USE_FORWARD_SLASH, true)             // use forward slash in directory separator
                    .option(Option.DISABLE_EVENT_EXPANSION, true)
                    .build();
            if (OSUtils.IS_WINDOWS) {
                reader.setVariable(LineReader.BLINK_MATCHING_PAREN, 0); // if enabled cursor remains in begin parenthesis (gitbash)
            }
            //
            // complete command registeries
            //
            consoleEngine.setLineReader(reader);
            builtins.setLineReader(reader);
            myCommands.setLineReader(reader);
            //
            // widgets and console initialization
            //
            TailTipWidgets ttw = new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TipType.COMPLETER);
            ttw.setDescriptionCache(false);
            KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
            keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));
            systemRegistry.initialize(Paths.get(root, "init.jline").toFile());
            //
            // REPL-loop
            //
            consoleEngine.println(terminal.getName()+": "+terminal.getType());
            while (true) {
                try {
                    systemRegistry.cleanUp();         // delete temporary variables and reset output streams
                    String line = reader.readLine("groovy-repl> ");
                    line = parser.getCommand(line).startsWith("!") ? line.replaceFirst("!", "! ") : line;
                    Object result = systemRegistry.execute(line);
                    consoleEngine.println(result);
                }
                catch (UserInterruptException e) {
                    // Ignore
                }
                catch (EndOfFileException e) {
                    break;
                }
                catch (Exception e) {
                    systemRegistry.trace(e);          // print exception and save it to console variable
                }
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

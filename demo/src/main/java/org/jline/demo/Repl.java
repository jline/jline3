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
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.utils.InfoCmp;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.OSUtils;

/**
 * Demo how to create REPL app with JLine.
 *
 * @author <a href="mailto:matti.rintanikkola@gmail.com">Matti Rinta-Nikkola</a>
 */
public class Repl {

    /**
     * Command execute and tab completer compiler methods for CommandRegistry
     * which have commands that will return execution value
     */
    private static abstract class ObjectCommand extends AbstractCommandRegistry {

        public ObjectCommand() {
            super();
        }

        public Object invoke(CommandRegistry.CommandSession session, String command, Object... args) throws Exception {
            exception = null;
            Object out = commandExecute.get(command(command)).executeFunction().apply(new Builtins.CommandInput(command, args, session));
            if (exception != null) {
                throw exception;
            }
            return out;
        }

        protected List<OptDesc> commandOptions(String command) {
            try {
                invoke(new CommandRegistry.CommandSession(), command, new Object[] {"--help"});
            } catch (HelpException e) {
                return Builtins.compileCommandOptions(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected List<Completer> defaultCompleter(String command) {
            List<Completer> completers = new ArrayList<>();
            completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                               , new OptionCompleter(NullCompleter.INSTANCE
                                                                   , this::commandOptions
                                                                   , 1)
                                                ));
            return completers;
        }

    }

    /**
     * Command execute and tab completer compilers methods for CommandRegistry
     * which have commands that will not return execution value
     */
    private static abstract class VoidCommand extends AbstractCommandRegistry {

        public VoidCommand() {
            super();
        }

        public Object execute(CommandRegistry.CommandSession session, String command, String[] args) throws Exception {
            exception = null;
            commandExecute.get(command(command)).execute().accept(new Builtins.CommandInput(command, args, session));
            if (exception != null) {
                throw exception;
            }
            return null;
        }

        protected List<OptDesc> commandOptions(String command) {
            try {
                execute(new CommandRegistry.CommandSession(), command, new String[] {"--help"});
            } catch (HelpException e) {
                return Builtins.compileCommandOptions(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        protected List<Completer> defaultCompleter(String command) {
            List<Completer> completers = new ArrayList<>();
            completers.add(new ArgumentCompleter(NullCompleter.INSTANCE
                                               , new OptionCompleter(NullCompleter.INSTANCE
                                                                   , this::commandOptions
                                                                   , 1)
                                                ));
            return completers;
        }

    }

    /**
     *
     *
     */
    private static abstract class AbstractCommandRegistry {
        protected final Map<String,Builtins.CommandMethods> commandExecute = new HashMap<>();
        protected Exception exception;
        protected Map<String,String> aliasCommand = new HashMap<>();

        public AbstractCommandRegistry() {}

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

        protected String command(String name) {
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

        public Options parseOptions(String[] usage, Object[] args) throws HelpException {
            Options opt = Options.compile(usage).parse(args);
            if (opt.isSet("help")) {
                throw new HelpException(opt.usage());
            }
            return opt;
        }

    }

    /**
     * CommandRegistry that have commands which manage in different way command parameters.
     *
     */
    private static class SubCommands extends ObjectCommand implements CommandRegistry {

        public SubCommands() {
            super();
            commandExecute.put("cmdKo1", new Builtins.CommandMethods(this::cmd1, this::defaultCompleter));
            commandExecute.put("cmdKo2", new Builtins.CommandMethods(this::cmd2, this::defaultCompleter));
            commandExecute.put("cmdOk", new Builtins.CommandMethods(this::cmd3, this::defaultCompleter));
        }

        private Object cmd1(Builtins.CommandInput input) {
            final String[] usage = {
                    "cmdKo1 -  parse input.args, return opt.argObjects[0]",
                    "          works only with string parameters",
                    "Usage: cmdKo1 [OBJECT]",
                    "  -? --help                       Displays command help"
            };
            Object out = null;
            try {
                Options opt = parseOptions(usage, input.args());
                List<Object> xargs = opt.argObjects();
                out = xargs.size() > 0 ? xargs.get(0) : null;
            } catch (Exception e) {
                exception = e;
            }
            return out;
        }

        private Object cmd2(Builtins.CommandInput input) {
            final String[] usage = {
                    "cmdKo2 -  parse input.xargs, return opt.args[0]",
                    "          works only with string parameters",
                    "Usage: cmdKo2 [OBJECT]",
                    "  -? --help                       Displays command help"
            };
            Object out = null;
            try {
                Options opt = parseOptions(usage, input.xargs());
                List<String> args = opt.args();
                out = args.size() > 0 ? args.get(0) : null;
            } catch (Exception e) {
                exception = e;
            }
            return out;
        }

        private Object cmd3(Builtins.CommandInput input) {
            final String[] usage = {
                    "cmdOk -  parse input.xargs, return opt.argObjects[0]",
                    "         manage correctly object parameters",
                    "Usage: cmdOk [OBJECT]",
                    "  -? --help                       Displays command help"
            };
            Object out = null;
            try {
                Options opt = parseOptions(usage, input.xargs());
                List<Object> xargs = opt.argObjects();
                out = xargs.size() > 0 ? xargs.get(0) : null;
            } catch (Exception e) {
                exception = e;
            }
            return out;
        }

    }

    private static class MyCommands extends VoidCommand implements CommandRegistry {
        private LineReader reader;
        private Supplier<Path> workDir;

        public MyCommands(Supplier<Path> workDir) {
            super();
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

        private void tput(Builtins.CommandInput input) {
            final String[] usage = {
                    "tput -  put terminal capability",
                    "Usage: tput [CAPABILITY]",
                    "  -? --help                       Displays command help"
            };
            try {
                Options opt = parseOptions(usage, input.args());
                List<String> argv = opt.args();
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
            try {
                parseOptions(usage, input.args());
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
            try {
                parseOptions(usage, input.args());
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
                parseOptions(usage, input.args());
            } catch (HelpException e) {
                exception = e;
                return;
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
            Thread executeThread = Thread.currentThread();
            terminal.handle(Signal.INT, signal -> executeThread.interrupt());
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
            SystemRegistryImpl systemRegistry = new SystemRegistryImpl(parser, terminal, Repl::workDir, configPath);
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
            new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TipType.COMPLETER);
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
            systemRegistry.close();                   // persist pipeline completer names etc
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
}

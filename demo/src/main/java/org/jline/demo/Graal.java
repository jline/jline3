package org.jline.demo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jline.builtins.*;
import org.jline.builtins.Completers.OptionCompleter;
import org.jline.builtins.Widgets.TailTipWidgets;
import org.jline.builtins.Widgets.TailTipWidgets.TipType;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.ConfigurationPath;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.DefaultParser.Bracket;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Terminal.Signal;
import org.jline.utils.InfoCmp;
import org.jline.utils.OSUtils;
import org.jline.utils.InfoCmp.Capability;

public class Graal {
    private static class MyCommands extends JlineCommandRegistry implements CommandRegistry {
        private LineReader reader;
        private Supplier<Path> workDir;

        public MyCommands(Supplier<Path> workDir) {
            super();
            this.workDir = workDir;
            Map<String,CommandMethods> commandExecute = new HashMap<>();
            commandExecute.put("tput", new CommandMethods(this::tput, this::tputCompleter));
            commandExecute.put("testkey", new CommandMethods(this::testkey, this::defaultCompleter));
            commandExecute.put("clear", new CommandMethods(this::clear, this::defaultCompleter));
            commandExecute.put("!", new CommandMethods(this::shell, this::defaultCompleter));
            commandExecute.put("objarg", new CommandMethods(this::objarg, this::defaultCompleter));
            registerCommands(commandExecute);
        }

        public void setLineReader(LineReader reader) {
            this.reader = reader;
        }

        private Terminal terminal() {
            return reader.getTerminal();
        }

        private Object objarg(CommandInput input) {
            final String[] usage = {
                    "objarg -  manage correctly object parameters",
                    "         parse input.xargs, return opt.argObjects[0]",
                    "Usage: objarg [OBJECT]",
                    "  -? --help                       Displays command help"
            };
            Object out = null;
            try {
                Options opt = parseOptions(usage, input.xargs());
                List<Object> xargs = opt.argObjects();
                out = xargs.size() > 0 ? xargs.get(0) : null;
            } catch (Exception e) {
                saveException(e);
            }
            return out;
        }

        private void tput(CommandInput input) {
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
                saveException(e);
            }
        }

        private void testkey(CommandInput input) {
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
                saveException(e);
            }
        }

        private void clear(CommandInput input) {
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
                saveException(e);
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

        private void shell(CommandInput input) {
            final String[] usage = { "!<command> -  execute shell command"
                                   , "Usage: !<command>"
                                   , "  -? --help                       Displays command help" };
            if (input.args().length == 1 && (input.args()[0].equals("-?") || input.args()[0].equals("--help"))) {
                try {
                    parseOptions(usage, input.args());
                } catch (Exception e) {
                    saveException(e);
                }
            } else {
                List<String> argv = new ArrayList<>();
                argv.addAll(Arrays.asList(input.args()));
                if (!argv.isEmpty()) {
                    try {
                        executeCmnd(argv);
                    } catch (Exception e) {
                        saveException(e);
                    }
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
            File file = new File(Graal.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            String root = file.getCanonicalPath().replace("classes", "").replaceAll("\\\\", "/"); // forward slashes works better also in windows!
            ConfigurationPath configPath = new ConfigurationPath(Paths.get(root), Paths.get(root));
            Builtins builtins = new Builtins(Graal::workDir, configPath, null);
            MyCommands myCommands = new MyCommands(Graal::workDir);
            SystemRegistryImpl systemRegistry = new SystemRegistryImpl(parser, terminal, Graal::workDir, configPath);
            systemRegistry.setCommandRegistries(builtins, myCommands);
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
            builtins.setLineReader(reader);
            myCommands.setLineReader(reader);
            //
            // widgets and console initialization
            //
            new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TipType.COMPLETER);
            KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
            keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));
            //
            // REPL-loop
            //
            System.out.println(terminal.getName()+": "+terminal.getType());
            while (true) {
                try {
                    systemRegistry.cleanUp();         // delete temporary variables and reset output streams
                    String line = reader.readLine("groovy-repl> ");
                    line = parser.getCommand(line).startsWith("!") ? line.replaceFirst("!", "! ") : line;
                    Object result = systemRegistry.execute(line);
                    if (result != null) {
                        System.out.println(result);
                    }
                }
                catch (UserInterruptException e) {
                    // Ignore
                }
                catch (EndOfFileException e) {
                    break;
                }
                catch (Exception e) {
                    systemRegistry.trace(true, e);    // print exception and save it to console variable
                }
            }
            systemRegistry.close();                   // persist pipeline completer names etc
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

}

/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.jline.console.impl.Builtins;
import org.jline.console.impl.Builtins.Command;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.Widgets.TailTipWidgets;
import org.jline.console.Widgets.TailTipWidgets.TipType;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.DefaultParser.Bracket;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Terminal.Signal;
import org.jline.utils.OSUtils;

public class Graal {

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
            // Command registeries
            //
            File file = new File(Graal.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            String root = file.getCanonicalPath().replace("graal", "").replaceAll("\\\\", "/"); // forward slashes works better also in windows!
            ConfigurationPath configPath = new ConfigurationPath(Paths.get(root), Paths.get(root));
            Set<Builtins.Command> commands = new HashSet<>(Arrays.asList(Builtins.Command.values()));
            commands.remove(Command.TTOP);                          // ttop command is not supported in GraalVM
            Builtins builtins = new Builtins(commands, Graal::workDir, configPath, null);
            Repl.MyCommands myCommands = new Repl.MyCommands(Graal::workDir);
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
            System.out.println(terminal.getName() + ": " + terminal.getType());
            while (true) {
                try {
                    systemRegistry.cleanUp();         // delete temporary variables and reset output streams
                    String line = reader.readLine("graal> ");
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

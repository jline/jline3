/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.graal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;

import org.jline.console.impl.Builtins;
import org.jline.console.impl.Builtins.Command;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.builtins.ConfigurationPath;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.LineReader.Option;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.Terminal.Signal;
import org.jline.utils.OSUtils;
import org.jline.widget.TailTipWidgets;
import org.jline.widget.TailTipWidgets.TipType;
import org.jline.widget.Widgets;

public class Graal {

    public static void main(String[] args) {
        try {
            Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
            //
            // Parser & Terminal
            //
            DefaultParser parser = new DefaultParser();
            parser.setEofOnUnclosedQuote(true);
            parser.setEscapeChars(null);
            parser.setRegexVariable(null);                          // we do not have console variables!
            Terminal terminal = TerminalBuilder.builder().build();
            Thread executeThread = Thread.currentThread();
            terminal.handle(Signal.INT, signal -> executeThread.interrupt());
            //
            // Command registries
            //
            File file = new File(Graal.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            String root = file.getCanonicalPath();
            root = root.substring(0, root.length() - 6);
            ConfigurationPath configPath = new ConfigurationPath(Paths.get(root), Paths.get(root));
            Set<Builtins.Command> commands = new HashSet<>(Arrays.asList(Builtins.Command.values()));
            commands.remove(Command.TTOP);                          // ttop command is not supported in GraalVM
            Builtins builtins = new Builtins(commands, workDir, configPath, null);
            SystemRegistryImpl systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, configPath);
            systemRegistry.setCommandRegistries(builtins);
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
            // complete command registries
            //
            builtins.setLineReader(reader);
            //
            // widgets and console initialization
            //
            new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TipType.COMPLETER);
            KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
            keyMap.bind(new Reference(Widgets.TAILTIP_TOGGLE), KeyMap.alt("s"));
            //
            // REPL-loop
            //
            System.out.println(terminal.getName() + ": " + terminal.getType());
            while (true) {
                try {
                    systemRegistry.cleanUp();            // reset output streams
                    String line = reader.readLine("graal> ");
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
                    systemRegistry.trace(true, e);    // print exception
                }
            }
            systemRegistry.close();
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

}

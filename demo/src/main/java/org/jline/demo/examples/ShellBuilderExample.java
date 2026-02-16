/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.nio.file.Paths;
import java.util.*;

import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.console.Shell;
import org.jline.console.impl.JlineCommandRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;

/**
 * Example demonstrating how Shell.builder() eliminates REPL boilerplate.
 * <p>
 * Compare this with {@link ReplConsoleExample} to see the reduction in setup code.
 */
public class ShellBuilderExample {

    // SNIPPET_START: ShellBuilderExample
    /**
     * A simple command registry for the demo.
     */
    static class MyCommands extends JlineCommandRegistry implements CommandRegistry {
        private LineReader reader;

        @SuppressWarnings("this-escape")
        MyCommands() {
            super();
            Map<String, CommandMethods> commandExecute = new HashMap<>();
            commandExecute.put("echo", new CommandMethods(this::echo, this::defaultCompleter));
            commandExecute.put("greet", new CommandMethods(this::greet, this::defaultCompleter));
            registerCommands(commandExecute);
        }

        @Override
        public void setLineReader(LineReader reader) {
            this.reader = reader;
        }

        private void echo(CommandInput input) {
            final String[] usage = {
                "echo - echo arguments",
                "Usage: echo [MESSAGE...]",
                "  -? --help                       Displays command help"
            };
            try {
                org.jline.builtins.Options opt = parseOptions(usage, input.args());
                if (!opt.args().isEmpty()) {
                    reader.getTerminal().writer().println(String.join(" ", opt.args()));
                }
            } catch (Exception e) {
                saveException(e);
            }
        }

        private void greet(CommandInput input) {
            final String[] usage = {
                "greet - greet someone",
                "Usage: greet [NAME]",
                "  -? --help                       Displays command help"
            };
            try {
                org.jline.builtins.Options opt = parseOptions(usage, input.args());
                String name = opt.args().isEmpty() ? "World" : opt.args().get(0);
                reader.getTerminal().writer().println("Hello, " + name + "!");
            } catch (Exception e) {
                saveException(e);
            }
        }
    }

    public static void main(String[] args) {
        try (Shell shell = Shell.builder()
                .prompt("demo> ")
                .commands(new MyCommands())
                .variable(LineReader.HISTORY_FILE, Paths.get(System.getProperty("user.home"), ".demo_history"))
                .option(Option.INSERT_BRACKET, true)
                .option(Option.DISABLE_EVENT_EXPANSION, true)
                .build()) {
            shell.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: ShellBuilderExample
}

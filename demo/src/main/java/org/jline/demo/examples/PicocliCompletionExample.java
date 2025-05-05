/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.shell.jline3.PicocliJLineCompleter;

/**
 * Example demonstrating integration of JLine with Picocli including tab completion.
 */
public class PicocliCompletionExample {

    // SNIPPET_START: PicocliCompletionExample
    public static void main(String[] args) {
        try {
            // Set up the terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Create the command line parser
            RootCommand rootCommand = new RootCommand(terminal);
            CommandLine commandLine = new CommandLine(rootCommand);

            // Add subcommands
            commandLine.addSubcommand("hello", new HelloCommand(terminal));
            commandLine.addSubcommand("echo", new EchoCommand(terminal));

            // Create a completer for the command line
            Completer completer = new PicocliJLineCompleter(commandLine.getCommandSpec());

            // Set up the line reader with completion
            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(completer)
                    .build();

            // Main interactive loop
            while (true) {
                String line = reader.readLine("cli> ");

                // Exit if requested
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    // Parse and execute the command
                    ParsedLine pl = reader.getParser().parse(line, 0);
                    String[] arguments = pl.words().toArray(new String[0]);
                    commandLine.execute(arguments);
                } catch (Exception e) {
                    terminal.writer().println("Error: " + e.getMessage());
                    terminal.flush();
                }
            }

            terminal.writer().println("Goodbye!");
            terminal.close();

        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }

    // Root command
    @Command(
            name = "cli",
            mixinStandardHelpOptions = true,
            version = "1.0",
            description = "Interactive CLI with JLine and Picocli")
    static class RootCommand implements Callable<Integer> {
        private final Terminal terminal;

        public RootCommand(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public Integer call() {
            terminal.writer().println("Use one of the available commands:");
            terminal.writer().println("  hello - Say hello");
            terminal.writer().println("  echo - Echo a message");
            terminal.writer().println("  help - Show help");
            terminal.writer().println("  exit - Exit the application");
            terminal.flush();
            return 0;
        }
    }

    // Hello command
    @Command(name = "hello", description = "Say hello")
    static class HelloCommand implements Callable<Integer> {
        private final Terminal terminal;

        @Option(
                names = {"-n", "--name"},
                description = "Name to greet")
        private String name = "World";

        public HelloCommand(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public Integer call() {
            terminal.writer().println("Hello, " + name + "!");
            terminal.flush();
            return 0;
        }
    }

    // Echo command
    @Command(name = "echo", description = "Echo a message")
    static class EchoCommand implements Callable<Integer> {
        private final Terminal terminal;

        @Option(
                names = {"-u", "--uppercase"},
                description = "Convert to uppercase")
        private boolean uppercase;

        @Parameters(description = "Message to echo")
        private List<String> message;

        public EchoCommand(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public Integer call() {
            if (message == null || message.isEmpty()) {
                terminal.writer().println("No message provided");
            } else {
                String result = String.join(" ", message);
                if (uppercase) {
                    result = result.toUpperCase();
                }
                terminal.writer().println(result);
            }
            terminal.flush();
            return 0;
        }
    }
    // SNIPPET_END: PicocliCompletionExample
}

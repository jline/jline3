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
import java.util.concurrent.Callable;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Example demonstrating integration of JLine with Picocli.
 */
public class PicocliJLineExample {

    // SNIPPET_START: PicocliJLineExample
    public static void main(String[] args) {
        try {
            // Set up the terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Set up the line reader
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // Create the command line parser
            MyCommand myCommand = new MyCommand(terminal);
            CommandLine cmd = new CommandLine(myCommand);

            // Main interactive loop
            while (true) {
                String line = reader.readLine("example> ");

                // Exit if requested
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    // Parse and execute the command
                    ParsedLine pl = reader.getParser().parse(line, 0);
                    String[] arguments = pl.words().toArray(new String[0]);
                    cmd.execute(arguments);
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

    // Define a command using Picocli annotations
    @Command(
            name = "example",
            mixinStandardHelpOptions = true,
            version = "1.0",
            description = "Example command using JLine and Picocli")
    static class MyCommand implements Callable<Integer> {
        private final Terminal terminal;

        @Option(
                names = {"-c", "--count"},
                description = "Number of times to repeat")
        private int count = 1;

        @Parameters(index = "0", description = "The message to display")
        private String message;

        public MyCommand(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public Integer call() {
            if (message == null) {
                terminal.writer().println("No message provided. Use --help for usage information.");
            } else {
                for (int i = 0; i < count; i++) {
                    terminal.writer().println(message);
                }
            }
            terminal.flush();
            return 0;
        }
    }
    // SNIPPET_END: PicocliJLineExample
}

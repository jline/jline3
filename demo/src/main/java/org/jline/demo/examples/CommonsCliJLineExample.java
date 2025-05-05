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
import java.util.Arrays;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating Apache Commons CLI integration with JLine.
 * Note: This is a simplified example that doesn't actually use Commons CLI,
 * as that would require adding Commons CLI dependencies to the demo project.
 */
public class CommonsCliJLineExample {

    // SNIPPET_START: CommonsCliJLineExample
    // This is a simplified example showing how JLine would be integrated with Commons CLI
    public static void main(String[] args) {
        try {
            // Set up the terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Set up the line reader
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // In a real application, we would define CLI options using Commons CLI:
            // Options options = new Options();
            // options.addOption(Option.builder("h").longOpt("help").desc("Show help").build());
            // options.addOption(Option.builder("g").longOpt("greet").desc("Greet
            // someone").hasArg().argName("name").build());
            // options.addOption(Option.builder("c").longOpt("count").desc("Count to a
            // number").hasArg().argName("number").type(Number.class).build());

            // Main interactive loop
            terminal.writer().println("Commons CLI with JLine Example");
            terminal.writer().println("Available commands:");
            terminal.writer().println("  --help, -h           Show help");
            terminal.writer().println("  --greet, -g <name>   Greet someone");
            terminal.writer().println("  --count, -c <number> Count to a number");
            terminal.writer().println("  exit, quit           Exit the application");
            terminal.writer().println();

            while (true) {
                String line = reader.readLine("cli> ");

                // Exit if requested
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                // Parse the command line (simplified version without Commons CLI)
                String[] arguments = line.split("\\s+");

                if (arguments.length > 0) {
                    String cmd = arguments[0];

                    if (cmd.equals("--help") || cmd.equals("-h")) {
                        terminal.writer().println("Help information:");
                        terminal.writer().println("  --help, -h           Show this help message");
                        terminal.writer().println("  --greet, -g <name>   Greet someone");
                        terminal.writer().println("  --count, -c <number> Count to a number");
                        terminal.writer().println("  exit, quit           Exit the application");
                    } else if (cmd.equals("--greet") || cmd.equals("-g")) {
                        String name = arguments.length > 1 ? arguments[1] : "World";
                        terminal.writer().println("Hello, " + name + "!");
                    } else if (cmd.equals("--count") || cmd.equals("-c")) {
                        try {
                            int count = arguments.length > 1 ? Integer.parseInt(arguments[1]) : 10;
                            for (int i = 1; i <= count; i++) {
                                terminal.writer().println(i);
                            }
                        } catch (NumberFormatException e) {
                            terminal.writer().println("Error: Invalid number format");
                        }
                    } else {
                        // Handle command arguments
                        terminal.writer().println("Arguments: " + Arrays.toString(arguments));
                        terminal.writer().println("Unknown command. Use --help for usage information.");
                    }
                }

                terminal.flush();
            }

            terminal.writer().println("Goodbye!");
            terminal.close();

        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }
    // SNIPPET_END: CommonsCliJLineExample
}

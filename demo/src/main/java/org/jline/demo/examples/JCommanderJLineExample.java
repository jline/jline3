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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating JCommander integration with JLine.
 * Note: This is a simplified example that doesn't actually use JCommander,
 * as that would require adding JCommander dependencies to the demo project.
 */
public class JCommanderJLineExample {

    // SNIPPET_START: JCommanderJLineExample
    // This is a simplified example showing how JLine would be integrated with JCommander
    public static void main(String[] args) {
        try {
            // Set up the terminal
            Terminal terminal = TerminalBuilder.builder().system(true).build();

            // Set up the line reader
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();

            // In a real application, we would define command objects using JCommander annotations:
            // MainCommand mainCommand = new MainCommand();
            // GreetCommand greetCommand = new GreetCommand();
            // CountCommand countCommand = new CountCommand();
            // JCommander jc = JCommander.newBuilder()
            //         .addObject(mainCommand)
            //         .addCommand("greet", greetCommand)
            //         .addCommand("count", countCommand)
            //         .build();

            // Main interactive loop
            terminal.writer().println("JCommander with JLine Example");
            terminal.writer().println("Available commands:");
            terminal.writer().println("  help                Show help");
            terminal.writer().println("  greet [--name=<name>] Greet someone");
            terminal.writer().println("  count [--number=<n>]  Count to a number");
            terminal.writer().println("  exit, quit          Exit the application");
            terminal.writer().println();

            while (true) {
                String line = reader.readLine("jcmd> ");

                // Exit if requested
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    // Parse the command line (simplified version without JCommander)
                    String[] arguments = line.split("\\s+");

                    if (arguments.length > 0) {
                        String cmd = arguments[0];

                        if (cmd.equals("help") || cmd.equals("--help")) {
                            terminal.writer().println("Help information:");
                            terminal.writer().println("  help                Show this help message");
                            terminal.writer().println("  greet [--name=<name>] Greet someone");
                            terminal.writer().println("  count [--number=<n>]  Count to a number");
                            terminal.writer().println("  exit, quit          Exit the application");
                        } else if (cmd.equals("greet")) {
                            String name = "World";
                            // Parse --name parameter
                            for (int i = 1; i < arguments.length; i++) {
                                if (arguments[i].startsWith("--name=")) {
                                    name = arguments[i].substring(7);
                                }
                            }
                            terminal.writer().println("Hello, " + name + "!");
                        } else if (cmd.equals("count")) {
                            int number = 10;
                            // Parse --number parameter
                            for (int i = 1; i < arguments.length; i++) {
                                if (arguments[i].startsWith("--number=")) {
                                    try {
                                        number = Integer.parseInt(arguments[i].substring(9));
                                    } catch (NumberFormatException e) {
                                        terminal.writer().println("Error: Invalid number format");
                                        number = 10;
                                    }
                                }
                            }
                            for (int i = 1; i <= number; i++) {
                                terminal.writer().println(i);
                            }
                        } else {
                            terminal.writer().println("Unknown command: " + cmd);
                            terminal.writer().println("Type 'help' for usage information.");
                        }
                    }
                } catch (Exception e) {
                    terminal.writer().println("Error: " + e.getMessage());
                    terminal.writer().println("Type 'help' for usage information.");
                }

                terminal.flush();
            }

            terminal.writer().println("Goodbye!");
            terminal.close();

        } catch (IOException e) {
            System.err.println("Error creating terminal: " + e.getMessage());
        }
    }

    // In a real application, these would be JCommander parameter classes:

    // static class MainCommand {
    //     @Parameter(names = {"--help", "-h"}, help = true, description = "Show help")
    //     boolean help;
    // }

    // @Parameters(commandDescription = "Greet someone")
    // static class GreetCommand {
    //     @Parameter(names = {"--name", "-n"}, description = "Name to greet")
    //     String name = "World";
    // }

    // @Parameters(commandDescription = "Count to a number")
    // static class CountCommand {
    //     @Parameter(names = {"--number", "-n"}, description = "Number to count to")
    //     int number = 10;
    //
    //     @Parameter(description = "Additional arguments")
    //     List<String> args = new ArrayList<>();
    // }
    // SNIPPET_END: JCommanderJLineExample
}

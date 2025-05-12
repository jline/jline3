/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating a simple command registry.
 */
public class SystemRegistryExample {

    // SNIPPET_START: SystemRegistryExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Set up working directory
        Path workDir = Paths.get(System.getProperty("user.dir"));

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .build();

        // Create a simple command registry
        Map<String, Function<String[], Integer>> commands = new HashMap<>();

        // Add commands to the registry
        commands.put("hello", cmdArgs -> {
            terminal.writer().println("Hello, JLine!");
            return 0;
        });

        commands.put("echo", cmdArgs -> {
            if (cmdArgs.length > 1) {
                for (int i = 1; i < cmdArgs.length; i++) {
                    terminal.writer().print(cmdArgs[i]);
                    if (i < cmdArgs.length - 1) {
                        terminal.writer().print(" ");
                    }
                }
                terminal.writer().println();
            }
            return 0;
        });

        commands.put("pwd", cmdArgs -> {
            terminal.writer().println(workDir.toAbsolutePath());
            return 0;
        });

        commands.put("help", cmdArgs -> {
            terminal.writer().println("Available commands:");
            for (String cmd : commands.keySet()) {
                terminal.writer().println("  " + cmd);
            }
            return 0;
        });

        // Print instructions
        terminal.writer().println("SystemRegistry Example");
        terminal.writer().println("Available commands: hello, echo, pwd, help");
        terminal.writer().println("Type 'exit' to quit");
        terminal.writer().println();

        // Main command loop
        while (true) {
            String line = reader.readLine("registry> ");

            if (line.equals("exit") || line.equals("quit")) {
                break;
            }

            try {
                // Parse the line
                ParsedLine pl = reader.getParser().parse(line, 0);
                String[] cmdArgs = pl.words().toArray(new String[0]);

                if (cmdArgs.length > 0) {
                    String command = cmdArgs[0];

                    // Execute the command if it exists
                    if (commands.containsKey(command)) {
                        commands.get(command).apply(cmdArgs);
                    } else {
                        terminal.writer().println("Unknown command: " + command);
                        terminal.writer().println("Type 'help' for a list of commands");
                    }
                }
            } catch (Exception e) {
                terminal.writer().println("Error: " + e.getMessage());
            }

            terminal.flush();
        }

        terminal.writer().println("Goodbye!");
        terminal.close();
    }
    // SNIPPET_END: SystemRegistryExample
}

/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jline.builtins.PosixCommands;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating file operations using JLine Builtins PosixCommands.
 */
public class FileOperationsExample {

    // SNIPPET_START: FileOperationsExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a temporary directory for file operations
        Path workDir = Files.createTempDirectory("jline-demo");
        workDir.toFile().deleteOnExit();

        // Variables
        Map<String, Object> variables = new HashMap<>();

        // Create some sample files
        Path file1 = workDir.resolve("sample1.txt");
        Path file2 = workDir.resolve("sample2.txt");
        Files.write(
                file1,
                "This is sample file 1\nWith multiple lines\nFor demonstration".getBytes(StandardCharsets.UTF_8));
        Files.write(
                file2,
                "This is sample file 2\nWith different content\nFor comparison".getBytes(StandardCharsets.UTF_8));

        // Create a context for POSIX commands
        PosixCommands.Context context = new PosixCommands.Context(
                terminal.input(),
                new PrintStream(terminal.output()),
                new PrintStream(terminal.output()),
                workDir,
                terminal,
                variables::get);

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .build();

        // Print instructions
        terminal.writer().println("File Operations Example using JLine Builtins");
        terminal.writer().println("Working directory: " + workDir);
        terminal.writer().println("Available commands: cat, ls, grep, pwd, echo, wc, head, tail, sort, date, clear");
        terminal.writer().println("Type 'help <command>' for command help, or 'exit' to quit");
        terminal.writer().println();

        // Main command loop
        while (true) {
            String line = reader.readLine("posix> ");

            if (line.equals("exit") || line.equals("quit")) {
                break;
            }

            try {
                // Parse the command
                String[] parts = line.split("\\s+");
                if (parts.length == 0) continue;

                String command = parts[0];

                // Execute the command using PosixCommands
                switch (command) {
                    case "cat":
                        PosixCommands.cat(context, parts);
                        break;
                    case "ls":
                        PosixCommands.ls(context, parts);
                        break;
                    case "grep":
                        PosixCommands.grep(context, parts);
                        break;
                    case "pwd":
                        PosixCommands.pwd(context, parts);
                        break;
                    case "echo":
                        PosixCommands.echo(context, parts);
                        break;
                    case "wc":
                        PosixCommands.wc(context, parts);
                        break;
                    case "head":
                        PosixCommands.head(context, parts);
                        break;
                    case "tail":
                        PosixCommands.tail(context, parts);
                        break;
                    case "sort":
                        PosixCommands.sort(context, parts);
                        break;
                    case "date":
                        PosixCommands.date(context, parts);
                        break;
                    case "clear":
                        PosixCommands.clear(context, parts);
                        break;
                    case "help":
                        if (parts.length > 1) {
                            // Show help for specific command
                            String[] helpArgs = {parts[1], "--help"};
                            switch (parts[1]) {
                                case "cat":
                                    PosixCommands.cat(context, helpArgs);
                                    break;
                                case "ls":
                                    PosixCommands.ls(context, helpArgs);
                                    break;
                                case "grep":
                                    PosixCommands.grep(context, helpArgs);
                                    break;
                                case "pwd":
                                    PosixCommands.pwd(context, helpArgs);
                                    break;
                                case "echo":
                                    PosixCommands.echo(context, helpArgs);
                                    break;
                                case "wc":
                                    PosixCommands.wc(context, helpArgs);
                                    break;
                                case "head":
                                    PosixCommands.head(context, helpArgs);
                                    break;
                                case "tail":
                                    PosixCommands.tail(context, helpArgs);
                                    break;
                                case "sort":
                                    PosixCommands.sort(context, helpArgs);
                                    break;
                                case "date":
                                    PosixCommands.date(context, helpArgs);
                                    break;
                                case "clear":
                                    PosixCommands.clear(context, helpArgs);
                                    break;
                                default:
                                    terminal.writer().println("Unknown command: " + parts[1]);
                            }
                        } else {
                            terminal.writer()
                                    .println(
                                            "Available commands: cat, ls, grep, pwd, echo, wc, head, tail, sort, date, clear");
                            terminal.writer().println("Use 'help <command>' for specific command help");
                        }
                        break;
                    default:
                        terminal.writer().println("Unknown command: " + command);
                        terminal.writer().println("Type 'help' for available commands");
                }
            } catch (Exception e) {
                terminal.writer().println("Error: " + e.getMessage());
            }

            terminal.flush();
        }

        terminal.writer().println("Goodbye!");
        terminal.close();
    }
    // SNIPPET_END: FileOperationsExample
}

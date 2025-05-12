/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating file operations using JLine.
 */
public class FileOperationsExample {

    // SNIPPET_START: FileOperationsExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a temporary directory for file operations
        Path workDir = Files.createTempDirectory("jline-demo");
        workDir.toFile().deleteOnExit();

        // Create some sample files
        Path file1 = workDir.resolve("sample1.txt");
        Path file2 = workDir.resolve("sample2.txt");
        Files.write(
                file1,
                "This is sample file 1\nWith multiple lines\nFor demonstration".getBytes(StandardCharsets.UTF_8));
        Files.write(
                file2,
                "This is sample file 2\nWith different content\nFor comparison".getBytes(StandardCharsets.UTF_8));

        // Create a line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .parser(new DefaultParser())
                .build();

        // Print instructions
        terminal.writer().println("File Operations Example");
        terminal.writer().println("Working directory: " + workDir);
        terminal.writer().println("Available commands: cat, ls, grep");
        terminal.writer().println("Type 'exit' to quit");
        terminal.writer().println();

        // Main command loop
        while (true) {
            String line = reader.readLine("file-ops> ");

            if (line.equals("exit") || line.equals("quit")) {
                break;
            }

            try {
                // Parse the command
                String[] parts = line.split("\\s+");
                String command = parts[0];

                // Execute the command
                if (command.equals("ls")) {
                    // List files in the working directory
                    File[] files = workDir.toFile().listFiles();
                    if (files != null) {
                        for (File file : files) {
                            terminal.writer().println(file.getName() + "\t" + file.length() + " bytes");
                        }
                    }
                } else if (command.equals("cat") && parts.length > 1) {
                    // Display file content
                    Path filePath = workDir.resolve(parts[1]);
                    if (Files.exists(filePath)) {
                        Files.lines(filePath).forEach(terminal.writer()::println);
                    } else {
                        terminal.writer().println("File not found: " + parts[1]);
                    }
                } else if (command.equals("grep") && parts.length > 2) {
                    // Search for pattern in file
                    String pattern = parts[1];
                    Path filePath = workDir.resolve(parts[2]);
                    if (Files.exists(filePath)) {
                        Files.lines(filePath)
                                .filter(line2 -> line2.contains(pattern))
                                .forEach(terminal.writer()::println);
                    } else {
                        terminal.writer().println("File not found: " + parts[2]);
                    }
                } else {
                    terminal.writer().println("Unknown command or invalid syntax: " + line);
                    terminal.writer().println("Available commands: cat <file>, ls, grep <pattern> <file>");
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

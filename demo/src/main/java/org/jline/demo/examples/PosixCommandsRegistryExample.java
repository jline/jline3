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

import org.jline.builtins.PosixCommandsRegistry;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating the PosixCommandsRegistry for easy POSIX command integration.
 */
public class PosixCommandsRegistryExample {

    // SNIPPET_START: PosixCommandsRegistryExample
    public static void main(String[] args) throws Exception {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().system(true).build();

        // Create a temporary directory for demonstration
        Path workDir = Files.createTempDirectory("jline-posix-demo");
        workDir.toFile().deleteOnExit();

        // Create some sample files
        Path file1 = workDir.resolve("sample1.txt");
        Path file2 = workDir.resolve("sample2.txt");
        Files.write(file1, "apple\nbanana\ncherry\ndate\nelderberry".getBytes(StandardCharsets.UTF_8));
        Files.write(file2, "one\ntwo\nthree\nfour\nfive".getBytes(StandardCharsets.UTF_8));

        Map<String, String> variables = new HashMap<>();

        // Create a POSIX commands registry
        PosixCommandsRegistry registry = new PosixCommandsRegistry(
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
        terminal.writer().println("POSIX Commands Registry Example");
        terminal.writer().println("Working directory: " + workDir);
        terminal.writer().println();
        terminal.writer().println("Try these commands:");
        terminal.writer().println("  ls -l");
        terminal.writer().println("  cat sample1.txt");
        terminal.writer().println("  grep 'a' sample1.txt");
        terminal.writer().println("  wc sample1.txt");
        terminal.writer().println("  head -n 3 sample1.txt");
        terminal.writer().println("  sort sample1.txt");
        terminal.writer().println("  help");
        terminal.writer().println();
        terminal.writer().println("Type 'exit' to quit");
        terminal.writer().println();

        // Main command loop
        while (true) {
            String line = reader.readLine("posix> ");

            if (line.equals("exit") || line.equals("quit")) {
                break;
            }

            if (line.trim().isEmpty()) {
                continue;
            }

            try {
                if (line.equals("help")) {
                    // Show available commands
                    registry.printHelp();
                } else if (line.startsWith("help ")) {
                    // Show help for specific command
                    String command = line.substring(5).trim();
                    registry.printHelp(command);
                } else {
                    // Execute the command using the registry
                    registry.execute(line);
                }
            } catch (Exception e) {
                terminal.writer().println("Error: " + e.getMessage());
            }

            terminal.flush();
        }

        terminal.writer().println("Goodbye!");
        terminal.close();
    }
    // SNIPPET_END: PosixCommandsRegistryExample
}

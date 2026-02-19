/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

import org.jline.picocli.PicocliCommandRegistry;
import org.jline.shell.Shell;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Example demonstrating integration of JLine with Picocli using
 * {@link PicocliCommandRegistry} and {@link Shell}.
 */
public class PicocliJLineExample {

    // SNIPPET_START: PicocliJLineExample
    public static void main(String[] args) {
        try {
            // Create the picocli command line with subcommands
            CommandLine commandLine = new CommandLine(new TopCommand());
            commandLine.addSubcommand("example", new MyCommand());

            // Create a PicocliCommandRegistry and run via Shell.builder()
            PicocliCommandRegistry registry = new PicocliCommandRegistry(commandLine);

            try (Shell shell =
                    Shell.builder().prompt("example> ").groups(registry).build()) {
                shell.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Command(name = "top", description = "Top-level command")
    static class TopCommand {}

    // Define a command using Picocli annotations
    @Command(
            name = "example",
            mixinStandardHelpOptions = true,
            version = "1.0",
            description = "Example command using JLine and Picocli")
    static class MyCommand implements Callable<Integer> {

        @Option(
                names = {"-c", "--count"},
                description = "Number of times to repeat")
        private int count = 1;

        @Parameters(index = "0", description = "The message to display", defaultValue = "")
        private String message;

        @CommandLine.Spec
        CommandLine.Model.CommandSpec spec;

        @Override
        public Integer call() {
            PrintWriter out = spec.commandLine().getOut();
            if (message == null || message.isEmpty()) {
                out.println("No message provided. Use --help for usage information.");
            } else {
                for (int i = 0; i < count; i++) {
                    out.println(message);
                }
            }
            return 0;
        }
    }
    // SNIPPET_END: PicocliJLineExample
}

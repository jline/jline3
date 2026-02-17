/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.Callable;

import org.jline.picocli.PicocliCommandRegistry;
import org.jline.shell.Shell;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Example demonstrating integration of JLine with Picocli including tab completion,
 * using {@link PicocliCommandRegistry} and {@link Shell}.
 */
public class PicocliCompletionExample {

    // SNIPPET_START: PicocliCompletionExample
    public static void main(String[] args) {
        try {
            // Create the command line parser with subcommands
            CommandLine commandLine = new CommandLine(new RootCommand());
            commandLine.addSubcommand("hello", new HelloCommand());
            commandLine.addSubcommand("echo", new EchoCommand());

            // Create a PicocliCommandRegistry -- tab completion is automatic
            PicocliCommandRegistry registry = new PicocliCommandRegistry(commandLine);

            // Shell.builder() wires completers, TailTipWidgets, and the REPL loop
            try (Shell shell = Shell.builder().prompt("cli> ").groups(registry).build()) {
                shell.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Root command
    @Command(
            name = "cli",
            mixinStandardHelpOptions = true,
            version = "1.0",
            description = "Interactive CLI with JLine and Picocli")
    static class RootCommand {}

    // Hello command
    @Command(name = "hello", description = "Say hello")
    static class HelloCommand implements Callable<Integer> {

        @CommandLine.Spec
        CommandLine.Model.CommandSpec spec;

        @Option(
                names = {"-n", "--name"},
                description = "Name to greet")
        private String name = "World";

        @Override
        public Integer call() {
            spec.commandLine().getOut().println("Hello, " + name + "!");
            return 0;
        }
    }

    // Echo command
    @Command(name = "echo", description = "Echo a message")
    static class EchoCommand implements Callable<Integer> {

        @CommandLine.Spec
        CommandLine.Model.CommandSpec spec;

        @Option(
                names = {"-u", "--uppercase"},
                description = "Convert to uppercase")
        private boolean uppercase;

        @Parameters(description = "Message to echo")
        private List<String> message;

        @Override
        public Integer call() {
            PrintWriter out = spec.commandLine().getOut();
            if (message == null || message.isEmpty()) {
                out.println("No message provided");
            } else {
                String result = String.join(" ", message);
                if (uppercase) {
                    result = result.toUpperCase();
                }
                out.println(result);
            }
            return 0;
        }
    }
    // SNIPPET_END: PicocliCompletionExample
}

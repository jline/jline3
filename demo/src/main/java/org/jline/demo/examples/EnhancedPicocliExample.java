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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.jline.builtins.ConfigurationPath;
import org.jline.console.CommandContext;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.console.picocli.ContextInjectingExecutionStrategy;
import org.jline.console.picocli.PicocliCommandRegistry;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.widget.AutosuggestionWidgets;
import org.jline.widget.TailTipWidgets;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Enhanced Picocli integration example showing the simplified API.
 * <p>
 * This example demonstrates:
 * <ul>
 *   <li>Clean CommandContext API without framework-specific dependencies</li>
 *   <li>Automatic context injection into Picocli commands</li>
 *   <li>Seamless integration with JLine's interactive features</li>
 *   <li>Rich completion and help generation from Picocli annotations</li>
 * </ul>
 */
public class EnhancedPicocliExample {

    // SNIPPET_START: EnhancedPicocliExample
    public static void main(String[] args) throws IOException {
        // Set up terminal and basic paths
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        Path workDir = Paths.get(System.getProperty("user.dir"));
        Path configDir = Paths.get(System.getProperty("user.home"), ".jline");
        ConfigurationPath configPath = new ConfigurationPath(configDir, configDir);

        // Create clean command context
        CommandContext context = CommandContext.builder()
                .terminal(terminal)
                .currentDir(workDir)
                .build();

        // Create Picocli registry with automatic context injection
        PicocliCommandRegistry picocliRegistry = new PicocliCommandRegistry(context)
                .register(new GreetCommand())
                .register(new FileCommand())
                .register(new MathCommand())
                .register(ExitCommand.class); // Can register classes too

        // Create system registry combining multiple command sources
        SystemRegistry systemRegistry = new SystemRegistryImpl(
                new DefaultParser(), terminal, () -> workDir, configPath);
        systemRegistry.setCommandRegistries(
                picocliRegistry,
                new Builtins(workDir, configPath, null)
        );

        // Create enhanced line reader
        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(systemRegistry.completer())
                .parser(new DefaultParser())
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ")
                .variable(LineReader.INDENTATION, 2)
                .option(LineReader.Option.INSERT_BRACKET, true)
                .build();

        // Add context to the context (for widgets that need it)
        CommandContext enhancedContext = CommandContext.builder()
                .terminal(terminal)
                .currentDir(workDir)
                .lineReader(reader)
                .systemRegistry(systemRegistry)
                .build();

        // Update registry with enhanced context
        PicocliCommandRegistry enhancedRegistry = new PicocliCommandRegistry(enhancedContext)
                .registerAll(
                        new GreetCommand(),
                        new FileCommand(),
                        new MathCommand(),
                        ExitCommand.class
                );

        systemRegistry.setCommandRegistries(enhancedRegistry, new Builtins(workDir, configPath, null));

        // Add smart widgets
        new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMBINED);
        new AutosuggestionWidgets(reader);

        // Interactive REPL
        String prompt = new AttributedString("enhanced> ", 
                AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)).toAnsi();

        System.out.println("Enhanced Picocli + JLine Example");
        System.out.println("Try: greet --name World, file --list, math --add 5 3, help, exit");
        System.out.println();

        while (true) {
            try {
                String line = reader.readLine(prompt);
                Object result = systemRegistry.execute(line);
                if (result != null && !result.toString().isEmpty()) {
                    System.out.println(result);
                }
            } catch (UserInterruptException e) {
                // Ctrl+C
            } catch (EndOfFileException e) {
                break;
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        System.out.println("Goodbye!");
        terminal.close();
    }

    // Example commands with different context injection patterns

    @Command(name = "greet", description = "Greet someone with style")
    static class GreetCommand implements Callable<Integer> {
        @Option(names = {"-n", "--name"}, description = "Name to greet", defaultValue = "World")
        private String name;

        @Option(names = {"-c", "--color"}, description = "Use colors")
        private boolean useColor;

        // Context injection via method parameter
        public Integer call(CommandContext context) {
            String greeting = "Hello, " + name + "!";
            
            if (useColor && context.isTty()) {
                AttributedString coloredGreeting = new AttributedString(greeting,
                        AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
                context.out().println(coloredGreeting.toAnsi());
            } else {
                context.out().println(greeting);
            }
            
            return 0;
        }
    }

    @Command(name = "file", description = "File operations with context awareness")
    static class FileCommand implements Callable<Integer> {
        @Option(names = {"-l", "--list"}, description = "List files in current directory")
        private boolean list;

        @Option(names = {"-d", "--dir"}, description = "Directory to operate on")
        private String directory;

        // Context injection via annotated parameter
        public Integer call(@ContextInjectingExecutionStrategy.Context CommandContext ctx) {
            Path targetDir = directory != null ? 
                    Paths.get(directory) : ctx.currentDir();

            if (list) {
                ctx.out().println("Files in " + targetDir + ":");
                try {
                    java.nio.file.Files.list(targetDir)
                            .forEach(path -> ctx.out().println("  " + path.getFileName()));
                } catch (Exception e) {
                    ctx.err().println("Error listing files: " + e.getMessage());
                    return 1;
                }
            }
            
            return 0;
        }
    }

    @Command(name = "math", description = "Simple math operations")
    static class MathCommand implements Callable<Integer> {
        @Option(names = {"--add"}, arity = "2", description = "Add two numbers")
        private int[] addNumbers;

        @Option(names = {"--multiply"}, arity = "2", description = "Multiply two numbers")
        private int[] multiplyNumbers;

        // Field injection (context injected into field)
        private CommandContext context;

        @Override
        public Integer call() {
            if (addNumbers != null) {
                int result = addNumbers[0] + addNumbers[1];
                context.out().println(addNumbers[0] + " + " + addNumbers[1] + " = " + result);
            } else if (multiplyNumbers != null) {
                int result = multiplyNumbers[0] * multiplyNumbers[1];
                context.out().println(multiplyNumbers[0] + " * " + multiplyNumbers[1] + " = " + result);
            } else {
                context.out().println("Please specify an operation (--add or --multiply)");
                return 1;
            }
            return 0;
        }
    }

    @Command(name = "exit", description = "Exit the application")
    static class ExitCommand implements Callable<Integer> {
        public Integer call() {
            System.exit(0);
            return 0;
        }
    }
    // SNIPPET_END: EnhancedPicocliExample
}

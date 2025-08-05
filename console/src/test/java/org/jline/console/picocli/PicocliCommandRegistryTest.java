/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.picocli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.jline.console.CommandContext;
import org.jline.console.CommandSession;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Tests for PicocliCommandRegistry.
 */
public class PicocliCommandRegistryTest {

    private CommandContext context;
    private ByteArrayOutputStream outputStream;
    private PrintStream printStream;
    private Terminal terminal;

    @BeforeEach
    void setUp() throws Exception {
        outputStream = new ByteArrayOutputStream();
        printStream = new PrintStream(outputStream);
        terminal = new DumbTerminal(System.in, outputStream);
        
        context = CommandContext.builder()
                .terminal(terminal)
                .currentDir(Paths.get("."))
                .build();
    }

    @Test
    void testBasicCommandRegistration() {
        PicocliCommandRegistry registry = new PicocliCommandRegistry(context);
        registry.register(new TestCommand());

        assertTrue(registry.hasCommand("test"));
        assertTrue(registry.commandNames().contains("test"));
    }

    @Test
    void testCommandExecution() throws Exception {
        PicocliCommandRegistry registry = new PicocliCommandRegistry(context);
        registry.register(new TestCommand());

        Object result = registry.invoke(new CommandSession(), "test", "Hello", "World");
        assertEquals(0, result);
        
        String output = outputStream.toString();
        assertTrue(output.contains("Hello World"));
    }

    @Test
    void testContextInjection() throws Exception {
        PicocliCommandRegistry registry = new PicocliCommandRegistry(context);
        registry.register(new ContextAwareCommand());

        Object result = registry.invoke(new CommandSession(), "context-test");
        assertEquals(0, result);
        
        String output = outputStream.toString();
        assertTrue(output.contains("Context injected successfully"));
    }

    @Test
    void testBuilderPattern() {
        PicocliCommandRegistry registry = PicocliCommandRegistry.builder(context)
                .register(new TestCommand())
                .register(ContextAwareCommand.class)
                .build();

        assertTrue(registry.hasCommand("test"));
        assertTrue(registry.hasCommand("context-test"));
    }

    @Test
    void testCommandInfo() {
        PicocliCommandRegistry registry = new PicocliCommandRegistry(context);
        registry.register(new TestCommand());

        var info = registry.commandInfo("test");
        assertFalse(info.isEmpty());
        assertTrue(info.get(0).contains("test"));
    }

    @Test
    void testCommandOptions() {
        PicocliCommandRegistry registry = new PicocliCommandRegistry(context);
        registry.register(new TestCommand());

        var options = registry.commandOptions("test");
        assertNotNull(options);
        assertTrue(options.stream().anyMatch(opt -> "help".equals(opt.longName())));
    }

    // Test commands

    @Command(name = "test", description = "A test command")
    static class TestCommand implements Callable<Integer> {
        @Parameters(description = "Words to echo")
        private String[] words = {};

        @Option(names = {"-u", "--uppercase"}, description = "Convert to uppercase")
        private boolean uppercase;

        @Override
        public Integer call() {
            String message = String.join(" ", words);
            if (uppercase) {
                message = message.toUpperCase();
            }
            System.out.println(message);
            return 0;
        }
    }

    @Command(name = "context-test", description = "Test context injection")
    static class ContextAwareCommand implements Callable<Integer> {
        private CommandContext context;

        @Override
        public Integer call() {
            if (context != null) {
                context.out().println("Context injected successfully");
                context.out().println("Current dir: " + context.currentDir());
            } else {
                System.out.println("Context injection failed");
            }
            return 0;
        }
    }

    @Command(name = "method-context-test", description = "Test method parameter context injection")
    static class MethodContextCommand implements Callable<Integer> {
        public Integer call(CommandContext ctx) {
            ctx.out().println("Method context injection successful");
            return 0;
        }
    }
}

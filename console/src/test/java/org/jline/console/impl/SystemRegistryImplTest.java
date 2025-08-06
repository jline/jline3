/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.jline.builtins.ConfigurationPath;
import org.jline.console.CommandInput;
import org.jline.console.CommandMethods;
import org.jline.console.CommandRegistry;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for SystemRegistryImpl class.
 */
public class SystemRegistryImplTest {

    private Terminal terminal;
    private Parser parser;
    private Supplier<Path> workDir;
    private ConfigurationPath configPath;
    private SystemRegistryImpl registry;
    private TestCommandRegistry testRegistry;
    private StringBuilder output;

    @BeforeEach
    public void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        parser = new DefaultParser();
        workDir = () -> Paths.get(System.getProperty("user.dir"));
        configPath = new ConfigurationPath(Paths.get("."), Paths.get("."));
        output = new StringBuilder();

        registry = new SystemRegistryImpl(parser, terminal, workDir, configPath);
        testRegistry = new TestCommandRegistry(output);

        // Set up the registry with our test command registry
        registry.setCommandRegistries(testRegistry);
    }

    /**
     * Test that demonstrates the ability to override built-in commands like "exit"
     * with custom implementations in a command registry.
     *
     * This test verifies the fix for issue #1232 where the order of command execution
     * checking in the execute method was inconsistent with other methods.
     */
    @Test
    public void testOverrideBuiltinCommand() throws Exception {
        // The "exit" command is a built-in command in SystemRegistryImpl
        // Our TestCommandRegistry also has an "exit" command
        // After our fix, the registry should use the TestCommandRegistry's "exit" command

        // Execute the "exit" command
        registry.execute("exit");

        // Verify that our custom "exit" command was executed
        assertEquals("Custom exit command executed", output.toString().trim());
    }

    /**
     * Test that variable assignment operations don't hang on macOS.
     *
     * This test verifies the fix for issues #1361 and #1360 where variable assignments
     * would hang on macOS due to PTY terminal creation in CommandOutputStream.
     * The fix removes PTY terminal usage and uses simple Java streams instead.
     *
     * Variable assignments trigger CommandOutputStream.open() which previously created
     * PTY terminals that could hang on BSD/macOS platforms.
     */
    @Test
    public void testVariableAssignmentDoesNotHang() throws Exception {
        // Add a test command that outputs some text
        TestCommandRegistry echoRegistry = new TestCommandRegistry(output);
        echoRegistry.addCommand("echo", (input) -> {
            if (input.args().length > 0) {
                StringBuilder sb = new StringBuilder();
                for (Object arg : input.args()) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(arg.toString());
                }
                // Print to System.out which will be captured by CommandOutputStream
                System.out.print(sb.toString());
            }
            return null;
        });

        registry.setCommandRegistries(echoRegistry);

        // Test variable assignment with a timeout to ensure it doesn't hang
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // This command uses variable assignment which triggers CommandOutputStream.open()
                // and previously could hang on macOS due to PTY terminal creation
                registry.execute("result=echo hello world");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            // If the operation hangs, this will throw TimeoutException
            future.get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AssertionError(
                    "Variable assignment operation hung - this indicates the macOS hang bug is present", e);
        }

        // If we get here, the operation completed without hanging
        // The exact output doesn't matter as much as the fact that it didn't hang
    }

    /**
     * A test command registry that provides a custom implementation of the "exit" command.
     */
    private static class TestCommandRegistry implements CommandRegistry {
        private final Map<String, CommandMethods> commandExecute = new HashMap<>();
        private final StringBuilder output;

        public TestCommandRegistry(StringBuilder output) {
            this.output = output;
            // Register our custom "exit" command
            commandExecute.put("exit", new CommandMethods(this::exit, this::defaultCompleter));
        }

        public void addCommand(String name, java.util.function.Function<CommandInput, Object> executor) {
            commandExecute.put(name, new CommandMethods(executor, this::defaultCompleter));
        }

        private Object exit(CommandInput input) {
            output.append("Custom exit command executed");
            return null;
        }

        private List<org.jline.reader.Completer> defaultCompleter(String command) {
            return new ArrayList<>();
        }

        @Override
        public Object invoke(CommandRegistry.CommandSession session, String command, Object... args) throws Exception {
            return commandExecute.get(command).execute().apply(new CommandInput(command, args, session));
        }

        @Override
        public boolean hasCommand(String command) {
            return commandExecute.containsKey(command);
        }

        @Override
        public Set<String> commandNames() {
            return commandExecute.keySet();
        }

        @Override
        public Map<String, String> commandAliases() {
            return new HashMap<>();
        }

        @Override
        public List<String> commandInfo(String command) {
            List<String> info = new ArrayList<>();
            if (command.equals("exit")) {
                info.add("Custom exit command");
            }
            return info;
        }

        @Override
        public org.jline.console.CmdDesc commandDescription(List<String> args) {
            return new org.jline.console.CmdDesc(false);
        }

        @Override
        public org.jline.reader.impl.completer.SystemCompleter compileCompleters() {
            return new org.jline.reader.impl.completer.SystemCompleter();
        }
    }
}

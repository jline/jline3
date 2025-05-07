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

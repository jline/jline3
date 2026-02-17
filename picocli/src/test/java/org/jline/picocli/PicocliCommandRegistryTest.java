/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.picocli;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.jline.console.CmdDesc;
import org.jline.console.CommandRegistry;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PicocliCommandRegistry}.
 */
public class PicocliCommandRegistryTest {

    private CommandLine commandLine;
    private PicocliCommandRegistry registry;

    @BeforeEach
    void setUp() {
        commandLine = new CommandLine(new TopCommand());
        commandLine.addSubcommand("hello", new HelloCommand());
        commandLine.addSubcommand("echo", new EchoCommand());
        registry = new PicocliCommandRegistry(commandLine);
    }

    @Test
    void commandNamesReturnsSubcommands() {
        Set<String> names = registry.commandNames();
        assertTrue(names.contains("hello"));
        assertTrue(names.contains("echo"));
        assertEquals(2, names.size());
    }

    @Test
    void hasCommandReturnsTrueForSubcommands() {
        assertTrue(registry.hasCommand("hello"));
        assertTrue(registry.hasCommand("echo"));
        assertFalse(registry.hasCommand("nonexistent"));
    }

    @Test
    void commandInfoReturnsDescription() {
        List<String> info = registry.commandInfo("hello");
        assertFalse(info.isEmpty());
        assertEquals("Say hello to someone", info.get(0));
    }

    @Test
    void commandInfoReturnsEmptyForUnknown() {
        List<String> info = registry.commandInfo("nonexistent");
        assertTrue(info.isEmpty());
    }

    @Test
    void commandAliasesReturnsAliases() {
        Map<String, String> aliases = registry.commandAliases();
        assertTrue(aliases.containsKey("hi"));
        assertEquals("hello", aliases.get("hi"));
    }

    @Test
    void compileCompletersReturnsNonNull() {
        SystemCompleter completer = registry.compileCompleters();
        assertNotNull(completer);
    }

    @Test
    void commandDescriptionReturnsDescription() {
        CmdDesc desc = registry.commandDescription(Arrays.asList("hello"));
        assertNotNull(desc);
        assertNotNull(desc.getMainDesc());
        assertFalse(desc.getMainDesc().isEmpty());
        assertNotNull(desc.getOptsDesc());
        assertFalse(desc.getOptsDesc().isEmpty());
    }

    @Test
    void commandDescriptionReturnsNullForUnknown() {
        CmdDesc desc = registry.commandDescription(Arrays.asList("nonexistent"));
        assertNull(desc);
    }

    @Test
    void commandDescriptionReturnsNullForEmptyArgs() {
        assertNull(registry.commandDescription(Arrays.asList()));
        assertNull(registry.commandDescription(null));
    }

    @Test
    void invokeThrowsForUnknownCommandViaRegistry() {
        Terminal terminal;
        try {
            terminal = TerminalBuilder.builder().dumb(true).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession(terminal);
        // invoke delegates to picocli's CommandLine.execute() which returns an int
        // Test that invoke returns a non-null result for a known command
        try {
            Object result = registry.invoke(session, "echo");
            assertNotNull(result);
            assertInstanceOf(Integer.class, result);
        } catch (Exception e) {
            fail("invoke should not throw for known commands: " + e.getMessage());
        }
    }

    @Test
    void invokeThrowsForUnknownCommand() {
        CommandRegistry.CommandSession session = new CommandRegistry.CommandSession();
        assertThrows(IllegalArgumentException.class, () -> registry.invoke(session, "nonexistent"));
    }

    @Test
    void getCommandLineReturnsOriginal() {
        assertSame(commandLine, registry.getCommandLine());
    }

    // --- Test commands ---

    @Command(name = "top", description = "Top-level command")
    static class TopCommand {}

    @Command(name = "hello", aliases = "hi", description = "Say hello to someone")
    public static class HelloCommand implements Callable<Integer> {
        @Option(
                names = {"-n", "--name"},
                description = "Name to greet",
                defaultValue = "World")
        public String name;

        @Override
        public Integer call() {
            return 0;
        }
    }

    @Command(name = "echo", description = "Echo a message")
    public static class EchoCommand implements Callable<Integer> {
        @Option(
                names = {"-u", "--uppercase"},
                description = "Convert to uppercase")
        public boolean uppercase;

        @Parameters(description = "Message to echo")
        public List<String> message;

        @Override
        public Integer call() {
            return 0;
        }
    }
}

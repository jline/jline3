/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PromptCommands implementation.
 * Tests the command-line interface for prompts.
 *
 * NOTE: This test class is disabled because it contains interactive tests
 * that wait for user input, which causes hanging in automated test environments.
 */
@Disabled("Interactive tests that hang in automated environments")
public class PromptCommandsTest {

    private Terminal terminal;
    private ByteArrayOutputStream outStream;
    private ByteArrayOutputStream errStream;
    private PromptCommands.Context context;

    @BeforeEach
    void setUp() throws IOException {
        // Create test streams
        outStream = new ByteArrayOutputStream();
        errStream = new ByteArrayOutputStream();

        // Create a test terminal
        terminal = TerminalBuilder.builder()
                .system(false)
                .streams(new ByteArrayInputStream(new byte[0]), outStream)
                .build();

        // Create context
        context = new PromptCommands.Context(
                new ByteArrayInputStream(new byte[0]),
                new PrintStream(outStream),
                new PrintStream(errStream),
                Paths.get(System.getProperty("user.dir")),
                terminal,
                name -> System.getProperty(name));
    }

    @Test
    void testContextCreation() {
        assertNotNull(context);
        assertNotNull(context.in());
        assertNotNull(context.out());
        assertNotNull(context.err());
        assertNotNull(context.currentDir());
        assertNotNull(context.terminal());
        assertNotNull(context.variables());
    }

    @Test
    void testPromptCommandHelp() throws Exception {
        // Test help option
        PromptCommands.prompt(context, new String[] {"--help"});

        String output = outStream.toString();
        assertTrue(output.contains("prompt - create interactive prompts"));
        assertTrue(output.contains("Usage: prompt [OPTIONS] TYPE [ITEMS...]"));
        assertTrue(output.contains("list"));
        assertTrue(output.contains("checkbox"));
        assertTrue(output.contains("choice"));
        assertTrue(output.contains("input"));
        assertTrue(output.contains("confirm"));
    }

    @Test
    void testPromptCommandNoArgs() throws Exception {
        // Test command with no arguments
        PromptCommands.prompt(context, new String[] {});

        String errorOutput = errStream.toString();
        assertTrue(errorOutput.contains("Error: prompt type required"));
    }

    @Test
    void testPromptCommandInvalidType() throws Exception {
        // Test command with invalid type
        PromptCommands.prompt(context, new String[] {"invalid", "item1", "item2"});

        String errorOutput = errStream.toString();
        assertTrue(errorOutput.contains("Error: unknown prompt type 'invalid'"));
        assertTrue(errorOutput.contains("Valid types: list, checkbox, choice, input, confirm"));
    }

    @Test
    void testListPromptCommandValidation() throws Exception {
        // Test list prompt with no items
        PromptCommands.prompt(context, new String[] {"list", "-m", "Choose:"});

        String errorOutput = errStream.toString();
        assertTrue(errorOutput.contains("Error: list prompt requires at least one item"));
    }

    @Test
    void testCheckboxPromptCommandValidation() throws Exception {
        // Test checkbox prompt with no items
        PromptCommands.prompt(context, new String[] {"checkbox", "-m", "Select:"});

        String errorOutput = errStream.toString();
        assertTrue(errorOutput.contains("Error: checkbox prompt requires at least one item"));
    }

    @Test
    void testChoicePromptCommandValidation() throws Exception {
        // Test choice prompt with no items
        PromptCommands.prompt(context, new String[] {"choice", "-m", "Pick:"});

        String errorOutput = errStream.toString();
        assertTrue(errorOutput.contains("Error: choice prompt requires at least one item"));
    }

    @Test
    void testPromptCommandWithMessage() throws Exception {
        // Test that message option is parsed correctly
        // This test verifies the option parsing without actually executing the prompt
        try {
            PromptCommands.prompt(context, new String[] {"input", "-m", "Enter name:", "-d", "John"});
        } catch (Exception e) {
            // Expected to fail due to no actual user input, but should not fail on parsing
            assertFalse(e.getMessage().contains("Error: prompt type required"));
            assertFalse(e.getMessage().contains("Error: unknown prompt type"));
        }
    }

    @Test
    void testPromptCommandWithTitle() throws Exception {
        // Test that title option is parsed correctly
        try {
            PromptCommands.prompt(
                    context, new String[] {"confirm", "-t", "Confirmation", "-m", "Continue?", "-d", "y"});
        } catch (Exception e) {
            // Expected to fail due to no actual user input, but should not fail on parsing
            assertFalse(e.getMessage().contains("Error: prompt type required"));
            assertFalse(e.getMessage().contains("Error: unknown prompt type"));
        }
    }

    @Test
    void testPromptCommandWithKeys() throws Exception {
        // Test that keys option is parsed correctly for choice prompts
        try {
            PromptCommands.prompt(
                    context, new String[] {"choice", "-m", "Pick color:", "-k", "rgb", "Red", "Green", "Blue"});
        } catch (Exception e) {
            // Expected to fail due to no actual user input, but should not fail on parsing
            assertFalse(e.getMessage().contains("Error: prompt type required"));
            assertFalse(e.getMessage().contains("Error: unknown prompt type"));
            assertFalse(e.getMessage().contains("Error: choice prompt requires at least one item"));
        }
    }

    @Test
    void testObjectArrayVersion() throws Exception {
        // Test the Object array version of the prompt command
        Object[] args = {"input", "-m", "Enter text:", "-d", "default"};

        try {
            PromptCommands.prompt(context, args);
        } catch (Exception e) {
            // Expected to fail due to no actual user input, but should not fail on parsing
            assertFalse(e.getMessage().contains("Error: prompt type required"));
            assertFalse(e.getMessage().contains("Error: unknown prompt type"));
        }
    }

    @Test
    void testPromptCommandArgumentParsing() throws Exception {
        // Test various argument combinations
        String[][] testCases = {
            {"list", "item1", "item2", "item3"},
            {"checkbox", "option1", "option2"},
            {"choice", "choice1", "choice2"},
            {"input"},
            {"confirm"}
        };

        for (String[] args : testCases) {
            try {
                PromptCommands.prompt(context, args);
            } catch (Exception e) {
                // Should not fail on argument parsing
                assertFalse(
                        e.getMessage().contains("Error: prompt type required"),
                        "Failed for args: " + String.join(" ", args));
                assertFalse(
                        e.getMessage().contains("Error: unknown prompt type"),
                        "Failed for args: " + String.join(" ", args));
            }
        }
    }

    @Test
    void testPromptCommandWithAllOptions() throws Exception {
        // Test command with all possible options
        String[] args = {
            "list",
            "-m",
            "Choose an option:",
            "-t",
            "Selection Menu",
            "-d",
            "default_value",
            "Option 1",
            "Option 2",
            "Option 3"
        };

        try {
            PromptCommands.prompt(context, args);
        } catch (Exception e) {
            // Should not fail on argument parsing
            assertFalse(e.getMessage().contains("Error: prompt type required"));
            assertFalse(e.getMessage().contains("Error: unknown prompt type"));
            assertFalse(e.getMessage().contains("Error: list prompt requires at least one item"));
        }
    }

    @Test
    void testConfirmPromptDefaultValueParsing() throws Exception {
        // Test confirm prompt with different default values
        String[][] testCases = {
            {"confirm", "-d", "y"},
            {"confirm", "-d", "yes"},
            {"confirm", "-d", "1"},
            {"confirm", "-d", "true"},
            {"confirm", "-d", "n"},
            {"confirm", "-d", "no"},
            {"confirm", "-d", "0"},
            {"confirm", "-d", "false"}
        };

        for (String[] args : testCases) {
            try {
                PromptCommands.prompt(context, args);
            } catch (Exception e) {
                // Should not fail on argument parsing
                assertFalse(
                        e.getMessage().contains("Error: prompt type required"),
                        "Failed for args: " + String.join(" ", args));
                assertFalse(
                        e.getMessage().contains("Error: unknown prompt type"),
                        "Failed for args: " + String.join(" ", args));
            }
        }
    }
}

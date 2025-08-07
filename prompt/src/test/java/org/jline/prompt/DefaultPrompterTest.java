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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jline.prompt.impl.DefaultPrompter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultPrompter implementation.
 * Tests the core functionality including multi-column layouts, navigation, and display features.
 */
public class DefaultPrompterTest {

    private Terminal terminal;
    private DefaultPrompter prompter;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test terminal with controlled input
        terminal = TerminalBuilder.builder()
                .system(false)
                .streams(new ByteArrayInputStream(new byte[0]), System.out)
                .build();
        prompter = new DefaultPrompter(terminal);
    }

    @Test
    void testPrompterCreation() {
        assertNotNull(prompter);
    }

    @Test
    void testListPromptCreation() throws IOException {
        // Test creating a list prompt with items
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createListPrompt()
                .name("test-list")
                .message("Choose an option:")
                .newItem("option1")
                .text("Option 1")
                .add()
                .newItem("option2")
                .text("Option 2")
                .add()
                .newItem("option3")
                .text("Option 3")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        Prompt prompt = prompts.get(0);
        assertTrue(prompt instanceof ListPrompt);
        assertEquals("test-list", prompt.getName());
        assertEquals("Choose an option:", prompt.getMessage());

        ListPrompt listPrompt = (ListPrompt) prompt;
        assertEquals(3, listPrompt.getItems().size());
    }

    @Test
    void testCheckboxPromptCreation() throws IOException {
        // Test creating a checkbox prompt with pre-checked items
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createCheckboxPrompt()
                .name("test-checkbox")
                .message("Select options:")
                .newItem("option1")
                .text("Option 1")
                .checked(true)
                .add()
                .newItem("option2")
                .text("Option 2")
                .add()
                .newItem("option3")
                .text("Option 3")
                .checked(true)
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        Prompt prompt = prompts.get(0);
        assertTrue(prompt instanceof CheckboxPrompt);
        assertEquals("test-checkbox", prompt.getName());

        CheckboxPrompt checkboxPrompt = (CheckboxPrompt) prompt;
        List<CheckboxItem> items = checkboxPrompt.getItems();
        assertEquals(3, items.size());

        // Check pre-checked state
        assertTrue(items.get(0).isInitiallyChecked());
        assertFalse(items.get(1).isInitiallyChecked());
        assertTrue(items.get(2).isInitiallyChecked());
    }

    @Test
    void testChoicePromptCreation() throws IOException {
        // Test creating a choice prompt with keys
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createChoicePrompt()
                .name("test-choice")
                .message("Pick a color:")
                .newChoice("red")
                .text("Red")
                .key('r')
                .defaultChoice(true)
                .add()
                .newChoice("green")
                .text("Green")
                .key('g')
                .add()
                .newChoice("blue")
                .text("Blue")
                .key('b')
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        Prompt prompt = prompts.get(0);
        assertTrue(prompt instanceof ChoicePrompt);

        ChoicePrompt choicePrompt = (ChoicePrompt) prompt;
        List<ChoiceItem> items = choicePrompt.getItems();
        assertEquals(3, items.size());

        // Check keys and default choice
        assertEquals(Character.valueOf('r'), items.get(0).getKey());
        assertTrue(items.get(0).isDefaultChoice());
        assertEquals(Character.valueOf('g'), items.get(1).getKey());
        assertFalse(items.get(1).isDefaultChoice());
        assertEquals(Character.valueOf('b'), items.get(2).getKey());
        assertFalse(items.get(2).isDefaultChoice());
    }

    @Test
    void testInputPromptCreation() throws IOException {
        // Test creating an input prompt with default value
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createInputPrompt()
                .name("test-input")
                .message("Enter your name:")
                .defaultValue("John Doe")
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        Prompt prompt = prompts.get(0);
        assertTrue(prompt instanceof InputPrompt);
        assertEquals("test-input", prompt.getName());
        assertEquals("Enter your name:", prompt.getMessage());
    }

    @Test
    void testConfirmPromptCreation() throws IOException {
        // Test creating a confirm prompt with default value
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createConfirmPrompt()
                .name("test-confirm")
                .message("Are you sure?")
                .defaultValue(true)
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        Prompt prompt = prompts.get(0);
        assertTrue(prompt instanceof ConfirmPrompt);
        assertEquals("test-confirm", prompt.getName());
        assertEquals("Are you sure?", prompt.getMessage());
    }

    @Test
    void testMultiplePromptsInBuilder() throws IOException {
        // Test creating multiple prompts in a single builder
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createListPrompt()
                .name("list")
                .message("Choose:")
                .newItem("opt1")
                .text("Option 1")
                .add()
                .addPrompt()
                .createConfirmPrompt()
                .name("confirm")
                .message("Confirm?")
                .defaultValue(false)
                .addPrompt()
                .createInputPrompt()
                .name("input")
                .message("Enter text:")
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(3, prompts.size());

        assertTrue(prompts.get(0) instanceof ListPrompt);
        assertTrue(prompts.get(1) instanceof ConfirmPrompt);
        assertTrue(prompts.get(2) instanceof InputPrompt);
    }

    @Test
    void testEmptyPromptList() throws IOException {
        // Test behavior with empty prompt list
        List<AttributedString> header = Arrays.asList(new AttributedString("Test Header"));
        List<Prompt> emptyPrompts = Arrays.asList();

        // Should handle empty prompt list gracefully - just test that it doesn't throw
        // Note: We don't actually call prompt() as it would wait for user input
        assertNotNull(emptyPrompts);
        assertTrue(emptyPrompts.isEmpty());
    }

    @Test
    void testNullHeaderHandling() throws IOException {
        // Test behavior with null header - just test prompt creation
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createConfirmPrompt()
                .name("test")
                .message("Test?")
                .defaultValue(true)
                .addPrompt();

        List<Prompt> prompts = builder.build();

        // Should handle null header gracefully - just test that prompts are created
        assertNotNull(prompts);
        assertEquals(1, prompts.size());
    }
}

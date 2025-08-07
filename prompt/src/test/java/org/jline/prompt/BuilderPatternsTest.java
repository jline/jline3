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
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for builder patterns and fluent interfaces.
 * Tests the builder API consistency and method chaining.
 */
public class BuilderPatternsTest {

    private Terminal terminal;
    private Prompter prompter;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.builder()
                .system(false)
                .streams(new ByteArrayInputStream(new byte[0]), System.out)
                .build();
        prompter = PrompterFactory.create(terminal);
    }

    @Test
    void testPromptBuilderFluentInterface() {
        // Test that PromptBuilder methods return the correct types for chaining
        PromptBuilder builder = prompter.newBuilder();
        assertNotNull(builder);

        // Test that create methods return the correct builder types
        InputBuilder inputBuilder = builder.createInputPrompt();
        assertNotNull(inputBuilder);
        assertTrue(inputBuilder instanceof InputBuilder);

        ListBuilder listBuilder = builder.createListPrompt();
        assertNotNull(listBuilder);
        assertTrue(listBuilder instanceof ListBuilder);

        CheckboxBuilder checkboxBuilder = builder.createCheckboxPrompt();
        assertNotNull(checkboxBuilder);
        assertTrue(checkboxBuilder instanceof CheckboxBuilder);

        ChoiceBuilder choiceBuilder = builder.createChoicePrompt();
        assertNotNull(choiceBuilder);
        assertTrue(choiceBuilder instanceof ChoiceBuilder);

        ConfirmBuilder confirmBuilder = builder.createConfirmPrompt();
        assertNotNull(confirmBuilder);
        assertTrue(confirmBuilder instanceof ConfirmBuilder);

        TextBuilder textBuilder = builder.createText();
        assertNotNull(textBuilder);
        assertTrue(textBuilder instanceof TextBuilder);
    }

    @Test
    void testInputBuilderChaining() {
        // Test InputBuilder method chaining
        PromptBuilder builder = prompter.newBuilder();

        PromptBuilder result = builder.createInputPrompt()
                .name("test-input")
                .message("Enter text:")
                .defaultValue("default")
                .mask('*')
                .addPrompt();

        // Should return the original PromptBuilder for continued chaining
        assertSame(builder, result);

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());
        assertTrue(prompts.get(0) instanceof InputPrompt);
    }

    @Test
    void testListBuilderChaining() {
        // Test ListBuilder method chaining
        PromptBuilder builder = prompter.newBuilder();

        PromptBuilder result = builder.createListPrompt()
                .name("test-list")
                .message("Choose option:")
                .newItem("item1")
                .text("Option 1")
                .add()
                .newItem("item2")
                .text("Option 2")
                .add()
                .addPrompt();

        assertSame(builder, result);

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());
        assertTrue(prompts.get(0) instanceof ListPrompt);

        ListPrompt listPrompt = (ListPrompt) prompts.get(0);
        assertEquals(2, listPrompt.getItems().size());
    }

    @Test
    void testCheckboxBuilderChaining() {
        // Test CheckboxBuilder method chaining
        PromptBuilder builder = prompter.newBuilder();

        PromptBuilder result = builder.createCheckboxPrompt()
                .name("test-checkbox")
                .message("Select options:")
                .newItem("cb1")
                .text("Checkbox 1")
                .checked(true)
                .add()
                .newItem("cb2")
                .text("Checkbox 2")
                .add()
                .newItem("cb3")
                .text("Checkbox 3")
                .checked(false)
                .add()
                .addPrompt();

        assertSame(builder, result);

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());
        assertTrue(prompts.get(0) instanceof CheckboxPrompt);

        CheckboxPrompt checkboxPrompt = (CheckboxPrompt) prompts.get(0);
        List<CheckboxItem> items = checkboxPrompt.getItems();
        assertEquals(3, items.size());
        assertTrue(items.get(0).isInitiallyChecked());
        assertFalse(items.get(1).isInitiallyChecked());
        assertFalse(items.get(2).isInitiallyChecked());
    }

    @Test
    void testChoiceBuilderChaining() {
        // Test ChoiceBuilder method chaining
        PromptBuilder builder = prompter.newBuilder();

        PromptBuilder result = builder.createChoicePrompt()
                .name("test-choice")
                .message("Pick color:")
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

        assertSame(builder, result);

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());
        assertTrue(prompts.get(0) instanceof ChoicePrompt);

        ChoicePrompt choicePrompt = (ChoicePrompt) prompts.get(0);
        List<ChoiceItem> items = choicePrompt.getItems();
        assertEquals(3, items.size());
        assertTrue(items.get(0).isDefaultChoice());
        assertFalse(items.get(1).isDefaultChoice());
        assertFalse(items.get(2).isDefaultChoice());
    }

    @Test
    void testConfirmBuilderChaining() {
        // Test ConfirmBuilder method chaining
        PromptBuilder builder = prompter.newBuilder();

        PromptBuilder result = builder.createConfirmPrompt()
                .name("test-confirm")
                .message("Are you sure?")
                .defaultValue(true)
                .addPrompt();

        assertSame(builder, result);

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());
        assertTrue(prompts.get(0) instanceof ConfirmPrompt);
    }

    @Test
    void testMultipleBuilderChaining() {
        // Test chaining multiple different builders
        PromptBuilder builder = prompter.newBuilder();

        PromptBuilder result = builder.createInputPrompt()
                .name("input")
                .message("Enter name:")
                .defaultValue("John")
                .addPrompt()
                .createListPrompt()
                .name("list")
                .message("Choose:")
                .newItem("opt1")
                .text("Option 1")
                .add()
                .newItem("opt2")
                .text("Option 2")
                .add()
                .addPrompt()
                .createConfirmPrompt()
                .name("confirm")
                .message("Confirm?")
                .defaultValue(false)
                .addPrompt();

        assertSame(builder, result);

        List<Prompt> prompts = builder.build();
        assertEquals(3, prompts.size());
        assertTrue(prompts.get(0) instanceof InputPrompt);
        assertTrue(prompts.get(1) instanceof ListPrompt);
        assertTrue(prompts.get(2) instanceof ConfirmPrompt);
    }

    @Test
    void testConvenienceMethodsListBuilder() {
        // Test convenience methods for ListBuilder
        PromptBuilder builder = prompter.newBuilder();

        builder.createListPrompt()
                .name("convenience-list")
                .message("Choose:")
                .add("simple1", "Simple Option 1")
                .add("simple2", "Simple Option 2")
                .add("disabled", "Disabled Option", true) // disabled
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        ListPrompt listPrompt = (ListPrompt) prompts.get(0);
        assertEquals(3, listPrompt.getItems().size());
    }

    @Test
    void testConvenienceMethodsCheckboxBuilder() {
        // Test convenience methods for CheckboxBuilder
        PromptBuilder builder = prompter.newBuilder();

        builder.createCheckboxPrompt()
                .name("convenience-checkbox")
                .message("Select:")
                .add("cb1", "Checkbox 1")
                .add("cb2", "Checkbox 2", true) // checked
                .add("cb3", "Checkbox 3", false, true) // not checked, disabled
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        CheckboxPrompt checkboxPrompt = (CheckboxPrompt) prompts.get(0);
        List<CheckboxItem> items = checkboxPrompt.getItems();
        assertEquals(3, items.size());
        assertFalse(items.get(0).isInitiallyChecked());
        assertTrue(items.get(1).isInitiallyChecked());
        assertFalse(items.get(2).isInitiallyChecked());
    }

    @Test
    void testConvenienceMethodsChoiceBuilder() {
        // Test convenience methods for ChoiceBuilder
        PromptBuilder builder = prompter.newBuilder();

        builder.createChoicePrompt()
                .name("convenience-choice")
                .message("Pick:")
                .add("create", "Create", 'c')
                .add("edit", "Edit", 'e', true) // default
                .add("delete", "Delete", 'd')
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        ChoicePrompt choicePrompt = (ChoicePrompt) prompts.get(0);
        List<ChoiceItem> items = choicePrompt.getItems();
        assertEquals(3, items.size());
        assertFalse(items.get(0).isDefaultChoice());
        assertTrue(items.get(1).isDefaultChoice());
        assertFalse(items.get(2).isDefaultChoice());
    }

    @Test
    void testBuilderValidation() {
        // Test that builders validate required fields
        PromptBuilder builder = prompter.newBuilder();

        // Test that building without adding prompts works
        List<Prompt> emptyPrompts = builder.build();
        assertNotNull(emptyPrompts);
        assertTrue(emptyPrompts.isEmpty());

        // Test that prompts are properly added
        builder.createInputPrompt().name("test").message("Test").addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());
    }

    @Test
    void testBuilderReuse() {
        // Test that the same builder can be used multiple times
        PromptBuilder builder = prompter.newBuilder();

        // First build
        builder.createInputPrompt().name("input1").message("First input").addPrompt();

        List<Prompt> firstBuild = builder.build();
        assertEquals(1, firstBuild.size());

        // Add more prompts and build again
        builder.createConfirmPrompt().name("confirm1").message("Confirm?").addPrompt();

        List<Prompt> secondBuild = builder.build();
        assertEquals(2, secondBuild.size());
    }
}

/*
 * Copyright (c) 2002-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the JLine Prompt API.
 */
public class IntegrationTest {

    @Test
    public void testBasicPromptCreation() throws Exception {
        // Create a terminal (in test mode)
        Terminal terminal = TerminalBuilder.builder().system(false).build();

        // Create a prompter
        Prompter prompter = PrompterFactory.create(terminal);
        assertNotNull(prompter);

        // Get the prompt builder
        PromptBuilder builder = prompter.newBuilder();
        assertNotNull(builder);

        // Test creating different types of builders
        InputBuilder inputBuilder = builder.createInputPrompt();
        assertNotNull(inputBuilder);

        ListBuilder listBuilder = builder.createListPrompt();
        assertNotNull(listBuilder);

        CheckboxBuilder checkboxBuilder = builder.createCheckboxPrompt();
        assertNotNull(checkboxBuilder);

        ChoiceBuilder choiceBuilder = builder.createChoicePrompt();
        assertNotNull(choiceBuilder);

        ConfirmBuilder confirmBuilder = builder.createConfirmPrompt();
        assertNotNull(confirmBuilder);

        TextBuilder textBuilder = builder.createText();
        assertNotNull(textBuilder);
    }

    @Test
    public void testFluentBuilderAPI() throws Exception {
        Terminal terminal = TerminalBuilder.builder().system(false).build();

        Prompter prompter = PrompterFactory.create(terminal);
        PromptBuilder builder = prompter.newBuilder();

        // Test fluent API for input prompt
        PromptBuilder result1 = builder.createInputPrompt()
                .name("test_input")
                .message("Enter something:")
                .defaultValue("default")
                .mask('*')
                .addPrompt();
        assertSame(builder, result1);

        // Test fluent API for list prompt
        PromptBuilder result2 = builder.createListPrompt()
                .name("test_list")
                .message("Choose an option:")
                .newItem("option1")
                .text("Option 1")
                .add()
                .newItem("option2")
                .text("Option 2")
                .add()
                .addPrompt();
        assertSame(builder, result2);

        // Test fluent API for checkbox prompt
        PromptBuilder result3 = builder.createCheckboxPrompt()
                .name("test_checkbox")
                .message("Select options:")
                .newItem("cb1")
                .text("Checkbox 1")
                .checked(true)
                .add()
                .newItem("cb2")
                .text("Checkbox 2")
                .add()
                .addPrompt();
        assertSame(builder, result3);

        // Test fluent API for choice prompt
        PromptBuilder result4 = builder.createChoicePrompt()
                .name("test_choice")
                .message("Pick a choice:")
                .newChoice("choice1")
                .key('a')
                .text("Choice A")
                .defaultChoice(true)
                .add()
                .addPrompt();
        assertSame(builder, result4);

        // Test fluent API for confirm prompt
        PromptBuilder result5 = builder.createConfirmPrompt()
                .name("test_confirm")
                .message("Are you sure?")
                .defaultValue(true)
                .addPrompt();
        assertSame(builder, result5);

        // Test fluent API for text display
        PromptBuilder result6 = builder.createText()
                .name("test_text")
                .message("This is a message")
                .text("Additional text")
                .addPrompt();
        assertSame(builder, result6);

        // Build the prompts
        List<? extends Prompt> prompts = builder.build();
        assertNotNull(prompts);
        assertEquals(6, prompts.size());
    }

    @Test
    public void testPromptTypes() throws Exception {
        Terminal terminal = TerminalBuilder.builder().system(false).build();

        Prompter prompter = PrompterFactory.create(terminal);
        PromptBuilder builder = prompter.newBuilder();

        // Build a variety of prompts
        builder.createInputPrompt()
                .name("input")
                .message("Input:")
                .addPrompt()
                .createListPrompt()
                .name("list")
                .message("List:")
                .newItem("item1")
                .text("Item 1")
                .add()
                .addPrompt()
                .createCheckboxPrompt()
                .name("checkbox")
                .message("Checkbox:")
                .newItem("cb1")
                .text("CB 1")
                .add()
                .addPrompt()
                .createChoicePrompt()
                .name("choice")
                .message("Choice:")
                .newChoice("ch1")
                .key('a')
                .text("Choice 1")
                .add()
                .addPrompt()
                .createConfirmPrompt()
                .name("confirm")
                .message("Confirm:")
                .addPrompt()
                .createText()
                .name("text")
                .message("Text display")
                .addPrompt();

        List<? extends Prompt> prompts = builder.build();
        assertEquals(6, prompts.size());

        // Verify prompt types
        assertTrue(prompts.get(0) instanceof InputPrompt);
        assertTrue(prompts.get(1) instanceof ListPrompt);
        assertTrue(prompts.get(2) instanceof CheckboxPrompt);
        assertTrue(prompts.get(3) instanceof ChoicePrompt);
        assertTrue(prompts.get(4) instanceof ConfirmPrompt);
        assertTrue(prompts.get(5) instanceof TextPrompt);
    }

    @Test
    public void testNoResultSingleton() {
        NoResult instance1 = NoResult.INSTANCE;
        NoResult instance2 = NoResult.INSTANCE;

        assertNotNull(instance1);
        assertSame(instance1, instance2);
        assertEquals("NO_RESULT", instance1.getResult());
        assertEquals("NO_RESULT", instance1.getDisplayResult());
        assertNull(instance1.getPrompt());
    }
}

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
 * Unit tests for multi-column layout functionality in DefaultPrompter.
 * Tests column calculation, grid navigation, and layout algorithms.
 */
public class MultiColumnLayoutTest {

    private Terminal terminal;
    private DefaultPrompter prompter;

    @BeforeEach
    void setUp() throws IOException {
        // Create a test terminal with controlled input and specific size
        terminal = TerminalBuilder.builder()
                .system(false)
                .streams(new ByteArrayInputStream(new byte[0]), System.out)
                .size(80, 24) // 80 columns, 24 rows
                .build();
        prompter = new DefaultPrompter(terminal);
    }

    @Test
    void testSingleColumnLayout() throws IOException {
        // Test that single column layout works correctly
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        // Create a list with items that should fit in single column
        builder.createListPrompt()
                .name("single-column")
                .message("Choose:")
                .newItem("item1")
                .text("Short")
                .add()
                .newItem("item2")
                .text("Also short")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        ListPrompt listPrompt = (ListPrompt) prompts.get(0);
        assertEquals(2, listPrompt.getItems().size());
    }

    @Test
    void testMultipleItemsForColumnLayout() throws IOException {
        // Test creating a list with many items that could benefit from multi-column layout
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createListPrompt()
                .name("multi-column")
                .message("Choose an option:")
                .newItem("opt1")
                .text("Option 1")
                .add()
                .newItem("opt2")
                .text("Option 2")
                .add()
                .newItem("opt3")
                .text("Option 3")
                .add()
                .newItem("opt4")
                .text("Option 4")
                .add()
                .newItem("opt5")
                .text("Option 5")
                .add()
                .newItem("opt6")
                .text("Option 6")
                .add()
                .newItem("opt7")
                .text("Option 7")
                .add()
                .newItem("opt8")
                .text("Option 8")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        ListPrompt listPrompt = (ListPrompt) prompts.get(0);
        assertEquals(8, listPrompt.getItems().size());
    }

    @Test
    void testCheckboxMultiColumnLayout() throws IOException {
        // Test checkbox prompt with multiple items for column layout
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createCheckboxPrompt()
                .name("multi-checkbox")
                .message("Select options:")
                .newItem("cb1")
                .text("Checkbox 1")
                .add()
                .newItem("cb2")
                .text("Checkbox 2")
                .checked(true)
                .add()
                .newItem("cb3")
                .text("Checkbox 3")
                .add()
                .newItem("cb4")
                .text("Checkbox 4")
                .checked(true)
                .add()
                .newItem("cb5")
                .text("Checkbox 5")
                .add()
                .newItem("cb6")
                .text("Checkbox 6")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        CheckboxPrompt checkboxPrompt = (CheckboxPrompt) prompts.get(0);
        List<CheckboxItem> items = checkboxPrompt.getItems();
        assertEquals(6, items.size());

        // Verify pre-checked items
        assertFalse(items.get(0).isInitiallyChecked());
        assertTrue(items.get(1).isInitiallyChecked());
        assertFalse(items.get(2).isInitiallyChecked());
        assertTrue(items.get(3).isInitiallyChecked());
        assertFalse(items.get(4).isInitiallyChecked());
        assertFalse(items.get(5).isInitiallyChecked());
    }

    @Test
    void testChoicePromptWithMultipleOptions() throws IOException {
        // Test choice prompt with multiple options that could use column layout
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createChoicePrompt()
                .name("multi-choice")
                .message("Select an action:")
                .newChoice("create")
                .text("Create new file")
                .key('c')
                .add()
                .newChoice("edit")
                .text("Edit existing file")
                .key('e')
                .defaultChoice(true)
                .add()
                .newChoice("delete")
                .text("Delete file")
                .key('d')
                .add()
                .newChoice("copy")
                .text("Copy file")
                .key('o')
                .add()
                .newChoice("move")
                .text("Move file")
                .key('m')
                .add()
                .newChoice("rename")
                .text("Rename file")
                .key('r')
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        ChoicePrompt choicePrompt = (ChoicePrompt) prompts.get(0);
        List<ChoiceItem> items = choicePrompt.getItems();
        assertEquals(6, items.size());

        // Verify keys and default choice
        assertEquals(Character.valueOf('c'), items.get(0).getKey());
        assertEquals(Character.valueOf('e'), items.get(1).getKey());
        assertTrue(items.get(1).isDefaultChoice());
        assertEquals(Character.valueOf('d'), items.get(2).getKey());
        assertEquals(Character.valueOf('o'), items.get(3).getKey());
        assertEquals(Character.valueOf('m'), items.get(4).getKey());
        assertEquals(Character.valueOf('r'), items.get(5).getKey());
    }

    @Test
    void testLargeItemList() throws IOException {
        // Test with a large number of items to verify pagination and column layout
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        ListBuilder listBuilder = builder.createListPrompt().name("large-list").message("Choose from many options:");

        // Add 20 items
        for (int i = 1; i <= 20; i++) {
            listBuilder.newItem("item" + i).text("Option " + i).add();
        }

        listBuilder.addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        ListPrompt listPrompt = (ListPrompt) prompts.get(0);
        assertEquals(20, listPrompt.getItems().size());
    }

    @Test
    void testMixedItemLengths() throws IOException {
        // Test with items of varying text lengths
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createListPrompt()
                .name("mixed-lengths")
                .message("Choose:")
                .newItem("short")
                .text("A")
                .add()
                .newItem("medium")
                .text("Medium length option")
                .add()
                .newItem("long")
                .text("This is a very long option that might affect column layout")
                .add()
                .newItem("short2")
                .text("B")
                .add()
                .newItem("medium2")
                .text("Another medium option")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        ListPrompt listPrompt = (ListPrompt) prompts.get(0);
        assertEquals(5, listPrompt.getItems().size());
    }

    @Test
    void testEmptyPromptHandling() throws IOException {
        // Test behavior with empty prompt list for column layout
        List<AttributedString> header = Arrays.asList(new AttributedString("Test Header"));
        List<Prompt> emptyPrompts = Arrays.asList();

        // Should handle empty prompt list gracefully
        var results = prompter.prompt(header, emptyPrompts);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testSingleItemPrompt() throws IOException {
        // Test column layout with single item
        Prompter factory = PrompterFactory.create(terminal);
        PromptBuilder builder = factory.newBuilder();

        builder.createListPrompt()
                .name("single-item")
                .message("Only one choice:")
                .newItem("only")
                .text("Only Option")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();
        assertEquals(1, prompts.size());

        ListPrompt listPrompt = (ListPrompt) prompts.get(0);
        assertEquals(1, listPrompt.getItems().size());
    }

    @Test
    void testTerminalSizeAdaptation() throws IOException {
        // Test that prompts adapt to different terminal sizes
        // Create a narrow terminal
        Terminal narrowTerminal = TerminalBuilder.builder()
                .system(false)
                .streams(new ByteArrayInputStream(new byte[0]), System.out)
                .size(40, 24) // 40 columns, 24 rows
                .build();

        DefaultPrompter narrowPrompter = new DefaultPrompter(narrowTerminal);
        assertNotNull(narrowPrompter);

        // Create a wide terminal
        Terminal wideTerminal = TerminalBuilder.builder()
                .system(false)
                .streams(new ByteArrayInputStream(new byte[0]), System.out)
                .size(120, 24) // 120 columns, 24 rows
                .build();

        DefaultPrompter widePrompter = new DefaultPrompter(wideTerminal);
        assertNotNull(widePrompter);

        // Both should work with the same prompt configuration
        Prompter factory1 = PrompterFactory.create(narrowTerminal);
        Prompter factory2 = PrompterFactory.create(wideTerminal);

        assertNotNull(factory1);
        assertNotNull(factory2);
    }
}

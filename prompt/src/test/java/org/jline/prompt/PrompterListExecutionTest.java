/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for List, Checkbox, and Choice prompts with simulated terminal input.
 */
class PrompterListExecutionTest {

    @Test
    void testListPromptSelectFirstItem() throws Exception {
        // Setup input/output streams
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Create terminal and prompter
        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        // Build a list prompt
        PromptBuilder builder = prompter.newBuilder();
        builder.createListPrompt()
                .name("color")
                .message("Choose a color:")
                .newItem("red")
                .text("Red")
                .add()
                .newItem("green")
                .text("Green")
                .add()
                .newItem("blue")
                .text("Blue")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();

        // Execute the prompt
        Map<String, ? extends PromptResult<? extends Prompt>> results =
                prompter.prompt(Collections.emptyList(), prompts);

        // Verify the result
        assertNotNull(results);
        assertTrue(results.containsKey("color"));

        ListResult result = (ListResult) results.get("color");
        assertEquals("red", result.getSelectedId());
    }

    @Test
    void testListPromptNavigateAndSelect() throws Exception {
        // Setup input/output streams
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        // Down arrow = ESC[B
        outIn.write("\033[B\033[B\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Create terminal and prompter
        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        // Build a list prompt
        PromptBuilder builder = prompter.newBuilder();
        builder.createListPrompt()
                .name("fruit")
                .message("Choose a fruit:")
                .newItem("apple")
                .text("Apple")
                .add()
                .newItem("banana")
                .text("Banana")
                .add()
                .newItem("cherry")
                .text("Cherry")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();

        // Execute the prompt
        Map<String, ? extends PromptResult<? extends Prompt>> results =
                prompter.prompt(Collections.emptyList(), prompts);

        // Verify the result
        assertNotNull(results);
        assertTrue(results.containsKey("fruit"));

        ListResult result = (ListResult) results.get("fruit");
        assertEquals("cherry", result.getSelectedId());
    }

    @Test
    void testCheckboxPromptSelectMultiple() throws Exception {
        // Setup input/output streams
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        // Space, Down arrow, Space, Enter
        outIn.write(" \033[B \n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Create terminal and prompter
        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        // Build a checkbox prompt
        PromptBuilder builder = prompter.newBuilder();
        builder.createCheckboxPrompt()
                .name("toppings")
                .message("Select toppings:")
                .newItem("cheese")
                .text("Cheese")
                .add()
                .newItem("pepperoni")
                .text("Pepperoni")
                .add()
                .newItem("mushrooms")
                .text("Mushrooms")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();

        // Execute the prompt
        Map<String, ? extends PromptResult<? extends Prompt>> results =
                prompter.prompt(Collections.emptyList(), prompts);

        // Verify the result
        assertNotNull(results);
        assertTrue(results.containsKey("toppings"));

        CheckboxResult result = (CheckboxResult) results.get("toppings");
        Set<String> selected = result.getSelectedIds();
        assertEquals(2, selected.size());
        assertTrue(selected.contains("cheese"));
        assertTrue(selected.contains("pepperoni"));
    }

    private Prompter createPrompter(String input) throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write(input.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        return PrompterFactory.create(terminal);
    }

    private ListBuilder buildFruitListPrompt(Prompter prompter) {
        return prompter.newBuilder()
                .createListPrompt()
                .name("fruit")
                .message("Choose a fruit:")
                .add("apple", "Apple")
                .add("banana", "Banana")
                .add("cherry", "Cherry");
    }

    @Test
    void testListPromptFilterableSelectsFilteredItem() throws Exception {
        // Type "ch" to filter, then Enter to select the filtered result ("Cherry")
        Prompter prompter = createPrompter("ch\n");
        PromptBuilder builder = buildFruitListPrompt(prompter).addPrompt();

        ListResult result = (ListResult)
                prompter.prompt(Collections.emptyList(), builder.build()).get("fruit");
        assertEquals("cherry", result.getSelectedId());
    }

    @Test
    void testListPromptNotFilterableIgnoresTypedCharacters() throws Exception {
        // Type "ch" (should be ignored), then Enter to select first item
        Prompter prompter = createPrompter("ch\n");
        PromptBuilder builder = buildFruitListPrompt(prompter).filterable(false).addPrompt();

        ListResult result = (ListResult)
                prompter.prompt(Collections.emptyList(), builder.build()).get("fruit");
        assertEquals("apple", result.getSelectedId());
    }

    private CheckboxBuilder buildToppingsCheckboxPrompt(Prompter prompter) {
        return prompter.newBuilder()
                .createCheckboxPrompt()
                .name("toppings")
                .message("Select toppings:")
                .add("cheese", "Cheese")
                .add("pepperoni", "Pepperoni")
                .add("mushrooms", "Mushrooms");
    }

    @Test
    void testCheckboxPromptFilterableSelectsFilteredItem() throws Exception {
        // Type "ch" to filter to items matching "ch", Space to toggle, Enter to confirm
        Prompter prompter = createPrompter("ch \n");
        PromptBuilder builder = buildToppingsCheckboxPrompt(prompter).addPrompt();

        CheckboxResult result = (CheckboxResult)
                prompter.prompt(Collections.emptyList(), builder.build()).get("toppings");
        Set<String> selected = result.getSelectedIds();
        assertEquals(1, selected.size());
        assertTrue(selected.contains("cheese"));
    }

    @Test
    void testCheckboxPromptNotFilterableIgnoresTypedCharacters() throws Exception {
        // Type "abc" (should be ignored since filterable=false), Space to toggle first item, Enter
        Prompter prompter = createPrompter("abc \n");
        PromptBuilder builder =
                buildToppingsCheckboxPrompt(prompter).filterable(false).addPrompt();

        CheckboxResult result = (CheckboxResult)
                prompter.prompt(Collections.emptyList(), builder.build()).get("toppings");
        Set<String> selected = result.getSelectedIds();
        assertEquals(1, selected.size());
        assertTrue(selected.contains("cheese"));
    }

    @Test
    void testChoicePromptSelectByKey() throws Exception {
        // Setup input/output streams
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("l".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Create terminal and prompter
        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        // Build a choice prompt
        PromptBuilder builder = prompter.newBuilder();
        builder.createChoicePrompt()
                .name("size")
                .message("Choose size:")
                .newChoice("small")
                .text("Small")
                .key('s')
                .add()
                .newChoice("medium")
                .text("Medium")
                .key('m')
                .add()
                .newChoice("large")
                .text("Large")
                .key('l')
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();

        // Execute the prompt
        Map<String, ? extends PromptResult<? extends Prompt>> results =
                prompter.prompt(Collections.emptyList(), prompts);

        // Verify the result
        assertNotNull(results);
        assertTrue(results.containsKey("size"));

        ChoiceResult result = (ChoiceResult) results.get("size");
        assertEquals("large", result.getSelectedId());
    }

    @Test
    void testListPromptShowsFooterForFocusedItemOnly() throws Exception {
        // Select the first item immediately, so only "red" is ever focused.
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createListPrompt()
                .name("color")
                .message("Choose a color:")
                .newItem("red")
                .text("Red")
                .footer("RED_FOOTER warm tone")
                .add()
                .newItem("green")
                .text("Green")
                .footer("GREEN_FOOTER calm tone")
                .add()
                .newItem("blue")
                .text("Blue")
                .footer("BLUE_FOOTER cool tone")
                .add()
                .addPrompt();

        ListResult result = (ListResult)
                prompter.prompt(Collections.emptyList(), builder.build()).get("color");
        assertEquals("red", result.getSelectedId());

        String rendered = out.toString("UTF-8");
        // The focused item's footer is shown ...
        assertTrue(rendered.contains("RED_FOOTER"), "focused item footer should be rendered");
        // ... while a never-focused item's footer is not (the pane tracks focus, it is not always-on).
        assertFalse(rendered.contains("BLUE_FOOTER"), "unfocused item footer should not be rendered");
    }

    @Test
    void testListPromptFooterFollowsFocus() throws Exception {
        // Down, Down, Enter -> focus moves red -> green -> blue.
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("\033[B\033[B\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createListPrompt()
                .name("color")
                .message("Choose a color:")
                .newItem("red")
                .text("Red")
                .footer("RED_FOOTER warm tone")
                .add()
                .newItem("green")
                .text("Green")
                .footer("GREEN_FOOTER calm tone")
                .add()
                .newItem("blue")
                .text("Blue")
                .footer("BLUE_FOOTER cool tone")
                .add()
                .addPrompt();

        ListResult result = (ListResult)
                prompter.prompt(Collections.emptyList(), builder.build()).get("color");
        assertEquals("blue", result.getSelectedId());

        String rendered = out.toString("UTF-8");
        // Footer was shown for the initially focused item and for the finally focused item.
        assertTrue(rendered.contains("RED_FOOTER"), "initial footer should be rendered");
        assertTrue(rendered.contains("BLUE_FOOTER"), "footer should follow focus to the last item");
    }

    @Test
    void testListPromptMultiLineFooter() throws Exception {
        // Focus the first item, whose footer has an explicit line break.
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createListPrompt()
                .name("color")
                .message("Choose a color:")
                .newItem("red")
                .text("Red")
                .footer("Alpha footer line\nBravo footer line")
                .add()
                .newItem("green")
                .text("Green")
                .add()
                .addPrompt();

        ListResult result = (ListResult)
                prompter.prompt(Collections.emptyList(), builder.build()).get("color");
        assertEquals("red", result.getSelectedId());

        String rendered = out.toString("UTF-8");
        assertTrue(rendered.contains("Alpha footer line"), "first footer line should be rendered");
        assertTrue(rendered.contains("Bravo footer line"), "second footer line should be rendered");
    }

    @Test
    void testCheckboxPromptShowsFooterForFocusedItemOnly() throws Exception {
        // Enter immediately, so only the first checkbox item is ever focused.
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createCheckboxPrompt()
                .name("toppings")
                .message("Select toppings:")
                .newItem("cheese")
                .text("Cheese")
                .footer("Footer for cheese")
                .add()
                .newItem("pepperoni")
                .text("Pepperoni")
                .footer("Footer for pepperoni")
                .add()
                .newItem("mushrooms")
                .text("Mushrooms")
                .footer("Footer for mushrooms")
                .add()
                .addPrompt();

        prompter.prompt(Collections.emptyList(), builder.build());

        String rendered = out.toString("UTF-8");
        assertTrue(rendered.contains("Footer for cheese"), "focused checkbox footer should be rendered");
        assertFalse(rendered.contains("Footer for mushrooms"), "unfocused checkbox footer should not be rendered");
    }

    @Test
    void testListPromptWithFootersOnShortTerminalStillRendersItems() throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 3));
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createListPrompt()
                .name("color")
                .message("Choose a color:")
                .newItem("red")
                .text("Red")
                .footer("Footer red")
                .add()
                .newItem("green")
                .text("Green")
                .footer("Footer green")
                .add()
                .newItem("blue")
                .text("Blue")
                .footer("Footer blue")
                .add()
                .addPrompt();

        ListResult result = (ListResult)
                prompter.prompt(Collections.emptyList(), builder.build()).get("color");
        assertEquals("red", result.getSelectedId());
        String rendered = out.toString("UTF-8");
        assertTrue(
                rendered.contains("Red") || rendered.contains("Green") || rendered.contains("Blue"),
                "at least one item must render on a short terminal (clamp prevents an empty list)");
    }

    @Test
    void testListPromptFilterWithFootersSelectsMatch() throws Exception {
        // Filtering down to a single match while footers are present must still select correctly.
        Prompter prompter = createPrompter("blue\n");
        PromptBuilder builder = prompter.newBuilder()
                .createListPrompt()
                .name("color")
                .message("Choose a color:")
                .newItem("red")
                .text("Red")
                .footer("Footer red")
                .add()
                .newItem("green")
                .text("Green")
                .footer("Footer green")
                .add()
                .newItem("blue")
                .text("Blue")
                .footer("Footer blue")
                .add()
                .addPrompt();

        ListResult result = (ListResult)
                prompter.prompt(Collections.emptyList(), builder.build()).get("color");
        assertEquals("blue", result.getSelectedId());
    }
}

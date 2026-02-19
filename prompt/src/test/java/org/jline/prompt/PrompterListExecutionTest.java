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
public class PrompterListExecutionTest {

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
        terminal.setSize(new Size(160, 80));
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
        terminal.setSize(new Size(160, 80));
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
        terminal.setSize(new Size(160, 80));
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
        terminal.setSize(new Size(160, 80));
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
}

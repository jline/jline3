/*
 * Copyright (c) 2025, the original author(s).
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

import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that actually execute the Prompter.prompt() method
 * with simulated terminal input and verify the results.
 */
public class PrompterExecutionTest {

    @Test
    void testInputPromptWithSimpleText() throws Exception {
        // Setup input/output streams
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("John\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Create terminal and prompter
        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(new Size(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        // Build an input prompt
        PromptBuilder builder = prompter.newBuilder();
        builder.createInputPrompt().name("username").message("Enter your name:").addPrompt();

        List<Prompt> prompts = builder.build();

        // Execute the prompt
        Map<String, ? extends PromptResult<? extends Prompt>> results =
                prompter.prompt(Collections.emptyList(), prompts);

        // Verify the result
        assertNotNull(results);
        assertTrue(results.containsKey("username"));

        PromptResult<?> result = results.get("username");
        assertTrue(result instanceof InputResult);

        InputResult inputResult = (InputResult) result;
        assertEquals("John", inputResult.getInput());
    }

    @Test
    void testInputPromptWithDefaultValue() throws Exception {
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

        // Build an input prompt with default value
        PromptBuilder builder = prompter.newBuilder();
        builder.createInputPrompt()
                .name("city")
                .message("Enter your city:")
                .defaultValue("New York")
                .addPrompt();

        List<Prompt> prompts = builder.build();

        // Execute the prompt
        Map<String, ? extends PromptResult<? extends Prompt>> results =
                prompter.prompt(Collections.emptyList(), prompts);

        // Verify the result
        assertNotNull(results);
        assertTrue(results.containsKey("city"));

        InputResult result = (InputResult) results.get("city");
        assertEquals("New York", result.getInput());
    }

    @Test
    void testConfirmPromptYes() throws Exception {
        // Setup input/output streams
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("y\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Create terminal and prompter
        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(new Size(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        // Build a confirm prompt
        PromptBuilder builder = prompter.newBuilder();
        builder.createConfirmPrompt().name("agree").message("Do you agree?").addPrompt();

        List<Prompt> prompts = builder.build();

        // Execute the prompt
        Map<String, ? extends PromptResult<? extends Prompt>> results =
                prompter.prompt(Collections.emptyList(), prompts);

        // Verify the result
        assertNotNull(results);
        assertTrue(results.containsKey("agree"));

        ConfirmResult result = (ConfirmResult) results.get("agree");
        assertEquals(ConfirmResult.ConfirmationValue.YES, result.getConfirmed());
    }

    @Test
    void testConfirmPromptNo() throws Exception {
        // Setup input/output streams
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("n\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Create terminal and prompter
        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(new Size(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        // Build a confirm prompt
        PromptBuilder builder = prompter.newBuilder();
        builder.createConfirmPrompt().name("delete").message("Delete file?").addPrompt();

        List<Prompt> prompts = builder.build();

        // Execute the prompt
        Map<String, ? extends PromptResult<? extends Prompt>> results =
                prompter.prompt(Collections.emptyList(), prompts);

        // Verify the result
        assertNotNull(results);
        assertTrue(results.containsKey("delete"));

        ConfirmResult result = (ConfirmResult) results.get("delete");
        assertEquals(ConfirmResult.ConfirmationValue.NO, result.getConfirmed());
    }
}

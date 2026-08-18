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
import java.util.function.Function;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the text typed at a password prompt is never surfaced in clear text,
 * neither through the result's display value nor on the terminal.
 */
class PasswordPromptMaskingTest {

    private static Map<String, ? extends PromptResult<? extends Prompt>> runPassword(
            String typed, boolean showMask, ByteArrayOutputStream out) throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);

        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        // Turn off the line discipline's own echo before any input is queued, so everything
        // captured in `out` is what the prompter rendered, not the terminal echoing the pipe.
        Attributes attributes = terminal.getAttributes();
        attributes.setLocalFlag(Attributes.LocalFlag.ECHO, false);
        terminal.setAttributes(attributes);
        outIn.write((typed + "\n").getBytes(StandardCharsets.UTF_8));
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createPasswordPrompt()
                .name("pw")
                .message("Password:")
                .showMask(showMask)
                .addPrompt();
        List<Prompt> prompts = builder.build();
        return prompter.prompt(Collections.emptyList(), prompts);
    }

    @Test
    void maskedDisplayNeverLeaksPassword() throws Exception {
        String secret = "hunter2";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, ? extends PromptResult<? extends Prompt>> results = runPassword(secret, true, out);

        InputResult result = (InputResult) results.get("pw");
        // The real value is still available to the caller.
        assertEquals(secret, result.getInput());
        // The display value that gets echoed back into the prompt header must be masked.
        assertEquals("*******", result.getDisplayResult());
        // The header line rendered on the terminal carries the mask, not the typed password.
        String rendered = out.toString(StandardCharsets.UTF_8);
        assertTrue(rendered.contains("*******"));
        assertFalse(rendered.contains(secret));
    }

    @Test
    void hiddenMaskShowsNothing() throws Exception {
        String secret = "s3cr3t";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Map<String, ? extends PromptResult<? extends Prompt>> results = runPassword(secret, false, out);

        InputResult result = (InputResult) results.get("pw");
        assertEquals(secret, result.getInput());
        assertEquals("", result.getDisplayResult());
        assertFalse(out.toString(StandardCharsets.UTF_8).contains(secret));
    }

    @Test
    void nullMaskPasswordPromptStillMasksDisplay() throws Exception {
        String secret = "hunter2";
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write((secret + "\n").getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        Prompter prompter = PrompterFactory.create(terminal);

        // A custom PasswordPrompt may leave getMask() null; the interface documents that
        // as "use the default mask '*'", so the display value must still be masked.
        PasswordPrompt prompt = new PasswordPrompt() {
            @Override
            public String getName() {
                return "pw";
            }

            @Override
            public String getMessage() {
                return "Password:";
            }

            @Override
            public String getDefaultValue() {
                return null;
            }

            @Override
            public Character getMask() {
                return null;
            }

            @Override
            public Completer getCompleter() {
                return null;
            }

            @Override
            public LineReader getLineReader() {
                return null;
            }

            @Override
            public Function<String, Boolean> getValidator() {
                return null;
            }
        };
        Map<String, ? extends PromptResult<? extends Prompt>> results =
                prompter.prompt(Collections.emptyList(), Collections.<Prompt>singletonList(prompt));

        InputResult result = (InputResult) results.get("pw");
        assertEquals(secret, result.getInput());
        assertEquals("*******", result.getDisplayResult());
    }
}

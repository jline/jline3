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
import java.util.Collections;
import java.util.List;

import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that Ctrl+C (byte 0x03) during prompts throws {@link UserInterruptException}.
 *
 * <p>ISIG must be cleared before writing the byte, otherwise the line discipline
 * consumes it as a signal rather than forwarding it to the binding reader.
 * In production, {@code enterRawMode()} clears ISIG; in tests we clear it
 * manually before feeding the byte into the pipe.
 */
class PromptCancelTest {

    private static final byte CTRL_C = 3;

    private Terminal createTerminal(PipedInputStream in, ByteArrayOutputStream out) throws Exception {
        Terminal terminal =
                TerminalBuilder.builder().type("ansi").streams(in, out).build();
        terminal.setSize(Size.of(160, 80));
        // Clear ISIG so 0x03 passes through the line discipline as a raw byte
        Attributes attr = terminal.getAttributes();
        attr.setLocalFlag(LocalFlag.ISIG, false);
        terminal.setAttributes(attr);
        return terminal;
    }

    @Test
    void testListPromptCtrlCThrowsUserInterrupt() throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal = createTerminal(in, out);
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createListPrompt()
                .name("x")
                .message("Pick one")
                .newItem("a")
                .text("a")
                .add()
                .newItem("b")
                .text("b")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();

        outIn.write(CTRL_C);
        outIn.flush();

        assertThrows(UserInterruptException.class, () -> prompter.prompt(Collections.emptyList(), prompts));
    }

    @Test
    void testCheckboxPromptCtrlCThrowsUserInterrupt() throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal = createTerminal(in, out);
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createCheckboxPrompt()
                .name("items")
                .message("Select items")
                .newItem("x")
                .text("X")
                .add()
                .newItem("y")
                .text("Y")
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();

        outIn.write(CTRL_C);
        outIn.flush();

        assertThrows(UserInterruptException.class, () -> prompter.prompt(Collections.emptyList(), prompts));
    }

    @Test
    void testConfirmPromptCtrlCThrowsUserInterrupt() throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal = createTerminal(in, out);
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createConfirmPrompt().name("ok").message("Continue?").addPrompt();

        List<Prompt> prompts = builder.build();

        outIn.write(CTRL_C);
        outIn.flush();

        assertThrows(UserInterruptException.class, () -> prompter.prompt(Collections.emptyList(), prompts));
    }

    @Test
    void testChoicePromptCtrlCThrowsUserInterrupt() throws Exception {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Terminal terminal = createTerminal(in, out);
        Prompter prompter = PrompterFactory.create(terminal);

        PromptBuilder builder = prompter.newBuilder();
        builder.createChoicePrompt()
                .name("choice")
                .message("Pick one")
                .newChoice("a")
                .text("A")
                .key('a')
                .add()
                .newChoice("b")
                .text("B")
                .key('b')
                .add()
                .addPrompt();

        List<Prompt> prompts = builder.build();

        outIn.write(CTRL_C);
        outIn.flush();

        assertThrows(UserInterruptException.class, () -> prompter.prompt(Collections.emptyList(), prompts));
    }
}

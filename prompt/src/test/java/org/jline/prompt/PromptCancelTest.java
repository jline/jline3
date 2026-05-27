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
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jline.reader.UserInterruptException;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class PromptCancelTest {

    private static final byte CTRL_C = 0x03;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long INPUT_DELAY_MS = 200;

    @Test
    void list() throws Exception {
        try (Fixture f = new Fixture()) {
            PromptBuilder b = f.prompter.newBuilder();
            b.createListPrompt()
                    .name("pick")
                    .message("Pick one")
                    .newItem("a")
                    .text("Alpha")
                    .add()
                    .newItem("b")
                    .text("Bravo")
                    .add()
                    .addPrompt();
            assertCancels(f, b.build());
        }
    }

    @Test
    void checkbox() throws Exception {
        try (Fixture f = new Fixture()) {
            PromptBuilder b = f.prompter.newBuilder();
            b.createCheckboxPrompt()
                    .name("toppings")
                    .message("Pick toppings")
                    .newItem("cheese")
                    .text("Cheese")
                    .add()
                    .newItem("ham")
                    .text("Ham")
                    .add()
                    .addPrompt();
            assertCancels(f, b.build());
        }
    }

    @Test
    void choice() throws Exception {
        try (Fixture f = new Fixture()) {
            PromptBuilder b = f.prompter.newBuilder();
            b.createChoicePrompt()
                    .name("color")
                    .message("Pick a color")
                    .newChoice("red")
                    .text("Red")
                    .key('r')
                    .add()
                    .newChoice("green")
                    .text("Green")
                    .key('g')
                    .add()
                    .addPrompt();
            assertCancels(f, b.build());
        }
    }

    @Test
    void confirm() throws Exception {
        try (Fixture f = new Fixture()) {
            PromptBuilder b = f.prompter.newBuilder();
            b.createConfirmPrompt().name("agree").message("Do you agree?").addPrompt();
            assertCancels(f, b.build());
        }
    }

    private static void assertCancels(Fixture f, List<Prompt> prompts) {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            f.sendCtrlCAfter(INPUT_DELAY_MS);
            assertThrows(UserInterruptException.class, () -> f.prompter.prompt(List.of(), prompts));
        });
    }

    private static final class Fixture implements AutoCloseable {
        final PipedOutputStream feeder;
        final Terminal terminal;
        final Prompter prompter;
        volatile Thread sender;

        Fixture() throws Exception {
            PipedInputStream in = new PipedInputStream();
            this.feeder = new PipedOutputStream(in);
            this.terminal = TerminalBuilder.builder()
                    .type("ansi")
                    .streams(in, new ByteArrayOutputStream())
                    .build();
            terminal.setSize(Size.of(160, 80));
            this.prompter = PrompterFactory.create(terminal);
        }

        // Delay so prompt() can call enterRawMode() first, else the pump consumes 0x03 as
        // VINTR under cooked-mode attributes. Block until interrupted so PipedInputStream
        // does not raise "Write end dead" before the keymap matches.
        void sendCtrlCAfter(long delayMillis) {
            sender = new Thread(
                    () -> {
                        try {
                            TimeUnit.MILLISECONDS.sleep(delayMillis);
                            feeder.write(CTRL_C);
                            feeder.flush();
                            Thread.sleep(Long.MAX_VALUE);
                        } catch (Exception ignored) {
                        }
                    },
                    "ctrl-c-sender");
            sender.setDaemon(true);
            sender.start();
        }

        @Override
        public void close() throws Exception {
            if (sender != null) {
                sender.interrupt();
            }
            try {
                terminal.close();
            } finally {
                feeder.close();
            }
        }
    }
}

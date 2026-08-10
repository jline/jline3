/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TmuxEncodingTest {

    @Test
    void paneDecodesMultiByteCharactersCorrectly() throws Exception {
        ByteArrayOutputStream masterOut = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "screen-256color", masterOut, StandardCharsets.UTF_8);
        terminal.setSize(Size.of(80, 24));

        CountDownLatch textWritten = new CountDownLatch(1);
        CountDownLatch testDone = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();

        Tmux tmux = new Tmux(terminal, new PrintStream(OutputStream.nullOutputStream()), paneTerminal -> {
            new Thread(
                            () -> {
                                try {
                                    paneTerminal.writer().print("Hello 世界 café");
                                    paneTerminal.writer().flush();
                                    textWritten.countDown();
                                    testDone.await(5, TimeUnit.SECONDS);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            },
                            "test-pane")
                    .start();
        });

        Thread tmuxThread = new Thread(
                () -> {
                    try {
                        tmux.run();
                    } catch (IOException e) {
                        // expected when terminal closes
                    }
                },
                "tmux-main");
        tmuxThread.setDaemon(true);
        tmuxThread.start();

        assertTrue(textWritten.await(5, TimeUnit.SECONDS), "Text should be written within timeout");

        // Poll for output using Object.wait() instead of Thread.sleep()
        long deadline = System.currentTimeMillis() + 3000;
        String output = "";
        while (System.currentTimeMillis() < deadline) {
            output = masterOut.toString(StandardCharsets.UTF_8);
            if (output.contains("世")) {
                break;
            }
            synchronized (masterOut) {
                masterOut.wait(100);
            }
        }

        testDone.countDown();
        terminal.close();
        tmuxThread.join(5000);

        assertNull(error.get(), "Runner should not throw");

        // Wide CJK characters occupy 2 terminal cells each, so the Display may
        // insert filler between them.  Check for individual characters.
        // Strip ANSI escape sequences to get plain text.
        String plain = output.replaceAll("\\[[^@-~]*[@-~]", "");
        assertTrue(plain.contains("世"), "Should contain first CJK character");
        assertTrue(plain.contains("界"), "Should contain second CJK character");
        assertTrue(plain.contains("café"), "Should contain accented characters");
    }

    @Test
    void paneCannotInjectControlBytesViaSupplementaryCodePoints() throws Exception {
        // U+1001B is a supplementary-plane code point whose low 16 bits are 0x1B (ESC).
        // The pane emulator stores full code points and never keeps C0/C1 controls in a
        // cell, so narrowing the cell to a Java char is the only way ESC reaches the outer
        // terminal.  A pane must not be able to smuggle a control sequence out this way.
        String smp = new String(Character.toChars(0x1001B));
        String paneText = "A" + smp + "]0;INJECTED";

        ByteArrayOutputStream masterOut = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "screen-256color", masterOut, StandardCharsets.UTF_8);
        terminal.setSize(Size.of(80, 24));

        CountDownLatch textWritten = new CountDownLatch(1);
        CountDownLatch testDone = new CountDownLatch(1);

        Tmux tmux = new Tmux(terminal, new PrintStream(OutputStream.nullOutputStream()), paneTerminal -> {
            new Thread(
                            () -> {
                                try {
                                    paneTerminal.writer().print(paneText);
                                    paneTerminal.writer().flush();
                                    textWritten.countDown();
                                    testDone.await(5, TimeUnit.SECONDS);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            },
                            "test-pane")
                    .start();
        });

        Thread tmuxThread = new Thread(
                () -> {
                    try {
                        tmux.run();
                    } catch (IOException e) {
                        // expected when terminal closes
                    }
                },
                "tmux-main");
        tmuxThread.setDaemon(true);
        tmuxThread.start();

        assertTrue(textWritten.await(5, TimeUnit.SECONDS), "Text should be written within timeout");

        long deadline = System.currentTimeMillis() + 3000;
        String output = "";
        while (System.currentTimeMillis() < deadline) {
            output = masterOut.toString(StandardCharsets.UTF_8);
            if (output.contains("INJECTED")) {
                break;
            }
            synchronized (masterOut) {
                masterOut.wait(100);
            }
        }

        testDone.countDown();
        terminal.close();
        tmuxThread.join(5000);

        // The outer terminal only ever receives CSI (ESC [ ...) from the Display, never the
        // OSC the pane tried to synthesize; its presence means the code point was truncated.
        assertFalse(
                output.contains("\u001b]0;INJECTED"), "pane must not inject an OSC sequence into the outer terminal");
        // and the supplementary character is rendered instead of a stray control byte
        String plain = output.replaceAll("\\[[^@-~]*[@-~]", "");
        assertTrue(plain.contains(smp), "supplementary code point should be rendered, not narrowed");
    }
}

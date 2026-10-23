/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Terminal.Signal;
import org.jline.utils.NonBlockingInputStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LineDisciplineTerminalTest {

    @Test
    void signalCharactersRespectEchoFlagsAndInputOrder() throws Exception {
        ControlChar[] chars = {ControlChar.VINTR, ControlChar.VQUIT, ControlChar.VSUSP};
        Signal[] signals = {Signal.INT, Signal.QUIT, Signal.TSTP};
        for (boolean echo : new boolean[] {false, true}) {
            for (boolean echoControl : new boolean[] {false, true}) {
                for (int i = 0; i < chars.length; i++) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    try (LineDisciplineTerminal terminal =
                            new LineDisciplineTerminal("test", "xterm", output, StandardCharsets.UTF_8)) {
                        Attributes attributes = terminal.getAttributes();
                        attributes.setLocalFlag(LocalFlag.ECHO, echo);
                        attributes.setLocalFlag(LocalFlag.ECHOCTL, echoControl);
                        terminal.setAttributes(attributes);
                        List<Signal> raised = new ArrayList<>();
                        terminal.handle(signals[i], raised::add);
                        int c = attributes.getControlChar(chars[i]);

                        terminal.processInputBytes(new byte[] {'a', (byte) c, 'b'});

                        String expected =
                                !echo ? "" : "a" + (echoControl ? "^" + (char) (c + '@') : "" + (char) c) + "b";
                        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
                        terminal.flush();
                        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
                        assertEquals(List.of(signals[i]), raised);
                        assertEquals('a', terminal.input().read());
                        assertEquals('b', terminal.input().read());
                        assertEquals(
                                NonBlockingInputStream.READ_EXPIRED,
                                ((NonBlockingInputStream) terminal.input()).read(1));
                    }
                }
            }
        }
    }

    @Test
    void programmaticSignalsDoNotEchoInputCharacters() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm", output, StandardCharsets.UTF_8)) {
            List<Signal> raised = new ArrayList<>();
            terminal.handle(Signal.INT, raised::add);
            terminal.raise(Signal.INT);
            terminal.flush();

            assertEquals(List.of(Signal.INT), raised);
            assertEquals("", output.toString(StandardCharsets.UTF_8));
        }
    }

    @Test
    void signalEchoRequestsOutputFlush() throws Exception {
        FlushTrackingOutputStream output = new FlushTrackingOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm", output, StandardCharsets.UTF_8)) {
            terminal.handle(Signal.INT, signal -> {});

            terminal.processInputByte(terminal.getAttributes().getControlChar(ControlChar.VINTR));

            assertEquals(1, output.flushCount);
        }
    }

    @Test
    void signalIsDeliveredWhenEchoFails() throws Exception {
        ControlChar[] chars = {ControlChar.VINTR, ControlChar.VQUIT, ControlChar.VSUSP};
        Signal[] signals = {Signal.INT, Signal.QUIT, Signal.TSTP};
        for (boolean echoControl : new boolean[] {false, true}) {
            for (int i = 0; i < chars.length; i++) {
                IOException failure = new IOException("echo failed");
                OutputStream output = new OutputStream() {
                    @Override
                    public void write(int value) throws IOException {
                        throw failure;
                    }
                };
                try (LineDisciplineTerminal terminal =
                        new LineDisciplineTerminal("test", "xterm", output, StandardCharsets.UTF_8)) {
                    Attributes attributes = terminal.getAttributes();
                    attributes.setLocalFlag(LocalFlag.ECHO, true);
                    attributes.setLocalFlag(LocalFlag.ECHOCTL, echoControl);
                    terminal.setAttributes(attributes);
                    List<Signal> raised = new ArrayList<>();
                    terminal.handle(signals[i], raised::add);
                    int c = attributes.getControlChar(chars[i]);

                    IOException thrown = assertThrows(IOException.class, () -> terminal.processInputByte(c));

                    assertSame(failure, thrown);
                    assertEquals(List.of(signals[i]), raised);
                }
            }
        }
    }

    private static final class FlushTrackingOutputStream extends ByteArrayOutputStream {
        private int flushCount;

        @Override
        public void flush() {
            flushCount++;
        }
    }

    @Test
    void printableSignalCharacterIsEchoedLiterally() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm", output, StandardCharsets.UTF_8)) {
            Attributes attributes = terminal.getAttributes();
            attributes.setControlChar(ControlChar.VINTR, 'x');
            terminal.setAttributes(attributes);
            List<Signal> raised = new ArrayList<>();
            terminal.handle(Signal.INT, raised::add);

            terminal.processInputByte('x');

            assertEquals(List.of(Signal.INT), raised);
            assertEquals("x", output.toString(StandardCharsets.UTF_8));
        }
    }
}

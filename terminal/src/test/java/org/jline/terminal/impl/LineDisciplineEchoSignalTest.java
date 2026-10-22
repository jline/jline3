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
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests that LineDisciplineTerminal echoes the signal character (e.g. ^C) only
 * when both ECHO and ECHOCTL are set, matching real TTY behaviour (Linux n_tty,
 * macOS/BSD). See <a href="https://github.com/jline/jline3/issues/2331">#2331</a>.
 */
class LineDisciplineEchoSignalTest {

    /**
     * Creates a LineDisciplineTerminal backed by piped streams.
     * The returned array contains [terminal, keysOut] so the caller can feed input.
     */
    private Object[] buildTerminal(ByteArrayOutputStream output, boolean echo, boolean echoctl) throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream keys = new PipedOutputStream(in);
        LineDisciplineTerminal terminal = new LineDisciplineTerminal("test", "dumb", output, StandardCharsets.UTF_8);
        // Wire the slave input — use processInputByte() to feed keys
        Attributes attrs = terminal.getAttributes();
        attrs.setLocalFlag(LocalFlag.ECHO, echo);
        attrs.setLocalFlag(LocalFlag.ECHOCTL, echoctl);
        terminal.setAttributes(attrs);
        // Install a no-op INT handler so raise() doesn't kill the JVM
        terminal.handle(Terminal.Signal.INT, sig -> {});
        return new Object[] {terminal, keys};
    }

    /**
     * With ECHO=off, pressing Ctrl-C must produce no output at all.
     */
    @Test
    void testNoEchoCtrlC_echoOff() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal = new LineDisciplineTerminal("test", "dumb", output, StandardCharsets.UTF_8);
        Attributes attrs = terminal.getAttributes();
        attrs.setLocalFlag(LocalFlag.ECHO, false);
        attrs.setLocalFlag(LocalFlag.ECHOCTL, true);
        terminal.setAttributes(attrs);
        terminal.handle(Terminal.Signal.INT, sig -> {});

        // Feed: 'a', Ctrl-C (0x03), 'b'
        terminal.processInputBytes(new byte[] {'a', 0x03, 'b'});
        terminal.flush();

        String result = output.toString(StandardCharsets.UTF_8);
        assertEquals("", result, "With ECHO off, ^C should not appear in output");
    }

    /**
     * With ECHO=on and ECHOCTL=off, pressing Ctrl-C must also produce no ^C echo.
     * (Some terminals support this mode to suppress the visible ^C while still processing the signal.)
     */
    @Test
    void testNoEchoCtrlC_echoctlOff() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal = new LineDisciplineTerminal("test", "dumb", output, StandardCharsets.UTF_8);
        Attributes attrs = terminal.getAttributes();
        attrs.setLocalFlag(LocalFlag.ECHO, true);
        attrs.setLocalFlag(LocalFlag.ECHOCTL, false);
        terminal.setAttributes(attrs);
        terminal.handle(Terminal.Signal.INT, sig -> {});

        // Feed: 'a', Ctrl-C (0x03), 'b'
        terminal.processInputBytes(new byte[] {'a', 0x03, 'b'});
        terminal.flush();

        String result = output.toString(StandardCharsets.UTF_8);
        assertEquals("ab", result, "With ECHOCTL off, ^C should not appear in output");
    }

    /**
     * With ECHO=on and ECHOCTL=on (the default), Ctrl-C should echo as ^C.
     * The ^C must appear in sequence with the surrounding characters (a^Cb),
     * not deferred to a later flush.
     */
    @Test
    void testEchoCtrlC_echoOn() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal = new LineDisciplineTerminal("test", "dumb", output, StandardCharsets.UTF_8);
        // ECHO and ECHOCTL are on by default in the initial attributes
        terminal.handle(Terminal.Signal.INT, sig -> {});

        // Feed: 'a', Ctrl-C (0x03), 'b'
        terminal.processInputBytes(new byte[] {'a', 0x03, 'b'});
        // No extra flush needed — the echo happens synchronously in processInputBytes

        String result = output.toString(StandardCharsets.UTF_8);
        assertEquals("a^Cb", result, "With ECHO and ECHOCTL on, ^C should echo as ^C in the right position");
    }
}

/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Attributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static org.junit.jupiter.api.Assertions.*;

class Vt100ResponseFeedbackTest {

    static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    private String readAvailable(InputStream in, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (in.available() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        byte[] buf = new byte[256];
        int total = 0;
        while (in.available() > 0 && total < buf.length) {
            int n = in.read(buf, total, buf.length - total);
            if (n <= 0) break;
            total += n;
        }
        return new String(buf, 0, total, StandardCharsets.UTF_8);
    }

    @Test
    void webTerminalFeedsDsrResponse() throws Exception {
        try (WebTerminal terminal = new WebTerminal("localhost", 0, 80, 24)) {
            Attributes attrs = terminal.getAttributes();
            attrs.setLocalFlag(Attributes.LocalFlag.ECHO, false);
            terminal.setAttributes(attrs);

            terminal.output().write("\033[6n".getBytes(StandardCharsets.UTF_8));
            terminal.output().flush();

            String response = readAvailable(terminal.input(), 2000);
            assertEquals("\033[1;1R", response);
        }
    }

    @Test
    void webTerminalFeedsDeviceAttributesResponse() throws Exception {
        try (WebTerminal terminal = new WebTerminal("localhost", 0, 80, 24)) {
            Attributes attrs = terminal.getAttributes();
            attrs.setLocalFlag(Attributes.LocalFlag.ECHO, false);
            terminal.setAttributes(attrs);

            terminal.output().write("\033[c".getBytes(StandardCharsets.UTF_8));
            terminal.output().flush();

            String response = readAvailable(terminal.input(), 2000);
            assertEquals("\033[?1;2c", response);
        }
    }

    @Test
    void webTerminalDsrReflectsCursorPosition() throws Exception {
        try (WebTerminal terminal = new WebTerminal("localhost", 0, 80, 24)) {
            Attributes attrs = terminal.getAttributes();
            attrs.setLocalFlag(Attributes.LocalFlag.ECHO, false);
            terminal.setAttributes(attrs);

            terminal.output().write("Hello".getBytes(StandardCharsets.UTF_8));
            terminal.output().flush();
            // drain any existing input
            readAvailable(terminal.input(), 100);

            terminal.output().write("\033[6n".getBytes(StandardCharsets.UTF_8));
            terminal.output().flush();

            String response = readAvailable(terminal.input(), 2000);
            assertEquals("\033[1;6R", response);
        }
    }

    @Test
    @DisabledIf("isHeadless")
    void swingTerminalFeedsDsrResponse() throws Exception {
        try (SwingTerminal terminal = new SwingTerminal(80, 24)) {
            Attributes attrs = terminal.getAttributes();
            attrs.setLocalFlag(Attributes.LocalFlag.ECHO, false);
            terminal.setAttributes(attrs);

            terminal.output().write("\033[6n".getBytes(StandardCharsets.UTF_8));
            terminal.output().flush();

            String response = readAvailable(terminal.input(), 2000);
            assertEquals("\033[1;1R", response);
        }
    }

    @Test
    @DisabledIf("isHeadless")
    void swingTerminalFeedsDeviceAttributesResponse() throws Exception {
        try (SwingTerminal terminal = new SwingTerminal(80, 24)) {
            Attributes attrs = terminal.getAttributes();
            attrs.setLocalFlag(Attributes.LocalFlag.ECHO, false);
            terminal.setAttributes(attrs);

            terminal.output().write("\033[c".getBytes(StandardCharsets.UTF_8));
            terminal.output().flush();

            String response = readAvailable(terminal.input(), 2000);
            assertEquals("\033[?1;2c", response);
        }
    }
}

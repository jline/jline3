/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class NanoTest {

    @Test
    @Timeout(1)
    public void nanoBufferLineOverflow() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal = new LineDisciplineTerminal("nano", "xterm", output, StandardCharsets.UTF_8);
        terminal.setSize(new Size(80, 25));
        for (int i = 0; i < 100; i++) {
            terminal.processInputByte(' ');
        }
        terminal.processInputByte(KeyMap.ctrl('X').getBytes()[0]);
        terminal.processInputByte('n');
        String[] argv = {"--ignorercfiles"};
        Nano nano = new Nano(
                terminal,
                Paths.get("target/test.txt"),
                Options.compile(Nano.usage()).parse(argv));
        nano.run();
    }

    @Test
    @Timeout(10)
    public void restrictedModeDoesNotReadArbitraryFiles() throws Exception {
        Path root = Files.createTempDirectory("nano-restricted-root");
        Path secret = Files.createTempFile("nano-restricted-secret", ".txt");
        String marker = "SECRET_OUTSIDE_ROOT_a1b2c3d4";
        Files.write(secret, marker.getBytes(StandardCharsets.UTF_8));
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            LineDisciplineTerminal terminal =
                    new LineDisciplineTerminal("nano", "xterm", output, StandardCharsets.UTF_8);
            terminal.setSize(new Size(80, 25));
            // Deliver Enter as a raw CR (nano runs in raw mode; avoid CR->NL translation)
            Attributes attrs = terminal.getAttributes();
            attrs.setInputFlag(InputFlag.ICRNL, false);
            terminal.setAttributes(attrs);
            // ^R (Read File), type an absolute path escaping the root, accept
            terminal.processInputByte(KeyMap.ctrl('R').getBytes()[0]);
            for (byte b : secret.toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8)) {
                terminal.processInputByte(b);
            }
            terminal.processInputByte('\r');
            // Quit both the (possibly opened) buffer and the original one, discarding changes
            terminal.processInputByte(KeyMap.ctrl('X').getBytes()[0]);
            terminal.processInputByte('n');
            terminal.processInputByte(KeyMap.ctrl('X').getBytes()[0]);
            terminal.processInputByte('n');
            String[] argv = {"--ignorercfiles", "--restricted"};
            Nano nano = new Nano(terminal, root, Options.compile(Nano.usage()).parse(argv));
            nano.run();
            terminal.close();
            assertFalse(
                    output.toString(StandardCharsets.UTF_8.name()).contains(marker),
                    "restricted mode must not read files outside the specified one");
        } finally {
            Files.deleteIfExists(secret);
            Files.deleteIfExists(root);
        }
    }
}

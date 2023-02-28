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
import java.nio.file.Paths;

import org.jline.keymap.KeyMap;
import org.jline.terminal.Size;
import org.jline.terminal.impl.LineDisciplineTerminal;
import org.junit.Test;

public class NanoTest {

    @Test(timeout = 1000)
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
}

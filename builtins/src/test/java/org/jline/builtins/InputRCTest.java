/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Macro;
import org.jline.reader.Reference;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class InputRCTest {

    @Test
    void testInput() throws Exception {
        testLineReader(null, "config1", lr -> {
            assertEquals(new Reference("universal-argument"), lr.getKeys().getBound("" + ((char) ('U' - 'A' + 1))));
            assertEquals(new Macro("Function Key \u2671"), lr.getKeys().getBound("\u001b[11~"));
            assertNull(lr.getKeys().getBound(((char) ('X' - 'A' + 1)) + "q"));
        });

        testLineReader(
                "Bash",
                "config1",
                lr -> assertEquals(
                        new Macro("\u001bb\"\u001bf\""), lr.getKeys().getBound(((char) ('X' - 'A' + 1)) + "q")));
    }

    @Test
    void testInput2() throws Exception {
        testLineReader(
                "Bash", "config2", lr -> assertNotNull(lr.getKeys().getBound("\u001b" + ((char) ('V' - 'A' + 1)))));
    }

    @Test
    void testInputBadConfig() throws Exception {
        testLineReader(
                "Bash",
                "config-bad",
                lr -> assertEquals(
                        new Macro("\u001bb\"\u001bf\""), lr.getKeys().getBound(((char) ('X' - 'A' + 1)) + "q")));
    }

    private void testLineReader(String appName, String config, Consumer<LineReader> consumer) throws IOException {
        try (Terminal terminal = TerminalBuilder.builder()
                .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                .build()) {
            LineReader lr = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .appName(appName)
                    .build();
            InputRC.configure(lr, getClass().getResource(config));
            consumer.accept(lr);
        }
    }
}

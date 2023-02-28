/*
 * Copyright (c) 2002-2018, the original author(s).
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

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Macro;
import org.jline.reader.Reference;
import org.jline.terminal.TerminalBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class InputRCTest {

    @Test
    public void testInput() throws Exception {
        LineReader lr = createReader(null, "config1");
        assertEquals(new Reference("universal-argument"), lr.getKeys().getBound("" + ((char) ('U' - 'A' + 1))));
        assertEquals(new Macro("Function Key \u2671"), lr.getKeys().getBound("\u001b[11~"));
        assertNull(lr.getKeys().getBound(((char) ('X' - 'A' + 1)) + "q"));

        lr = createReader("Bash", "config1");
        assertEquals(new Macro("\u001bb\"\u001bf\""), lr.getKeys().getBound(((char) ('X' - 'A' + 1)) + "q"));
    }

    @Test
    public void testInput2() throws Exception {
        LineReader lr = createReader("Bash", "config2");
        assertNotNull(lr.getKeys().getBound("\u001b" + ((char) ('V' - 'A' + 1))));
    }

    @Test
    public void testInputBadConfig() throws Exception {
        LineReader lr = createReader("Bash", "config-bad");
        assertEquals(new Macro("\u001bb\"\u001bf\""), lr.getKeys().getBound(((char) ('X' - 'A' + 1)) + "q"));
    }

    private LineReader createReader(String appName, String config) throws IOException {
        LineReader lr = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.builder()
                        .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                        .build())
                .appName(appName)
                .build();
        InputRC.configure(lr, getClass().getResource(config));
        return lr;
    }
}

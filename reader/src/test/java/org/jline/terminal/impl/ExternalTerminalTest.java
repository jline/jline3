/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Cursor;
import org.jline.terminal.Terminal;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ExternalTerminalTest {

    @Test
    public void testInput() throws IOException, InterruptedException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExternalTerminal console = new ExternalTerminal("foo", "ansi", in, out, StandardCharsets.UTF_8);

        testConsole(outIn, out, console);
    }

    /* SANDBOX JANSI
    @Test
    public void testPosix() throws IOException, InterruptedException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Console terminal = new PosixPtyConsole("ansi", new ConsoleReaderBuilder(), NativePty.open(null, null), in, out, "UTF-8");

        testConsole(outIn, out, terminal);
    }
    */

    private void testConsole(PipedOutputStream outIn, ByteArrayOutputStream out, Terminal terminal) throws IOException, InterruptedException {
        Attributes attributes = terminal.getAttributes();
        attributes.setLocalFlag(LocalFlag.ECHO, true);
        attributes.setInputFlag(InputFlag.IGNCR, true);
        attributes.setOutputFlags(EnumSet.of(OutputFlag.OPOST));
        terminal.setAttributes(attributes);

        outIn.write("a\r\nb".getBytes());
        while (out.size() < 3) {
            Thread.sleep(100);
        }

        String output = out.toString();
        assertEquals("a\nb", output);
    }

    @Test
    @Ignore("This test very often fails on Travis CI")
    public void testInterrupt() throws Exception {
        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExternalTerminal console = new ExternalTerminal("foo", "ansi", in, out, StandardCharsets.UTF_8);
        Attributes attributes = console.getAttributes();
        attributes.setLocalFlag(LocalFlag.ISIG, true);
        attributes.setControlChar(ControlChar.VINTR, 3);
        console.setAttributes(attributes);
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(console).build();
        assertNotNull(lineReader);
        Thread th = new Thread() {
            public void run() {
                try {
                    outIn.write('a');
                    outIn.write('b');
                    outIn.flush();
                    Thread.sleep(50);
                    outIn.write(3);
                    Thread.sleep(50);
                    outIn.write('c');
                    outIn.flush();
                    Thread.sleep(50);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        try {
            lineReader.readLine();
            fail("Expected UserInterruptException");
        } catch (UserInterruptException e) {
            assertEquals("ab", e.getPartialLine());
        }
        th.join();
    }

    @Test
    public void testCursorPosition() throws IOException  {
        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExternalTerminal console = new ExternalTerminal("foo", "ansi", in, out, StandardCharsets.UTF_8);

        outIn.write(new byte[] { 'a', '\033', 'b', '\033', '[', '2', ';', '3', 'R', 'f'});
        outIn.flush();

        StringBuilder sb = new StringBuilder();
        Cursor cursor = console.getCursorPosition(c -> sb.append((char) c));
        assertNotNull(cursor);
        assertEquals(2, cursor.getX());
        assertEquals(1, cursor.getY());
        assertEquals("a\033b", sb.toString());
        assertEquals('f', console.reader().read());
    }

}

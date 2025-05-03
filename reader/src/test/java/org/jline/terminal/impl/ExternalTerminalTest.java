/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ExternalTerminalTest {

    @BeforeEach
    public void setup() {
        System.setProperty(TerminalBuilder.PROP_PROVIDERS, "exec");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(TerminalBuilder.PROP_PROVIDERS);
    }

    @Test
    void testEOL() throws IOException {
        {
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream outIn = new PipedOutputStream(in);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            outIn.write("abc\rdef\nghi\r\njkl\r".getBytes());

            assertEquals("abc", reader.readLine());
            assertEquals("def", reader.readLine());
            assertEquals("ghi", reader.readLine());
            assertEquals("jkl", reader.readLine());
        }
        {
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream outIn = new PipedOutputStream(in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            outIn.write("abc\rdef\nghi\r\njkl\n".getBytes());

            Terminal terminal = TerminalBuilder.builder()
                    .type("ansi")
                    .streams(in, out)
                    .paused(true)
                    .build();
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            Attributes attributes = terminal.getAttributes();
            attributes.setInputFlag(InputFlag.INORMEOL, true);
            terminal.setAttributes(attributes);
            terminal.resume();

            assertEquals("abc", reader.readLine());
            assertEquals("def", reader.readLine());
            assertEquals("ghi", reader.readLine());
            assertEquals("jkl", reader.readLine());
        }
    }

    @Test
    public void testInput() throws IOException, InterruptedException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExternalTerminal console = new ExternalTerminal("foo", "ansi", in, out, StandardCharsets.UTF_8);

        testConsole(outIn, out, console);
    }

    private void testConsole(PipedOutputStream outIn, ByteArrayOutputStream out, Terminal terminal)
            throws IOException, InterruptedException {
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
    @Disabled("This test very often fails on Travis CI")
    public void testInterrupt() throws Exception {
        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExternalTerminal console = new ExternalTerminal("foo", "ansi", in, out, StandardCharsets.UTF_8);
        Attributes attributes = console.getAttributes();
        attributes.setLocalFlag(LocalFlag.ISIG, true);
        attributes.setControlChar(ControlChar.VINTR, 3);
        console.setAttributes(attributes);
        LineReader lineReader = LineReaderBuilder.builder().terminal(console).build();
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
    public void testCursorPosition() throws IOException {
        PipedInputStream in = new PipedInputStream();
        final PipedOutputStream outIn = new PipedOutputStream(in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExternalTerminal console = new ExternalTerminal("foo", "ansi", in, out, StandardCharsets.UTF_8);

        outIn.write(new byte[] {'a', '\033', 'b', '\033', '[', '2', ';', '3', 'R', 'f'});
        outIn.flush();

        StringBuilder sb = new StringBuilder();
        Cursor cursor = console.getCursorPosition(c -> sb.append((char) c));
        assertNotNull(cursor);
        assertEquals(2, cursor.getX());
        assertEquals(1, cursor.getY());
        assertEquals("a\033b", sb.toString());
        assertEquals('f', console.reader().read());
    }

    @Test
    public void testPaused() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream("abcdefghijklmnopqrstuvwxyz".getBytes());
        Terminal term = TerminalBuilder.builder()
                .system(false)
                .streams(bais, baos)
                .paused(true)
                .build();
        assertTrue(term.paused());
    }

    @Test
    public void testExceptionOnInputStream() throws IOException, InterruptedException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteArrayInputStream bais = new ByteArrayInputStream("abcdefghijklmnopqrstuvwxyz".getBytes());
        InputStream in = new FilterInputStream(bais) {
            @Override
            public int read() throws IOException {
                int r = super.read();
                if (r == 'm') {
                    throw new IOException("Inject IOException");
                }
                return r;
            }

            public int read(byte b[], int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException();
                } else if (off < 0 || len < 0 || len > b.length - off) {
                    throw new IndexOutOfBoundsException();
                } else if (len == 0) {
                    return 0;
                }
                int c = read();
                if (c == -1) {
                    return -1;
                }
                b[off] = (byte) c;
                int i = 1;
                for (; i < len; i++) {
                    c = read();
                    if (c == -1) {
                        break;
                    }
                    b[off + i] = (byte) c;
                }
                return i;
            }
        };
        Terminal term =
                TerminalBuilder.builder().system(false).streams(in, baos).build();
        Thread.sleep(100);
        try {
            term.input().read();
            fail("Should have thrown an exception");
        } catch (IOException error) {
            // expected
        }
    }

    @Test
    public void testReadUntilEof() throws IOException, InterruptedException {
        String str = "test 1\n" + "test 2\n" + "test 3\n" + "exit\n";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in = new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
        Terminal term =
                TerminalBuilder.builder().system(false).streams(in, baos).build();
        Thread.sleep(100);
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        byte[] buffer = new byte[1014];
        int l;
        for (; ; ) {
            l = term.input().read(buffer);
            if (l >= 0) {
                baos2.write(buffer, 0, l);
            } else {
                break;
            }
        }
        ;
        String str2 = new String(baos2.toByteArray(), StandardCharsets.UTF_8);
        assertEquals(str, str2);
    }
}

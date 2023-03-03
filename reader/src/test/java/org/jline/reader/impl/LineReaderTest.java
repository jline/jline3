/*
 * Copyright (c) 2002-2022, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.reader.Candidate;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.AbstractWindowsTerminal;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.AttributedString;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.jline.terminal.impl.AbstractWindowsTerminal.TYPE_WINDOWS_CONEMU;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LineReaderTest {

    @Test(expected = EndOfFileException.class)
    @Ignore
    public void emptyStringGivesEOFWithJna() throws Exception {
        String inputString = "";
        InputStream inputStream = new ByteArrayInputStream(inputString.getBytes());

        LineReaderBuilder builder =
                LineReaderBuilder.builder()
                        .terminal(TerminalBuilder.builder()
                                .streams(inputStream, System.out)
                                .jna(true)
                                .build());

        LineReader reader = builder.build();

        // this gets trapped in an infinite loop
        reader.readLine();
        fail("Should have thrown an EndOfFileException");
    }

    @Test(expected = EndOfFileException.class)
    @Ignore
    public void emptyStringGivesEOFNoJna() throws Exception {
        String inputString = "";
        InputStream inputStream = new ByteArrayInputStream(inputString.getBytes());

        LineReaderBuilder builder =
                LineReaderBuilder.builder()
                        .terminal(TerminalBuilder.builder()
                                .streams(inputStream, System.out)
                                .jna(false)
                                .build());

        LineReader reader = builder.build();

        // this gets trapped in an infinite loop
        reader.readLine();
        fail("Should have thrown an EndOfFileException");
    }

    @Test
    public void testGroup() throws Exception {
        List<Candidate> c = new ArrayList<>();
        c.add(new Candidate("option1", "option1", "group1", null, null, null, false));
        c.add(new Candidate("option2", "option2", "group1", null, null, null, false));
        c.add(new Candidate("option3", "option3", "group2", null, null, null, false));
        c.add(new Candidate("option4", "option4", "group2", null, null, null, false));

        assertEquals("group1\noption1   option2\ngroup2\noption3   option4", computeGroupPost(c, true, true));
        assertEquals("group1\ngroup2\noption1   option2   option3   option4", computeGroupPost(c, true, false));
        assertEquals("option1   option2   option3   option4", computeGroupPost(c, false, false));
        assertEquals("option1   option2\noption3   option4", computeGroupPost(c, false, true));
    }

    private String computeGroupPost(List<Candidate> c, boolean autoGroup, boolean groupName) throws IOException {
        Terminal terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        return new LineReaderImpl(terminal).computePost(c, null, null, "", s -> AttributedString.fromAnsi(s).columnLength(), 80, autoGroup, groupName, true).post.toString();
    }

    @Test
    public void testConEmuLineReaderClearScreen() throws IOException {
        System.setProperty("org.jline.terminal.conemu.disable-activate", "false");
        StringWriter sw = new StringWriter();
        AbstractWindowsTerminal<?> terminal = new AbstractWindowsTerminal<Object>(new BufferedWriter(sw), "name", TYPE_WINDOWS_CONEMU, Charset.defaultCharset(),
                false, Terminal.SignalHandler.SIG_DFL, null, null) {
            @Override
            protected int getConsoleMode(Object console) {
                return 0;
            }

            @Override
            protected void setConsoleMode(Object console, int mode) {
            }

            @Override
            protected boolean processConsoleInput() throws IOException {
                return false;
            }

            @Override
            public Size getSize() {
                return new Size(80, 25);
            }
        };
        assertTrue(sw.toString().contains("\u001b[9999E"));
        LineReader reader = new LineReaderImpl(terminal);
        new Thread(() -> {
            try {
                Thread.sleep(50);
                terminal.processInputChar((char) 12);
                Thread.sleep(50);
                terminal.processInputChar('a');
                terminal.processInputChar((char) 13);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        String line = reader.readLine();
        assertTrue(sw.toString().contains("\u001b[H\u001b[J"));
        assertTrue(sw.toString().contains("\u001b[9999E"));
    }

    @Test
    public void testInheritAppNameFromTerminal() throws IOException {
        final String expectedAppName = "BOB";
        final Terminal terminal = TerminalBuilder.builder()
                .name(expectedAppName)
                .build();
        final LineReader lineReader = new LineReaderImpl(terminal);

        assertEquals("Did not inherit appName from terminal",
                expectedAppName, lineReader.getAppName());
    }

    @Test
    public void testPreferAppNameFromConstructor() throws IOException {
        final String expectedAppName = "NANCY";
        final Terminal terminal = TerminalBuilder.builder()
                .name(expectedAppName + "X")
                .build();
        final LineReader lineReader =
                new LineReaderImpl(terminal, expectedAppName);

        assertEquals("Did not prefer appName from builder",
                expectedAppName, lineReader.getAppName());
    }

    @Test
    public void terminalLineInfiniteLoop() throws IOException
    {
        ByteArrayInputStream in = new ByteArrayInputStream( "hello\nworld\n".getBytes( StandardCharsets.UTF_8 ) );
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        Terminal terminal = TerminalBuilder.builder().streams( in, out ).build();
        terminal.setSize(new Size(0, 48));
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal( terminal )
                .variable( LineReader.SECONDARY_PROMPT_PATTERN, "%P " )
                .build();

        String read1 = lineReader.readLine("Input1: ");
        String read2 = lineReader.readLine("Input2: ");

        assertEquals( "hello", read1 );
        assertEquals( "world", read2 );
    }
}

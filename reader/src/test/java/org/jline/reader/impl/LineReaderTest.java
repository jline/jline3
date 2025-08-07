/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.jline.terminal.impl.AbstractWindowsTerminal.TYPE_WINDOWS_CONEMU;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LineReaderTest {

    @Test
    @Disabled
    @SuppressWarnings("deprecation")
    public void emptyStringGivesEOFWithJna() throws Exception {
        String inputString = "";
        InputStream inputStream = new ByteArrayInputStream(inputString.getBytes());

        LineReaderBuilder builder = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.builder()
                        .streams(inputStream, System.out)
                        .build());

        LineReader reader = builder.build();

        // this gets trapped in an infinite loop
        assertThrows(EndOfFileException.class, () -> reader.readLine());
    }

    @Test
    @Disabled
    @SuppressWarnings("deprecation")
    public void emptyStringGivesEOFNoJna() throws Exception {
        String inputString = "";
        InputStream inputStream = new ByteArrayInputStream(inputString.getBytes());

        LineReaderBuilder builder = LineReaderBuilder.builder()
                .terminal(TerminalBuilder.builder()
                        .streams(inputStream, System.out)
                        .build());

        LineReader reader = builder.build();

        // this gets trapped in an infinite loop
        assertThrows(EndOfFileException.class, () -> reader.readLine());
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
        return new LineReaderImpl(terminal)
                .computePost(
                        c,
                        null,
                        null,
                        "",
                        s -> AttributedString.fromAnsi(s).columnLength(),
                        80,
                        autoGroup,
                        groupName,
                        true)
                .post
                .toString();
    }

    @Test
    public void testConEmuLineReaderClearScreen() throws IOException {
        System.setProperty("org.jline.terminal.conemu.disable-activate", "false");
        StringWriter sw = new StringWriter();
        AbstractWindowsTerminal<?> terminal =
                new AbstractWindowsTerminal<Object>(
                        null,
                        null,
                        new BufferedWriter(sw),
                        "name",
                        TYPE_WINDOWS_CONEMU,
                        Charset.defaultCharset(),
                        false,
                        Terminal.SignalHandler.SIG_DFL,
                        null,
                        0,
                        null,
                        0) {
                    @Override
                    protected int getConsoleMode(Object console) {
                        return 0;
                    }

                    @Override
                    protected void setConsoleMode(Object console, int mode) {}

                    @Override
                    public int getDefaultForegroundColor() {
                        return -1;
                    }

                    @Override
                    public int getDefaultBackgroundColor() {
                        return -1;
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
                })
                .start();
        String line = reader.readLine();
        assertTrue(sw.toString().contains("\u001b[H\u001b[J"));
        assertTrue(sw.toString().contains("\u001b[9999E"));
    }

    @Test
    public void testInheritAppNameFromTerminal() throws IOException {
        final String expectedAppName = "BOB";
        final Terminal terminal =
                TerminalBuilder.builder().name(expectedAppName).build();
        final LineReader lineReader = new LineReaderImpl(terminal);

        assertEquals(expectedAppName, lineReader.getAppName(), "Did not inherit appName from terminal");
    }

    @Test
    public void testPreferAppNameFromConstructor() throws IOException {
        final String expectedAppName = "NANCY";
        final Terminal terminal =
                TerminalBuilder.builder().name(expectedAppName + "X").build();
        final LineReader lineReader = new LineReaderImpl(terminal, expectedAppName);

        assertEquals(expectedAppName, lineReader.getAppName(), "Did not prefer appName from builder");
    }

    @Test
    public void terminalLineInfiniteLoop() throws IOException {
        PipedInputStream in = new PipedInputStream();
        PipedOutputStream outIn = new PipedOutputStream(in);
        outIn.write("hello\nworld\n".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

        Terminal terminal = TerminalBuilder.builder().streams(in, out).build();
        terminal.setSize(new Size(0, 48));
        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .variable(LineReader.SECONDARY_PROMPT_PATTERN, "%P ")
                .build();

        String read1 = lineReader.readLine("Input1: ");
        String read2 = lineReader.readLine("Input2: ");

        assertEquals("hello", read1);
        assertEquals("world", read2);
    }

    @Test
    public void testNoBackspaceInOutputOnDumbTerminal() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {'\n'});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (Terminal terminal = new DumbTerminal(in, out)) {
            LineReader r = new LineReaderImpl(terminal);
            r.readLine("123");
            String written = out.toString();
            assertEquals("123", written);
        }
    }
}

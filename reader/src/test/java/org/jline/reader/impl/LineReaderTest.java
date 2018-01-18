/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl;

import org.jline.reader.Candidate;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;
import org.jline.utils.AttributedString;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
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

        assertEquals("group1\noption1   option2\ngroup2\noption3   option4",  computeGroupPost(c, true,   true));
        assertEquals("group1\ngroup2\noption1   option2   option3   option4", computeGroupPost(c, true,   false));
        assertEquals("option1   option2   option3   option4",                 computeGroupPost(c, false,  false));
        assertEquals("option1   option2\noption3   option4",                  computeGroupPost(c, false,  true));
    }

    private String computeGroupPost(List<Candidate> c, boolean autoGroup, boolean groupName) throws IOException {
        Terminal terminal = new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        return new LineReaderImpl(terminal).computePost(c, null, null, "", s -> AttributedString.fromAnsi(s).columnLength(), 80, autoGroup, groupName, true).post.toString();
    }

}

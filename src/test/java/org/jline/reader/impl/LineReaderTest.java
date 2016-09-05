/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.TerminalBuilder;
import org.junit.Test;

import static org.junit.Assert.fail;

public class LineReaderTest {

    @Test(expected = EndOfFileException.class)
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

}

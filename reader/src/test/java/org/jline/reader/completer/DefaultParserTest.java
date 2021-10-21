/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.completer;

import java.util.Arrays;

import org.jline.reader.EOFError;
import org.jline.reader.EndOfFileException;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.ReaderTestSupport;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DefaultParser}.
 *
 * @author <a href="mailto:mdrob@apache.org">Mike Drob</a>
 */
public class DefaultParserTest extends ReaderTestSupport {

    ParsedLine delimited;
    DefaultParser parser;

    @Before
    public void setUp() {
        parser = new DefaultParser();
    }

    @Test
    public void testDelimit() {
        // These all passed before adding quoting and escaping
        delimited = parser.parse("1 2 3", 0);
        assertEquals(Arrays.asList("1", "2", "3"), delimited.words());

        delimited = parser.parse("1  2  3", 0);
        assertEquals(Arrays.asList("1", "2", "3"), delimited.words());
    }

    @Test(expected = EOFError.class)
    public void testThatExceptionThrows() {
        parser.setEofOnUnclosedQuote(true);
        delimited = parser.parse("'1 2 3", 0, Parser.ParseContext.SPLIT_LINE);
    }

    @Test
    public void testQuotedDelimit() {
        delimited = parser.parse("\"1 2\" 3", 0);
        assertEquals(Arrays.asList("1 2", "3"), delimited.words());

        delimited = parser.parse("'1 2' 3", 0);
        assertEquals(Arrays.asList("1 2", "3"), delimited.words());

        delimited = parser.parse("1 '2 3'", 0);
        assertEquals(Arrays.asList("1", "2 3"), delimited.words());

        delimited = parser.parse("0'1 2' 3", 0);
        assertEquals(Arrays.asList("0'1 2'", "3"), delimited.words());

        delimited = parser.parse("'01 '2 3", 0);
        assertEquals(Arrays.asList("01 2", "3"), delimited.words());
    }

    @Test
    public void testMixedQuotes() {
        delimited = parser.parse("\"1' '2\" 3", 0);
        assertEquals(Arrays.asList("1' '2", "3"), delimited.words());

        delimited = parser.parse("'1\" 2' 3\"", 0);
        assertEquals(Arrays.asList("1\" 2", "3\""), delimited.words());
    }

    @Test
    public void testEscapedSpace() {
        delimited = parser.parse("1\\ 2 3", 0);
        assertEquals(Arrays.asList("1 2", "3"), delimited.words());
    }

    @Test
    public void testEscapedQuotes() {
        delimited = parser.parse("'1 \\'2' 3", 0);
        assertEquals(Arrays.asList("1 '2", "3"), delimited.words());

        delimited = parser.parse("\\'1 '2' 3", 0);
        assertEquals(Arrays.asList("'1", "2", "3"), delimited.words());

        delimited = parser.parse("'1 '2\\' 3", 0);
        assertEquals(Arrays.asList("1 2'", "3"), delimited.words());
    }
}

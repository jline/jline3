/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.reader.CompletingParsedLine;
import org.jline.reader.Parser.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DefaultParserTest {

    @Test
    public void testEscapedWord() {
        DefaultParser parser = new DefaultParser();
        CompletingParsedLine line = (CompletingParsedLine) parser.parse("foo second\\ param \"quoted param\"", 15);
        assertNotNull(line);
        assertNotNull(line.words());
        assertEquals("foo second\\ param \"quoted param\"", line.line());
        assertEquals(15, line.cursor());
        assertEquals(3, line.words().size());
        assertEquals("second param", line.word());
        assertEquals(10, line.wordCursor());
        assertEquals(11, line.rawWordCursor());
        assertEquals(13, line.rawWordLength());
    }

    @Test
    public void testQuotedWord() {
        DefaultParser parser = new DefaultParser();
        CompletingParsedLine line = (CompletingParsedLine) parser.parse("foo second\\ param \"quoted param\"", 20);
        assertNotNull(line);
        assertNotNull(line.words());
        assertEquals("foo second\\ param \"quoted param\"", line.line());
        assertEquals(20, line.cursor());
        assertEquals(3, line.words().size());
        assertEquals("quoted param", line.word());
        assertEquals(1, line.wordCursor());
        assertEquals(2, line.rawWordCursor());
        assertEquals(14, line.rawWordLength());
    }

    @Test
    public void testCommand() {
        DefaultParser parser = new DefaultParser();
        assertEquals("command", parser.getCommand("variable=command"));
        assertEquals("command", parser.getCommand("variable.key=command"));
        assertEquals("command", parser.getCommand("variable['key']=command"));
        assertEquals("command", parser.getCommand("variable[0]=command"));
        assertEquals("", parser.getCommand("variable['key'] = statement"));
    }

    @Test
    public void testVariable() {
        DefaultParser parser = new DefaultParser();
        assertEquals("variable", parser.getVariable("variable=command"));
        assertEquals("variable.key", parser.getVariable("variable.key=command"));
        assertEquals("variable['key']", parser.getVariable("variable['key']=command"));
        assertEquals("variable['key']", parser.getVariable("variable['key'] = statement"));
    }

    @Test
    public void testSplitLine() {
        DefaultParser parser = new DefaultParser();
        CompletingParsedLine line =
                (CompletingParsedLine) parser.parse("foo second\\ param \"quoted param\"", 0, ParseContext.SPLIT_LINE);
        assertNotNull(line);
        assertNotNull(line.words());
        assertEquals("foo", line.words().get(0));
        assertEquals("second\\ param", line.words().get(1));
        assertEquals("\"quoted param\"", line.words().get(2));
    }
}

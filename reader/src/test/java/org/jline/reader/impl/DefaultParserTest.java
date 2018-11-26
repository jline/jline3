/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.jline.reader.CompletingParsedLine;
import org.jline.reader.ParsedLine;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

}

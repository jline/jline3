/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.completer;

import java.util.Collections;
import java.util.List;

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser.ParseContext;
import org.jline.reader.impl.DefaultParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link DefaultParser}.
 *
 * @author <a href="mailto:mdrob@apache.org">Mike Drob</a>
 */
class DefaultParserTest {

    ParsedLine delimited;
    DefaultParser parser;

    @BeforeEach
    public void setUp() {
        parser = new DefaultParser();
    }

    @Test
    void testDelimit() {
        // These all passed before adding quoting and escaping
        delimited = parser.parse("1 2 3", 0);
        assertEquals(List.of("1", "2", "3"), delimited.words());

        delimited = parser.parse("1  2  3", 0);
        assertEquals(List.of("1", "2", "3"), delimited.words());
    }

    @Test
    void testQuotedDelimit() {
        delimited = parser.parse("\"1 2\" 3", 0);
        assertEquals(List.of("1 2", "3"), delimited.words());

        delimited = parser.parse("'1 2' 3", 0);
        assertEquals(List.of("1 2", "3"), delimited.words());

        delimited = parser.parse("1 '2 3'", 0);
        assertEquals(List.of("1", "2 3"), delimited.words());

        delimited = parser.parse("0'1 2' 3", 0);
        assertEquals(List.of("0'1 2'", "3"), delimited.words());

        delimited = parser.parse("'01 '2 3", 0);
        assertEquals(List.of("01 2", "3"), delimited.words());
    }

    @Test
    void testMixedQuotes() {
        delimited = parser.parse("\"1' '2\" 3", 0);
        assertEquals(List.of("1' '2", "3"), delimited.words());

        delimited = parser.parse("'1\" 2' 3\"", 0);
        assertEquals(List.of("1\" 2", "3\""), delimited.words());
    }

    @Test
    void testEscapedSpace() {
        delimited = parser.parse("1\\ 2 3", 0);
        assertEquals(List.of("1 2", "3"), delimited.words());
    }

    @Test
    void testEscapedQuotes() {
        delimited = parser.parse("'1 \\'2' 3", 0);
        assertEquals(List.of("1 '2", "3"), delimited.words());

        delimited = parser.parse("\\'1 '2' 3", 0);
        assertEquals(List.of("'1", "2", "3"), delimited.words());

        delimited = parser.parse("'1 '2\\' 3", 0);
        assertEquals(List.of("1 2'", "3"), delimited.words());
    }

    @Test
    void testNullStartBlockCommentDelim() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultParser.BlockCommentDelims(null, "*/"));
    }

    @Test
    void testNullEndBlockCommentsDelim() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultParser.BlockCommentDelims("/*", null));
    }

    @Test
    void testEqualBlockCommentsDelims() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultParser.BlockCommentDelims("/*", "/*"));
    }

    @Test
    void testEmptyStartBlockCommentsDelims() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultParser.BlockCommentDelims("", "*/"));
    }

    @Test
    void testEmptyEndBlockCommentsDelims() {
        assertThrows(IllegalArgumentException.class, () -> new DefaultParser.BlockCommentDelims("/*", ""));
    }

    @Test
    void testBashComments() {
        parser.setLineCommentDelims(new String[] {"#"});
        delimited = parser.parse("1 2 # 3", 0);
        assertEquals(List.of("1", "2"), delimited.words());

        delimited = parser.parse("#\\'1 '2' 3", 0);
        assertEquals(Collections.emptyList(), delimited.words());

        delimited = parser.parse("'#'\\'1 '2' 3", 0);
        assertEquals(List.of("#'1", "2", "3"), delimited.words());

        delimited = parser.parse("#1 " + System.lineSeparator() + " '2' 3", 0);
        assertEquals(List.of("2", "3"), delimited.words());
    }

    @Test
    void testJavaComments() {
        parser.setLineCommentDelims(new String[] {"//"});
        parser.setBlockCommentDelims(new DefaultParser.BlockCommentDelims("/*", "*/"));

        delimited = parser.parse("1 2 # 3", 0);
        assertEquals(List.of("1", "2", "#", "3"), delimited.words());

        delimited = parser.parse("1 2 // 3", 0);
        assertEquals(List.of("1", "2"), delimited.words());

        delimited = parser.parse("/*\\'1 \n '2' \n3*/", 0);
        assertEquals(Collections.emptyList(), delimited.words());

        delimited = parser.parse("'//'\\'1 /*'2'\n */3", 0);
        assertEquals(List.of("//'1", "3"), delimited.words());

        delimited = parser.parse("hello/*comment*/world", 0);
        assertEquals(List.of("hello", "world"), delimited.words());
    }

    @Test
    void testSqlComments() {
        // The test check sql line comment --
        // and sql block comments /* */
        parser.setLineCommentDelims(new String[] {"--"});
        parser.setBlockCommentDelims(new DefaultParser.BlockCommentDelims("/*", "*/"));

        delimited = parser.parse("/*/g */", 0);
        assertEquals(Collections.emptyList(), delimited.words());

        delimited = parser.parse("/**/g", 0);
        assertEquals(List.of("g"), delimited.words());

        delimited = parser.parse("select '--';", 0);
        assertEquals(List.of("select", "--;"), delimited.words());
        delimited = parser.parse("select --; '--';", 0);
        assertEquals(List.of("select"), delimited.words());

        delimited = parser.parse("select 1/* 789*/ ; '--';", 0);
        assertEquals(List.of("select", "1", ";", "--;"), delimited.words());
        delimited = parser.parse("select 1/* 789 \n */ ; '--';", 0);
        assertEquals(List.of("select", "1", ";", "--;"), delimited.words());

        delimited = parser.parse("select 1/* 789 \n * / ; '--';*/", 0);
        assertEquals(List.of("select", "1"), delimited.words());
        delimited = parser.parse("select '1';--comment", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1';-----comment", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1';--comment\n", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1';--comment\n\n", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1'; --comment", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1';\n--comment", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1';\n\n--comment", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1';\n \n--comment", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1'\n;\n--comment", 0);
        assertEquals(List.of("select", "1", ";"), delimited.words());

        delimited = parser.parse("select '1'\n\n;--comment", 0);
        assertEquals(List.of("select", "1", ";"), delimited.words());

        delimited = parser.parse("select '1'\n\n;---comment", 0);
        assertEquals(List.of("select", "1", ";"), delimited.words());

        delimited = parser.parse("select '1'\n\n;-- --comment", 0);
        assertEquals(List.of("select", "1", ";"), delimited.words());

        delimited = parser.parse("select '1'\n\n;\n--comment", 0);
        assertEquals(List.of("select", "1", ";"), delimited.words());

        delimited = parser.parse("select '1'/*comment*/", 0);
        assertEquals(List.of("select", "1"), delimited.words());

        delimited = parser.parse("select '1';/*---comment */", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1';/*comment\n*/\n", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1';/*comment*/\n\n", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1'; /*--comment*/", 0);
        assertEquals(List.of("select", "1;"), delimited.words());

        delimited = parser.parse("select '1/*' as \"asd\";", 0);
        assertEquals(List.of("select", "1/*", "as", "asd;"), delimited.words());

        delimited = parser.parse("select '/*' as \"asd*/\";", 0);
        assertEquals(List.of("select", "/*", "as", "asd*/;"), delimited.words());

        delimited = parser.parse("select '1' as \"'a'\\\ns'd\\\n\n\" from t;", 0);
        assertEquals(List.of("select", "1", "as", "'a'\ns'd\n\n", "from", "t;"), delimited.words());
    }

    @Test
    void testTrailingSpace() {
        // Issue #1489: trailing space should not emit an empty word in non-COMPLETE contexts
        delimited = parser.parse("echo foo bar ", 0);
        assertEquals(List.of("echo", "foo", "bar"), delimited.words());

        delimited = parser.parse("echo foo bar ", "echo foo bar ".length());
        assertEquals(List.of("echo", "foo", "bar"), delimited.words());

        // In COMPLETE context, trailing space should emit an empty word for tab-completion
        delimited = parser.parse("echo foo bar ", "echo foo bar ".length(), ParseContext.COMPLETE);
        assertEquals(List.of("echo", "foo", "bar", ""), delimited.words());
        assertEquals(3, delimited.wordIndex());
        assertEquals(0, delimited.wordCursor());

        // Explicit empty quoted string should always be preserved
        delimited = parser.parse("echo foo bar \"\"", "echo foo bar \"\"".length());
        assertEquals(List.of("echo", "foo", "bar", ""), delimited.words());

        // Empty quoted string in the middle should also be preserved
        delimited = parser.parse("echo \"\" bar", 0);
        assertEquals(List.of("echo", "", "bar"), delimited.words());

        // Multiple trailing spaces should not emit empty word
        delimited = parser.parse("echo foo   ", "echo foo   ".length());
        assertEquals(List.of("echo", "foo"), delimited.words());
    }

    @Test
    void testMissingOpeningBlockComment() {
        parser.setBlockCommentDelims(new DefaultParser.BlockCommentDelims("/*", "*/"));
        assertThrows(EOFError.class, () -> parser.parse("1, 2, 3 */", 0));
    }
}

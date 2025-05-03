/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.completer;

import org.jline.reader.LineReader;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.ReaderTestSupport;
import org.jline.reader.impl.completer.StringsCompleter;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StringsCompleter}.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class StringsCompleterTest extends ReaderTestSupport {
    @Test
    public void test1() throws Exception {
        reader.setCompleter(new StringsCompleter("foo", "bar", "baz"));

        assertBuffer("foo ", new TestBuffer("f").tab());
        // single tab completes to unambiguous "ba"
        assertBuffer("ba", new TestBuffer("b").tab());
        assertBuffer("ba", new TestBuffer("ba").tab());
        assertBuffer("baz ", new TestBuffer("baz").tab());
    }

    @Test
    public void escapeCharsNull() throws Exception {
        DefaultParser dp = (DefaultParser) reader.getParser();
        dp.setEscapeChars(null);
        reader.setVariable(LineReader.ERRORS, 0);
        reader.setParser(dp);
        reader.setCompleter(new StringsCompleter("foo bar", "bar"));

        assertBuffer("'foo bar' ", new TestBuffer("f").tab());
        assertBuffer("'foo bar' ", new TestBuffer("'f").tab());
        assertBuffer("foo'b", new TestBuffer("foo'b").tab());
        assertBuffer("bar'f", new TestBuffer("bar'f").tab());
    }

    @Test
    public void escapeCharsEmpty() throws Exception {
        DefaultParser dp = (DefaultParser) reader.getParser();
        dp.setEscapeChars(new char[] {});
        reader.setVariable(LineReader.ERRORS, 0);
        reader.setParser(dp);
        reader.setCompleter(new StringsCompleter("foo bar", "bar"));

        assertBuffer("foo bar ", new TestBuffer("f").tab());
        assertBuffer("'foo bar' ", new TestBuffer("'f").tab());
        assertBuffer("foo'b", new TestBuffer("foo'b").tab());
        assertBuffer("bar'f", new TestBuffer("bar'f").tab());
    }

    @Test
    public void escapeChars() throws Exception {
        DefaultParser dp = (DefaultParser) reader.getParser();
        dp.setEscapeChars(new char[] {'\\'});
        reader.setVariable(LineReader.ERRORS, 0);
        reader.setParser(dp);
        reader.setCompleter(new StringsCompleter("foo bar", "bar"));

        assertBuffer("foo\\ bar ", new TestBuffer("f").tab());
        assertBuffer("'bar' ", new TestBuffer("'b").tab());
        assertBuffer("'bar'f", new TestBuffer("'bar'f").tab());
        assertBuffer("bar'f", new TestBuffer("bar'f").tab());
    }

    @Test
    public void middleQuotesEscapeCharsNull() throws Exception {
        DefaultParser dp = (DefaultParser) reader.getParser();
        dp.setEscapeChars(null);
        reader.setVariable(LineReader.ERRORS, 0);
        reader.setParser(dp);
        reader.setCompleter(new StringsCompleter("/foo?name='foo bar'", "/foo?name='foo qux'"));

        assertBuffer("/foo?name='foo ", new TestBuffer("/f").tab());
        assertBuffer("/foo?name='foo bar' ", new TestBuffer("/foo?name='foo b").tab());
    }

    @Test
    public void middleQuotesEscapeChars() throws Exception {
        DefaultParser dp = (DefaultParser) reader.getParser();
        dp.setEscapeChars(new char[] {'\\'});
        reader.setVariable(LineReader.ERRORS, 0);
        reader.setParser(dp);
        reader.setCompleter(new StringsCompleter("/foo?name='foo bar'", "/foo?name='foo qux'"));

        assertBuffer("/foo?name='foo ", new TestBuffer("/f").tab());
        assertBuffer("/foo?name='foo bar' ", new TestBuffer("/foo?name='foo b").tab());
    }
}

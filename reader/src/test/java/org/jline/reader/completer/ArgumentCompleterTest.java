/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.completer;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.ReaderTestSupport;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.StringsCompleter;
import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link ArgumentCompleter}.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ArgumentCompleterTest
    extends ReaderTestSupport
{
    @Test
    public void test1() throws Exception {
        reader.setCompleter(new ArgumentCompleter(new StringsCompleter("foo", "bar", "baz")));

        assertBuffer("foo foo ", new TestBuffer("foo f").tab());
        assertBuffer("foo ba", new TestBuffer("foo b").tab());
        assertBuffer("foo ba", new TestBuffer("foo ba").tab());
        assertBuffer("foo baz ", new TestBuffer("foo baz").tab());

        // test completion in the mid range
        assertBuffer("foo baz", new TestBuffer("f baz").left().left().left().left().tab());
        assertBuffer("ba foo", new TestBuffer("b foo").left().left().left().left().tab());
        assertBuffer("foo ba baz", new TestBuffer("foo b baz").left().left().left().left().tab());
        assertBuffer("foo foo baz", new TestBuffer("foo f baz").left().left().left().left().tab());
    }

    @Test
    public void testMultiple() throws Exception {
        ArgumentCompleter argCompleter = new ArgumentCompleter(
                new StringsCompleter("bar", "baz"),
                new StringsCompleter("foo"),
                new StringsCompleter("ree"));
        reader.setCompleter(argCompleter);

        assertBuffer("bar foo ", new TestBuffer("bar f").tab());
        assertBuffer("baz foo ", new TestBuffer("baz f").tab());
        // co completion of 2nd arg in strict mode when 1st argument is not matched exactly
        assertBuffer("ba f", new TestBuffer("ba f").tab());
        assertBuffer("bar fo r", new TestBuffer("bar fo r").tab());

        argCompleter.setStrict(false);
        assertBuffer("ba foo ", new TestBuffer("ba f").tab());
        assertBuffer("ba fo ree ", new TestBuffer("ba fo r").tab());
    }

    @Test
    public void test2() throws Exception {
        reader.setCompleter(
                new ArgumentCompleter(
                        new StringsCompleter("some", "any"),
                        new StringsCompleter("foo", "bar", "baz")));

        assertBuffer("some foo ", new TestBuffer("some fo").tab());
    }

    private static class NoSpaceCompleter implements Completer {
        String[] commands;

        public NoSpaceCompleter(String... commands) {
            this.commands = commands;
        }

        @Override
        public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
            for (String command : commands) {
                candidates.add(new Candidate(command, command, null, null, null,
                        "no-space", true));
            }
        }
    }

    @Test
    public void testOptionNoExtraSpace() throws Exception {
        reader.setCompleter(new ArgumentCompleter(new NoSpaceCompleter("foo", "bar", "baz")));

        assertBuffer("foo foo", new TestBuffer("foo f").tab());
    }
}

/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.console.completer;

import jline.console.ConsoleReaderTestSupport;
import jline.console.completer.ArgumentCompleter;
import jline.console.completer.StringsCompleter;
import org.junit.Test;

/**
 * Tests for {@link jline.console.completer.ArgumentCompleter}.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class ArgumentCompleterTest
    extends ConsoleReaderTestSupport
{
    @Test
    public void test1() throws Exception {
        console.addCompleter(new ArgumentCompleter(new StringsCompleter("foo", "bar", "baz")));

        assertBuffer("foo foo ", new Buffer("foo f").tab());
        assertBuffer("foo ba", new Buffer("foo b").tab());
        assertBuffer("foo ba", new Buffer("foo ba").tab());
        assertBuffer("foo baz ", new Buffer("foo baz").tab());

        // test completion in the mid range
        assertBuffer("foo baz", new Buffer("f baz").left().left().left().left().tab());
        assertBuffer("ba foo", new Buffer("b foo").left().left().left().left().tab());
        assertBuffer("foo ba baz", new Buffer("foo b baz").left().left().left().left().tab());
        assertBuffer("foo foo baz", new Buffer("foo f baz").left().left().left().left().tab());
    }
}
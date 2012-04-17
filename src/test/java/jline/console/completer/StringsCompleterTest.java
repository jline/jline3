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
import jline.console.completer.StringsCompleter;
import org.junit.Test;

/**
 * Tests for {@link jline.console.completer.StringsCompleter}.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class StringsCompleterTest
    extends ConsoleReaderTestSupport
{
    @Test
    public void test1() throws Exception {
        console.addCompleter(new StringsCompleter("foo", "bar", "baz"));

        assertBuffer("foo ", new Buffer("f").tab());
        // single tab completes to unambiguous "ba"
        assertBuffer("ba", new Buffer("b").tab());
        assertBuffer("ba", new Buffer("ba").tab());
        assertBuffer("baz ", new Buffer("baz").tab());
    }
}
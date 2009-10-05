/*
 * Copyright (c) 2002-2007, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline.console;

import static jline.console.Operation.COMPLETE;
import jline.console.completers.ArgumentCompleter;
import jline.console.completers.StringsCompleter;
import org.junit.Test;

import java.util.Iterator;

/**
 * Tests completion.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class CompletionTest
    extends ConsoleReaderTestSupport
{
    @Test
    public void testSimpleCompletor() throws Exception {
        // clear any current completers
        for (Iterator i = console.getCompleters().iterator(); i.hasNext(); console.removeCompletor((Completer) i.next())) {
            // empty
        }

        console.addCompletor(new StringsCompleter("foo", "bar", "baz"));

        assertBuffer("foo ", new Buffer("f").op(COMPLETE));
        // single tab completes to unabbiguous "ba"
        assertBuffer("ba", new Buffer("b").op(COMPLETE));
        assertBuffer("ba", new Buffer("ba").op(COMPLETE));
        assertBuffer("baz ", new Buffer("baz").op(COMPLETE));
    }

    @Test
    public void testArgumentCompletor() throws Exception {
        // clear any current completers
        for (Iterator i = console.getCompleters().iterator(); i.hasNext(); console.removeCompletor((Completer) i.next())) {
            // empty
        }

        console.addCompletor(new ArgumentCompleter(new StringsCompleter("foo", "bar", "baz")));

        assertBuffer("foo foo ", new Buffer("foo f").op(COMPLETE));
        assertBuffer("foo ba", new Buffer("foo b").op(COMPLETE));
        assertBuffer("foo ba", new Buffer("foo ba").op(COMPLETE));
        assertBuffer("foo baz ", new Buffer("foo baz").op(COMPLETE));

        // test completion in the mid range
        assertBuffer("foo baz", new Buffer("f baz").left().left().left().left().op(COMPLETE));
        assertBuffer("ba foo", new Buffer("b foo").left().left().left().left().op(COMPLETE));
        assertBuffer("foo ba baz", new Buffer("foo b baz").left().left().left().left().op(COMPLETE));
        assertBuffer("foo foo baz", new Buffer("foo f baz").left().left().left().left().op(COMPLETE));
    }
}
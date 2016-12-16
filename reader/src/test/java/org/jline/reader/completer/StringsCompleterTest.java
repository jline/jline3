/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.completer;

import org.jline.reader.impl.ReaderTestSupport;
import org.jline.reader.impl.completer.StringsCompleter;
import org.junit.Test;

/**
 * Tests for {@link StringsCompleter}.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class StringsCompleterTest
    extends ReaderTestSupport
{
    @Test
    public void test1() throws Exception {
        reader.setCompleter(new StringsCompleter("foo", "bar", "baz"));

        assertBuffer("foo ", new TestBuffer("f").tab());
        // single tab completes to unambiguous "ba"
        assertBuffer("ba", new TestBuffer("b").tab());
        assertBuffer("ba", new TestBuffer("ba").tab());
        assertBuffer("baz ", new TestBuffer("baz").tab());
    }
}

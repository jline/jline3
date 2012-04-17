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
import jline.console.completer.NullCompleter;
import org.junit.Test;

/**
 * Tests for {@link NullCompleter}.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class NullCompleterTest
    extends ConsoleReaderTestSupport
{
    @Test
    public void test1() throws Exception {
        console.addCompleter(NullCompleter.INSTANCE);

        assertBuffer("f", new Buffer("f").tab());
        assertBuffer("ba", new Buffer("ba").tab());
        assertBuffer("baz", new Buffer("baz").tab());
    }
}
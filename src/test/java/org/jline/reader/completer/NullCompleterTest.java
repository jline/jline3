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
import org.jline.reader.impl.completer.NullCompleter;
import org.junit.Test;

/**
 * Tests for {@link NullCompleter}.
 *
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 */
public class NullCompleterTest
    extends ReaderTestSupport
{
    @Test
    public void test1() throws Exception {
        reader.setCompleter(NullCompleter.INSTANCE);

        assertBuffer("f", new TestBuffer("f").tab());
        assertBuffer("ba", new TestBuffer("ba").tab());
        assertBuffer("baz", new TestBuffer("baz").tab());
    }
}

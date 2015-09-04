/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import org.junit.Test;

public class MultiByteCharTest extends ReaderTestSupport {

    @Test
    public void testMbs() {
        TestBuffer b = new TestBuffer("\uD834\uDD21").enter();
        assertLine("\uD834\uDD21", b, true);

        b = new TestBuffer("\uD834\uDD21").back().enter();
        assertLine("", b, true);

        b = new TestBuffer("\uD834\uDD21").left().ctrlD().enter();
        assertLine("", b, true);

    }
}

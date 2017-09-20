/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.jline.utils.InputStreamReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultiByteCharTest extends ReaderTestSupport {

    @Test
    public void testInputStreamReader() throws IOException {
        String str = "e\uD834\uDD21";

        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        pos.write(str.getBytes(StandardCharsets.UTF_8));
        Reader r = new InputStreamReader(pis, StandardCharsets.UTF_8);
        int c0 = r.read();
        int c1 = r.read();
        int c2 = r.read();

        assertEquals(c0, str.charAt(0));
        assertEquals(c1, str.charAt(1));
        assertEquals(c2, str.charAt(2));
    }

    @Test
    public void testMbs() throws IOException {
        TestBuffer b = new TestBuffer("\uD834\uDD21").enter();
        assertLine("\uD834\uDD21", b, true);

        b = new TestBuffer("\uD834\uDD21").back().enter();
        assertLine("", b, true);

        b = new TestBuffer("\uD834\uDD21").left().ctrlD().enter();
        assertLine("", b, true);

    }
}

/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import org.junit.Test;

import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class WriterOutputStreamTest {

    @Test
    public void testWideChar() throws Exception {
        StringWriter sw = new StringWriter();
        WriterOutputStream wos = new WriterOutputStream(sw, StandardCharsets.UTF_8);
        byte[] bytes = "㐀".getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            wos.write(b);
        }
        wos.flush();
        assertEquals("㐀", sw.toString());
    }
}

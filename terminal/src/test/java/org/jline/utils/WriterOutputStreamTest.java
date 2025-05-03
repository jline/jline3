/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

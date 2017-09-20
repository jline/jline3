/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class PumpReaderTest {

    private PumpReader writeInput() {
        PumpReader pump = new PumpReader();
        PrintWriter writer = new PrintWriter(pump.getWriter());

        // Write some input
        writer.println("Hello world!");
        writer.println("㐀");

        return pump;
    }

    @Test
    public void testReader() throws IOException {
        PumpReader pump = writeInput();

        // Read it again
        BufferedReader reader = new BufferedReader(pump);
        assertEquals("Hello world!", reader.readLine());
        assertEquals("㐀", reader.readLine());
    }

    @Test
    public void testInputStream() throws IOException {
        PumpReader pump = writeInput();

        // Read it using an input stream
        BufferedReader reader = new BufferedReader(new InputStreamReader(pump.createInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertEquals("Hello world!", reader.readLine());
        assertEquals("㐀", reader.readLine());
    }

}

/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.Thread.State;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PumpReaderTest {

    private PumpReader writeInput() {
        PumpReader pump = new PumpReader();
        PrintWriter writer = new PrintWriter(pump.getWriter());

        // Write some input
        writer.println("Hello world!");
        writer.println("\uD83D\uDE0A㐀");

        return pump;
    }

    @Test
    public void testReader() throws IOException {
        PumpReader pump = writeInput();

        // Read it again
        BufferedReader reader = new BufferedReader(pump);
        assertEquals("Hello world!", reader.readLine());
        assertEquals("\uD83D\uDE0A㐀", reader.readLine());
    }

    @Test
    public void testInputStream() throws IOException {
        PumpReader pump = writeInput();

        // Read it using an input stream
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(pump.createInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertEquals("Hello world!", reader.readLine());
        assertEquals("\uD83D\uDE0A㐀", reader.readLine());
    }

    @Test
    public void testSmallBuffer() throws IOException {
        PumpReader pump = new PumpReader(12);
        PrintWriter writer = new PrintWriter(pump.getWriter());

        // Write some input
        new Thread(() -> {
                    writer.println("Hello world!");
                    writer.println("\uD83D\uDE0A㐀");
                })
                .start();

        // Read it using an input stream
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(pump.createInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
        assertEquals("Hello world!", reader.readLine());
        assertEquals("\uD83D\uDE0A㐀", reader.readLine());
    }

    @Test
    public void testSplitSurrogatePair() throws IOException {
        PumpReader pump = new PumpReader();
        Writer writer = pump.getWriter();
        // Only provide high surrogate
        writer.write('\uD83D');
        Thread thread = Thread.currentThread();

        new Thread(() -> {
                    // Busy wait until InputStream blocks for more chars to encode
                    // (rather brittle, but cannot be easily implemented in a different way)
                    while (thread.getState() != State.WAITING && thread.getState() != State.TIMED_WAITING) {
                        Thread.yield();
                    }
                    try {
                        // Complete the surrogate pair
                        writer.write('\uDE0A');
                        writer.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .start();

        InputStream inputStream = pump.createInputStream(StandardCharsets.UTF_8);
        byte[] expectedEncoded = "\uD83D\uDE0A".getBytes(StandardCharsets.UTF_8);
        assertEquals(4, expectedEncoded.length); // verify that test is correctly implemented
        assertEquals(expectedEncoded[0] & 0xff, inputStream.read());
        assertEquals(expectedEncoded[1] & 0xff, inputStream.read());
        assertEquals(expectedEncoded[2] & 0xff, inputStream.read());
        assertEquals(expectedEncoded[3] & 0xff, inputStream.read());
        assertEquals(-1, inputStream.read());
    }

    @Test
    public void testTrailingHighSurrogate() throws IOException {
        PumpReader pump = new PumpReader();
        Writer writer = pump.getWriter();
        writer.write('\uD83D');
        writer.close();

        InputStream inputStream = pump.createInputStream(StandardCharsets.UTF_8);
        // Encoder should have replaced incomplete trailing high surrogate
        assertEquals('?', inputStream.read());
        assertEquals(-1, inputStream.read());
    }
}

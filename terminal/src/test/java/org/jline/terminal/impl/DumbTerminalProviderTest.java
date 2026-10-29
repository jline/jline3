/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the output stream {@link DumbTerminalProvider} uses for system terminals.
 */
public class DumbTerminalProviderTest {

    private static final int LENGTH = 15 * 1024;

    @Test
    public void testSingleByteWritesAreBuffered() throws IOException {
        // AnsiOutputStream writes one byte at a time; this used to be one write(2) per byte
        RecordingOutputStream fd = new RecordingOutputStream();
        OutputStream out = DumbTerminalProvider.systemOutput(fd);
        byte[] data = data();

        for (byte b : data) {
            out.write(b);
        }
        out.flush();

        assertArrayEquals(data, fd.bytes.toByteArray());
        assertTrue(fd.writes <= 3, "expected a few chunked writes, got " + fd.writes);
    }

    @Test
    public void testBulkWritesAreNotSplitIntoBytes() throws IOException {
        RecordingOutputStream fd = new RecordingOutputStream();
        OutputStream out = DumbTerminalProvider.systemOutput(fd);
        byte[] data = data();

        out.write(data, 0, data.length);
        out.flush();

        assertArrayEquals(data, fd.bytes.toByteArray());
        assertTrue(fd.writes <= 3, "expected a few chunked writes, got " + fd.writes);
    }

    @Test
    public void testOutputIsHeldUntilFlushed() throws IOException {
        RecordingOutputStream fd = new RecordingOutputStream();
        OutputStream out = DumbTerminalProvider.systemOutput(fd);

        out.write("partial".getBytes(StandardCharsets.UTF_8));
        assertEquals(0, fd.bytes.size(), "output must be buffered until flushed");

        out.flush();
        assertEquals("partial", fd.bytes.toString("UTF-8"));
    }

    @Test
    public void testCloseFlushesButKeepsDescriptorOpen() throws IOException {
        RecordingOutputStream fd = new RecordingOutputStream();
        OutputStream out = DumbTerminalProvider.systemOutput(fd);

        out.write("pending".getBytes(StandardCharsets.UTF_8));
        out.close();

        assertEquals("pending", fd.bytes.toString("UTF-8"));
        assertFalse(fd.closed, "the system file descriptor must not be closed");
    }

    @Test
    public void testConcurrentWritesDoNotLoseBytes() throws Exception {
        // the stream is shared by everything writing to the system terminal, possibly from several threads
        RecordingOutputStream fd = new RecordingOutputStream();
        OutputStream out = DumbTerminalProvider.systemOutput(fd);
        int threads = 4;
        int bytesPerThread = 100_000;
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> writers = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            byte b = (byte) ('a' + i);
            Thread writer = new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < bytesPerThread; j++) {
                        out.write(b);
                    }
                } catch (Throwable t) {
                    synchronized (failures) {
                        failures.add(t);
                    }
                }
            });
            writer.start();
            writers.add(writer);
        }
        start.countDown();
        for (Thread writer : writers) {
            writer.join();
        }
        out.flush();

        assertTrue(failures.isEmpty(), "writers failed: " + failures);
        int[] counts = new int[threads];
        for (byte b : fd.bytes.toByteArray()) {
            counts[b - 'a']++;
        }
        for (int i = 0; i < threads; i++) {
            assertEquals(bytesPerThread, counts[i], "bytes written by thread " + i);
        }
    }

    private static byte[] data() {
        byte[] data = new byte[LENGTH];
        Arrays.fill(data, (byte) 'x');
        return data;
    }

    private static class RecordingOutputStream extends OutputStream {

        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int writes;
        boolean closed;

        @Override
        public void write(int b) {
            writes++;
            bytes.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            writes++;
            bytes.write(b, off, len);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}

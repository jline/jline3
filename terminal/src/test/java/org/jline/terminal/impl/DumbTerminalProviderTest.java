/*
 * Copyright (c) the original author(s).
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
import java.util.Arrays;

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

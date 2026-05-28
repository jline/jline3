/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NonCloseableStreamTest {

    @Test
    void inputStreamCloseDoesNotCloseUnderlying() throws IOException {
        TrackingInputStream underlying = new TrackingInputStream(new byte[] {1, 2, 3});
        NonCloseableInputStream wrapper = new NonCloseableInputStream(underlying);

        wrapper.close();

        assertFalse(underlying.closed, "Underlying stream should not be closed");
        assertEquals(1, wrapper.read(), "Should still be readable after close");
    }

    @Test
    void inputStreamDelegatesReads() throws IOException {
        byte[] data = {10, 20, 30, 40};
        NonCloseableInputStream wrapper = new NonCloseableInputStream(new ByteArrayInputStream(data));

        assertEquals(10, wrapper.read());
        byte[] buf = new byte[3];
        assertEquals(3, wrapper.read(buf));
        assertArrayEquals(new byte[] {20, 30, 40}, buf);
    }

    @Test
    void outputStreamCloseFlushesButDoesNotClose() throws IOException {
        TrackingOutputStream underlying = new TrackingOutputStream();
        NonCloseableOutputStream wrapper = new NonCloseableOutputStream(underlying);

        wrapper.write(new byte[] {1, 2, 3});
        wrapper.close();

        assertTrue(underlying.flushed, "Underlying stream should be flushed on close");
        assertFalse(underlying.closed, "Underlying stream should not be closed");
    }

    @Test
    void outputStreamStillWritableAfterClose() throws IOException {
        TrackingOutputStream underlying = new TrackingOutputStream();
        NonCloseableOutputStream wrapper = new NonCloseableOutputStream(underlying);

        wrapper.close();
        wrapper.write(42);

        assertFalse(underlying.closed, "Underlying stream should not be closed");
        assertArrayEquals(new byte[] {42}, underlying.toByteArray());
    }

    private static class TrackingInputStream extends InputStream {
        private final byte[] data;
        private int pos = 0;
        boolean closed = false;

        TrackingInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            return pos < data.length ? data[pos++] & 0xFF : -1;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static class TrackingOutputStream extends ByteArrayOutputStream {
        boolean closed = false;
        boolean flushed = false;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        @Override
        public void flush() throws IOException {
            flushed = true;
            super.flush();
        }
    }
}

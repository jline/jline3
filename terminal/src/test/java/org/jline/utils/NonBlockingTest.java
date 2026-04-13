/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class NonBlockingTest {

    @Test
    void testNonBlockingReaderOnNonBlockingStream() throws IOException {
        NonBlockingInputStream nbis = new NonBlockingInputStream() {
            private int call = 0;
            private int idx = 0;
            private final byte[] input = "中英字典".getBytes(StandardCharsets.UTF_8);

            @Override
            public int read(long timeout, boolean isPeek) throws IOException {
                if (call++ % 3 == 0) {
                    try {
                        Thread.sleep(timeout);
                    } catch (InterruptedException e) {
                        throw (InterruptedIOException) new InterruptedIOException().initCause(e);
                    }
                    return -2;
                } else if (idx < input.length) {
                    return input[idx++] & 0x00FF;
                } else {
                    return -1;
                }
            }
        };
        NonBlockingReader nbr = NonBlocking.nonBlocking("name", nbis, StandardCharsets.UTF_8);
        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        assertEquals('中', nbr.read(100));
        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        assertEquals('英', nbr.read(100));
        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        assertEquals('字', nbr.read(100));
        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        assertEquals('典', nbr.read(100));
        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        assertEquals(-1, nbr.read(100));
    }

    @Test
    void testNonBlockingReaderBufferedWithNonBufferedInput() throws IOException {
        NonBlockingInputStream nbis = new NonBlockingInputStream() {
            private int idx = 0;
            private final byte[] input = "中英字典".getBytes(StandardCharsets.UTF_8);

            @Override
            public int read(long timeout, boolean isPeek) {
                if (idx < input.length) {
                    return input[idx++] & 0x00FF;
                } else {
                    return -1;
                }
            }
        };
        NonBlockingReader nbr = NonBlocking.nonBlocking("name", nbis, StandardCharsets.UTF_8);
        char[] buf = new char[4];
        assertEquals(1, nbr.readBuffered(buf, 0));
        assertEquals('中', buf[0]);
        assertEquals(1, nbr.readBuffered(buf, 0));
        assertEquals('英', buf[0]);
        assertEquals(1, nbr.readBuffered(buf, 0));
        assertEquals('字', buf[0]);
        assertEquals(1, nbr.readBuffered(buf, 0));
        assertEquals('典', buf[0]);
    }

    @Test
    void testNonBlockingReaderBufferedWithBufferedInput() throws IOException {
        NonBlockingInputStream nbis = new NonBlockingInputStream() {
            private int idx = 0;
            private final byte[] input = "中英字典".getBytes(StandardCharsets.UTF_8);

            @Override
            public int read(long timeout, boolean isPeek) {
                if (idx < input.length) {
                    return input[idx++] & 0x00FF;
                } else {
                    return -1;
                }
            }

            @Override
            public int readBuffered(byte[] b, int off, int len, long timeout) {
                int i = 0;
                while (i < len && idx < input.length) {
                    b[off + i++] = input[idx++];
                }
                return i > 0 ? i : -1;
            }
        };
        NonBlockingReader nbr = NonBlocking.nonBlocking("name", nbis, StandardCharsets.UTF_8);
        char[] buf = new char[4];
        assertEquals(4, nbr.readBuffered(buf, 0));
        assertEquals('中', buf[0]);
        assertEquals('英', buf[1]);
        assertEquals('字', buf[2]);
        assertEquals('典', buf[3]);
    }

    @Test
    void testNonBlockingPumpReader() throws IOException {
        NonBlockingPumpReader nbr = NonBlocking.nonBlockingPumpReader();
        Writer writer = nbr.getWriter();

        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        writer.write('中');
        assertEquals('中', nbr.read(100));
        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));

        long t0 = System.currentTimeMillis();
        new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        writer.write('中');
                    } catch (Exception e) {
                        fail();
                    }
                })
                .start();
        int c = nbr.read(0);
        long t1 = System.currentTimeMillis();
        assertEquals('中', c);
        assertTrue(t1 - t0 >= 100);
    }

    @Test
    void testNonBlockStreamOnReader() throws IOException {
        NonBlockingPumpReader reader = NonBlocking.nonBlockingPumpReader();
        NonBlockingInputStream is = NonBlocking.nonBlockingStream(reader, StandardCharsets.UTF_8);

        String s = "aaaaaaaaaaaaa";
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        reader.getWriter().write(s);
        for (int i = 0; i < bytes.length; i++) {
            int b = is.read(100L);
            assertEquals(bytes[i], b, "Mismatch at " + i);
        }
        assertEquals(NonBlockingInputStream.READ_EXPIRED, is.read(100));

        s = "aaaaaaaaa中";
        bytes = s.getBytes(StandardCharsets.UTF_8);
        reader.getWriter().write(s);
        for (int i = 0; i < bytes.length; i++) {
            int b = is.read(100L);
            assertEquals(bytes[i], b, "Mismatch at " + i);
        }
        assertEquals(NonBlockingInputStream.READ_EXPIRED, is.read(100));

        s = "aaaaaaaaa\uD801\uDC37";
        bytes = s.getBytes(StandardCharsets.UTF_8);
        reader.getWriter().write(s);
        for (int i = 0; i < bytes.length; i++) {
            int b = is.read(100L);
            assertEquals(bytes[i], b, "Mismatch at " + i);
        }
        assertEquals(NonBlockingInputStream.READ_EXPIRED, is.read(100));
    }

    @Test
    void testPumpBulkRead() throws IOException {
        NonBlockingPumpInputStream pump = new NonBlockingPumpInputStream();
        OutputStream out = pump.getOutputStream();

        byte[] data = "Hello, JLine!".getBytes(StandardCharsets.UTF_8);
        out.write(data);
        out.flush();

        byte[] buf = new byte[64];
        int n = pump.read(buf, 0, buf.length);
        assertEquals(data.length, n);
        assertArrayEquals(data, Arrays.copyOf(buf, n));
    }

    @Test
    void testPumpBulkReadPartial() throws IOException {
        NonBlockingPumpInputStream pump = new NonBlockingPumpInputStream();
        OutputStream out = pump.getOutputStream();

        byte[] data = "Hello, JLine!".getBytes(StandardCharsets.UTF_8);
        out.write(data);
        out.flush();

        // Read fewer bytes than available
        byte[] buf = new byte[5];
        int n = pump.read(buf, 0, buf.length);
        assertEquals(5, n);
        assertArrayEquals("Hello".getBytes(StandardCharsets.UTF_8), buf);
    }

    @Test
    void testPumpBulkReadWrapAround() throws IOException {
        // Small buffer to force wrap-around
        NonBlockingPumpInputStream pump = new NonBlockingPumpInputStream(16);
        OutputStream out = pump.getOutputStream();

        // Fill most of the buffer and consume it to advance the read position
        byte[] filler = new byte[12];
        Arrays.fill(filler, (byte) 'x');
        out.write(filler);
        out.flush();
        byte[] discard = new byte[12];
        assertEquals(12, pump.read(discard, 0, 12));

        // Now write data that wraps around the circular buffer boundary
        byte[] data = "ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8); // 10 bytes, wraps at position 16
        out.write(data);
        out.flush();

        // Bulk read should get all 10 bytes across the wrap
        byte[] buf = new byte[16];
        int n = pump.read(buf, 0, buf.length);
        assertEquals(10, n);
        assertArrayEquals(data, Arrays.copyOf(buf, n));
    }

    @Test
    void testPumpBulkReadClosed() throws IOException {
        NonBlockingPumpInputStream pump = new NonBlockingPumpInputStream();
        pump.close();

        byte[] buf = new byte[16];
        assertThrows(ClosedException.class, () -> pump.read(buf, 0, buf.length));
    }

    @Test
    void testPumpReadBufferedBulk() throws IOException {
        NonBlockingPumpInputStream pump = new NonBlockingPumpInputStream();
        OutputStream out = pump.getOutputStream();

        byte[] data = "Hello, JLine!".getBytes(StandardCharsets.UTF_8);
        out.write(data);
        out.flush();

        byte[] buf = new byte[64];
        int n = pump.readBuffered(buf, 0, buf.length, 100);
        assertEquals(data.length, n);
        assertArrayEquals(data, Arrays.copyOf(buf, n));
    }

    @Test
    void testPumpReadBufferedWrapAround() throws IOException {
        // Small buffer to force wrap-around
        NonBlockingPumpInputStream pump = new NonBlockingPumpInputStream(16);
        OutputStream out = pump.getOutputStream();

        // Advance positions past the middle of the buffer
        byte[] filler = new byte[12];
        Arrays.fill(filler, (byte) 'x');
        out.write(filler);
        out.flush();
        byte[] discard = new byte[12];
        pump.readBuffered(discard, 0, 12, 100);

        // Write data that wraps around
        byte[] data = "ABCDEFGHIJ".getBytes(StandardCharsets.UTF_8);
        out.write(data);
        out.flush();

        byte[] buf = new byte[16];
        int n = pump.readBuffered(buf, 0, buf.length, 100);
        assertEquals(10, n);
        assertArrayEquals(data, Arrays.copyOf(buf, n));
    }

    @Test
    void testPumpBulkReadLargePayload() throws IOException {
        NonBlockingPumpInputStream pump = new NonBlockingPumpInputStream(4096);
        OutputStream out = pump.getOutputStream();

        // Simulate a large payload written in chunks from another thread
        byte[] payload = new byte[10000];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) (i & 0xFF);
        }

        Thread writer = new Thread(() -> {
            try {
                out.write(payload);
                out.flush();
                out.close();
            } catch (IOException e) {
                fail(e);
            }
        });
        writer.start();

        // Read it all back using bulk reads
        byte[] result = new byte[payload.length];
        int total = 0;
        boolean sawBulk = false;
        while (total < payload.length) {
            int n = pump.read(result, total, result.length - total);
            if (n == -1) break;
            if (n == NonBlockingInputStream.READ_EXPIRED) continue;
            assertTrue(n > 0, "read should return at least 1 byte, got " + n);
            if (n > 1) sawBulk = true;
            total += n;
        }
        assertEquals(payload.length, total);
        assertArrayEquals(payload, result);
        assertTrue(sawBulk, "At least one bulk read (n > 1) should have occurred");
    }
}

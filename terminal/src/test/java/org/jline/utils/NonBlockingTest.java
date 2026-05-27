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
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void testNonBlockingInputStreamRecoverAfterEof() throws IOException {
        java.io.InputStream in = new java.io.InputStream() {
            private int callCount = 0;

            @Override
            public int read() {
                int n = callCount++;
                if (n == 0) {
                    return -1;
                }
                return 'A';
            }
        };
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("test", in);
        try {
            assertEquals(-1, nbis.read(200));
            assertEquals('A', nbis.read(200));
        } finally {
            nbis.close();
        }
    }

    // --- Pump thread shutdown/close lifecycle tests ---

    @Test
    @Timeout(5)
    void testInputStreamShutdownTerminatesPumpThread() throws Exception {
        LatchInputStream in = new LatchInputStream();
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("shutdown-test", in);

        assertEquals(NonBlockingInputStream.READ_EXPIRED, nbis.read(100));
        assertTrue(in.waitUntilBlocked(), "Pump thread should be blocked in read");
        Thread pumpThread = findThread("shutdown-test non blocking reader thread");
        assertNotNull(pumpThread, "Pump thread should have been started");

        in.release();
        nbis.shutdown();
        pumpThread.join(2000);
        assertFalse(pumpThread.isAlive(), "Pump thread should have exited after shutdown");
    }

    @Test
    @Timeout(5)
    void testInputStreamCloseStopsPumpAndMarksClosed() throws Exception {
        LatchInputStream in = new LatchInputStream();
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("close-test", in);

        assertEquals(NonBlockingInputStream.READ_EXPIRED, nbis.read(100));
        assertTrue(in.waitUntilBlocked(), "Pump thread should be blocked in read");
        Thread pumpThread = findThread("close-test non blocking reader thread");
        assertNotNull(pumpThread);

        in.release();
        nbis.close();
        pumpThread.join(2000);
        assertFalse(pumpThread.isAlive(), "Pump thread should have exited after close");
        assertThrows(ClosedException.class, () -> nbis.read(100));
    }

    @Test
    @Timeout(5)
    void testInputStreamCloseWithNonCloseableDoesNotCloseUnderlying() throws Exception {
        LatchInputStream tracking = new LatchInputStream();
        NonCloseableInputStream wrapper = new NonCloseableInputStream(tracking);
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("noncloseable-test", wrapper);

        assertEquals(NonBlockingInputStream.READ_EXPIRED, nbis.read(100));
        assertTrue(tracking.waitUntilBlocked(), "Pump thread should be blocked in read");

        tracking.release();
        nbis.close();
        assertFalse(tracking.closed.get(), "Underlying stream should not be closed through NonCloseable wrapper");
        assertThrows(ClosedException.class, () -> nbis.read(100));
    }

    @Test
    @Timeout(5)
    void testReaderShutdownTerminatesPumpThread() throws Exception {
        LatchReader reader = new LatchReader();
        NonBlockingReaderImpl nbr = new NonBlockingReaderImpl("reader-shutdown-test", reader);

        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        assertTrue(reader.waitUntilBlocked(), "Pump thread should be blocked in read");
        Thread pumpThread = findThread("reader-shutdown-test non blocking reader thread");
        assertNotNull(pumpThread, "Pump thread should have been started");

        reader.release();
        nbr.shutdown();
        pumpThread.join(2000);
        assertFalse(pumpThread.isAlive(), "Pump thread should have exited after shutdown");
    }

    @Test
    @Timeout(5)
    void testReaderCloseStopsPumpAndMarksClosed() throws Exception {
        LatchReader reader = new LatchReader();
        NonBlockingReaderImpl nbr = new NonBlockingReaderImpl("reader-close-test", reader);

        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        assertTrue(reader.waitUntilBlocked(), "Pump thread should be blocked in read");
        Thread pumpThread = findThread("reader-close-test non blocking reader thread");
        assertNotNull(pumpThread);

        reader.release();
        nbr.close();
        pumpThread.join(2000);
        assertFalse(pumpThread.isAlive(), "Pump thread should have exited after close");
        assertThrows(ClosedException.class, () -> nbr.read(100));
    }

    @Test
    @Timeout(5)
    void testCloseFromAnotherThreadDoesNotHang() throws Exception {
        LatchInputStream in = new LatchInputStream();
        NonBlockingInputStreamImpl nbis = new NonBlockingInputStreamImpl("cross-thread-test", in);

        AtomicReference<Throwable> readError = new AtomicReference<>();
        CountDownLatch readStarted = new CountDownLatch(1);

        Thread readThread = new Thread(() -> {
            try {
                readStarted.countDown();
                nbis.read(10_000);
            } catch (ClosedException | InterruptedIOException e) {
                // Expected
            } catch (Throwable t) {
                readError.set(t);
            }
        });
        readThread.start();

        assertTrue(readStarted.await(1, TimeUnit.SECONDS));
        assertTrue(in.waitUntilBlocked(), "Pump thread should be blocked in read");

        in.release();
        nbis.close();
        readThread.join(2000);
        assertFalse(readThread.isAlive(), "Read thread should have exited after close");
        assertNull(readError.get(), "Read thread should not have thrown unexpected error");
    }

    private static Thread findThread(String name) {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static class LatchInputStream extends InputStream {
        private final CountDownLatch blocked = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);
        final AtomicBoolean closed = new AtomicBoolean(false);

        @Override
        public int read() throws IOException {
            blocked.countDown();
            try {
                released.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
            return -1;
        }

        @Override
        public void close() throws IOException {
            closed.set(true);
            released.countDown();
        }

        void release() {
            released.countDown();
        }

        boolean waitUntilBlocked() throws InterruptedException {
            return blocked.await(2, TimeUnit.SECONDS);
        }
    }

    private static class LatchReader extends Reader {
        private final CountDownLatch blocked = new CountDownLatch(1);
        private final CountDownLatch released = new CountDownLatch(1);

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            blocked.countDown();
            try {
                released.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
            return -1;
        }

        @Override
        public void close() throws IOException {
            released.countDown();
        }

        void release() {
            released.countDown();
        }

        boolean waitUntilBlocked() throws InterruptedException {
            return blocked.await(2, TimeUnit.SECONDS);
        }
    }
}

/*
 * Copyright (c) 2023-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class NonBlockingTest {

    @Test
    public void testNonBlockingReaderOnNonBlockingStream() throws IOException {
        NonBlockingInputStream nbis = new NonBlockingInputStream() {
            int call = 0;
            int idx = 0;
            byte[] input = "中英字典".getBytes(StandardCharsets.UTF_8);

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
    public void testNonBlockingReaderBufferedWithNonBufferedInput() throws IOException {
        NonBlockingInputStream nbis = new NonBlockingInputStream() {
            int idx = 0;
            byte[] input = "中英字典".getBytes(StandardCharsets.UTF_8);

            @Override
            public int read(long timeout, boolean isPeek) throws IOException {
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
    public void testNonBlockingReaderBufferedWithBufferedInput() throws IOException {
        NonBlockingInputStream nbis = new NonBlockingInputStream() {
            int idx = 0;
            byte[] input = "中英字典".getBytes(StandardCharsets.UTF_8);

            @Override
            public int read(long timeout, boolean isPeek) throws IOException {
                if (idx < input.length) {
                    return input[idx++] & 0x00FF;
                } else {
                    return -1;
                }
            }

            @Override
            public int readBuffered(byte[] b, int off, int len, long timeout) throws IOException {
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
    public void testNonBlockingPumpReader() throws IOException {
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
    public void testNonBlockStreamOnReader() throws IOException {
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
}

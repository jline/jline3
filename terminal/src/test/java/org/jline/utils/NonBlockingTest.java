package org.jline.utils;

import org.junit.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

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
    public void testNonBlockingPumpReader() throws IOException {
        NonBlockingPumpReader nbr = NonBlocking.nonBlockingPumpReader();
        Writer writer = nbr.getWriter();

        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
        writer.write('中');
        assertEquals('中', nbr.read(100));
        assertEquals(NonBlockingReader.READ_EXPIRED, nbr.read(100));
    }

    @Test
    public void testNonBlockStreamOnReader() throws IOException {
        NonBlockingPumpReader reader = NonBlocking.nonBlockingPumpReader();
        NonBlockingInputStream is = NonBlocking.nonBlockingStream(reader, StandardCharsets.UTF_8);

        String s = "aaaaaaaaaaaaa";
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        reader.getWriter().write(s);
        for(int i = 0; i < bytes.length; i++) {
            int b = is.read(100L);
            assertEquals("Mismatch at " + i, bytes[i], b);
        }
        assertEquals(NonBlockingInputStream.READ_EXPIRED, is.read(100));

        s = "aaaaaaaaa中";
        bytes = s.getBytes(StandardCharsets.UTF_8);
        reader.getWriter().write(s);
        for(int i = 0; i < bytes.length; i++) {
            int b = is.read(100L);
            assertEquals("Mismatch at " + i, bytes[i], b);
        }
        assertEquals(NonBlockingInputStream.READ_EXPIRED, is.read(100));

        s = "aaaaaaaaa\uD801\uDC37";
        bytes = s.getBytes(StandardCharsets.UTF_8);
        reader.getWriter().write(s);
        for(int i = 0; i < bytes.length; i++) {
            int b = is.read(100L);
            assertEquals("Mismatch at " + i, bytes[i], b);
        }
        assertEquals(NonBlockingInputStream.READ_EXPIRED, is.read(100));
    }
}

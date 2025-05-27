/*
 * Copyright (c) 2002-2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for NonBlockingInputStreamReader encoding issues.
 * This test attempts to reproduce the intermittent encoding failures
 * seen in Windows CI environments.
 */
public class NonBlockingEncodingTest {

    /**
     * Test that simulates slow byte-by-byte reading with timeouts
     * to force decoder underflow conditions.
     */
    @Test
    public void testSlowByteByByteReading() throws IOException {
        String testString = "café";
        byte[] bytes = testString.getBytes(StandardCharsets.ISO_8859_1);

        // Create a slow input stream that introduces delays between bytes
        SlowInputStream slowInput = new SlowInputStream(bytes, 10); // 10ms delay between bytes

        NonBlockingReader reader = NonBlocking.nonBlocking("test", slowInput, StandardCharsets.ISO_8859_1);

        StringBuilder result = new StringBuilder();
        int c;
        int readCount = 0;
        int timeoutCount = 0;
        while (readCount < testString.length() && timeoutCount < 100) { // Allow up to 100 timeouts
            c = reader.read(1); // 1ms timeout - shorter than byte delay
            if (c == -1) { // EOF
                break;
            } else if (c == -2) { // READ_EXPIRED (timeout)
                timeoutCount++;
                continue; // Keep trying
            } else if (c >= 0) { // Valid character
                result.append((char) c);
                readCount++;
                System.out.println("Read " + readCount + ": '" + (char) c + "' (0x" + Integer.toHexString(c) + ")");
            }
        }

        System.out.println("Expected: '" + testString + "'");
        System.out.println("Actual: '" + result.toString() + "'");
        assertEquals(testString, result.toString(), "Slow byte-by-byte reading should preserve encoding");
    }

    /**
     * Test that simulates the exact conditions from MultiEncodingTerminalTest
     * but with controlled timing.
     */
    @Test
    public void testControlledTimingReading() throws IOException {
        String testString = "café";
        byte[] bytes = testString.getBytes(StandardCharsets.ISO_8859_1);

        // Create input stream with specific timing patterns
        TimedInputStream timedInput = new TimedInputStream(bytes);

        NonBlockingReader reader = NonBlocking.nonBlocking("test", timedInput, StandardCharsets.ISO_8859_1);

        StringBuilder result = new StringBuilder();
        int c;
        int timeoutCount = 0;
        while (timeoutCount < 1000) { // Allow up to 1000 timeouts before giving up
            c = reader.read(1);
            if (c == -1) { // EOF
                break;
            } else if (c == -2) { // READ_EXPIRED (timeout)
                timeoutCount++;
                continue; // Keep trying
            } else if (c >= 0) { // Valid character
                result.append((char) c);
            }
        }

        assertEquals(testString, result.toString(), "Controlled timing should preserve encoding");
    }

    /**
     * Test with multiple iterations to catch intermittent issues.
     */
    @Test
    public void testRepeatedReading() throws IOException {
        String testString = "café";

        for (int i = 0; i < 100; i++) {
            byte[] bytes = testString.getBytes(StandardCharsets.ISO_8859_1);
            SlowInputStream slowInput = new SlowInputStream(bytes, 1 + (i % 5)); // Variable delays

            NonBlockingReader reader = NonBlocking.nonBlocking("test-" + i, slowInput, StandardCharsets.ISO_8859_1);

            StringBuilder result = new StringBuilder();
            int c;
            int timeoutCount = 0;
            while (timeoutCount < 1000) { // Allow up to 1000 timeouts before giving up
                c = reader.read(1);
                if (c == -1) { // EOF
                    break;
                } else if (c == -2) { // READ_EXPIRED (timeout)
                    timeoutCount++;
                    continue; // Keep trying
                } else if (c >= 0) { // Valid character
                    result.append((char) c);
                }
            }

            assertEquals(testString, result.toString(), "Iteration " + i + " should preserve encoding");
        }
    }

    /**
     * Test with buffer boundary conditions.
     */
    @Test
    public void testBufferBoundaryConditions() throws IOException {
        String testString = "café";
        byte[] bytes = testString.getBytes(StandardCharsets.ISO_8859_1);

        // Test with input that forces buffer compacting
        BufferStressingInputStream stressingInput = new BufferStressingInputStream(bytes);

        NonBlockingReader reader = NonBlocking.nonBlocking("test", stressingInput, StandardCharsets.ISO_8859_1);

        StringBuilder result = new StringBuilder();
        int c;
        int timeoutCount = 0;
        while (timeoutCount < 1000) { // Allow up to 1000 timeouts before giving up
            c = reader.read(1);
            if (c == -1) { // EOF
                break;
            } else if (c == -2) { // READ_EXPIRED (timeout)
                timeoutCount++;
                continue; // Keep trying
            } else if (c >= 0) { // Valid character
                result.append((char) c);
            }
        }

        assertEquals(testString, result.toString(), "Buffer boundary conditions should preserve encoding");
    }

    /**
     * Input stream that introduces delays between bytes to simulate slow reading.
     */
    private static class SlowInputStream extends InputStream {
        private final byte[] data;
        private final long delayMs;
        private final AtomicInteger position = new AtomicInteger(0);

        public SlowInputStream(byte[] data, long delayMs) {
            this.data = data;
            this.delayMs = delayMs;
        }

        @Override
        public int read() throws IOException {
            int pos = position.getAndIncrement();
            if (pos >= data.length) {
                return -1;
            }

            if (pos > 0 && delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", e);
                }
            }

            return data[pos] & 0xFF;
        }

        @Override
        public int available() throws IOException {
            return Math.max(0, data.length - position.get());
        }
    }

    /**
     * Input stream with specific timing patterns to trigger decoder issues.
     */
    private static class TimedInputStream extends InputStream {
        private final byte[] data;
        private final AtomicInteger position = new AtomicInteger(0);

        public TimedInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() throws IOException {
            int pos = position.getAndIncrement();
            if (pos >= data.length) {
                return -1;
            }

            // Introduce specific delays for the 'é' character (0xE9 in ISO-8859-1)
            if (pos == 3 && data[pos] == (byte) 0xE9) {
                try {
                    Thread.sleep(5); // Longer delay for the problematic character
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", e);
                }
            }

            return data[pos] & 0xFF;
        }

        @Override
        public int available() throws IOException {
            return Math.max(0, data.length - position.get());
        }
    }

    /**
     * Input stream that stresses buffer management by returning bytes in patterns
     * that force buffer compacting and state management.
     */
    private static class BufferStressingInputStream extends InputStream {
        private final byte[] data;
        private final AtomicInteger position = new AtomicInteger(0);
        private int readCount = 0;

        public BufferStressingInputStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() throws IOException {
            int pos = position.getAndIncrement();
            if (pos >= data.length) {
                return -1;
            }

            readCount++;

            // Force timeout every few reads to stress buffer management
            if (readCount % 3 == 0) {
                try {
                    Thread.sleep(2); // Force timeout in NonBlockingReader
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted", e);
                }
            }

            return data[pos] & 0xFF;
        }

        @Override
        public int available() throws IOException {
            return Math.max(0, data.length - position.get());
        }
    }
}

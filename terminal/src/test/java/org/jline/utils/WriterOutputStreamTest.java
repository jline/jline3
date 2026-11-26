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
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WriterOutputStreamTest {

    @Test
    void testWideChar() throws Exception {
        StringWriter sw = new StringWriter();
        WriterOutputStream wos = new WriterOutputStream(sw, StandardCharsets.UTF_8);
        byte[] bytes = "㐀".getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            wos.write(b);
        }
        wos.flush();
        assertEquals("㐀", sw.toString());
    }

    @Test
    void testConcurrentWrites() throws Exception {
        StringWriter sw = new StringWriter();
        WriterOutputStream wos = new WriterOutputStream(sw, StandardCharsets.UTF_8);
        // Exceeds both decoderIn (256 bytes) and decoderOut (128 chars) and includes multi-byte UTF-8
        String line = "0123456789abcdef-㐀-".repeat(15) + "\n";
        byte[] payload = line.getBytes(StandardCharsets.UTF_8);

        int threads = 8;
        int iterations = 1_000;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        for (int i = 0; i < iterations; i++) {
                            wos.write(payload, 0, payload.length);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    } catch (IOException e) {
                        throw new AssertionError(e);
                    }
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        wos.flush();

        String result = sw.toString();
        assertEquals(line.repeat(threads * iterations), result);
    }

    @Test
    void testConcurrentWritesFromMultiplePrintStreams() throws Exception {
        StringWriter sw = new StringWriter();
        WriterOutputStream wos = new WriterOutputStream(sw, StandardCharsets.UTF_8);
        PrintStream ps1 = new PrintStream(wos, true, StandardCharsets.UTF_8);
        PrintStream ps2 = new PrintStream(wos, true, StandardCharsets.UTF_8);
        String msg = "0123456789abcdef0123456789abcdef";

        int threads = 8;
        int iterations = 1_000;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                PrintStream ps = (t % 2 == 0) ? ps1 : ps2;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        for (int i = 0; i < iterations; i++) {
                            ps.println(msg);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
        wos.close();

        assertFalse(ps1.checkError());
        assertFalse(ps2.checkError());
        String expectedLine = msg + System.lineSeparator();
        assertEquals(threads * iterations * expectedLine.length(), sw.toString().length());
    }
}

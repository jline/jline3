/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The infocmp capability caches are shared static state populated once per terminal
 * construction (via {@link InfoCmp#getInfoCmp(String)}), so a process that builds
 * terminals on several threads at once writes them concurrently. This exercises that
 * access pattern: on a plain {@code HashMap} concurrent puts drop entries or throw
 * during a table resize.
 */
public class InfoCmpConcurrencyTest {

    @Test
    public void testConcurrentLoadedCapsAccess() throws Exception {
        int threads = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
        int perThread = 4000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CopyOnWriteArrayList<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int t = 0; t < threads; t++) {
            final int tid = t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        String key = "concurrency-test-" + tid + "-" + i;
                        InfoCmp.setLoadedInfoCmp(key, key);
                        InfoCmp.getLoadedInfoCmp("concurrency-test-" + tid + "-" + (i / 2));
                    }
                } catch (Throwable e) {
                    errors.add(e);
                }
            });
        }

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS), "workers did not finish in time");

        assertEquals(java.util.Collections.emptyList(), errors, "concurrent cache access threw");
        for (int t = 0; t < threads; t++) {
            for (int i = 0; i < perThread; i++) {
                String key = "concurrency-test-" + t + "-" + i;
                assertEquals(key, InfoCmp.getLoadedInfoCmp(key), "cache lost entry " + key);
            }
        }
    }
}

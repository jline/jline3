/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl.history;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.IntStream;

import org.jline.reader.LineReader;
import org.jline.reader.impl.ReaderTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

/**
 * Tests file history.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class HistoryPersistenceTest extends ReaderTestSupport {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Files.deleteIfExists(Paths.get("test"));
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get("test"));
    }



    private void doTestFileHistory(int count, CyclicBarrier barrier) {
        DefaultHistory history = new DefaultHistory(reader);
        try {
            barrier.await();
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
        assertEquals(count, history.size());
        IntStream.range(0, count)
                .forEach(i -> history.add("cmd" + i));
        // we need to synchronize here
        // if we don't, multiple writes can occur at the same time and some
        // history items may be lost, we'd have to use a file lock to fix that
        // but that's not testable
        // what we're testing here is the fact that only *new* items are
        // written to the file incrementally and that we're not rewriting the
        // whole file
        synchronized (reader) {
            try {
                history.save();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testFileHistory() throws Exception {
        reader.setVariable(LineReader.HISTORY_FILE, Paths.get("test"));
        reader.unsetOpt(LineReader.Option.HISTORY_INCREMENTAL);

        int cmdsPerThread = 3;
        int nbThreads = 3;

        DefaultHistory history = new DefaultHistory(reader);
        IntStream.range(0, cmdsPerThread)
                .forEach(i -> history.add("cmd" + i));
        history.save();

        List<String> lines = Files.readAllLines(Paths.get("test"));
        assertEquals(cmdsPerThread, lines.size());

        final CyclicBarrier barrier = new CyclicBarrier(nbThreads);
        List<Thread> ts = IntStream.range(0, nbThreads)
                .mapToObj(i -> new Thread(() -> {
                    doTestFileHistory(cmdsPerThread, barrier);
                }))
                .collect(toList());
        ts.forEach(Thread::start);
        for (Thread t : ts) {
            t.join();
        }

        lines = Files.readAllLines(Paths.get("test"));
        assertEquals(cmdsPerThread * (nbThreads + 1), lines.size());
    }
}

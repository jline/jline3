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

    private void doTestFileHistory(int count) {
        reader.setVariable(LineReader.HISTORY_FILE, Paths.get("test"));
        DefaultHistory history = new DefaultHistory(reader);
        IntStream.range(0, count)
                .forEach(i -> history.add("cmd" + i));
        history.save();
    }

    @Test
    public void testFileHistory() throws Exception {
        doTestFileHistory(3);
        List<Thread> ts = IntStream.range(0, 3)
                .mapToObj(i -> new Thread(() -> doTestFileHistory(3)))
                .collect(toList());
        ts.forEach(Thread::start);
        for (Thread t : ts) {
            t.join();
        }

        List<String> lines = Files.readAllLines(Paths.get("test"));
        assertEquals(3 * 4, lines.size());
    }
}

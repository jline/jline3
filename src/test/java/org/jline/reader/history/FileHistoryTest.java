/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.history;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.IntStream;

import org.jline.reader.impl.history.FileHistory;
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
public class FileHistoryTest {

    @Before
    public void setUp() {
        new File("test").delete();
    }

    @After
    public void tearDown() {
        new File("test").delete();
    }

    private void doTestFileHistory(int count, boolean append) {
        try {
            FileHistory history = new FileHistory(new File("test"), append);
            IntStream.range(0, count)
                    .forEach(i -> history.add("cmd" + i));
            history.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void testFileHistory() throws IOException {
        doTestFileHistory(3, false);

        List<String> lines = Files.readAllLines(new File("test").toPath());
        assertEquals(3, lines.size());
    }

    @Test
    public void testFileHistoryAppend() throws Exception {
        doTestFileHistory(3, true);
        List<Thread> ts = IntStream.range(0, 3)
                .mapToObj(i -> new Thread(() -> doTestFileHistory(3, true)))
                .collect(toList());
        ts.stream().forEach(Thread::start);
        for (Thread t : ts) {
            t.join();
        }

        List<String> lines = Files.readAllLines(new File("test").toPath());
        assertEquals(3 * 4, lines.size());
    }
}

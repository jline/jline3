/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.console.history;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for {@link MemoryHistory}.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class MemoryHistoryTest
{
    private MemoryHistory history;

    @Before
    public void setUp() {
        history = new MemoryHistory();
    }

    @After
    public void tearDown() {
        history = null;
    }

    @Test
    public void testAdd() {
        assertEquals(0, history.size());

        history.add("test");

        assertEquals(1, history.size());
        assertEquals("test", history.get(0));
        assertEquals(1, history.index());
    }

    private void assertHistoryContains(final int offset, final String... items) {
        assertEquals(items.length, history.size());
        int i=0;
        for (History.Entry entry : history) {
            assertEquals(offset + i, entry.index());
            assertEquals(items[i++], entry.value());
        }
    }

    @Test
    public void testOffset() {
        history.setMaxSize(5);

        assertEquals(0, history.size());
        assertEquals(0, history.index());

        history.add("a");
        history.add("b");
        history.add("c");
        history.add("d");
        history.add("e");

        assertEquals(5, history.size());
        assertEquals(5, history.index());
        assertHistoryContains(0, "a", "b", "c", "d", "e");

        history.add("f");

        assertEquals(5, history.size());
        assertEquals(6, history.index());

        assertHistoryContains(1, "b", "c", "d", "e", "f");
        assertEquals("f", history.get(5));
    }

    @Test
    public void testReplace() {
        assertEquals(0, history.size());

        history.add("a");
        history.add("b");
        history.replace("c");

        assertHistoryContains(0, "a", "c");
    }

    @Test
    public void testSet() {
        history.add("a");
        history.add("b");
        history.add("c");

        history.set(1, "d");

        assertHistoryContains(0, "a", "d", "c");
    }

    @Test
    public void testRemove() {
        history.add("a");
        history.add("b");
        history.add("c");

        history.remove(1);

        assertHistoryContains(0, "a", "c");
    }

    @Test
    public void testRemoveFirst() {
        history.add("a");
        history.add("b");
        history.add("c");

        history.removeFirst();

        assertHistoryContains(0, "b", "c");
    }

    @Test
    public void testRemoveLast() {
        history.add("a");
        history.add("b");
        history.add("c");

        history.removeLast();

        assertHistoryContains(0, "a", "b");
    }
}

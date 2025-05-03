/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import org.junit.jupiter.api.Test;

import static org.jline.reader.LineReader.BACKWARD_KILL_WORD;
import static org.jline.reader.LineReader.BACKWARD_WORD;
import static org.jline.reader.LineReader.KILL_WHOLE_LINE;
import static org.jline.reader.LineReader.KILL_WORD;
import static org.jline.reader.LineReader.YANK;
import static org.jline.reader.LineReader.YANK_POP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the {@link KillRing}.
 */
public class KillRingTest extends ReaderTestSupport {

    @Test
    public void testEmptyKillRing() {
        KillRing killRing = new KillRing();
        assertNull(killRing.yank());
    }

    @Test
    public void testOneElementKillRing() {
        KillRing killRing = new KillRing();
        killRing.add("foo");
        String yanked = killRing.yank();
        assertNotNull(yanked);
        assertEquals(yanked, "foo");
    }

    @Test
    public void testKillKill() {
        // A kill followed by another kill will be saved in the same
        // slot.
        KillRing killRing = new KillRing();
        killRing.add("foo");
        killRing.add(" bar");
        String yanked = killRing.yank();
        assertNotNull(yanked);
        assertEquals(yanked, "foo bar");
    }

    @Test
    public void testYankTwice() {
        // A yank followed by another yank should yield the same
        // string.
        KillRing killRing = new KillRing();
        killRing.add("foo");
        killRing.resetLastKill();
        killRing.add("bar");

        String yanked = killRing.yank();
        assertNotNull(yanked);
        assertEquals(yanked, "bar");

        yanked = killRing.yank();
        assertNotNull(yanked);
        assertEquals(yanked, "bar");
    }

    @Test
    public void testYankPopNoPreviousYank() {
        // A yank-pop without a previous yank should return null.
        KillRing killRing = new KillRing();
        killRing.add("foo");
        String yanked = killRing.yankPop();
        assertNull(yanked);
    }

    @Test
    public void testYankPopWithOneSlot() {
        // Verifies that the ring works fine with one element.
        KillRing killRing = new KillRing();
        killRing.add("foo");

        String yanked = killRing.yank();
        assertNotNull(yanked);
        assertEquals(yanked, "foo");
        //
        yanked = killRing.yankPop();
        assertNotNull(yanked);
        assertEquals(yanked, "foo");
        //
        yanked = killRing.yankPop();
        assertNotNull(yanked);
        assertEquals(yanked, "foo");
    }

    @Test
    public void testYankPop() {
        // Verifies that the ring actually works like that, ie, a
        // series of yank-pop commands should eventually start
        // repeating.
        KillRing killRing = new KillRing();
        killRing.add("foo");
        killRing.resetLastKill();
        killRing.add("bar");
        killRing.resetLastKill();
        killRing.add("baz");

        String yanked = killRing.yank();
        assertNotNull(yanked);
        assertEquals(yanked, "baz");
        //
        yanked = killRing.yankPop();
        assertNotNull(yanked);
        assertEquals(yanked, "bar");
        //
        yanked = killRing.yankPop();
        assertNotNull(yanked);
        assertEquals(yanked, "foo");
        // Back to the beginning.
        yanked = killRing.yankPop();
        assertNotNull(yanked);
        assertEquals(yanked, "baz");
    }

    // Those tests are run using a buffer.

    @Test
    public void testBufferEmptyRing() throws Exception {
        TestBuffer b = new TestBuffer("This is a test");
        assertBuffer("This is a test", b = b.op(BACKWARD_WORD));
        assertBuffer("This is a test", b = b.op(YANK));
    }

    @Test
    public void testBufferWordRuboutOnce() throws Exception {
        TestBuffer b = new TestBuffer("This is a test");
        assertBuffer("This is a ", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("This is a test", b = b.op(YANK));
    }

    @Test
    public void testBufferWordRuboutTwice() throws Exception {
        TestBuffer b = new TestBuffer("This is a test");
        assertBuffer("This is a ", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("This is ", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("This is a test", b = b.op(YANK));
    }

    @Test
    public void testBufferYankPop() throws Exception {
        TestBuffer b = new TestBuffer("This is a test");
        b = b.op(BACKWARD_WORD);
        b = b.op(BACKWARD_WORD);
        assertBuffer("This a test", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("This a test", b = b.op(BACKWARD_WORD));
        assertBuffer(" a test", b = b.op(KILL_WORD));
        assertBuffer("This a test", b = b.op(YANK));
        assertBuffer("is  a test", b = b.op(YANK_POP));
    }

    @Test
    public void testBufferMixedKillsAndYank() throws Exception {
        TestBuffer b = new TestBuffer("This is a test");
        b = b.op(BACKWARD_WORD);
        b = b.op(BACKWARD_WORD);
        assertBuffer("This is  test", b = b.op(KILL_WORD));
        assertBuffer("This  test", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("This ", b = b.op(KILL_WORD));
        assertBuffer("", b = b.op(BACKWARD_KILL_WORD));
        assertBuffer("This is a test", b = b.op(YANK));
    }

    @Test
    public void testKillWholeLine() throws Exception {
        TestBuffer b = new TestBuffer("b");
        assertBuffer("", b = b.op(KILL_WHOLE_LINE));
        assertBuffer("b", b = b.op(YANK));
    }
}

/*
 * Copyright (c) 2002-2012, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package jline.console;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the {@link KillRing}.
 */
public class KillRingTest {

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
}

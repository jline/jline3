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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BufferTest {

    @Test
    public void testUpDown() {
        BufferImpl buffer = new BufferImpl();
        buffer.write("a\ncd\nefg\nhijk\nlmn\nop\nq");
        buffer.cursor(13); // after k
        assertTrue(buffer.up()); // after g
        assertEquals(8, buffer.cursor());
        buffer.move(-1); // on g
        assertEquals(7, buffer.cursor());
        assertTrue(buffer.up()); // after d
        assertEquals(4, buffer.cursor());
        assertTrue(buffer.up());
        assertEquals(1, buffer.cursor());
        assertFalse(buffer.up());
        assertEquals(1, buffer.cursor());
        assertTrue(buffer.down());
        assertEquals(4, buffer.cursor());
        assertTrue(buffer.down());
        assertEquals(7, buffer.cursor());
        assertTrue(buffer.down());
        assertEquals(11, buffer.cursor());
        assertTrue(buffer.down());
        assertEquals(16, buffer.cursor());
        assertTrue(buffer.down());
        assertEquals(20, buffer.cursor());
        assertTrue(buffer.down());
        assertEquals(22, buffer.cursor());
        assertFalse(buffer.down());
    }
}

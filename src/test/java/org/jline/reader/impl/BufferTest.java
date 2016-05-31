/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BufferTest {

    @Test
    public void testUpDown() {
        BufferImpl buffer = new BufferImpl();
        buffer.write("a\ncd\nefg\nhijk\nlmn\nop\nq");
        buffer.cursor(13);                 // after k
        assertTrue(buffer.up());           // after g
        assertEquals(8, buffer.cursor());
        buffer.move(-1);                   // on g
        assertEquals(7, buffer.cursor());
        assertTrue(buffer.up());           // after d
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

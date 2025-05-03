/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ColorsTest {

    @Test
    public void testRounding() {
        assertEquals(2, Colors.roundColor(71, 16, "cam02"));
        assertEquals(2, Colors.roundColor(71, 16, "camlab(1,2)"));

        assertEquals(8, Colors.roundColor(71, 16, "rgb"));
        assertEquals(8, Colors.roundColor(71, 16, "rgb(2,4,3)"));
        assertEquals(2, Colors.roundColor(71, 16, "cie76"));
        assertEquals(2, Colors.roundColor(71, 16, "cie94"));
        assertEquals(2, Colors.roundColor(71, 16, "cie00"));
    }

    @Test
    public void testRgb() {
        assertEquals(2, Colors.roundRgbColor(0, 128, 0, 16));
    }
}

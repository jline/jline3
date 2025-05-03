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

/**
 * Tests for the Curses utility class.
 */
public class CursesTest {

    @Test
    public void testTputs() throws Exception {

        assertEquals("{\033[3;4r", Curses.tputs("\\173\\E[%i%p1%d;%p2%dr", 2, 3));
    }

    @Test
    public void testInitc() throws Exception {

        assertEquals(
                "\033]4;123;rgb:3F/00/22\033\\",
                Curses.tputs(
                        "\\E]4;%p1%d;rgb\\:%p2%{255}%*%{1000}%/%2.2X/%p3%{255}%*%{1000}%/%2.2X/%p4%{255}%*%{1000}%/%2.2X\\E\\\\",
                        123, 0xfa, 0x00, 0x89));
    }
}

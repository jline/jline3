/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.StringWriter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class CursesTest {

    @Test
    public void testTputs() throws Exception {

        assertEquals("{\033[3;4r", Curses.tputs("\\173\\E[%i%p1%d;%p2%dr", 2, 3));

    }

}

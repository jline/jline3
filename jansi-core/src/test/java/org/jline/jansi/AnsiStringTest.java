/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 */
public class AnsiStringTest {

    @Test
    public void testCursorPosition() {
        Ansi ansi = Ansi.ansi().cursor(3, 6).reset();
        assertEquals("\u001B[3;6H\u001B[m", ansi.toString());
    }
}

/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class AbstractPtyTest {

    @Test
    public void testDescriptor() {
        assertNotNull(AbstractPty.newDescriptor(4));
    }
}

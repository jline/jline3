/*
 * Copyright (c) 2002-2021, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JansiTerminalProviderTest
{

    @Test
    public void testJansiVersion() {
        assertEquals(2, JansiTerminalProvider.JANSI_MAJOR_VERSION);
        assertEquals(4, JansiTerminalProvider.JANSI_MINOR_VERSION);
    }
}

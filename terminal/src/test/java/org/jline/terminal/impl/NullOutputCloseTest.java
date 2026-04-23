/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NullOutputCloseTest {

    @Test
    void testCloseWithNullMasterOutput() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ExternalTerminal terminal = new ExternalTerminal("test", "dumb", in, null, StandardCharsets.UTF_8);
        assertDoesNotThrow(terminal::close);
    }
}

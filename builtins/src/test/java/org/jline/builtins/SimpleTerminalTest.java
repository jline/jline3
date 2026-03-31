/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.awt.GraphicsEnvironment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimpleTerminalTest {

    static boolean isHeadless() {
        return GraphicsEnvironment.isHeadless();
    }

    @Test
    void testWebTerminalCreation() throws Exception {
        try (WebTerminal terminal = new WebTerminal("localhost", 0, 40, 20)) {
            assertNotNull(terminal);
            assertNotNull(terminal.getUrl());
        }
    }

    @Test
    @DisabledIf("isHeadless")
    void testSwingTerminalCreation() throws Exception {
        try (SwingTerminal terminal = new SwingTerminal(40, 20)) {
            assertNotNull(terminal);
            assertNotNull(terminal.getComponent());
        }
    }
}

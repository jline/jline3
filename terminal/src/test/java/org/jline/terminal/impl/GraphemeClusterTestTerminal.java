/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Test utility that creates a {@link LineDisciplineTerminal} with grapheme
 * cluster mode (mode 2027) enabled. Use in unit tests that need a
 * terminal-aware grapheme cluster context.
 *
 * <pre>
 * LineDisciplineTerminal t = GraphemeClusterTestTerminal.create();
 * try {
 *     // t.getGraphemeClusterMode() == true
 * } finally {
 *     t.close();
 * }
 * </pre>
 */
public final class GraphemeClusterTestTerminal {

    private GraphemeClusterTestTerminal() {}

    /**
     * Creates a terminal with grapheme cluster mode enabled.
     */
    public static LineDisciplineTerminal create() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", out, StandardCharsets.UTF_8);
        // Feed DECRPM probe response indicating mode 2027 is supported
        terminal.slaveInputPipe.write("\033[?2027;2$y".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();
        terminal.setGraphemeClusterMode(true);
        return terminal;
    }
}

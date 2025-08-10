/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import org.jline.builtins.Snake;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public final class SnakeDemo {
    private SnakeDemo() {}

    public static void main(String[] args) throws Exception {
        try (Terminal terminal = TerminalBuilder.builder().system(true).build()) {
            new Snake(terminal).run();
        }
    }
}

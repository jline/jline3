/*
 * Copyright (c) 2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class TerminalDemo {

    public static void main(String[] args) throws Exception {
        try (Terminal terminal = TerminalBuilder.terminal()) {
            terminal.enterRawMode();

            terminal.writer().println("Terminal: " + terminal);
            terminal.writer()
                    .println("Type characters, which will be echoed to the terminal. Q will also exit this example.");
            terminal.writer().println();
            terminal.writer().flush();

            while (true) {
                int c = terminal.reader().read(16);
                if (c >= 0) {
                    terminal.writer().write(c);
                    terminal.writer().flush();

                    // Use "q" to quit early
                    if (c == 81 || c == 113) break;
                } else {
                    if (c == -1) break; // Got EOF
                }
            }
        }
    }
}

/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp;

/**
 * Example demonstrating terminal capabilities in JLine.
 */
public class TerminalCapabilitiesExample {

    // SNIPPET_START: TerminalCapabilitiesExample
    public void checkCapabilities(Terminal terminal) {
        // Check if the terminal supports cursor movement
        boolean supportsCursorMovement = terminal.getStringCapability(InfoCmp.Capability.save_cursor) != null
                && terminal.getStringCapability(InfoCmp.Capability.restore_cursor) != null
                && terminal.getStringCapability(InfoCmp.Capability.cursor_address) != null;
        if (supportsCursorMovement) {
            // Save cursor
            terminal.puts(InfoCmp.Capability.save_cursor);
            // Move cursor to position (1, 1)
            terminal.puts(InfoCmp.Capability.cursor_address, 1, 1);
            // Print message
            terminal.writer().println("Cursor movement supported");
            // Restore cursor
            terminal.puts(InfoCmp.Capability.restore_cursor);
        }
    }
    // SNIPPET_END: TerminalCapabilitiesExample
}

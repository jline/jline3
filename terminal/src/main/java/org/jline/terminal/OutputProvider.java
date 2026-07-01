/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

import java.io.PrintWriter;

import org.jline.utils.InfoCmp.Capability;

/**
 * Provides character output and terminal capability sequences for the line editing engine.
 *
 * <p>This interface abstracts the output destination so that the editing engine
 * can render to any target, not just a JLine {@code Terminal}.
 * TUI frameworks implement this interface to direct output to their
 * own rendering pipeline.</p>
 *
 * @since 4.1
 * @see EditingTerminal
 */
public interface OutputProvider {

    /**
     * Returns a print writer for character output.
     *
     * <p>The editing engine writes prompt text, completion candidates,
     * and other display content through this writer.</p>
     *
     * @return a print writer for output
     */
    PrintWriter writer();

    /**
     * Flushes any buffered output.
     */
    void flush();

    /**
     * Emits the escape sequence for the given terminal capability.
     *
     * <p>This is the primary mechanism for cursor movement, screen clearing,
     * keypad mode, and other terminal control operations.</p>
     *
     * @param capability the capability whose escape sequence should be emitted
     * @param params     optional parameters for parameterized capabilities
     * @return {@code true} if the capability was emitted, {@code false} if not supported
     */
    boolean puts(Capability capability, Object... params);

    /**
     * Enables or disables mouse tracking.
     *
     * <p>The default implementation returns {@code false}, indicating that mouse
     * tracking is not supported.</p>
     *
     * @param tracking the desired mouse tracking mode
     * @return {@code true} if the tracking mode was set, {@code false} if not supported
     */
    default boolean trackMouse(Terminal.MouseTracking tracking) {
        return false;
    }
}

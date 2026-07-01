/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import java.util.function.IntConsumer;

import org.jline.terminal.Cursor;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

/**
 * Provides terminal capability information for the line editing engine.
 *
 * <p>This interface abstracts terminal capability queries so that the editing
 * engine can function with any capability source, not just a JLine {@code Terminal}.
 * TUI frameworks implement this interface to report the capabilities of their
 * rendering environment.</p>
 *
 * @since 4.1
 * @see EditingTerminal
 */
public interface CapabilityProvider {

    /**
     * Returns the string value of the given terminal capability.
     *
     * @param capability the capability to query
     * @return the capability's string value, or {@code null} if not supported
     */
    String getStringCapability(Capability capability);

    /**
     * Returns whether the given boolean terminal capability is set.
     *
     * @param capability the capability to query
     * @return {@code true} if the capability is set
     */
    boolean getBooleanCapability(Capability capability);

    /**
     * Returns whether grapheme cluster mode is enabled for character width calculation.
     *
     * <p>When enabled, the editing engine uses Unicode grapheme cluster boundaries
     * for cursor movement and display width calculations.</p>
     *
     * @return {@code true} if grapheme cluster mode is enabled
     */
    default boolean getGraphemeClusterMode() {
        return false;
    }

    /**
     * Queries the current cursor position.
     *
     * <p>The default implementation returns {@code null}, indicating that cursor
     * position queries are not supported.</p>
     *
     * @param discarded a consumer that receives characters read while waiting for the response
     * @return the cursor position, or {@code null} if not supported
     */
    default Cursor getCursorPosition(IntConsumer discarded) {
        return null;
    }

    /**
     * Returns the name of the terminal or editing context.
     *
     * @return the name
     */
    default String getName() {
        return "embedded";
    }

    /**
     * Returns the terminal type identifier.
     *
     * <p>The type is used for behavior adjustments (e.g., dumb terminal detection).
     * The default returns {@link Terminal#TYPE_DUMB}.</p>
     *
     * @return the terminal type string
     */
    default String getType() {
        return Terminal.TYPE_DUMB;
    }
}

/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

import java.util.function.IntSupplier;

import org.jline.utils.NonBlockingReader;

/**
 * Provides character input for the line editing engine.
 *
 * <p>This interface abstracts the input source so that the editing engine
 * can be driven by any event loop, not just a JLine {@code Terminal}.
 * TUI frameworks implement this interface to feed key events from their
 * own input pipeline.</p>
 *
 * @since 4.1
 * @see EditingTerminal
 */
public interface InputProvider {

    /**
     * Returns a non-blocking reader that supplies characters to the editing engine.
     *
     * <p>The returned reader is used to create a {@code BindingReader} which maps
     * character sequences to key bindings.</p>
     *
     * @return a non-blocking character reader
     */
    NonBlockingReader reader();

    /**
     * Reads a mouse event from the input stream.
     *
     * <p>The default implementation returns {@code null}, indicating that mouse
     * events are not supported. Implementations that support mouse input should
     * override this method to decode mouse escape sequences from the input.</p>
     *
     * @param reader   a supplier that reads the next character from input
     * @param lastBinding the last key binding string, used to detect mouse event prefixes
     * @return the decoded mouse event, or {@code null} if mouse is not supported
     */
    default MouseEvent readMouseEvent(IntSupplier reader, String lastBinding) {
        return null;
    }
}

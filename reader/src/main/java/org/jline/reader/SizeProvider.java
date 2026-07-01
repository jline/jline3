/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import org.jline.terminal.Size;

/**
 * Provides viewport dimensions for the line editing engine.
 *
 * <p>This interface abstracts the size source so that the editing engine
 * can adapt to any viewport, not just a JLine {@code Terminal}.
 * TUI frameworks implement this interface to report the dimensions
 * of the widget or region hosting the line editor.</p>
 *
 * @since 4.1
 * @see EditingTerminal
 */
public interface SizeProvider {

    /**
     * Returns the visible terminal or viewport size.
     *
     * <p>This is used for display calculations such as line wrapping
     * and completion menu layout.</p>
     *
     * @return the current size (columns and rows)
     */
    Size getSize();

    /**
     * Returns the buffer size, which may include scrollback.
     *
     * <p>For many implementations, this is the same as {@link #getSize()}.
     * Terminals with scrollback buffers may report a larger row count here.</p>
     *
     * @return the buffer size (columns and rows, possibly including scrollback)
     */
    Size getBufferSize();
}

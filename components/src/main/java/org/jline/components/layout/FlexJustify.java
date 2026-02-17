/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.layout;

/**
 * Main-axis justification for flexbox layout.
 */
public enum FlexJustify {
    /** Pack children toward start. */
    START,
    /** Center children on main axis. */
    CENTER,
    /** Pack children toward end. */
    END,
    /** Distribute space between children. */
    SPACE_BETWEEN,
    /** Distribute space around children. */
    SPACE_AROUND
}

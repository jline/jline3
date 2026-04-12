/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

/**
 * Represents an object (e.g. a terminal) which has a size in character cells.
 * <ul>
 *   <li><b>Columns</b> - The width of this object in character cells</li>
 *   <li><b>Rows</b> - The height of this object in character cells</li>
 * </ul>
 */
public interface Sized {

    /**
     * Returns the number of columns (width) in this object.
     *
     * <p>
     * The number of columns represents the width of the terminal in character cells.
     * </p>
     *
     * @return The number of columns (width) in this object.
     */
    int getColumns();

    /**
     * Returns the number of rows (height) in this object.
     *
     * <p>
     * The number of rows represents the height of the terminal in character cells.
     * </p>
     *
     * @return The number of rows (height) in this object.
     */
    int getRows();
}

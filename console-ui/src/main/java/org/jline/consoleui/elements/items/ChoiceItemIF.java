/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements.items;

/**
 * User: Andreas Wegmann
 * Date: 13.01.16
 */
public interface ChoiceItemIF extends ConsoleUIItemIF, ListItemIF {
    default boolean isDefaultChoice() {
        return false;
    }

    default Character getKey() {
        return ' ';
    }
}

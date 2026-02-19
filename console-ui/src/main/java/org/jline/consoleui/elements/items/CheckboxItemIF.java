/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements.items;

public interface CheckboxItemIF extends ConsoleUIItemIF {
    default boolean isChecked() {
        return false;
    }
}

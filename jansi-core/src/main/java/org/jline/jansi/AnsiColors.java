/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi;

/**
 * Colors support.
 *
 * @since 2.1
 */
public enum AnsiColors {
    Colors16("16 colors"),
    Colors256("256 colors"),
    TrueColor("24-bit colors");

    private final String description;

    AnsiColors(String description) {
        this.description = description;
    }

    String getDescription() {
        return description;
    }
}

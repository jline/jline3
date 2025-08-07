/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi.io;

/**
 * Helper class for dealing with color rounding.
 * This is a simplified version of the JLine's one at
 *   https://github.com/jline/jline3/blob/a24636dc5de83baa6b65049e8215fb372433b3b1/terminal/src/main/java/org/jline/utils/Colors.java
 */
public class Colors {

    /**
     * Creates a new Colors.
     */
    public Colors() {
        // Default constructor
    }

    /**
     * Default 256 colors palette
     */
    public static final int[] DEFAULT_COLORS_256 = org.jline.utils.Colors.DEFAULT_COLORS_256;

    public static int roundColor(int col, int max) {
        return org.jline.utils.Colors.roundColor(col, max);
    }

    public static int roundRgbColor(int r, int g, int b, int max) {
        return org.jline.utils.Colors.roundRgbColor(r, g, b, max);
    }
}

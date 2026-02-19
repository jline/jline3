/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StylesTest {
    private static final int TIMEOUT = 1;

    private static final String STANDARD_COLORS = "di=1;91:ex=1;92:ln=1;96:fi=";

    private static final String SEPARATOR = ":";

    private static final String DIRECTORY_STYLE_ELEMENT = "di=1";

    private static final String ASTERISK_TILDE_STYLE_ELEMENT = "*~=00;90";

    private static final int REPEATED_ELEMENTS = 1000;

    @Test
    public void testIsStylePatternStandardColors() {
        final boolean stylePattern = Styles.isStylePattern(STANDARD_COLORS);

        assertTrue(stylePattern);
    }

    @Test
    @Timeout(TIMEOUT)
    public void testIsStylePatternRepeatedDirectory() {
        final StringBuilder builder = getRepeatedStyleBuilder();

        final String style = builder.toString();
        final boolean stylePattern = Styles.isStylePattern(style);

        assertTrue(stylePattern);
    }

    @Test
    @Timeout(TIMEOUT)
    public void testIsStylePatternAsteriskTilde() {
        final StringBuilder builder = getRepeatedStyleBuilder();
        builder.append(ASTERISK_TILDE_STYLE_ELEMENT);
        builder.append(SEPARATOR);

        final String style = builder.toString();
        final boolean stylePattern = Styles.isStylePattern(style);

        assertFalse(stylePattern);
    }

    private StringBuilder getRepeatedStyleBuilder() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < REPEATED_ELEMENTS; i++) {
            builder.append(DIRECTORY_STYLE_ELEMENT);
            builder.append(SEPARATOR);
        }
        return builder;
    }
}

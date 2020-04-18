/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import static org.junit.Assert.assertEquals;

import org.jline.utils.AttributedStyle;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link StyleResolver}.
 */
public class AnsiStyleResolverTest extends StyleTestSupport {

    private StyleResolver underTest;

    @Override
    @Before
    public void setUp() {
        super.setUp();

        source = StyleSourceHelper.appendAnsiStyleDefinition(source,
            StyleSourceHelper.GREP_COLORS, StyleSourceHelper.DEFAULT_GREP_COLORS);

        underTest = new StyleResolver(source, StyleSourceHelper.GREP_COLORS);
    }

    @Test
    public void resolveBold() {
        AttributedStyle style = underTest.resolve(".se");
        assertEquals(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN), style);
    }
}

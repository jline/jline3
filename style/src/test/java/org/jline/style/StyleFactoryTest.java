/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import org.jline.utils.AttributedString;
import org.junit.Before;
import org.junit.Test;

import static org.jline.utils.AttributedStyle.BOLD;
import static org.jline.utils.AttributedStyle.RED;
import static org.jline.utils.AttributedStyle.YELLOW;

/**
 * Tests for {@link StyleFactory}.
 */
public class StyleFactoryTest extends StyleTestSupport {

    private StyleFactory underTest;

    @Before
    public void setUp() {
        super.setUp();
        this.underTest = new StyleFactory(new StyleResolver(source, "test"));
    }

    @Test
    public void styleDirect() {
        AttributedString string = underTest.style("bold,fg:red", "foo bar");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo bar", BOLD.foreground(RED)));
    }

    @Test
    public void styleReferenced() {
        source.set("test", "very-red", "bold,fg:red");
        AttributedString string = underTest.style(".very-red", "foo bar");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo bar", BOLD.foreground(RED)));
    }

    @Test
    public void missingReferencedStyleWithDefault() {
        AttributedString string = underTest.style(".very-red:-bold,fg:red", "foo bar");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo bar", BOLD.foreground(RED)));
    }

    @Test
    public void missingReferencedStyleWithCustomized() {
        source.set("test", "very-red", "bold,fg:yellow");
        AttributedString string = underTest.style(".very-red:-bold,fg:red", "foo bar");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo bar", BOLD.foreground(YELLOW)));
    }

    @Test
    public void styleFormat() {
        AttributedString string = underTest.style("bold", "%s", "foo");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo", BOLD));
    }

    @Test
    public void evaluateExpression() {
        AttributedString string = underTest.evaluate("@{bold foo}");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo", BOLD));
    }

    @Test
    public void evaluateExpressionWithFormat() {
        AttributedString string = underTest.evaluate("@{bold %s}", "foo");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo", BOLD));
    }
}

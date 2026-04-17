/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import org.jline.utils.AttributedString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jline.utils.AttributedStyle.BOLD;
import static org.jline.utils.AttributedStyle.RED;
import static org.jline.utils.AttributedStyle.YELLOW;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link StyleFactory}.
 */
class StyleFactoryTest extends StyleTestSupport {

    private StyleFactory underTest;

    @BeforeEach
    void setUp() {
        super.setUp();
        this.underTest = new StyleFactory(new StyleResolver(source, "test"));
    }

    @Test
    void styleDirect() {
        AttributedString string = underTest.style("bold,fg:red", "foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(RED)), string);
    }

    @Test
    void styleReferenced() {
        source.set("test", "very-red", "bold,fg:red");
        AttributedString string = underTest.style(".very-red", "foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(RED)), string);
    }

    @Test
    void missingReferencedStyleWithDefault() {
        AttributedString string = underTest.style(".very-red:-bold,fg:red", "foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(RED)), string);
    }

    @Test
    void missingReferencedStyleWithCustomized() {
        source.set("test", "very-red", "bold,fg:yellow");
        AttributedString string = underTest.style(".very-red:-bold,fg:red", "foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(YELLOW)), string);
    }

    @Test
    void styleFormat() {
        AttributedString string = underTest.style("bold", "%s", "foo");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo", BOLD), string);
    }

    @Test
    void evaluateExpression() {
        AttributedString string = underTest.evaluate("@{bold foo}");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo", BOLD), string);
    }

    @Test
    void evaluateExpressionWithFormat() {
        AttributedString string = underTest.evaluate("@{bold %s}", "foo");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo", BOLD), string);
    }
}

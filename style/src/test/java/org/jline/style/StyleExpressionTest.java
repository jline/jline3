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
import org.jline.utils.AttributedStringBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jline.utils.AttributedStyle.BOLD;
import static org.jline.utils.AttributedStyle.CYAN;
import static org.jline.utils.AttributedStyle.DEFAULT;
import static org.jline.utils.AttributedStyle.RED;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link StyleExpression}.
 */
class StyleExpressionTest extends StyleTestSupport {

    private StyleExpression underTest;

    @BeforeEach
    void setUp() {
        super.setUp();
        this.underTest = new StyleExpression(new StyleResolver(source, "test"));
    }

    @Test
    void evaluateExpressionWithPrefixAndSuffix() {
        AttributedString result = underTest.evaluate("foo @{bold bar} baz");
        System.out.println(result.toAnsi());
        assertEquals(
                new AttributedStringBuilder()
                        .append("foo ")
                        .append("bar", BOLD)
                        .append(" baz")
                        .toAttributedString(),
                result);
    }

    @Test
    void evaluateExpressionWithPrefix() {
        AttributedString result = underTest.evaluate("foo @{bold bar}");
        System.out.println(result.toAnsi());
        assertEquals(
                new AttributedStringBuilder().append("foo ").append("bar", BOLD).toAttributedString(), result);
    }

    @Test
    void evaluateExpressionWithSuffix() {
        AttributedString result = underTest.evaluate("@{bold foo} bar");
        System.out.println(result.toAnsi());
        assertEquals(
                new AttributedStringBuilder().append("foo", BOLD).append(" bar").toAttributedString(), result);
    }

    @Test
    void evaluateExpression() {
        AttributedString result = underTest.evaluate("@{bold foo}");
        System.out.println(result.toAnsi());
        assertEquals(new AttributedString("foo", BOLD), result);
    }

    @Test
    void evaluateExpressionWithDefault() {

        AttributedString result = underTest.evaluate("@{.foo:-bold foo}");
        System.out.println(result.toAnsi());
        assertEquals(new AttributedString("foo", BOLD), result);
    }

    @Test
    void evaluateExpressionWithMultipleReplacements() {
        AttributedString result = underTest.evaluate("@{bold foo} @{fg:red bar} @{underline baz}");
        System.out.println(result.toAnsi());
        assertEquals(
                new AttributedStringBuilder()
                        .append("foo", BOLD)
                        .append(" ")
                        .append("bar", DEFAULT.foreground(RED))
                        .append(" ")
                        .append("baz", DEFAULT.underline())
                        .toAttributedString(),
                result);
    }

    @Test
    void evaluateExpressionWithRecursiveReplacements() {
        AttributedString result = underTest.evaluate("@{underline foo @{fg:cyan bar}}");
        System.out.println(result.toAnsi());
        assertEquals(
                new AttributedStringBuilder()
                        .append("foo ", DEFAULT.underline())
                        .append("bar", DEFAULT.underline().foreground(CYAN))
                        .toAttributedString(),
                result);
    }

    @Test
    void evaluateExpressionMissingVvalue() {
        AttributedString result = underTest.evaluate("@{bold}");
        System.out.println(result.toAnsi());
        assertEquals(new AttributedString("@{bold}", DEFAULT), result);
    }

    @Test
    void evaluateExpressionMissingTokens() {
        AttributedString result = underTest.evaluate("foo");
        System.out.println(result.toAnsi());
        assertEquals(new AttributedString("foo", DEFAULT), result);
    }

    @Test
    void evaluateExpressionWithPlaceholderValue() {
        AttributedString result = underTest.evaluate("@{bold,fg:cyan ${foo\\}}");
        System.out.println(result.toAnsi());
        assertEquals(new AttributedString("${foo}", DEFAULT.bold().foreground(CYAN)), result);
    }

    @Test
    void evaluateWithStyleReference() {
        source.set("test", "very-red", "bold,fg:red");
        AttributedString string = underTest.evaluate("@{.very-red foo bar}");
        assertEquals(new AttributedString("foo bar", BOLD.foreground(RED)), string);
    }
}

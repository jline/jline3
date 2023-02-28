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
import org.jline.utils.AttributedStringBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.jline.utils.AttributedStyle.BOLD;
import static org.jline.utils.AttributedStyle.CYAN;
import static org.jline.utils.AttributedStyle.DEFAULT;
import static org.jline.utils.AttributedStyle.RED;

/**
 * Tests for {@link StyleExpression}.
 */
public class StyleExpressionTest extends StyleTestSupport {

    private StyleExpression underTest;

    @Before
    public void setUp() {
        super.setUp();
        this.underTest = new StyleExpression(new StyleResolver(source, "test"));
    }

    @Test
    public void evaluateExpressionWithPrefixAndSuffix() {
        AttributedString result = underTest.evaluate("foo @{bold bar} baz");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedStringBuilder()
                .append("foo ")
                .append("bar", BOLD)
                .append(" baz")
                .toAttributedString());
    }

    @Test
    public void evaluateExpressionWithPrefix() {
        AttributedString result = underTest.evaluate("foo @{bold bar}");
        System.out.println(result.toAnsi());
        assert result.equals(
                new AttributedStringBuilder().append("foo ").append("bar", BOLD).toAttributedString());
    }

    @Test
    public void evaluateExpressionWithSuffix() {
        AttributedString result = underTest.evaluate("@{bold foo} bar");
        System.out.println(result.toAnsi());
        assert result.equals(
                new AttributedStringBuilder().append("foo", BOLD).append(" bar").toAttributedString());
    }

    @Test
    public void evaluateExpression() {
        AttributedString result = underTest.evaluate("@{bold foo}");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedString("foo", BOLD));
    }

    @Test
    public void evaluateExpressionWithDefault() {

        AttributedString result = underTest.evaluate("@{.foo:-bold foo}");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedString("foo", BOLD));
    }

    @Test
    public void evaluateExpressionWithMultipleReplacements() {
        AttributedString result = underTest.evaluate("@{bold foo} @{fg:red bar} @{underline baz}");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedStringBuilder()
                .append("foo", BOLD)
                .append(" ")
                .append("bar", DEFAULT.foreground(RED))
                .append(" ")
                .append("baz", DEFAULT.underline())
                .toAttributedString());
    }

    @Test
    public void evaluateExpressionWithRecursiveReplacements() {
        AttributedString result = underTest.evaluate("@{underline foo @{fg:cyan bar}}");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedStringBuilder()
                .append("foo ", DEFAULT.underline())
                .append("bar", DEFAULT.underline().foreground(CYAN))
                .toAttributedString());
    }

    @Test
    public void evaluateExpressionMissingVvalue() {
        AttributedString result = underTest.evaluate("@{bold}");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedString("@{bold}", DEFAULT));
    }

    @Test
    public void evaluateExpressionMissingTokens() {
        AttributedString result = underTest.evaluate("foo");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedString("foo", DEFAULT));
    }

    @Test
    public void evaluateExpressionWithPlaceholderValue() {
        AttributedString result = underTest.evaluate("@{bold,fg:cyan ${foo\\}}");
        System.out.println(result.toAnsi());
        assert result.equals(new AttributedString("${foo}", DEFAULT.bold().foreground(CYAN)));
    }

    @Test
    public void evaluateWithStyleReference() {
        source.set("test", "very-red", "bold,fg:red");
        AttributedString string = underTest.evaluate("@{.very-red foo bar}");
        assert string.equals(new AttributedString("foo bar", BOLD.foreground(RED)));
    }
}

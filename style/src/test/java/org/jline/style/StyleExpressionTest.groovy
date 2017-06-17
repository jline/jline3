/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style

import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

import static org.jline.utils.AttributedStyle.*

/**
 * Tests for {@link StyleExpression}.
 */
class StyleExpressionTest
        extends StyleTestSupport {
    private StyleExpression underTest

    @Before
    void setUp() {
        super.setUp()
        this.underTest = new StyleExpression(new StyleResolver(source, 'test'))
    }

    @Test
    void 'evaluate expression with prefix and suffix'() {
        def result = underTest.evaluate('foo @{bold bar} baz')
        println result.toAnsi()
        assert result == new AttributedStringBuilder()
                .append('foo ')
                .append('bar', BOLD)
                .append(' baz')
                .toAttributedString()
    }

    @Test
    void 'evaluate expression with prefix'() {
        def result = underTest.evaluate('foo @{bold bar}')
        println result.toAnsi()
        assert result == new AttributedStringBuilder()
                .append('foo ')
                .append('bar', BOLD)
                .toAttributedString()
    }

    @Test
    void 'evaluate expression with suffix'() {
        def result = underTest.evaluate('@{bold foo} bar')
        println result.toAnsi()
        assert result == new AttributedStringBuilder()
                .append('foo', BOLD)
                .append(' bar')
                .toAttributedString()
    }

    @Test
    void 'evaluate expression'() {
        def result = underTest.evaluate('@{bold foo}')
        println result.toAnsi()
        assert result == new AttributedString('foo', BOLD)
    }

    @Test
    void 'evaluate expression with default'() {
        def result = underTest.evaluate('@{.foo:-bold foo}')
        println result.toAnsi()
        assert result == new AttributedString('foo', BOLD)
    }

    @Test
    void 'evaluate expression with multiple replacements'() {
        def result = underTest.evaluate('@{bold foo} @{fg:red bar} @{underline baz}')
        println result.toAnsi()
        assert result == new AttributedStringBuilder()
                .append('foo', BOLD)
                .append(' ')
                .append('bar', DEFAULT.foreground(RED))
                .append(' ')
                .append('baz', DEFAULT.underline())
                .toAttributedString()
    }

    @Test
    void 'evaluate expression with recursive replacements'() {
        def result = underTest.evaluate('@{underline foo @{fg:cyan bar}}')
        println result.toAnsi()
        assert result == new AttributedStringBuilder()
                .append('foo ', DEFAULT.underline())
                .append('bar', DEFAULT.underline().foreground(CYAN))
                .toAttributedString()
    }

    @Test
    void 'evaluate expression missing value'() {
        def result = underTest.evaluate('@{bold}')
        println result.toAnsi()
        assert result == new AttributedString('@{bold}', DEFAULT)
    }

    @Test
    void 'evaluate expression missing tokens'() {
        def result = underTest.evaluate('foo')
        println result.toAnsi()
        assert result == new AttributedString('foo', DEFAULT)
    }

    @Test
    void 'evaluate expression with ${} value'() {
        def result = underTest.evaluate('@{bold,fg:cyan ${foo\\}}')
        println result.toAnsi()
        assert result == new AttributedString('${foo}', DEFAULT.bold().foreground(CYAN))
    }

    @Test
    void 'evaluate with style reference'() {
        source.set('test', 'very-red', 'bold,fg:red')
        def string = underTest.evaluate('@{.very-red foo bar}')
        assert string == new AttributedString('foo bar', BOLD.foreground(RED))
    }
}

/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style

import org.jline.utils.Colors
import org.junit.Before
import org.junit.Test

import static org.jline.utils.AttributedStyle.*

/**
 * Tests for {@link StyleResolver}.
 */
class StyleResolverTest
        extends StyleTestSupport {
    private StyleResolver underTest

    @Before
    void setUp() {
        super.setUp()
        this.underTest = new StyleResolver(source, 'test')
    }

    @Test
    void 'resolve bold'() {
        def style = underTest.resolve('bold')
        assert style == BOLD
    }

    @Test
    void 'resolve fg:red'() {
        def style = underTest.resolve('fg:red')
        assert style == DEFAULT.foreground(RED)
    }

    @Test
    void 'resolve fg:red with whitespace'() {
        def style = underTest.resolve(' fg:  red ')
        assert style == DEFAULT.foreground(RED)
    }

    @Test
    void 'resolve bg:red'() {
        def style = underTest.resolve('bg:red')
        assert style == DEFAULT.background(RED)
    }

    @Test
    void 'resolve invalid color-mode'() {
        def style = underTest.resolve('invalid:red')
        assert style == DEFAULT
    }

    @Test
    void 'resolve invalid color-name'() {
        def style = underTest.resolve('fg:invalid')
        assert style == DEFAULT
    }

    @Test
    void 'resolve bold,fg:red'() {
        def style = underTest.resolve('bold,fg:red')
        assert style == BOLD.foreground(RED)
    }

    @Test
    void 'resolve with whitespace'() {
        def style = underTest.resolve('  bold ,   fg:red   ')
        assert style == BOLD.foreground(RED)
    }

    @Test
    void 'resolve with missing values'() {
        def style = underTest.resolve('bold,,,,,fg:red')
        assert style == BOLD.foreground(RED)
    }

    @Test
    void 'resolve referenced style'() {
        source.set('test', 'very-red', 'bold,fg:red')
        def style = underTest.resolve('.very-red')
        assert style == BOLD.foreground(RED)
    }

    @Test
    void 'resolve referenced style-missing with default direct'() {
        def style = underTest.resolve('.very-red:-bold,fg:red')
        assert style == BOLD.foreground(RED)
    }

    @Test
    void 'resolve referenced style-missing with default direct and whitespace'() {
        def style = underTest.resolve('.very-red   :-   bold,fg:red')
        assert style == BOLD.foreground(RED)
    }

    @Test
    void 'resolve referenced style-missing with default referenced'() {
        source.set('test', 'more-red', 'bold,fg:red')
        def style = underTest.resolve('.very-red:-.more-red')
        assert style == BOLD.foreground(RED)
    }

    @Test
    void 'resolve fg:bright-red'() {
        def style = underTest.resolve('fg:bright-red')
        assert style == DEFAULT.foreground(BRIGHT + RED)
    }

    @Test
    void 'resolve fg:!red'() {
        def style = underTest.resolve('fg:!red')
        assert style == DEFAULT.foreground(BRIGHT + RED)
    }

    @Test
    void 'resolve fg:~olive'() {
        def style = underTest.resolve('fg:~olive')
        assert style == DEFAULT.foreground(Colors.rgbColor("olive"))
    }

    @Test
    void 'resolve invalid xterm256 syntax'() {
        def style = underTest.resolve('fg:~')
        assert style == DEFAULT
    }

    @Test
    void 'resolve invalid xterm256 color name'() {
        def style = underTest.resolve('fg:~foo')
        assert style == DEFAULT
    }

    @Test
    void 'check color ordinal'() {
        assert 86 == Colors.rgbColor("aquamarine1")
    }
}

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
import org.junit.Before
import org.junit.Test

import static org.jline.utils.AttributedStyle.*

/**
 * Tests for {@link StyleFactory}.
 */
class StyleFactoryTest
        extends StyleTestSupport {
    private StyleFactory underTest

    @Before
    void setUp() {
        super.setUp()
        this.underTest = new StyleFactory(new StyleResolver(source, 'test'))
    }

    @Test
    void 'style direct'() {
        def string = underTest.style('bold,fg:red', 'foo bar')
        println string.toAnsi()
        assert string == new AttributedString('foo bar', BOLD.foreground(RED))
    }

    @Test
    void 'style referenced'() {
        source.set('test', 'very-red', 'bold,fg:red')
        def string = underTest.style('.very-red', 'foo bar')
        println string.toAnsi()
        assert string == new AttributedString('foo bar', BOLD.foreground(RED))
    }

    @Test
    void 'missing referenced style with default'() {
        def string = underTest.style('.very-red:-bold,fg:red', 'foo bar')
        println string.toAnsi()
        assert string == new AttributedString('foo bar', BOLD.foreground(RED))
    }

    @Test
    void 'missing referenced style with customized'() {
        source.set('test', 'very-red', 'bold,fg:yellow')
        def string = underTest.style('.very-red:-bold,fg:red', 'foo bar')
        println string.toAnsi()
        assert string == new AttributedString('foo bar', BOLD.foreground(YELLOW))
    }

    @Test
    void 'style format'() {
        def string = underTest.style('bold', '%s', 'foo')
        println string.toAnsi()
        assert string == new AttributedString('foo', BOLD)
    }

    @Test
    void 'evaluate expression'() {
        def string = underTest.evaluate('@{bold foo}')
        println string.toAnsi()
        assert string == new AttributedString('foo', BOLD)
    }

    @Test
    void 'evaluate expression with format'() {
        def string = underTest.evaluate('@{bold %s}', 'foo')
        println string.toAnsi()
        assert string == new AttributedString('foo', BOLD)
    }
}

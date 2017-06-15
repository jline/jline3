/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style

import org.jline.style.StyleBundle.DefaultStyle
import org.jline.style.StyleBundle.StyleGroup
import org.jline.style.StyleBundle.StyleName
import org.jline.style.StyleBundleInvocationHandler.InvalidStyleBundleMethodException
import org.jline.style.StyleBundleInvocationHandler.InvalidStyleGroupException
import org.jline.style.StyleBundleInvocationHandler.StyleBundleMethodMissingDefaultStyleException
import org.jline.utils.AttributedString
import org.junit.Test

import static org.jline.utils.AttributedStyle.*

/**
 * Tests for {@link StyleBundleInvocationHandler}.
 */
class StyleBundleInvocationHandlerTest
        extends StyleTestSupport {
    @StyleGroup('test')
    static interface Styles
            extends StyleBundle {
        @DefaultStyle('bold,fg:red')
        AttributedString boldRed(String value)

        @StyleName('boldRed')
        @DefaultStyle('bold,fg:red')
        AttributedString boldRedObjectWithStyleName(Object value)

        AttributedString missingDefaultStyle(String value)

        void invalidReturn(String value)

        AttributedString notEnoughArguments()

        AttributedString tooManyArguments(int a, int b)
    }

    static interface MissingStyleGroupStyles
            extends StyleBundle {
        @DefaultStyle('bold,fg:red')
        AttributedString boldRed(String value)
    }

    @Test
    void 'bundle missing style-group'() {
        try {
            StyleBundleInvocationHandler.create(source, MissingStyleGroupStyles.class)
            assert false
        }
        catch (InvalidStyleGroupException e) {
            // expected
        }
    }

    @Test
    void 'bundle proxy-toString'() {
        def styles = StyleBundleInvocationHandler.create(source, Styles.class)
        assert styles.toString() == Styles.class.getName()
    }

    @Test
    void 'bundle default-style'() {
        def styles = StyleBundleInvocationHandler.create(source, Styles.class)
        def string = styles.boldRed('foo bar')
        println string.toAnsi()
        assert string == new AttributedString('foo bar', BOLD.foreground(RED))
    }

    @Test
    void 'bundle default-style missing'() {
        def styles = StyleBundleInvocationHandler.create(source, Styles.class)
        try {
            styles.missingDefaultStyle('foo bar')
            assert false
        }
        catch (StyleBundleMethodMissingDefaultStyleException e) {
            // expected
        }
    }

    @Test
    void 'bundle default-style missing but source-referenced'() {
        source.set('test', 'missingDefaultStyle', 'bold')
        def styles = StyleBundleInvocationHandler.create(source, Styles.class)
        def string = styles.missingDefaultStyle('foo bar')
        println string.toAnsi()
        assert string == new AttributedString('foo bar', BOLD)
    }

    @Test
    void 'bundle style-name with default-style'() {
        def styles = StyleBundleInvocationHandler.create(source, Styles.class)
        def string = styles.boldRedObjectWithStyleName('foo bar')
        println string.toAnsi()
        assert string == new AttributedString('foo bar', BOLD.foreground(RED))
    }

    @Test
    void 'bundle sourced-style'() {
        source.set('test', 'boldRed', 'bold,fg:yellow')
        def styles = StyleBundleInvocationHandler.create(source, Styles.class)
        def string = styles.boldRed('foo bar')
        println string.toAnsi()
        assert string == new AttributedString('foo bar', BOLD.foreground(YELLOW))
    }

    @Test
    void 'bundle explicit style-group'() {
        source.set('test2', 'boldRed', 'bold,fg:yellow')
        def styles = StyleBundleInvocationHandler.create(new StyleResolver(source, 'test2'), Styles.class)
        def string = styles.boldRed('foo bar')
        println string.toAnsi()
        assert string == new AttributedString('foo bar', BOLD.foreground(YELLOW))
    }

    @Test
    void 'bundle method validation'() {
        def styles = StyleBundleInvocationHandler.create(source, Styles.class)

        try {
            styles.invalidReturn('foo')
            assert false
        }
        catch (InvalidStyleBundleMethodException e) {
            // expected
        }

        try {
            styles.notEnoughArguments()
            assert false
        }
        catch (InvalidStyleBundleMethodException e) {
            // expected
        }

        try {
            styles.tooManyArguments(1, 2)
            assert false
        }
        catch (InvalidStyleBundleMethodException e) {
            // expected
        }
    }
}

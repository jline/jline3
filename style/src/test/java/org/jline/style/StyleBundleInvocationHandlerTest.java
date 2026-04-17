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
import org.junit.jupiter.api.Test;

import static org.jline.utils.AttributedStyle.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link StyleBundleInvocationHandler}.
 */
class StyleBundleInvocationHandlerTest extends StyleTestSupport {

    @Test
    void bundleMissingStyleGroup() {
        assertThrows(
                StyleBundleInvocationHandler.InvalidStyleGroupException.class,
                () -> StyleBundleInvocationHandler.create(source, MissingStyleGroupStyles.class));
    }

    @Test
    void bundleProxyToString() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        assertEquals(Styles.class.getName(), styles.toString());
    }

    @Test
    void bundleDefaultStyle() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.boldRed("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(RED)), string);
    }

    @Test
    void bundleDefaultStyleMissing() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        assertThrows(
                StyleBundleInvocationHandler.StyleBundleMethodMissingDefaultStyleException.class,
                () -> styles.missingDefaultStyle("foo bar"));
    }

    @Test
    void bundleDefaultStyleMissingButSourceReferenced() {
        source.set("test", "missingDefaultStyle", "bold");
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.missingDefaultStyle("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD), string);
    }

    @Test
    void bundleStyleNameWithDefaultStyle() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.boldRedObjectWithStyleName("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(RED)), string);
    }

    @Test
    void bundleSourcedStyle() {
        source.set("test", "boldRed", "bold,fg:yellow");
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.boldRed("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(YELLOW)), string);
    }

    @Test
    void bundleExplicitStyleGroup() {
        source.set("test2", "boldRed", "bold,fg:yellow");
        Styles styles = StyleBundleInvocationHandler.create(new StyleResolver(source, "test2"), Styles.class);
        AttributedString string = styles.boldRed("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(YELLOW)), string);
    }

    @Test
    void bundleMethodValidation() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);

        assertThrows(
                StyleBundleInvocationHandler.InvalidStyleBundleMethodException.class,
                () -> styles.invalidReturn("foo"));

        assertThrows(StyleBundleInvocationHandler.InvalidStyleBundleMethodException.class, styles::notEnoughArguments);

        assertThrows(
                StyleBundleInvocationHandler.InvalidStyleBundleMethodException.class,
                () -> styles.tooManyArguments(1, 2));
    }

    @StyleBundle.StyleGroup("test")
    interface Styles extends StyleBundle {

        @DefaultStyle("bold,fg:red")
        AttributedString boldRed(String value);

        @StyleName("boldRed")
        @DefaultStyle("bold,fg:red")
        AttributedString boldRedObjectWithStyleName(Object value);

        AttributedString missingDefaultStyle(String value);

        void invalidReturn(String value);

        AttributedString notEnoughArguments();

        AttributedString tooManyArguments(int a, int b);
    }

    interface MissingStyleGroupStyles extends StyleBundle {

        @SuppressWarnings("unused")
        @DefaultStyle("bold,fg:red")
        AttributedString boldRed(String value);
    }
}

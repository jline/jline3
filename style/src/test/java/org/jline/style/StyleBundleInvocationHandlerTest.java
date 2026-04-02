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
public class StyleBundleInvocationHandlerTest extends StyleTestSupport {

    @Test
    public void bundleMissingStyleGroup() {
        assertThrows(StyleBundleInvocationHandler.InvalidStyleGroupException.class, () -> {
            StyleBundleInvocationHandler.create(source, MissingStyleGroupStyles.class);
        });
    }

    @Test
    public void bundleProxyToString() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        assertEquals(Styles.class.getName(), styles.toString());
    }

    @Test
    public void bundleDefaultStyle() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.boldRed("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(RED)), string);
    }

    @Test
    public void bundleDefaultStyleMissing() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        assertThrows(StyleBundleInvocationHandler.StyleBundleMethodMissingDefaultStyleException.class, () -> {
            styles.missingDefaultStyle("foo bar");
        });
    }

    @Test
    public void bundleDefaultStyleMissingButSourceReferenced() {
        source.set("test", "missingDefaultStyle", "bold");
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.missingDefaultStyle("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD), string);
    }

    @Test
    public void bundleStyleNameWithDefaultStyle() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.boldRedObjectWithStyleName("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(RED)), string);
    }

    @Test
    public void bundleSourcedStyle() {
        source.set("test", "boldRed", "bold,fg:yellow");
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.boldRed("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(YELLOW)), string);
    }

    @Test
    public void bundleExplicitStyleGroup() {
        source.set("test2", "boldRed", "bold,fg:yellow");
        Styles styles = StyleBundleInvocationHandler.create(new StyleResolver(source, "test2"), Styles.class);
        AttributedString string = styles.boldRed("foo bar");
        System.out.println(string.toAnsi());
        assertEquals(new AttributedString("foo bar", BOLD.foreground(YELLOW)), string);
    }

    @Test
    public void bundleMethodValidation() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);

        assertThrows(StyleBundleInvocationHandler.InvalidStyleBundleMethodException.class, () -> {
            styles.invalidReturn("foo");
        });

        assertThrows(StyleBundleInvocationHandler.InvalidStyleBundleMethodException.class, () -> {
            styles.notEnoughArguments();
        });

        assertThrows(StyleBundleInvocationHandler.InvalidStyleBundleMethodException.class, () -> {
            styles.tooManyArguments(1, 2);
        });
    }

    @StyleBundle.StyleGroup("test")
    public interface Styles extends StyleBundle {

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

    public interface MissingStyleGroupStyles extends StyleBundle {

        @DefaultStyle("bold,fg:red")
        AttributedString boldRed(String value);
    }
}

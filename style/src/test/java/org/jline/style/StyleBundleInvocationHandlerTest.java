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
import org.junit.Test;

import static org.jline.utils.AttributedStyle.*;

/**
 * Tests for {@link StyleBundleInvocationHandler}.
 */
public class StyleBundleInvocationHandlerTest extends StyleTestSupport {

    @Test
    public void bundleMissingStyleGroup() {
        try {
            StyleBundleInvocationHandler.create(source, MissingStyleGroupStyles.class);
            assert false;
        } catch (StyleBundleInvocationHandler.InvalidStyleGroupException e) {
            // expected
        }
    }

    @Test
    public void bundleProxyToString() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        assert styles.toString().equals(Styles.class.getName());
    }

    @Test
    public void bundleDefaultStyle() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.boldRed("foo bar");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo bar", BOLD.foreground(RED)));
    }

    @Test
    public void bundleDefaultStyleMissing() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        try {
            styles.missingDefaultStyle("foo bar");
            assert false;
        } catch (StyleBundleInvocationHandler.StyleBundleMethodMissingDefaultStyleException e) {
            // expected
        }
    }

    @Test
    public void bundleDefaultStyleMissingButSourceReferenced() {
        source.set("test", "missingDefaultStyle", "bold");
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.missingDefaultStyle("foo bar");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo bar", BOLD));
    }

    @Test
    public void bundleStyleNameWithDefaultStyle() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.boldRedObjectWithStyleName("foo bar");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo bar", BOLD.foreground(RED)));
    }

    @Test
    public void bundleSourcedStyle() {
        source.set("test", "boldRed", "bold,fg:yellow");
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);
        AttributedString string = styles.boldRed("foo bar");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo bar", BOLD.foreground(YELLOW)));
    }

    @Test
    public void bundleExplicitStyleGroup() {
        source.set("test2", "boldRed", "bold,fg:yellow");
        Styles styles = StyleBundleInvocationHandler.create(new StyleResolver(source, "test2"), Styles.class);
        AttributedString string = styles.boldRed("foo bar");
        System.out.println(string.toAnsi());
        assert string.equals(new AttributedString("foo bar", BOLD.foreground(YELLOW)));
    }

    @Test
    public void bundleMethodValidation() {
        Styles styles = StyleBundleInvocationHandler.create(source, Styles.class);

        try {
            styles.invalidReturn("foo");
            assert false;
        } catch (StyleBundleInvocationHandler.InvalidStyleBundleMethodException e) {
            // expected
        }

        try {
            styles.notEnoughArguments();
            assert false;
        } catch (StyleBundleInvocationHandler.InvalidStyleBundleMethodException e) {
            // expected
        }

        try {
            styles.tooManyArguments(1, 2);
            assert false;
        } catch (StyleBundleInvocationHandler.InvalidStyleBundleMethodException e) {
            // expected
        }
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

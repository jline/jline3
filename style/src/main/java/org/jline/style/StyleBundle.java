/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker interface for proxy-based style bundles.
 * <p>
 * A StyleBundle is an interface that defines methods for creating styled text.
 * Each method in the interface should return an {@link org.jline.utils.AttributedString}
 * and take a single parameter that will be styled according to the method's
 * {@link DefaultStyle} annotation or a style definition from a {@link StyleSource}.
 * </p>
 * <p>
 * StyleBundle interfaces are not implemented directly. Instead, they are used with
 * the {@link Styler#bundle(Class)} or {@link Styler#bundle(String, Class)} methods
 * to create dynamic proxies that implement the interface.
 * </p>
 * <p>
 * Example:
 * </p>
 * <pre>
 * &#64;StyleBundle.StyleGroup("mygroup")
 * interface MyStyles extends StyleBundle {
 *     &#64;StyleBundle.DefaultStyle("bold,fg:red")
 *     AttributedString error(String message);
 *
 *     &#64;StyleBundle.DefaultStyle("bold,fg:yellow")
 *     AttributedString warning(String message);
 * }
 *
 * MyStyles styles = Styler.bundle(MyStyles.class);
 * AttributedString errorText = styles.error("Error message");
 * </pre>
 *
 * @since 3.4
 * @see Styler#bundle(Class)
 * @see Styler#bundle(String, Class)
 */
public interface StyleBundle {
    /**
     * Annotation that specifies the style group name for a StyleBundle interface.
     * <p>
     * This annotation is required on StyleBundle interfaces used with
     * {@link Styler#bundle(Class)}. It specifies the style group to use when
     * looking up named styles in a {@link StyleSource}.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * &#64;StyleBundle.StyleGroup("mygroup")
     * interface MyStyles extends StyleBundle {
     *     // methods...
     * }
     * </pre>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    @interface StyleGroup {
        String value();
    }

    /**
     * Annotation that allows overriding the style name for a method in a StyleBundle interface.
     * <p>
     * By default, the style name used for a method is the method name itself.
     * This annotation allows specifying a different name to use when looking up
     * styles in a {@link StyleSource}.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * &#64;StyleBundle.StyleGroup("mygroup")
     * interface MyStyles extends StyleBundle {
     *     &#64;StyleBundle.StyleName("error-style")
     *     &#64;StyleBundle.DefaultStyle("bold,fg:red")
     *     AttributedString error(String message);
     * }
     * </pre>
     * <p>
     * In this example, the style name "error-style" will be used instead of "error"
     * when looking up the style in the style source.
     * </p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    @interface StyleName {
        String value();
    }

    /**
     * Annotation that provides a default style specification for a method in a StyleBundle interface.
     * <p>
     * This annotation specifies the style to use if no style is found in the
     * {@link StyleSource} for the method. The value should be a valid style
     * specification string as understood by {@link org.jline.utils.StyleResolver}.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * &#64;StyleBundle.StyleGroup("mygroup")
     * interface MyStyles extends StyleBundle {
     *     &#64;StyleBundle.DefaultStyle("bold,fg:red")
     *     AttributedString error(String message);
     *
     *     &#64;StyleBundle.DefaultStyle("fg:blue,underline")
     *     AttributedString link(String url);
     * }
     * </pre>
     * <p>
     * If this annotation is not present and no style is found in the style source,
     * a {@link StyleBundleInvocationHandler.StyleBundleMethodMissingDefaultStyleException}
     * will be thrown when the method is called.
     * </p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    @interface DefaultStyle {
        String value();
    }
}

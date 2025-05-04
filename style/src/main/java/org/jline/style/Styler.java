/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jline.style.StyleBundle.StyleGroup;

import static java.util.Objects.requireNonNull;

/**
 * Style facade that provides static utility methods for working with styles.
 * <p>
 * The Styler class is the main entry point for the JLine style system. It provides
 * access to the global {@link StyleSource} and factory methods for creating
 * {@link StyleResolver}s, {@link StyleFactory}s, and {@link StyleBundle} proxies.
 * </p>
 * <p>
 * Typical usage patterns include:
 * </p>
 * <pre>
 * // Configure a global style source
 * Styler.setSource(new MemoryStyleSource());
 *
 * // Create a style resolver for a specific group
 * StyleResolver resolver = Styler.resolver("mygroup");
 *
 * // Create a style factory for a specific group
 * StyleFactory factory = Styler.factory("mygroup");
 *
 * // Create a style bundle proxy
 * MyStyles styles = Styler.bundle(MyStyles.class);
 * </pre>
 *
 * @see StyleBundle
 * @see StyleFactory
 * @see StyleSource
 * @see StyleResolver
 * @since 3.4
 */
public class Styler {
    private static final Logger log = Logger.getLogger(Styler.class.getName());

    private static volatile StyleSource source = new NopStyleSource();

    private Styler() {
        // empty
    }

    /**
     * Returns the global {@link StyleSource} used by the Styler.
     * <p>
     * The global style source is used by all style operations that don't explicitly
     * specify a different source. By default, a {@link NopStyleSource} is used,
     * which doesn't provide any styles.
     * </p>
     *
     * @return the global style source
     * @see #setSource(StyleSource)
     */
    public static StyleSource getSource() {
        return source;
    }

    /**
     * Installs a new global {@link StyleSource}.
     * <p>
     * This method sets the global style source used by all style operations
     * that don't explicitly specify a different source. A common implementation
     * to use is {@link MemoryStyleSource}.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * // Create and configure a style source
     * MemoryStyleSource source = new MemoryStyleSource();
     * source.set("mygroup", "error", "bold,fg:red");
     * source.set("mygroup", "warning", "bold,fg:yellow");
     *
     * // Set it as the global source
     * Styler.setSource(source);
     * </pre>
     *
     * @param source the new global style source (must not be null)
     * @throws NullPointerException if source is null
     * @see #getSource()
     */
    public static void setSource(final StyleSource source) {
        Styler.source = requireNonNull(source);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Source: " + source);
        }
    }

    /**
     * Creates a {@link StyleResolver} for the given style group.
     * <p>
     * The style resolver is used to resolve style specifications into
     * {@link org.jline.utils.AttributedStyle} objects. It uses the global
     * style source to look up named styles within the specified group.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * StyleResolver resolver = Styler.resolver("mygroup");
     * AttributedStyle style = resolver.resolve("bold,fg:red");
     * AttributedStyle namedStyle = resolver.resolve(".error"); // Looks up "error" in "mygroup"
     * </pre>
     *
     * @param group the style group name (must not be null)
     * @return a new style resolver for the specified group
     * @throws NullPointerException if group is null
     */
    public static StyleResolver resolver(final String group) {
        return new StyleResolver(source, group);
    }

    /**
     * Creates a {@link StyleFactory} for the given style group.
     * <p>
     * The style factory provides methods for creating styled strings using
     * style specifications or expressions. It uses a {@link StyleResolver}
     * for the specified group to resolve styles.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * StyleFactory factory = Styler.factory("mygroup");
     * AttributedString text = factory.style("bold,fg:red", "Important message");
     * AttributedString namedText = factory.style(".error", "Error message");
     * AttributedString expr = factory.evaluate("Normal text with @{bold,fg:red important} parts");
     * </pre>
     *
     * @param group the style group name (must not be null)
     * @return a new style factory for the specified group
     * @throws NullPointerException if group is null
     */
    public static StyleFactory factory(final String group) {
        return new StyleFactory(resolver(group));
    }

    /**
     * Creates a {@link StyleBundle} proxy for the specified interface.
     * <p>
     * This method creates a dynamic proxy that implements the specified interface.
     * Each method in the interface is expected to return an {@link org.jline.utils.AttributedString}
     * and take a single parameter that will be styled according to the method's
     * {@link StyleBundle.DefaultStyle} annotation or a style definition from the
     * global style source.
     * </p>
     * <p>
     * The target interface must be annotated with {@link StyleGroup} to specify
     * the style group to use for style lookups.
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
     * @param <T> the interface type to proxy
     * @param type the interface class to proxy (must not be null and must be annotated with {@link StyleGroup})
     * @return a proxy implementing the specified interface
     * @throws NullPointerException if type is null
     * @throws StyleBundleInvocationHandler.InvalidStyleGroupException if the interface is not annotated with {@link StyleGroup}
     */
    public static <T extends StyleBundle> T bundle(final Class<T> type) {
        return StyleBundleInvocationHandler.create(source, type);
    }

    /**
     * Creates a {@link StyleBundle} proxy for the specified interface with an explicit style group.
     * <p>
     * This method is similar to {@link #bundle(Class)}, but it allows specifying the
     * style group explicitly instead of requiring the interface to be annotated with
     * {@link StyleGroup}.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * interface MyStyles extends StyleBundle {
     *     &#64;StyleBundle.DefaultStyle("bold,fg:red")
     *     AttributedString error(String message);
     * }
     *
     * MyStyles styles = Styler.bundle("mygroup", MyStyles.class);
     * AttributedString errorText = styles.error("Error message");
     * </pre>
     *
     * @param <T> the interface type to proxy
     * @param group the style group name to use (must not be null)
     * @param type the interface class to proxy (must not be null)
     * @return a proxy implementing the specified interface
     * @throws NullPointerException if group or type is null
     */
    public static <T extends StyleBundle> T bundle(final String group, final Class<T> type) {
        return StyleBundleInvocationHandler.create(resolver(group), type);
    }
}

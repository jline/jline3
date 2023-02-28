/*
 * Copyright (c) 2002-2018, the original author(s).
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
 * Style facade.
 *
 * @see StyleBundle
 * @see StyleFactory
 * @see StyleSource
 * @since 3.4
 */
public class Styler {
    private static final Logger log = Logger.getLogger(Styler.class.getName());

    private static volatile StyleSource source = new NopStyleSource();

    private Styler() {
        // empty
    }

    /**
     * Returns global {@link StyleSource}.
     *
     * @return the global style source
     */
    public static StyleSource getSource() {
        return source;
    }

    /**
     * Install global {@link StyleSource}.
     *
     * @param source the new global style source
     */
    public static void setSource(final StyleSource source) {
        Styler.source = requireNonNull(source);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Source: " + source);
        }
    }

    /**
     * Create a resolver for the given style-group.
     *
     * @param group the group
     * @return the resolver
     */
    public static StyleResolver resolver(final String group) {
        return new StyleResolver(source, group);
    }

    /**
     * Create a factory for the given style-group.
     *
     * @param group the group
     * @return the factory
     */
    public static StyleFactory factory(final String group) {
        return new StyleFactory(resolver(group));
    }

    /**
     * Create a {@link StyleBundle} proxy.
     * <p>
     * Target class must be annotated with {@link StyleGroup}.
     *
     * @param <T> the interface to proxy
     * @param type the interface to proxy
     * @return the proxy
     */
    public static <T extends StyleBundle> T bundle(final Class<T> type) {
        return StyleBundleInvocationHandler.create(source, type);
    }

    /**
     * Create a {@link StyleBundle} proxy with explicit style-group.
     *
     * @param <T> the interface to proxy
     * @param group the group
     * @param type the interface to proxy
     * @return the proxy
     */
    public static <T extends StyleBundle> T bundle(final String group, final Class<T> type) {
        return StyleBundleInvocationHandler.create(resolver(group), type);
    }
}

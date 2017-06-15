/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style;

import org.jline.style.StyleBundle.StyleGroup;

import java.util.logging.Level;
import java.util.logging.Logger;

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
     */
    public static StyleSource getSource() {
        return source;
    }

    /**
     * Install global {@link StyleSource}.
     */
    public static void setSource(final StyleSource source) {
        Styler.source = requireNonNull(source);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Source: " + source);
        }
    }

    /**
     * Create a resolver for the given style-group.
     */
    public static StyleResolver resolver(final String group) {
        return new StyleResolver(source, group);
    }

    /**
     * Create a factory for the given style-group.
     */
    public static StyleFactory factory(final String group) {
        return new StyleFactory(resolver(group));
    }

    /**
     * Create a {@link StyleBundle} proxy.
     * <p>
     * Target class must be annotated with {@link StyleGroup}.
     */
    public static <T extends StyleBundle> T bundle(final Class<T> type) {
        return StyleBundleInvocationHandler.create(source, type);
    }

    /**
     * Create a {@link StyleBundle} proxy with explicit style-group.
     */
    public static <T extends StyleBundle> T bundle(final String group, final Class<T> type) {
        return StyleBundleInvocationHandler.create(resolver(group), type);
    }
}

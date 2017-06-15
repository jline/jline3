/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.style;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Provides the source of style configuration.
 *
 * @since 3.4
 */
public interface StyleSource {
    /**
     * Returns the appropriate style for the given style-group and style-name, or {@code null} if missing.
     */
    @Nullable
    String get(String group, String name);

    /**
     * Set a specific style in a style-group.
     */
    void set(String group, String name, String style);

    /**
     * Remove all styles for given style-group.
     */
    void remove(String group);

    /**
     * Remove a specific style from style-group.
     */
    void remove(String group, String name);

    /**
     * Clear all styles.
     */
    void clear();

    /**
     * Returns configured style-group names.
     *
     * @return Immutable collection.
     */
    Iterable<String> groups();

    /**
     * Returns configured styles for given style-group.
     *
     * @return Immutable map.
     */
    Map<String, String> styles(String group);
}

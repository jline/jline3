/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import java.util.Map;
import javax.annotation.Nullable;

/**
 * Provides the source of style configuration.
 *
 * @since 3.4
 */
public interface StyleSource {
    /**
     * Returns the appropriate style for the given style-group and style-name, or {@code null} if missing.
     *
     * @param group the group
     * @param name the style name
     * @return the style
     */
    @Nullable
    String get(String group, String name);

    /**
     * Set a specific style in a style-group.
     *
     * @param group the group
     * @param name the style name
     * @param style the style to set
     */
    void set(String group, String name, String style);

    /**
     * Remove all styles for given style-group.
     *
     * @param group the group
     */
    void remove(String group);

    /**
     * Remove a specific style from style-group.
     *
     * @param group the group
     * @param name the style name to remove
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
     * @param group  the style group
     * @return Immutable map.
     */
    Map<String, String> styles(String group);
}

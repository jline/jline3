/*
 * Copyright (c) 2002-2025, the original author(s).
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
 * Interface for sources of style configuration.
 * <p>
 * A StyleSource provides access to style definitions organized by groups and names.
 * It allows retrieving, setting, and removing style definitions, as well as listing
 * available style groups and styles.
 * </p>
 * <p>
 * Style definitions are stored as strings in the format understood by
 * {@link org.jline.utils.StyleResolver}, such as "bold,fg:red" or "underline,fg:blue".
 * </p>
 * <p>
 * Implementations of this interface include:
 * </p>
 * <ul>
 *   <li>{@link MemoryStyleSource} - Stores styles in memory</li>
 *   <li>{@link NopStyleSource} - Always returns null (no styles)</li>
 * </ul>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * StyleSource source = new MemoryStyleSource();
 * source.set("mygroup", "error", "bold,fg:red");
 * source.set("mygroup", "warning", "bold,fg:yellow");
 *
 * String errorStyle = source.get("mygroup", "error"); // Returns "bold,fg:red"
 * </pre>
 *
 * @since 3.4
 * @see Styler
 * @see StyleResolver
 */
public interface StyleSource {
    /**
     * Returns the style definition for the given style group and name, or {@code null} if not found.
     * <p>
     * This method retrieves a style definition from the source. Style definitions are
     * strings in the format understood by {@link org.jline.utils.StyleResolver},
     * such as "bold,fg:red" or "underline,fg:blue".
     * </p>
     * <p>
     * Style groups are used to organize styles by category or purpose, such as
     * "error", "warning", or "info" styles within a "messages" group.
     * </p>
     *
     * @param group the style group name (must not be null)
     * @param name the style name within the group (must not be null)
     * @return the style definition string, or {@code null} if no style is defined for the given group and name
     * @throws NullPointerException if group or name is null
     */
    @Nullable
    String get(String group, String name);

    /**
     * Sets a style definition for the given style group and name.
     * <p>
     * This method stores a style definition in the source. Style definitions are
     * strings in the format understood by {@link org.jline.utils.StyleResolver},
     * such as "bold,fg:red" or "underline,fg:blue".
     * </p>
     * <p>
     * If a style with the same group and name already exists, it will be replaced.
     * </p>
     * <p>
     * Example:
     * </p>
     * <pre>
     * source.set("messages", "error", "bold,fg:red");
     * source.set("messages", "warning", "bold,fg:yellow");
     * source.set("links", "url", "fg:blue,underline");
     * </pre>
     *
     * @param group the style group name (must not be null)
     * @param name the style name within the group (must not be null)
     * @param style the style definition string (must not be null)
     * @throws NullPointerException if any parameter is null
     */
    void set(String group, String name, String style);

    /**
     * Removes all styles for the given style group.
     * <p>
     * This method removes all style definitions associated with the specified group.
     * If the group does not exist or has no styles, this method has no effect.
     * </p>
     *
     * @param group the style group name to remove (must not be null)
     * @throws NullPointerException if group is null
     */
    void remove(String group);

    /**
     * Removes a specific style from a style group.
     * <p>
     * This method removes the style definition for the specified group and name.
     * If the style does not exist, this method has no effect.
     * </p>
     *
     * @param group the style group name (must not be null)
     * @param name the style name to remove (must not be null)
     * @throws NullPointerException if group or name is null
     */
    void remove(String group, String name);

    /**
     * Clears all style definitions from this source.
     * <p>
     * This method removes all style groups and their associated styles from the source.
     * After calling this method, the source will be empty.
     * </p>
     */
    void clear();

    /**
     * Returns the names of all configured style groups.
     * <p>
     * This method returns an iterable of all style group names that have been
     * configured in this source. If no groups have been configured, an empty
     * iterable is returned.
     * </p>
     *
     * @return an immutable iterable of style group names (never null)
     */
    Iterable<String> groups();

    /**
     * Returns all configured styles for the given style group.
     * <p>
     * This method returns a map of style names to style definitions for the
     * specified group. If the group does not exist or has no styles, an empty
     * map is returned.
     * </p>
     *
     * @param group the style group name (must not be null)
     * @return an immutable map of style names to style definitions (never null)
     * @throws NullPointerException if group is null
     */
    Map<String, String> styles(String group);
}

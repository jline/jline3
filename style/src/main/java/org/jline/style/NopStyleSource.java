/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * A no-operation implementation of {@link StyleSource} that always returns {@code null}.
 * <p>
 * This class provides an implementation of StyleSource that does not store or
 * retrieve any styles. All methods that modify styles are no-ops, and all methods
 * that retrieve styles return empty results.
 * </p>
 * <p>
 * This class is useful as a default or fallback StyleSource when no styles are
 * needed or available. It is used by default in {@link Styler} until a different
 * StyleSource is set.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * // Create a StyleResolver with a NopStyleSource
 * StyleResolver resolver = new StyleResolver(new NopStyleSource(), "group");
 *
 * // Named style references will always resolve to null
 * AttributedStyle style = resolver.resolve(".error"); // Uses default style if provided
 * </pre>
 *
 * @since 3.4
 * @see StyleSource
 * @see MemoryStyleSource
 * @see Styler#getSource()
 */
public class NopStyleSource implements StyleSource {
    // NOTE: preconditions here to help validate usage when this impl is used

    /**
     * Always returns {@code null} for any style lookup.
     * <p>
     * This implementation validates that the parameters are not null but
     * otherwise always returns null, indicating that no style is defined.
     * </p>
     *
     * @param group the style group name (must not be null)
     * @param name the style name within the group (must not be null)
     * @return always {@code null}
     * @throws NullPointerException if group or name is null
     */
    @Nullable
    @Override
    public String get(final String group, final String name) {
        requireNonNull(group);
        requireNonNull(name);
        return null;
    }

    /**
     * No-operation implementation of set that does nothing.
     * <p>
     * This implementation validates that the parameters are not null but
     * otherwise does nothing. The style is not stored anywhere.
     * </p>
     *
     * @param group the style group name (must not be null)
     * @param name the style name within the group (must not be null)
     * @param style the style definition string (must not be null)
     * @throws NullPointerException if any parameter is null
     */
    @Override
    public void set(final String group, final String name, final String style) {
        requireNonNull(group);
        requireNonNull(name);
        requireNonNull(style);
    }

    /**
     * No-operation implementation of remove that does nothing.
     * <p>
     * This implementation validates that the parameter is not null but
     * otherwise does nothing.
     * </p>
     *
     * @param group the style group name to remove (must not be null)
     * @throws NullPointerException if group is null
     */
    @Override
    public void remove(final String group) {
        requireNonNull(group);
    }

    /**
     * No-operation implementation of remove that does nothing.
     * <p>
     * This implementation validates that the parameters are not null but
     * otherwise does nothing.
     * </p>
     *
     * @param group the style group name (must not be null)
     * @param name the style name to remove (must not be null)
     * @throws NullPointerException if group or name is null
     */
    @Override
    public void remove(final String group, final String name) {
        requireNonNull(group);
        requireNonNull(name);
    }

    /**
     * No-operation implementation of clear that does nothing.
     * <p>
     * Since this implementation doesn't store any styles, this method has no effect.
     * </p>
     */
    @Override
    public void clear() {
        // empty
    }

    /**
     * Always returns an empty list of style groups.
     * <p>
     * Since this implementation doesn't store any styles, this method always
     * returns an empty, immutable list.
     * </p>
     *
     * @return an empty, immutable iterable
     */
    @Override
    public Iterable<String> groups() {
        return Collections.unmodifiableList(Collections.emptyList());
    }

    /**
     * Always returns an empty map of styles.
     * <p>
     * Since this implementation doesn't store any styles, this method always
     * returns an empty, immutable map regardless of the group specified.
     * </p>
     *
     * @param group the style group name (not used in this implementation)
     * @return an empty, immutable map
     */
    @Override
    public Map<String, String> styles(final String group) {
        return Collections.unmodifiableMap(Collections.emptyMap());
    }
}

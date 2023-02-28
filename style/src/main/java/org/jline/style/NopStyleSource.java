/*
 * Copyright (c) 2002-2017, the original author(s).
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
 * {@link StyleSource} which always returns {@code null}.
 *
 * @since 3.4
 */
public class NopStyleSource implements StyleSource {
    // NOTE: preconditions here to help validate usage when this impl is used

    /**
     * Always returns {@code null}.
     */
    @Nullable
    @Override
    public String get(final String group, final String name) {
        requireNonNull(group);
        requireNonNull(name);
        return null;
    }

    /**
     * Non-operation.
     */
    @Override
    public void set(final String group, final String name, final String style) {
        requireNonNull(group);
        requireNonNull(name);
        requireNonNull(style);
    }

    /**
     * Non-operation.
     */
    @Override
    public void remove(final String group) {
        requireNonNull(group);
    }

    /**
     * Non-operation.
     */
    @Override
    public void remove(final String group, final String name) {
        requireNonNull(group);
        requireNonNull(name);
    }

    /**
     * Non-operation.
     */
    @Override
    public void clear() {
        // empty
    }

    /**
     * Always returns empty list.
     */
    @Override
    public Iterable<String> groups() {
        return Collections.unmodifiableList(Collections.emptyList());
    }

    /**
     * Always returns empty map.
     */
    @Override
    public Map<String, String> styles(final String group) {
        return Collections.unmodifiableMap(Collections.emptyMap());
    }
}

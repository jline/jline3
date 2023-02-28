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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * In-memory {@link StyleSource}.
 *
 * @since 3.4
 */
public class MemoryStyleSource implements StyleSource {
    private static final Logger log = Logger.getLogger(MemoryStyleSource.class.getName());

    private final Map<String, Map<String, String>> backing = new ConcurrentHashMap<>();

    @Nullable
    @Override
    public String get(final String group, final String name) {
        String style = null;
        Map<String, String> styles = backing.get(group);
        if (styles != null) {
            style = styles.get(name);
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest(String.format("Get: [%s] %s -> %s", group, name, style));
        }

        return style;
    }

    @Override
    public void set(final String group, final String name, final String style) {
        requireNonNull(group);
        requireNonNull(name);
        requireNonNull(style);
        backing.computeIfAbsent(group, k -> new ConcurrentHashMap<>()).put(name, style);

        if (log.isLoggable(Level.FINEST)) {
            log.finest(String.format("Set: [%s] %s -> %s", group, name, style));
        }
    }

    @Override
    public void remove(final String group) {
        requireNonNull(group);
        if (backing.remove(group) != null) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest(String.format("Removed: [%s]", group));
            }
        }
    }

    @Override
    public void remove(final String group, final String name) {
        requireNonNull(group);
        requireNonNull(name);
        Map<String, String> styles = backing.get(group);
        if (styles != null) {
            styles.remove(name);

            if (log.isLoggable(Level.FINEST)) {
                log.finest(String.format("Removed: [%s] %s", group, name));
            }
        }
    }

    @Override
    public void clear() {
        backing.clear();
        log.finest("Cleared");
    }

    @Override
    public Iterable<String> groups() {
        return Collections.unmodifiableSet(backing.keySet());
    }

    @Override
    public Map<String, String> styles(final String group) {
        requireNonNull(group);
        Map<String, String> result = backing.get(group);
        if (result == null) {
            result = Collections.emptyMap();
        }
        return Collections.unmodifiableMap(result);
    }
}

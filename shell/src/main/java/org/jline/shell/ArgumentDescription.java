/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jline.utils.AttributedString;

/**
 * Describes a command argument with a name and optional description.
 *
 * @see CommandDescription
 * @since 4.0
 */
public class ArgumentDescription {

    private final String name;
    private final List<AttributedString> description;

    /**
     * Creates a new argument description with the specified name and an empty description.
     *
     * @param name the argument name
     * @throws IllegalArgumentException if the name contains spaces or tabs
     */
    public ArgumentDescription(String name) {
        this(name, new ArrayList<>());
    }

    /**
     * Creates a new argument description with the specified name and description.
     *
     * @param name the argument name
     * @param description the description lines
     * @throws IllegalArgumentException if the name contains spaces or tabs
     */
    public ArgumentDescription(String name, List<AttributedString> description) {
        if (name.contains("\t") || name.contains(" ")) {
            throw new IllegalArgumentException("Bad argument name: " + name);
        }
        this.name = name;
        this.description = Collections.unmodifiableList(new ArrayList<>(description));
    }

    /**
     * Returns the argument name.
     *
     * @return the name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the argument description lines.
     *
     * @return the description, never null
     */
    public List<AttributedString> description() {
        return description;
    }

    /**
     * Creates a list of argument descriptions from names.
     * Each description will have an empty description list.
     *
     * @param names the argument names
     * @return a list of argument descriptions
     */
    public static List<ArgumentDescription> of(String... names) {
        List<ArgumentDescription> out = new ArrayList<>();
        for (String n : names) {
            out.add(new ArgumentDescription(n));
        }
        return out;
    }
}

/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.util.ArrayList;
import java.util.List;

import org.jline.utils.AttributedString;

/**
 * Represents a command argument description used for generating command help and documentation.
 * This class stores the name of an argument and its description as a list of attributed strings,
 * which can include formatting and styling.
 */
public class ArgDesc {
    /** The name of the argument */
    private final String name;
    /** The description of the argument as a list of attributed strings */
    private final List<AttributedString> description;

    /**
     * Creates a new argument description with the specified name and an empty description.
     *
     * @param name the name of the argument
     * @throws IllegalArgumentException if the name contains spaces or tabs
     */
    public ArgDesc(String name) {
        this(name, new ArrayList<>());
    }

    /**
     * Creates a new argument description with the specified name and description.
     *
     * @param name the name of the argument
     * @param description the description of the argument as a list of attributed strings
     * @throws IllegalArgumentException if the name contains spaces or tabs
     */
    public ArgDesc(String name, List<AttributedString> description) {
        if (name.contains("\t") || name.contains(" ")) {
            throw new IllegalArgumentException("Bad argument name: " + name);
        }
        this.name = name;
        this.description = new ArrayList<>(description);
    }

    /**
     * Returns the name of the argument.
     *
     * @return the argument name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of the argument as a list of attributed strings.
     *
     * @return the argument description
     */
    public List<AttributedString> getDescription() {
        return description;
    }

    /**
     * Creates a list of argument descriptions from a list of argument names.
     * Each argument description will have an empty description.
     *
     * @param names the list of argument names
     * @return a list of argument descriptions
     */
    public static List<ArgDesc> doArgNames(List<String> names) {
        List<ArgDesc> out = new ArrayList<>();
        for (String n : names) {
            out.add(new ArgDesc(n));
        }
        return out;
    }
}

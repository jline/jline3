/*
 * Copyright (c) 2002-2021, the original author(s).
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

public class ArgDesc {
    private final String name;
    private final List<AttributedString> description;

    public ArgDesc(String name) {
        this(name, new ArrayList<>());
    }

    public ArgDesc(String name, List<AttributedString> description) {
        if (name.contains("\t") || name.contains(" ")) {
            throw new IllegalArgumentException("Bad argument name: " + name);
        }
        this.name = name;
        this.description = new ArrayList<>(description);
    }

    public String getName() {
        return name;
    }

    public List<AttributedString> getDescription() {
        return description;
    }

    public static List<ArgDesc> doArgNames(List<String> names) {
        List<ArgDesc> out = new ArrayList<>();
        for (String n : names) {
            out.add(new ArgDesc(n));
        }
        return out;
    }
}

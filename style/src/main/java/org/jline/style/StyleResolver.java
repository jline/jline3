/*
 * Copyright (c) 2002-2017, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import org.jline.utils.AttributedStyle;

import static java.util.Objects.requireNonNull;

// TODO: document style specification

/**
 * Resolves named (or source-referenced) {@link AttributedStyle}.
 *
 * @since 3.4
 */
public class StyleResolver extends org.jline.utils.StyleResolver {

    private final StyleSource source;

    private final String group;

    public StyleResolver(final StyleSource source, final String group) {
        super(s -> source.get(group, s));
        this.source = requireNonNull(source);
        this.group = requireNonNull(group);
    }

    public StyleSource getSource() {
        return source;
    }

    // TODO: could consider a small cache to reduce style calculations?

    public String getGroup() {
        return group;
    }
}

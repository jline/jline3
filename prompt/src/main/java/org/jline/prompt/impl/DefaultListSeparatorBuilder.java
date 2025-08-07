/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.ListBuilder;
import org.jline.prompt.ListSeparatorBuilder;
import org.jline.prompt.SeparatorItem;

/**
 * Default implementation of ListSeparatorBuilder.
 */
public class DefaultListSeparatorBuilder implements ListSeparatorBuilder {

    private final DefaultListBuilder parent;
    private String text;

    public DefaultListSeparatorBuilder(DefaultListBuilder parent) {
        this.parent = parent;
        this.text = "";
    }

    public DefaultListSeparatorBuilder(DefaultListBuilder parent, String text) {
        this.parent = parent;
        this.text = text != null ? text : "";
    }

    @Override
    public ListSeparatorBuilder text(String text) {
        this.text = text != null ? text : "";
        return this;
    }

    @Override
    public ListBuilder add() {
        SeparatorItem separator = new DefaultSeparatorItem(text);
        parent.addSeparator(separator);
        return parent;
    }
}

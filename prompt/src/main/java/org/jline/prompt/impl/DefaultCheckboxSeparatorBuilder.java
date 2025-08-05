/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.CheckboxBuilder;
import org.jline.prompt.CheckboxSeparatorBuilder;
import org.jline.prompt.SeparatorItem;

/**
 * Default implementation of CheckboxSeparatorBuilder.
 */
public class DefaultCheckboxSeparatorBuilder implements CheckboxSeparatorBuilder {

    private final DefaultCheckboxBuilder parent;
    private String text;

    public DefaultCheckboxSeparatorBuilder(DefaultCheckboxBuilder parent) {
        this.parent = parent;
        this.text = "";
    }

    public DefaultCheckboxSeparatorBuilder(DefaultCheckboxBuilder parent, String text) {
        this.parent = parent;
        this.text = text != null ? text : "";
    }

    @Override
    public CheckboxSeparatorBuilder text(String text) {
        this.text = text != null ? text : "";
        return this;
    }

    @Override
    public CheckboxBuilder add() {
        SeparatorItem separator = new DefaultSeparatorItem(text);
        parent.addSeparator(separator);
        return parent;
    }
}

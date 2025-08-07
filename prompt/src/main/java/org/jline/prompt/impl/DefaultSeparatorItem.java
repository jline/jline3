/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.SeparatorItem;

/**
 * Default implementation of SeparatorItem interface.
 */
public class DefaultSeparatorItem implements SeparatorItem {

    private final String text;

    public DefaultSeparatorItem(String text) {
        this.text = text != null ? text : "";
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "DefaultSeparatorItem{text='" + text + "'}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DefaultSeparatorItem that = (DefaultSeparatorItem) obj;
        return text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }
}

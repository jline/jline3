/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements.items.impl;

import org.jline.consoleui.elements.items.ListItemIF;

public class ListItem implements ListItemIF {
    String text;
    String name;

    public ListItem(String text, String name) {
        this.text = text;
        if (name == null) {
            this.name = text;
        } else {
            this.name = name;
        }
    }

    public ListItem(String text) {
        this(text, text);
    }

    public ListItem() {
        this(null, null);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getName() {
        return name;
    }

    public boolean isSelectable() {
        return true;
    }
}

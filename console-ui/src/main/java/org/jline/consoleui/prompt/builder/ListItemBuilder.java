/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.items.impl.ListItem;

/**
 * Created by andy on 22.01.16.
 */
public class ListItemBuilder {
    private final ListPromptBuilder listPromptBuilder;
    private String text;
    private String name;

    public ListItemBuilder(ListPromptBuilder listPromptBuilder) {
        this.listPromptBuilder = listPromptBuilder;
    }

    public ListItemBuilder text(String text) {
        this.text = text;
        return this;
    }

    public ListItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ListPromptBuilder add() {
        listPromptBuilder.addItem(new ListItem(text, name));
        return listPromptBuilder;
    }
}

/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt.builder;

import java.util.ArrayList;
import java.util.List;

import org.jline.consoleui.elements.ListChoice;
import org.jline.consoleui.elements.PageSizeType;
import org.jline.consoleui.elements.items.ListItemIF;
import org.jline.consoleui.elements.items.impl.ListItem;

/**
 * Created by andy on 22.01.16.
 */
public class ListPromptBuilder {
    private final PromptBuilder promptBuilder;
    private String name;
    private String message;
    private int pageSize;
    private PageSizeType pageSizeType;
    private final List<ListItemIF> itemList = new ArrayList<>();

    public ListPromptBuilder(PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
        this.pageSize = 10;
        this.pageSizeType = PageSizeType.ABSOLUTE;
    }

    public ListPromptBuilder name(String name) {
        this.name = name;
        if (message != null) {
            this.message = name;
        }
        return this;
    }

    public ListPromptBuilder message(String message) {
        this.message = message;
        if (name == null) {
            name = message;
        }
        return this;
    }

    public ListPromptBuilder pageSize(int absoluteSize) {
        this.pageSize = absoluteSize;
        this.pageSizeType = PageSizeType.ABSOLUTE;
        return this;
    }

    public ListPromptBuilder relativePageSize(int relativePageSize) {
        this.pageSize = relativePageSize;
        this.pageSizeType = PageSizeType.RELATIVE;
        return this;
    }

    public ListItemBuilder newItem() {
        return new ListItemBuilder(this);
    }

    public ListItemBuilder newItem(String name) {
        ListItemBuilder listItemBuilder = new ListItemBuilder(this);
        return listItemBuilder.name(name).text(name);
    }

    public PromptBuilder addPrompt() {
        ListChoice listChoice = new ListChoice(message, name, pageSize, pageSizeType, itemList);
        promptBuilder.addPrompt(listChoice);
        return promptBuilder;
    }

    void addItem(ListItem listItem) {
        this.itemList.add(listItem);
    }
}

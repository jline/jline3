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

import org.jline.consoleui.elements.Checkbox;
import org.jline.consoleui.elements.PageSizeType;
import org.jline.consoleui.elements.items.CheckboxItemIF;

/**
 * Created by andy on 22.01.16.
 */
public class CheckboxPromptBuilder {
    private final PromptBuilder promptBuilder;
    private String name;
    private String message;
    private int pageSize;
    private PageSizeType pageSizeType;
    private final List<CheckboxItemIF> itemList;

    public CheckboxPromptBuilder(PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
        this.pageSize = 10;
        this.pageSizeType = PageSizeType.ABSOLUTE;
        itemList = new ArrayList<>();
    }

    void addItem(CheckboxItemIF checkboxItem) {
        itemList.add(checkboxItem);
    }

    public CheckboxPromptBuilder name(String name) {
        this.name = name;
        if (message == null) {
            message = name;
        }
        return this;
    }

    public CheckboxPromptBuilder message(String message) {
        this.message = message;
        if (name == null) {
            name = message;
        }
        return this;
    }

    public CheckboxPromptBuilder pageSize(int absoluteSize) {
        this.pageSize = absoluteSize;
        this.pageSizeType = PageSizeType.ABSOLUTE;
        return this;
    }

    public CheckboxPromptBuilder relativePageSize(int relativePageSize) {
        this.pageSize = relativePageSize;
        this.pageSizeType = PageSizeType.RELATIVE;
        return this;
    }

    public CheckboxItemBuilder newItem() {
        return new CheckboxItemBuilder(this);
    }

    public CheckboxItemBuilder newItem(String name) {
        CheckboxItemBuilder checkboxItemBuilder = new CheckboxItemBuilder(this);
        return checkboxItemBuilder.name(name);
    }

    public PromptBuilder addPrompt() {
        Checkbox checkbox = new Checkbox(message, name, pageSize, pageSizeType, itemList);
        promptBuilder.addPrompt(checkbox);
        return promptBuilder;
    }

    public CheckboxSeparatorBuilder newSeparator() {
        return new CheckboxSeparatorBuilder(this);
    }

    public CheckboxSeparatorBuilder newSeparator(String text) {
        CheckboxSeparatorBuilder checkboxSeperatorBuilder = new CheckboxSeparatorBuilder(this);
        return checkboxSeperatorBuilder.text(text);
    }
}

/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.items.impl.ChoiceItem;

public class ExpandableChoiceItemBuilder {
    private final ExpandableChoicePromptBuilder choicePromptBuilder;
    private String name;
    private String message;
    private Character key;
    private boolean asDefault;

    public ExpandableChoiceItemBuilder(ExpandableChoicePromptBuilder choicePromptBuilder) {
        this.choicePromptBuilder = choicePromptBuilder;
    }

    public ExpandableChoiceItemBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ExpandableChoiceItemBuilder message(String message) {
        this.message = message;
        return this;
    }

    public ExpandableChoiceItemBuilder key(char key) {
        this.key = key;
        return this;
    }

    public ExpandableChoicePromptBuilder add() {
        ChoiceItem choiceItem = new ChoiceItem(key, name, message, asDefault);
        choicePromptBuilder.addItem(choiceItem);
        return choicePromptBuilder;
    }

    public ExpandableChoiceItemBuilder asDefault() {
        this.asDefault = true;
        return this;
    }
}

/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.items.impl.Separator;

public class ExpandableChoiceSeparatorBuilder {
    private final ExpandableChoicePromptBuilder expandableChoicePromptBuilder;
    private String text;

    public ExpandableChoiceSeparatorBuilder(ExpandableChoicePromptBuilder expandableChoicePromptBuilder) {
        this.expandableChoicePromptBuilder = expandableChoicePromptBuilder;
    }

    public ExpandableChoiceSeparatorBuilder text(String text) {
        this.text = text;
        return this;
    }

    public ExpandableChoicePromptBuilder add() {
        Separator separator = new Separator(text);
        expandableChoicePromptBuilder.addItem(separator);

        return expandableChoicePromptBuilder;
    }
}

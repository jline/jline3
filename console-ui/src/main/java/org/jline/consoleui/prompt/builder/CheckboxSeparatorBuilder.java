/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.items.impl.Separator;

public class CheckboxSeparatorBuilder {
    private final CheckboxPromptBuilder promptBuilder;
    private String text;

    public CheckboxSeparatorBuilder(CheckboxPromptBuilder checkboxPromptBuilder) {
        this.promptBuilder = checkboxPromptBuilder;
    }

    public CheckboxPromptBuilder add() {
        Separator separator = new Separator(text);
        promptBuilder.addItem(separator);

        return promptBuilder;
    }

    public CheckboxSeparatorBuilder text(String text) {
        this.text = text;
        return this;
    }
}

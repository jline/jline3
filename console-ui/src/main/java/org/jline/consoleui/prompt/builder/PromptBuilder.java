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

import org.jline.consoleui.elements.PromptableElementIF;

/**
 * PromptBuilder is the builder class which creates
 */
public class PromptBuilder {
    List<PromptableElementIF> promptList = new ArrayList<>();

    public List<PromptableElementIF> build() {
        return promptList;
    }

    public void addPrompt(PromptableElementIF promptableElement) {
        promptList.add(promptableElement);
    }

    public InputValueBuilder createInputPrompt() {
        return new InputValueBuilder(this);
    }

    public ListPromptBuilder createListPrompt() {
        return new ListPromptBuilder(this);
    }

    public ExpandableChoicePromptBuilder createChoicePrompt() {
        return new ExpandableChoicePromptBuilder(this);
    }

    public CheckboxPromptBuilder createCheckboxPrompt() {
        return new CheckboxPromptBuilder(this);
    }

    public ConfirmPromptBuilder createConfirmPromp() {
        return new ConfirmPromptBuilder(this);
    }

    public TextBuilder createText() {
        return new TextBuilder(this);
    }
}

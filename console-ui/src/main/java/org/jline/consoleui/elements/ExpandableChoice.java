/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.elements;

import java.util.List;

import org.jline.consoleui.elements.items.ChoiceItemIF;

public class ExpandableChoice extends AbstractPromptableElement {

    private final List<ChoiceItemIF> choiceItems;

    public ExpandableChoice(String message, String name, List<ChoiceItemIF> choiceItems) {
        super(message, name);
        this.choiceItems = choiceItems;
    }

    public List<ChoiceItemIF> getChoiceItems() {
        return choiceItems;
    }
}

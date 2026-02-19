/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.items.CheckboxItemIF;
import org.jline.consoleui.elements.items.impl.CheckboxItem;

public class CheckboxItemBuilder {
    private final CheckboxPromptBuilder checkboxPromptBuilder;
    private boolean checked;
    private String name;
    private String text;
    private String disabledText;

    public CheckboxItemBuilder(CheckboxPromptBuilder checkboxPromptBuilder) {
        this.checkboxPromptBuilder = checkboxPromptBuilder;
    }

    public CheckboxItemBuilder name(String name) {
        if (text == null) {
            text = name;
        }
        this.name = name;
        return this;
    }

    public CheckboxItemBuilder text(String text) {
        if (this.name == null) {
            this.name = text;
        }
        this.text = text;
        return this;
    }

    public CheckboxPromptBuilder add() {
        CheckboxItemIF item = new CheckboxItem(checked, text, disabledText, name);
        checkboxPromptBuilder.addItem(item);
        return checkboxPromptBuilder;
    }

    public CheckboxItemBuilder disabledText(String disabledText) {
        this.disabledText = disabledText;
        return this;
    }

    public CheckboxItemBuilder check() {
        this.checked = true;
        return this;
    }

    public CheckboxItemBuilder checked(boolean checked) {
        this.checked = checked;
        return this;
    }
}

/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.List;

import org.jline.prompt.*;

/**
 * Default implementation of NumberBuilder.
 */
public class DefaultNumberBuilder implements NumberBuilder {

    private final PromptBuilder parent;
    private String name;
    private String message;
    private String defaultValue;
    private Double min;
    private Double max;
    private boolean allowDecimals = true;
    private String invalidNumberMessage;
    private String outOfRangeMessage;

    public DefaultNumberBuilder(PromptBuilder parent) {
        this.parent = parent;
    }

    @Override
    public NumberBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public NumberBuilder message(String message) {
        this.message = message;
        return this;
    }

    @Override
    public NumberBuilder defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    @Override
    public NumberBuilder min(Double min) {
        this.min = min;
        return this;
    }

    @Override
    public NumberBuilder max(Double max) {
        this.max = max;
        return this;
    }

    @Override
    public NumberBuilder allowDecimals(boolean allowDecimals) {
        this.allowDecimals = allowDecimals;
        return this;
    }

    @Override
    public NumberBuilder invalidNumberMessage(String message) {
        this.invalidNumberMessage = message;
        return this;
    }

    @Override
    public NumberBuilder outOfRangeMessage(String message) {
        this.outOfRangeMessage = message;
        return this;
    }

    @Override
    public PromptBuilder addPrompt() {
        NumberPrompt prompt = new DefaultNumberPrompt(
                name, message, min, max, allowDecimals, defaultValue, invalidNumberMessage, outOfRangeMessage);
        parent.addPrompt(prompt);
        return parent;
    }

    // Delegate methods from PromptBuilder
    @Override
    public List<Prompt> build() {
        return parent.build();
    }

    @Override
    public void addPrompt(Prompt prompt) {
        parent.addPrompt(prompt);
    }

    @Override
    public InputBuilder createInputPrompt() {
        return parent.createInputPrompt();
    }

    @Override
    public ListBuilder createListPrompt() {
        return parent.createListPrompt();
    }

    @Override
    public ChoiceBuilder createChoicePrompt() {
        return parent.createChoicePrompt();
    }

    @Override
    public CheckboxBuilder createCheckboxPrompt() {
        return parent.createCheckboxPrompt();
    }

    @Override
    public ConfirmBuilder createConfirmPrompt() {
        return parent.createConfirmPrompt();
    }

    @Override
    public TextBuilder createText() {
        return parent.createText();
    }

    @Override
    public PasswordBuilder createPasswordPrompt() {
        return parent.createPasswordPrompt();
    }

    @Override
    public NumberBuilder createNumberPrompt() {
        return parent.createNumberPrompt();
    }

    @Override
    public <T> SearchBuilder<T> createSearchPrompt() {
        return parent.createSearchPrompt();
    }

    @Override
    public EditorBuilder createEditorPrompt() {
        return parent.createEditorPrompt();
    }
}

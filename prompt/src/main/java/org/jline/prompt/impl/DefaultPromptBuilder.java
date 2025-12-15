/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.ArrayList;
import java.util.List;

import org.jline.prompt.*;

/**
 * Default implementation of the PromptBuilder interface.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultPromptBuilder implements PromptBuilder {

    private final List<Prompt> prompts = new ArrayList<>();

    /**
     * Create a new DefaultPromptBuilder.
     */
    public DefaultPromptBuilder() {
        // Native implementation - no delegate needed
    }

    @Override
    public List<Prompt> build() {
        return new ArrayList<>(prompts);
    }

    @Override
    public void addPrompt(Prompt prompt) {
        prompts.add(prompt);
    }

    @Override
    public InputBuilder createInputPrompt() {
        return new DefaultInputBuilder(this);
    }

    @Override
    public ListBuilder createListPrompt() {
        return new DefaultListBuilder(this);
    }

    @Override
    public ChoiceBuilder createChoicePrompt() {
        return new DefaultChoiceBuilder(this);
    }

    @Override
    public CheckboxBuilder createCheckboxPrompt() {
        return new DefaultCheckboxBuilder(this);
    }

    @Override
    public ConfirmBuilder createConfirmPrompt() {
        return new DefaultConfirmBuilder(this);
    }

    @Override
    public TextBuilder createText() {
        return new DefaultTextBuilder(this);
    }

    @Override
    public PasswordBuilder createPasswordPrompt() {
        return new DefaultPasswordBuilder(this);
    }

    @Override
    public NumberBuilder createNumberPrompt() {
        return new DefaultNumberBuilder(this);
    }

    @Override
    public <T> SearchBuilder<T> createSearchPrompt() {
        return new DefaultSearchBuilder<>(this);
    }

    @Override
    public EditorBuilder createEditorPrompt() {
        return new DefaultEditorBuilder(this);
    }
}

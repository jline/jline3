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
 * Default implementation of EditorBuilder.
 */
public class DefaultEditorBuilder implements EditorBuilder {

    private final PromptBuilder parent;
    private String name;
    private String message;
    private String initialText;
    private String fileExtension = "txt";
    private String title;
    private boolean showLineNumbers = false;
    private boolean enableWrapping = false;

    public DefaultEditorBuilder(PromptBuilder parent) {
        this.parent = parent;
    }

    @Override
    public EditorBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public EditorBuilder message(String message) {
        this.message = message;
        return this;
    }

    @Override
    public EditorBuilder initialText(String initialText) {
        this.initialText = initialText;
        return this;
    }

    @Override
    public EditorBuilder fileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
        return this;
    }

    @Override
    public EditorBuilder title(String title) {
        this.title = title;
        return this;
    }

    @Override
    public EditorBuilder showLineNumbers(boolean showLineNumbers) {
        this.showLineNumbers = showLineNumbers;
        return this;
    }

    @Override
    public EditorBuilder enableWrapping(boolean enableWrapping) {
        this.enableWrapping = enableWrapping;
        return this;
    }

    @Override
    public PromptBuilder addPrompt() {
        EditorPrompt prompt = new DefaultEditorPrompt(
                name, message, initialText, fileExtension, title, showLineNumbers, enableWrapping);
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

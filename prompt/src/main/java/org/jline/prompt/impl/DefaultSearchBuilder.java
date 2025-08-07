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
import java.util.function.Function;

import org.jline.prompt.*;

/**
 * Default implementation of SearchBuilder.
 */
public class DefaultSearchBuilder<T> implements SearchBuilder<T> {

    private final PromptBuilder parent;
    private String name;
    private String message;
    private Function<String, List<T>> searchFunction;
    private Function<T, String> displayFunction;
    private Function<T, String> valueFunction;
    private String placeholder = "Type to search...";
    private int minSearchLength = 0;
    private int maxResults = 10;

    public DefaultSearchBuilder(PromptBuilder parent) {
        this.parent = parent;
    }

    @Override
    public SearchBuilder<T> name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public SearchBuilder<T> message(String message) {
        this.message = message;
        return this;
    }

    @Override
    public SearchBuilder<T> searchFunction(Function<String, List<T>> searchFunction) {
        this.searchFunction = searchFunction;
        return this;
    }

    @Override
    public SearchBuilder<T> displayFunction(Function<T, String> displayFunction) {
        this.displayFunction = displayFunction;
        return this;
    }

    @Override
    public SearchBuilder<T> valueFunction(Function<T, String> valueFunction) {
        this.valueFunction = valueFunction;
        return this;
    }

    @Override
    public SearchBuilder<T> placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Override
    public SearchBuilder<T> minSearchLength(int minSearchLength) {
        this.minSearchLength = minSearchLength;
        return this;
    }

    @Override
    public SearchBuilder<T> maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    @Override
    public PromptBuilder addPrompt() {
        SearchPrompt<T> prompt = new DefaultSearchPrompt<>(
                name,
                message,
                searchFunction,
                displayFunction,
                valueFunction,
                placeholder,
                minSearchLength,
                maxResults);
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
    public <U> SearchBuilder<U> createSearchPrompt() {
        return parent.createSearchPrompt();
    }

    @Override
    public EditorBuilder createEditorPrompt() {
        return parent.createEditorPrompt();
    }
}

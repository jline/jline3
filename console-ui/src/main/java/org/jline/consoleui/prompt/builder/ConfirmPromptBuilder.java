/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.ConfirmChoice;

public class ConfirmPromptBuilder {
    private final PromptBuilder promptBuilder;
    private String name;
    private String message;
    private ConfirmChoice.ConfirmationValue defaultConfirmationValue;

    public ConfirmPromptBuilder(PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    public ConfirmPromptBuilder name(String name) {
        this.name = name;
        if (message == null) {
            message = name;
        }
        return this;
    }

    public ConfirmPromptBuilder message(String message) {
        this.message = message;
        if (name == null) {
            name = message;
        }
        return this;
    }

    public ConfirmPromptBuilder defaultValue(ConfirmChoice.ConfirmationValue confirmationValue) {
        this.defaultConfirmationValue = confirmationValue;
        return this;
    }

    public PromptBuilder addPrompt() {
        promptBuilder.addPrompt(new ConfirmChoice(message, name, defaultConfirmationValue));
        return promptBuilder;
    }
}

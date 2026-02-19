/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.InputValue;
import org.jline.reader.Completer;

public class InputValueBuilder {
    private final PromptBuilder promptBuilder;
    private String name;
    private String defaultValue;
    private String message;
    private Character mask;
    private Completer completer;

    public InputValueBuilder(PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    public InputValueBuilder name(String name) {
        this.name = name;
        return this;
    }

    public InputValueBuilder defaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public InputValueBuilder message(String message) {
        this.message = message;
        return this;
    }

    public InputValueBuilder mask(char mask) {
        this.mask = mask;
        return this;
    }

    public PromptBuilder addPrompt() {
        InputValue inputValue = new InputValue(name, message, null, defaultValue);
        if (mask != null) {
            inputValue.setMask(mask);
        }
        if (completer != null) {
            inputValue.setCompleter(completer);
        }
        promptBuilder.addPrompt(inputValue);
        return promptBuilder;
    }

    public InputValueBuilder addCompleter(Completer completer) {
        this.completer = completer;
        return this;
    }
}

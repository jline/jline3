/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.InputPrompt;
import org.jline.prompt.InputResult;

/**
 * Implementation of InputResult.
 */
public class DefaultInputResult extends AbstractPromptResult<InputPrompt> implements InputResult {

    private final String input;
    private final String displayInput;

    /**
     * Create a new DefaultInputResult with the given input, display input, and item.
     *
     * @param input the input text
     * @param displayInput the display input text (e.g., masked for passwords)
     * @param item the associated PromptItem, or null if none
     */
    public DefaultInputResult(String input, String displayInput, InputPrompt prompt) {
        super(prompt);
        this.input = input;
        this.displayInput = displayInput;
    }

    @Override
    public String getInput() {
        return input;
    }

    @Override
    public String getResult() {
        return input;
    }

    @Override
    public String getDisplayResult() {
        return displayInput;
    }

    @Override
    public String toString() {
        return "InputResult{input='" + input + "'}";
    }
}

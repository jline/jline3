/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

public class InputResult implements PromptResultItemIF {
    private final String input;
    private final String displayInput;

    public InputResult(String input, String displayInput) {
        this.input = input;
        this.displayInput = displayInput;
    }

    public String getDisplayResult() {
        return displayInput;
    }

    public String getResult() {
        return input;
    }

    @Override
    public String toString() {
        return "InputResult{" + "input='" + input + '\'' + '}';
    }
}

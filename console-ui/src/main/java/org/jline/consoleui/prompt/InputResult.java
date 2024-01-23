/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

/**
 *
 * User: Andreas Wegmann
 * Date: 03.02.16
 */
public class InputResult implements PromptResultItemIF {
    private final String input;

    public InputResult(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }

    public String getResult() {
        return input;
    }

    @Override
    public String toString() {
        return "InputResult{" + "input='" + input + '\'' + '}';
    }
}

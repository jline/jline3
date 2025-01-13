/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

public class NoResult implements PromptResultItemIF {

    public static final NoResult INSTANCE = new NoResult();

    private NoResult() {}

    public String getDisplayResult() {
        return "";
    }

    public String getResult() {
        return "";
    }

    @Override
    public String toString() {
        return "NoResult{}";
    }
}

/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import org.jline.prompt.EditorPrompt;
import org.jline.prompt.EditorResult;

/**
 * Implementation of EditorResult.
 */
public class DefaultEditorResult extends AbstractPromptResult<EditorPrompt> implements EditorResult {

    private final String text;

    /**
     * Create a new DefaultEditorResult with the given text.
     *
     * @param text the edited text content
     * @param prompt the associated EditorPrompt
     */
    public DefaultEditorResult(String text, EditorPrompt prompt) {
        super(prompt);
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getResult() {
        return text;
    }

    @Override
    public String getDisplayResult() {
        return text != null && text.length() > 50 ? text.substring(0, 47) + "..." : text;
    }

    @Override
    public String toString() {
        return "EditorResult{text='" + (text != null && text.length() > 50 ? text.substring(0, 47) + "..." : text)
                + "'}";
    }
}

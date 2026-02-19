/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

/**
 * Result of an editor prompt. Contains the edited text content.
 */
public interface EditorResult extends PromptResult<EditorPrompt> {

    /**
     * Get the edited text content.
     *
     * @return the edited text
     */
    String getText();
}

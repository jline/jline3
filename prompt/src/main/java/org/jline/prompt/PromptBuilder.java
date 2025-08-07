/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.List;

/**
 * Interface for building prompts in the Prompter.
 */
public interface PromptBuilder {

    /**
     * Build the list of prompts.
     *
     * @return the list of prompts
     */
    List<Prompt> build();

    /**
     * Add a prompt to the builder.
     *
     * @param prompt the prompt to add
     */
    void addPrompt(Prompt prompt);

    /**
     * Create a builder for input prompts.
     *
     * @return an input prompt builder
     */
    InputBuilder createInputPrompt();

    /**
     * Create a builder for list prompts.
     *
     * @return a list prompt builder
     */
    ListBuilder createListPrompt();

    /**
     * Create a builder for choice prompts.
     *
     * @return a choice prompt builder
     */
    ChoiceBuilder createChoicePrompt();

    /**
     * Create a builder for checkbox prompts.
     *
     * @return a checkbox prompt builder
     */
    CheckboxBuilder createCheckboxPrompt();

    /**
     * Create a builder for confirmation prompts.
     *
     * @return a confirmation prompt builder
     */
    ConfirmBuilder createConfirmPrompt();

    /**
     * Create a builder for text displays.
     *
     * @return a text builder
     */
    TextBuilder createText();

    /**
     * Create a builder for password prompts.
     *
     * @return a password prompt builder
     */
    PasswordBuilder createPasswordPrompt();

    /**
     * Create a builder for number prompts.
     *
     * @return a number prompt builder
     */
    NumberBuilder createNumberPrompt();

    /**
     * Create a builder for search prompts.
     *
     * @return a search prompt builder
     */
    <T> SearchBuilder<T> createSearchPrompt();

    /**
     * Create a builder for editor prompts.
     *
     * @return an editor prompt builder
     */
    EditorBuilder createEditorPrompt();
}

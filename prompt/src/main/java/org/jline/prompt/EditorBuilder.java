/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

/**
 * Builder interface for editor prompts.
 */
public interface EditorBuilder extends PromptBuilder {

    /**
     * Set the name of the prompt.
     *
     * @param name the name
     * @return this builder
     */
    EditorBuilder name(String name);

    /**
     * Set the message to display.
     *
     * @param message the message
     * @return this builder
     */
    EditorBuilder message(String message);

    /**
     * Set the initial text for the editor.
     *
     * @param initialText the initial text
     * @return this builder
     */
    EditorBuilder initialText(String initialText);

    /**
     * Set the file extension for syntax highlighting.
     *
     * @param fileExtension the file extension (without dot)
     * @return this builder
     */
    EditorBuilder fileExtension(String fileExtension);

    /**
     * Set the title for the editor.
     *
     * @param title the editor title
     * @return this builder
     */
    EditorBuilder title(String title);

    /**
     * Set whether to show line numbers.
     *
     * @param showLineNumbers true to show line numbers
     * @return this builder
     */
    EditorBuilder showLineNumbers(boolean showLineNumbers);

    /**
     * Set whether to enable text wrapping.
     *
     * @param enableWrapping true to enable wrapping
     * @return this builder
     */
    EditorBuilder enableWrapping(boolean enableWrapping);

    /**
     * Add the editor prompt to the builder.
     *
     * @return the parent builder
     */
    PromptBuilder addPrompt();
}

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
 * Interface for editor prompts.
 * An editor prompt opens an external editor for multi-line text input.
 */
public interface EditorPrompt extends Prompt {

    /**
     * Get the initial text to populate the editor with.
     *
     * @return the initial text, or null for empty
     */
    default String getInitialText() {
        return null;
    }

    /**
     * Get the file extension to use for the temporary file.
     * This can affect syntax highlighting in the editor.
     *
     * @return the file extension (without dot), or null for .txt
     */
    default String getFileExtension() {
        return "txt";
    }

    /**
     * Get the title to display in the editor.
     *
     * @return the editor title, or null for default
     */
    default String getTitle() {
        return null;
    }

    /**
     * Whether to show line numbers in the editor.
     *
     * @return true to show line numbers, false to hide
     */
    default boolean showLineNumbers() {
        return false;
    }

    /**
     * Whether to enable text wrapping in the editor.
     *
     * @return true to enable wrapping, false to disable
     */
    default boolean enableWrapping() {
        return false;
    }
}

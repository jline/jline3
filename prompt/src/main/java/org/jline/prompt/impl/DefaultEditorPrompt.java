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

/**
 * Default implementation of EditorPrompt interface.
 */
public class DefaultEditorPrompt extends DefaultPrompt implements EditorPrompt {

    private final String initialText;
    private final String fileExtension;
    private final String title;
    private final boolean showLineNumbers;
    private final boolean enableWrapping;

    public DefaultEditorPrompt(String name, String message) {
        this(name, message, null, "txt", null, false, false);
    }

    public DefaultEditorPrompt(String name, String message, String initialText) {
        this(name, message, initialText, "txt", null, false, false);
    }

    public DefaultEditorPrompt(
            String name,
            String message,
            String initialText,
            String fileExtension,
            String title,
            boolean showLineNumbers,
            boolean enableWrapping) {
        super(name, message);
        this.initialText = initialText;
        this.fileExtension = fileExtension;
        this.title = title;
        this.showLineNumbers = showLineNumbers;
        this.enableWrapping = enableWrapping;
    }

    @Override
    public String getInitialText() {
        return initialText;
    }

    @Override
    public String getFileExtension() {
        return fileExtension;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean showLineNumbers() {
        return showLineNumbers;
    }

    @Override
    public boolean enableWrapping() {
        return enableWrapping;
    }
}

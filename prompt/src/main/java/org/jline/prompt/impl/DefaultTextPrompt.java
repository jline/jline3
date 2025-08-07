/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt.impl;

import java.util.ArrayList;
import java.util.List;

import org.jline.prompt.TextPrompt;
import org.jline.utils.AttributedString;

/**
 * Default implementation of TextPrompt interface.
 * This is now the native implementation that doesn't depend on console-ui.
 */
public class DefaultTextPrompt extends DefaultPrompt implements TextPrompt {

    private final String text;
    private final List<AttributedString> lines;

    public DefaultTextPrompt(String name, String message, String text) {
        super(name, message);
        this.text = text;
        this.lines = new ArrayList<>();
        if (text != null) {
            for (String line : text.split("\n")) {
                this.lines.add(new AttributedString(line));
            }
        }
    }

    public DefaultTextPrompt(String name, String message, List<AttributedString> lines) {
        super(name, message);
        this.lines = new ArrayList<>(lines);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(lines.get(i).toString());
        }
        this.text = sb.toString();
    }

    @Override
    public String getText() {
        return text;
    }

    public List<AttributedString> getLines() {
        return new ArrayList<>(lines);
    }
}

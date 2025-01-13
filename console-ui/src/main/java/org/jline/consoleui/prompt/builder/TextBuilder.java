/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jline.consoleui.elements.Text;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class TextBuilder {
    private final PromptBuilder promptBuilder;
    private final List<AttributedString> lines = new ArrayList<>();

    public TextBuilder(PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    public TextBuilder addLine(AttributedString text) {
        lines.add(text);
        return this;
    }

    public TextBuilder addLine(String line) {
        lines.add(new AttributedString(line));
        return this;
    }

    public TextBuilder addLine(AttributedStyle style, String line) {
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.append(line, style);
        lines.add(asb.toAttributedString());
        return this;
    }

    public TextBuilder addLines(AttributedString... lines) {
        this.lines.addAll(Arrays.asList(lines));
        return this;
    }

    public TextBuilder addLines(String... lines) {
        for (String s : lines) {
            this.lines.add(new AttributedString(s));
        }
        return this;
    }

    public TextBuilder addLines(AttributedStyle style, String... lines) {
        for (String s : lines) {
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.append(s, style);
            this.lines.add(asb.toAttributedString());
        }
        return this;
    }

    public PromptBuilder addPrompt() {
        Text text = new Text(lines);
        promptBuilder.addPrompt(text);
        return promptBuilder;
    }
}

/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.ui;

import org.jline.components.Canvas;
import org.jline.components.layout.Size;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Horizontal line separator with optional centered title.
 */
public class Separator extends AbstractComponent {

    private final char character;
    private final AttributedStyle style;
    private final String title;

    private Separator(Builder builder) {
        this.character = builder.character;
        this.style = builder.style;
        this.title = builder.title;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Size getPreferredSize() {
        if (title != null && !title.isEmpty()) {
            return new Size(title.length() + 4, 1); // space-title-space + some line chars
        }
        return new Size(1, 1);
    }

    @Override
    public void render(Canvas canvas, int width, int height) {
        markClean();
        if (width <= 0 || height <= 0) return;

        if (title != null && !title.isEmpty() && width > title.length() + 4) {
            // Draw line with centered title
            int titleLen = title.length() + 2; // space around title
            int leftLen = (width - titleLen) / 2;
            int rightLen = width - leftLen - titleLen;

            for (int c = 0; c < leftLen; c++) {
                canvas.put(c, 0, character, style);
            }
            canvas.put(leftLen, 0, ' ', style);
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.style(style);
            sb.append(title);
            canvas.text(leftLen + 1, 0, sb.toAttributedString());
            canvas.put(leftLen + 1 + title.length(), 0, ' ', style);
            for (int c = leftLen + titleLen; c < width; c++) {
                canvas.put(c, 0, character, style);
            }
        } else {
            // Full line
            for (int c = 0; c < width; c++) {
                canvas.put(c, 0, character, style);
            }
        }
    }

    public static class Builder {
        private char character = '\u2500'; // ─
        private AttributedStyle style = AttributedStyle.DEFAULT;
        private String title;

        Builder() {}

        public Builder character(char character) {
            this.character = character;
            return this;
        }

        public Builder style(AttributedStyle style) {
            this.style = style;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Separator build() {
            return new Separator(this);
        }
    }
}

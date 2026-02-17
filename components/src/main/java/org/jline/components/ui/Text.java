/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.ui;

import java.util.ArrayList;
import java.util.List;

import org.jline.components.Canvas;
import org.jline.components.layout.FlexAlign;
import org.jline.components.layout.Size;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Styled text component with optional word wrapping.
 */
public class Text extends AbstractComponent {

    private String text;
    private AttributedStyle style;
    private boolean wrap;
    private int maxWidth;
    private FlexAlign alignment;

    private Text(Builder builder) {
        this.text = builder.text;
        this.style = builder.style;
        this.wrap = builder.wrap;
        this.maxWidth = builder.maxWidth;
        this.alignment = builder.alignment;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setText(String text) {
        if (!java.util.Objects.equals(this.text, text)) {
            this.text = text;
            invalidate();
        }
    }

    public String getText() {
        return text;
    }

    @Override
    public Size getPreferredSize() {
        if (text == null || text.isEmpty()) {
            return Size.ZERO;
        }
        if (!wrap) {
            String[] lines = text.split("\n", -1);
            int w = 0;
            for (String line : lines) {
                w = Math.max(w, line.length());
            }
            return new Size(maxWidth > 0 ? Math.min(w, maxWidth) : w, lines.length);
        }
        if (maxWidth <= 0) {
            // Wrapping enabled but no maxWidth: use text length as width,
            // render() will re-wrap at the allocated width
            String[] lines = text.split("\n", -1);
            int w = 0;
            for (String line : lines) {
                w = Math.max(w, line.length());
            }
            return new Size(w, lines.length);
        }
        List<String> wrapped = wrapText(text, maxWidth);
        int w = 0;
        for (String line : wrapped) {
            w = Math.max(w, line.length());
        }
        return new Size(w, wrapped.size());
    }

    @Override
    public void render(Canvas canvas, int width, int height) {
        markClean();
        if (text == null || text.isEmpty() || width <= 0 || height <= 0) {
            return;
        }

        List<String> lines;
        if (wrap) {
            lines = wrapText(text, width);
        } else {
            String[] split = text.split("\n", -1);
            lines = new ArrayList<>(split.length);
            for (String s : split) {
                lines.add(s);
            }
        }

        for (int i = 0; i < lines.size() && i < height; i++) {
            String line = lines.get(i);
            if (line.length() > width) {
                line = line.substring(0, width);
            }

            int x = 0;
            if (alignment == FlexAlign.CENTER) {
                x = (width - line.length()) / 2;
            } else if (alignment == FlexAlign.END) {
                x = width - line.length();
            }

            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.style(style);
            sb.append(line);
            canvas.text(x, i, sb.toAttributedString());
        }
    }

    private static List<String> wrapText(String text, int width) {
        List<String> result = new ArrayList<>();
        if (width <= 0) {
            result.add(text);
            return result;
        }
        String[] paragraphs = text.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }
            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                if (line.length() == 0) {
                    if (word.length() > width) {
                        // Break long word
                        for (int i = 0; i < word.length(); i += width) {
                            result.add(word.substring(i, Math.min(i + width, word.length())));
                        }
                    } else {
                        line.append(word);
                    }
                } else if (line.length() + 1 + word.length() <= width) {
                    line.append(' ').append(word);
                } else {
                    result.add(line.toString());
                    line = new StringBuilder();
                    if (word.length() > width) {
                        for (int i = 0; i < word.length(); i += width) {
                            result.add(word.substring(i, Math.min(i + width, word.length())));
                        }
                    } else {
                        line.append(word);
                    }
                }
            }
            if (line.length() > 0) {
                result.add(line.toString());
            }
        }
        return result;
    }

    public static class Builder {
        private String text = "";
        private AttributedStyle style = AttributedStyle.DEFAULT;
        private boolean wrap = false;
        private int maxWidth = 0;
        private FlexAlign alignment = FlexAlign.START;

        Builder() {}

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder style(AttributedStyle style) {
            this.style = style;
            return this;
        }

        public Builder wrap(boolean wrap) {
            this.wrap = wrap;
            return this;
        }

        public Builder maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        public Builder alignment(FlexAlign alignment) {
            this.alignment = alignment;
            return this;
        }

        public Text build() {
            return new Text(this);
        }
    }
}

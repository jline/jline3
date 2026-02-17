/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.ui;

import java.util.Collections;
import java.util.List;

import org.jline.components.Canvas;
import org.jline.components.Component;
import org.jline.components.layout.Size;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Hyperlink component that renders as styled text.
 *
 * <p>Renders the link text with configurable styling (default: blue underline).
 * The URL is stored for programmatic access but is not embedded as an OSC 8
 * terminal hyperlink since {@link org.jline.utils.AttributedString} does not
 * currently support raw escape sequence embedding.</p>
 */
public class Hyperlink extends AbstractComponent {

    private final String url;
    private final String text;
    private final Component inner;
    private final AttributedStyle style;

    private Hyperlink(Builder builder) {
        this.url = builder.url;
        this.text = builder.text;
        this.inner = builder.inner;
        this.style = builder.style;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getUrl() {
        return url;
    }

    @Override
    public List<Component> getChildren() {
        return inner != null ? Collections.singletonList(inner) : Collections.emptyList();
    }

    @Override
    public boolean isDirty() {
        if (super.isDirty()) return true;
        return inner != null && inner.isDirty();
    }

    @Override
    public Size getPreferredSize() {
        if (inner != null) {
            return inner.getPreferredSize();
        }
        return new Size(text != null ? text.length() : 0, 1);
    }

    @Override
    public void render(Canvas canvas, int width, int height) {
        markClean();
        if (width <= 0 || height <= 0) return;

        if (inner != null) {
            inner.render(canvas, width, height);
        } else if (text != null) {
            AttributedStringBuilder sb = new AttributedStringBuilder();
            sb.style(style);
            int len = Math.min(text.length(), width);
            sb.append(text, 0, len);
            canvas.text(0, 0, sb.toAttributedString());
        }
    }

    public static class Builder {
        private String url = "";
        private String text;
        private Component inner;
        private AttributedStyle style =
                AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE).underline();

        Builder() {}

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            this.inner = null;
            return this;
        }

        public Builder inner(Component inner) {
            this.inner = inner;
            this.text = null;
            return this;
        }

        public Builder style(AttributedStyle style) {
            this.style = style;
            return this;
        }

        public Hyperlink build() {
            return new Hyperlink(this);
        }
    }
}

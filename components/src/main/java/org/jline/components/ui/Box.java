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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jline.components.Canvas;
import org.jline.components.Component;
import org.jline.components.layout.FlexAlign;
import org.jline.components.layout.FlexDirection;
import org.jline.components.layout.FlexJustify;
import org.jline.components.layout.Insets;
import org.jline.components.layout.LayoutEngine;
import org.jline.components.layout.Size;
import org.jline.utils.AttributedStyle;

/**
 * Flexbox container component with optional border.
 */
public class Box extends AbstractComponent {

    private final FlexDirection direction;
    private final FlexJustify justify;
    private final FlexAlign align;
    private final int gap;
    private final Insets padding;
    private final BorderStyle borderStyle;
    private final AttributedStyle borderColor;
    private final List<Component> children;

    private Box(Builder builder) {
        this.direction = builder.direction;
        this.justify = builder.justify;
        this.align = builder.align;
        this.gap = builder.gap;
        this.padding = builder.padding;
        this.borderStyle = builder.borderStyle;
        this.borderColor = builder.borderColor;
        this.children = Collections.unmodifiableList(new ArrayList<>(builder.children));
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Component> getChildren() {
        return children;
    }

    @Override
    public boolean isDirty() {
        if (super.isDirty()) return true;
        for (Component child : children) {
            if (child.isDirty()) return true;
        }
        return false;
    }

    @Override
    public Size getPreferredSize() {
        boolean horizontal = direction == FlexDirection.ROW;
        int borderExtra = borderStyle != BorderStyle.NONE ? 2 : 0;

        int mainSize = 0;
        int crossSize = 0;
        for (int i = 0; i < children.size(); i++) {
            Size pref = children.get(i).getPreferredSize();
            int childMain = horizontal ? pref.width() : pref.height();
            int childCross = horizontal ? pref.height() : pref.width();
            mainSize += childMain;
            if (i > 0) mainSize += gap;
            crossSize = Math.max(crossSize, childCross);
        }

        int w, h;
        if (horizontal) {
            w = mainSize + padding.horizontal() + borderExtra;
            h = crossSize + padding.vertical() + borderExtra;
        } else {
            w = crossSize + padding.horizontal() + borderExtra;
            h = mainSize + padding.vertical() + borderExtra;
        }
        return new Size(w, h);
    }

    @Override
    public void render(Canvas canvas, int width, int height) {
        if (width <= 0 || height <= 0) {
            markClean();
            return;
        }

        int borderWidth = borderStyle != BorderStyle.NONE ? 1 : 0;

        // Draw border
        if (borderStyle != BorderStyle.NONE) {
            drawBorder(canvas, width, height);
        }

        // Layout children in the inner area (inside border and padding)
        int innerX = borderWidth;
        int innerY = borderWidth;
        int innerW = width - 2 * borderWidth;
        int innerH = height - 2 * borderWidth;

        if (innerW > 0 && innerH > 0) {
            Canvas innerCanvas = canvas.subRegion(innerX, innerY, innerW, innerH);
            LayoutEngine.layout(innerCanvas, children, innerW, innerH, direction, justify, align, gap, padding);
        }

        markClean();
    }

    private void drawBorder(Canvas canvas, int width, int height) {
        char[] bc = borderStyle.chars();
        AttributedStyle style = borderColor;

        // Top row
        canvas.put(0, 0, bc[0], style); // top-left
        for (int c = 1; c < width - 1; c++) {
            canvas.put(c, 0, bc[1], style); // horizontal
        }
        if (width > 1) canvas.put(width - 1, 0, bc[2], style); // top-right

        // Sides
        for (int r = 1; r < height - 1; r++) {
            canvas.put(0, r, bc[3], style); // left
            if (width > 1) canvas.put(width - 1, r, bc[3], style); // right
        }

        // Bottom row
        if (height > 1) {
            canvas.put(0, height - 1, bc[4], style); // bottom-left
            for (int c = 1; c < width - 1; c++) {
                canvas.put(c, height - 1, bc[1], style); // horizontal
            }
            if (width > 1) canvas.put(width - 1, height - 1, bc[5], style); // bottom-right
        }
    }

    /**
     * Border styles for boxes.
     */
    public enum BorderStyle {
        NONE(new char[] {}),
        SINGLE(new char[] {'\u250c', '\u2500', '\u2510', '\u2502', '\u2514', '\u2518'}),
        DOUBLE(new char[] {'\u2554', '\u2550', '\u2557', '\u2551', '\u255a', '\u255d'}),
        ROUNDED(new char[] {'\u256d', '\u2500', '\u256e', '\u2502', '\u2570', '\u256f'});

        // chars: [topLeft, horizontal, topRight, vertical, bottomLeft, bottomRight]
        private final char[] borderChars;

        BorderStyle(char[] borderChars) {
            this.borderChars = borderChars;
        }

        char[] chars() {
            return borderChars;
        }
    }

    public static class Builder {
        private FlexDirection direction = FlexDirection.COLUMN;
        private FlexJustify justify = FlexJustify.START;
        private FlexAlign align = FlexAlign.STRETCH;
        private int gap = 0;
        private Insets padding = Insets.NONE;
        private BorderStyle borderStyle = BorderStyle.NONE;
        private AttributedStyle borderColor = AttributedStyle.DEFAULT;
        private final List<Component> children = new ArrayList<>();

        Builder() {}

        public Builder direction(FlexDirection direction) {
            this.direction = direction;
            return this;
        }

        public Builder justify(FlexJustify justify) {
            this.justify = justify;
            return this;
        }

        public Builder align(FlexAlign align) {
            this.align = align;
            return this;
        }

        public Builder gap(int gap) {
            this.gap = gap;
            return this;
        }

        public Builder padding(Insets padding) {
            this.padding = padding;
            return this;
        }

        public Builder padding(int all) {
            this.padding = Insets.of(all);
            return this;
        }

        public Builder borderStyle(BorderStyle borderStyle) {
            this.borderStyle = borderStyle;
            return this;
        }

        public Builder borderColor(AttributedStyle borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        public Builder children(Component... children) {
            this.children.addAll(Arrays.asList(children));
            return this;
        }

        public Builder child(Component child) {
            this.children.add(child);
            return this;
        }

        public Box build() {
            return new Box(this);
        }
    }
}

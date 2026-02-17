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
import org.jline.utils.AttributedStyle;

/**
 * Progress bar component showing a fill percentage.
 *
 * <p>Uses true-color (24-bit RGB) for clean rendering. By default renders
 * as a thin bar using the upper one-eighth block character (▔) with solid
 * colors, matching the {@link IndeterminateProgressBar} style.</p>
 */
public class ProgressBar extends AbstractComponent {

    private double progress;
    private final int preferredWidth;
    private final char filledChar;
    private final char emptyChar;
    private final boolean showPercentage;
    private final int[] filledColorStart; // RGB
    private final int[] filledColorEnd; // RGB
    private final int[] emptyColor; // RGB, null means default
    private final AttributedStyle percentageStyle;

    private ProgressBar(Builder builder) {
        this.progress = builder.progress;
        this.preferredWidth = builder.width;
        this.filledChar = builder.filledChar;
        this.emptyChar = builder.emptyChar;
        this.showPercentage = builder.showPercentage;
        this.filledColorStart = builder.filledColorStart;
        this.filledColorEnd = builder.filledColorEnd;
        this.emptyColor = builder.emptyColor;
        this.percentageStyle = builder.percentageStyle;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setProgress(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        if (this.progress != clamped) {
            this.progress = clamped;
            invalidate();
        }
    }

    public double getProgress() {
        return progress;
    }

    @Override
    public Size getPreferredSize() {
        int w = preferredWidth;
        if (showPercentage) {
            w += 5; // " 100%"
        }
        return new Size(w, 1);
    }

    @Override
    public void render(Canvas canvas, int width, int height) {
        markClean();
        if (width <= 0 || height <= 0) return;

        String pctText = showPercentage ? String.format(" %3d%%", (int) (progress * 100)) : "";
        int barWidth;
        if (showPercentage && width > pctText.length()) {
            barWidth = width - pctText.length();
        } else {
            barWidth = width;
            pctText = ""; // no room for percentage
        }

        int filled = (int) (barWidth * progress);

        // Render filled portion with gradient
        for (int i = 0; i < filled && i < barWidth; i++) {
            float t = filled > 1 ? (float) i / (filled - 1) : 0;
            int r = clamp((int) (filledColorStart[0] + t * (filledColorEnd[0] - filledColorStart[0])));
            int g = clamp((int) (filledColorStart[1] + t * (filledColorEnd[1] - filledColorStart[1])));
            int b = clamp((int) (filledColorStart[2] + t * (filledColorEnd[2] - filledColorStart[2])));
            canvas.put(i, 0, filledChar, AttributedStyle.DEFAULT.foreground(r, g, b));
        }

        // Render empty portion
        AttributedStyle emptyStyle = emptyColor != null
                ? AttributedStyle.DEFAULT.foreground(emptyColor[0], emptyColor[1], emptyColor[2])
                : AttributedStyle.DEFAULT;
        for (int i = filled; i < barWidth; i++) {
            canvas.put(i, 0, emptyChar, emptyStyle);
        }

        // Render percentage text
        if (!pctText.isEmpty()) {
            for (int i = 0; i < pctText.length() && barWidth + i < width; i++) {
                canvas.put(barWidth + i, 0, pctText.charAt(i), percentageStyle);
            }
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    public static class Builder {
        private double progress = 0.0;
        private int width = 20;
        private char filledChar = '\u2588'; // █
        private char emptyChar = '\u2591'; // ░
        private boolean showPercentage = true;
        private int[] filledColorStart = {59, 130, 246}; // blue-500
        private int[] filledColorEnd = {147, 197, 253}; // blue-300
        private int[] emptyColor = {51, 65, 85}; // slate-700
        private AttributedStyle percentageStyle = AttributedStyle.DEFAULT;

        Builder() {}

        public Builder progress(double progress) {
            this.progress = Math.max(0.0, Math.min(1.0, progress));
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder filledChar(char ch) {
            this.filledChar = ch;
            return this;
        }

        public Builder emptyChar(char ch) {
            this.emptyChar = ch;
            return this;
        }

        public Builder showPercentage(boolean show) {
            this.showPercentage = show;
            return this;
        }

        /**
         * Set the filled portion color as a single RGB color (no gradient).
         */
        public Builder filledColor(int r, int g, int b) {
            this.filledColorStart = new int[] {r, g, b};
            this.filledColorEnd = new int[] {r, g, b};
            return this;
        }

        /**
         * Set the filled portion as a gradient between two RGB colors.
         */
        public Builder filledGradient(int r1, int g1, int b1, int r2, int g2, int b2) {
            this.filledColorStart = new int[] {r1, g1, b1};
            this.filledColorEnd = new int[] {r2, g2, b2};
            return this;
        }

        /**
         * Set the empty portion color (RGB).
         */
        public Builder emptyColor(int r, int g, int b) {
            this.emptyColor = new int[] {r, g, b};
            return this;
        }

        public Builder percentageStyle(AttributedStyle style) {
            this.percentageStyle = style;
            return this;
        }

        /**
         * @deprecated Use {@link #filledColor(int, int, int)} for RGB colors
         */
        @Deprecated
        public Builder filledStyle(AttributedStyle style) {
            return this;
        }

        /**
         * @deprecated Use {@link #emptyColor(int, int, int)} for RGB colors
         */
        @Deprecated
        public Builder emptyStyle(AttributedStyle style) {
            return this;
        }

        public ProgressBar build() {
            return new ProgressBar(this);
        }
    }
}

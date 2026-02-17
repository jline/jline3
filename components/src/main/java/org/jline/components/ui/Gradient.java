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
import org.jline.components.animation.Animatable;
import org.jline.components.layout.Size;
import org.jline.utils.AttributedStyle;

/**
 * Gradient-colored text component with optional shimmer animation.
 *
 * <p>Uses true-color (24-bit RGB) for rendering. When animated, a small window
 * of lighter-colored characters sweeps across the text, creating a subtle
 * shimmer effect. The text is rendered in a base color with the highlight
 * window smoothly fading in and out using gaussian falloff.</p>
 */
public class Gradient extends AbstractComponent implements Animatable {

    private final String text;
    private final int[] baseColor; // RGB
    private final int[] highlightColor; // RGB
    private final int[][] gradientColors; // for static (non-animated) multi-stop gradients
    private final boolean animate;
    private final long cycleDurationMs;
    private final int glowWidth;
    private double phase; // 0.0 to 1.0, smooth animation phase

    private Gradient(Builder builder) {
        this.text = builder.text;
        this.baseColor = builder.baseColor;
        this.highlightColor = builder.highlightColor;
        this.gradientColors = builder.gradientColors;
        this.animate = builder.animate;
        this.cycleDurationMs = builder.cycleDurationMs;
        this.glowWidth = builder.glowWidth;
        this.phase = 0.0;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Size getPreferredSize() {
        return new Size(text.length(), 1);
    }

    @Override
    public void render(Canvas canvas, int width, int height) {
        markClean();
        if (width <= 0 || height <= 0 || text.isEmpty()) return;

        int len = Math.min(text.length(), width);

        if (animate) {
            renderShimmer(canvas, len);
        } else {
            renderStaticGradient(canvas, len);
        }
    }

    private void renderShimmer(Canvas canvas, int len) {
        // Ping-pong: sweep right then left
        double t = phase;
        double pingPong = t <= 0.5 ? t * 2.0 : 2.0 - t * 2.0;
        // Smoothstep ease-in-out
        double eased = pingPong * pingPong * (3.0 - 2.0 * pingPong);

        // Center of the highlight window
        double center = eased * (len - 1);
        double sigma = glowWidth / 2.5;

        for (int i = 0; i < len; i++) {
            double dist = Math.abs(i - center);
            double intensity = Math.exp(-(dist * dist) / (2.0 * sigma * sigma));

            int r = clamp((int) (baseColor[0] + intensity * (highlightColor[0] - baseColor[0])));
            int g = clamp((int) (baseColor[1] + intensity * (highlightColor[1] - baseColor[1])));
            int b = clamp((int) (baseColor[2] + intensity * (highlightColor[2] - baseColor[2])));

            canvas.put(i, 0, text.charAt(i), AttributedStyle.DEFAULT.foreground(r, g, b));
        }
    }

    private void renderStaticGradient(Canvas canvas, int len) {
        if (gradientColors == null || gradientColors.length < 2) {
            // Single color
            for (int i = 0; i < len; i++) {
                canvas.put(
                        i,
                        0,
                        text.charAt(i),
                        AttributedStyle.DEFAULT.foreground(baseColor[0], baseColor[1], baseColor[2]));
            }
            return;
        }

        for (int i = 0; i < len; i++) {
            float t = len > 1 ? (float) i / (len - 1) : 0;
            float segment = t * (gradientColors.length - 1);
            int idx = Math.min((int) segment, gradientColors.length - 2);
            float localT = segment - idx;

            int r = clamp(
                    (int) (gradientColors[idx][0] + localT * (gradientColors[idx + 1][0] - gradientColors[idx][0])));
            int g = clamp(
                    (int) (gradientColors[idx][1] + localT * (gradientColors[idx + 1][1] - gradientColors[idx][1])));
            int b = clamp(
                    (int) (gradientColors[idx][2] + localT * (gradientColors[idx + 1][2] - gradientColors[idx][2])));

            canvas.put(i, 0, text.charAt(i), AttributedStyle.DEFAULT.foreground(r, g, b));
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public boolean onTick(long elapsedMs) {
        if (!animate) return false;
        double newPhase = (elapsedMs % cycleDurationMs) / (double) cycleDurationMs;
        if (Math.abs(newPhase - phase) > 0.001) {
            phase = newPhase;
            invalidate();
            return true;
        }
        return false;
    }

    @Override
    public long getIntervalMs() {
        return 16; // ~60fps for smooth animation
    }

    public static class Builder {
        private String text = "";
        private int[] baseColor = {59, 130, 246}; // blue-500
        private int[] highlightColor = {219, 234, 254}; // blue-100
        private int[][] gradientColors;
        private boolean animate = false;
        private long cycleDurationMs = 3000;
        private int glowWidth = 3;

        Builder() {}

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * Set the base text color (RGB). Used as the default color
         * for all characters when animated.
         */
        public Builder baseColor(int r, int g, int b) {
            this.baseColor = new int[] {r, g, b};
            return this;
        }

        /**
         * Set the highlight/shimmer color (RGB). The moving window
         * fades from base color to this color at its center.
         */
        public Builder highlightColor(int r, int g, int b) {
            this.highlightColor = new int[] {r, g, b};
            return this;
        }

        /**
         * Set gradient color stops as RGB triplets for static (non-animated) rendering.
         * When animated, use {@link #baseColor} and {@link #highlightColor} instead.
         * @param colors array of {r, g, b} arrays
         */
        public Builder colors(int[]... colors) {
            if (colors.length >= 2) {
                this.gradientColors = colors;
                this.baseColor = colors[0];
                this.highlightColor = colors[colors.length - 1];
            }
            return this;
        }

        public Builder animate(boolean animate) {
            this.animate = animate;
            return this;
        }

        /**
         * Number of characters in the highlight window.
         * The glow fades smoothly from center outward.
         * Default is 3.
         */
        public Builder glowWidth(int glowWidth) {
            this.glowWidth = Math.max(1, glowWidth);
            return this;
        }

        /**
         * Duration of one full animation cycle in milliseconds.
         * Default is 3000ms (3 seconds). Must be positive; values
         * less than 1 are clamped to 1.
         */
        public Builder cycleDuration(long cycleDurationMs) {
            this.cycleDurationMs = Math.max(1, cycleDurationMs);
            return this;
        }

        public Gradient build() {
            return new Gradient(this);
        }
    }
}

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
 * Indeterminate progress bar with a gradient glow sweeping back and forth
 * over a dark background, similar to the loading indicator at the top
 * of modern web UIs.
 *
 * <p>The glow uses a smooth gaussian-like falloff from a bright center
 * that fades into the dark track color, creating an elegant light sweep
 * effect. Uses the upper one-eighth block character (▔) by default for
 * a thin, subtle look.</p>
 */
public class IndeterminateProgressBar extends AbstractComponent implements Animatable {

    private final int preferredWidth;
    private final int glowRadius;
    private final long cycleDurationMs;
    private final char barChar;
    private final int[] trackColor; // RGB
    private final int[] glowColor; // RGB
    private double position; // 0.0 to 1.0, representing sweep position

    private IndeterminateProgressBar(Builder builder) {
        this.preferredWidth = builder.width;
        this.glowRadius = builder.glowRadius;
        this.cycleDurationMs = builder.cycleDurationMs;
        this.barChar = builder.barChar;
        this.trackColor = builder.trackColor;
        this.glowColor = builder.glowColor;
        this.position = 0.0;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Size getPreferredSize() {
        return new Size(preferredWidth, 1);
    }

    @Override
    public void render(Canvas canvas, int width, int height) {
        markClean();
        if (width <= 0 || height <= 0) return;

        // Compute the center position of the glow
        double t = position;
        // Ping-pong: 0→1→0 over one full cycle
        double pingPong = t <= 0.5 ? t * 2.0 : 2.0 - t * 2.0;
        // Smoothstep ease-in-out
        double eased = pingPong * pingPong * (3.0 - 2.0 * pingPong);

        double center = eased * (width - 1);

        for (int c = 0; c < width; c++) {
            double dist = Math.abs(c - center);
            // Gaussian-like falloff: intensity = exp(-dist²/(2*sigma²))
            double sigma = glowRadius / 2.5;
            double intensity = Math.exp(-(dist * dist) / (2.0 * sigma * sigma));

            // Interpolate from track color to glow color
            int r = clamp((int) (trackColor[0] + intensity * (glowColor[0] - trackColor[0])));
            int g = clamp((int) (trackColor[1] + intensity * (glowColor[1] - trackColor[1])));
            int b = clamp((int) (trackColor[2] + intensity * (glowColor[2] - trackColor[2])));

            canvas.put(c, 0, barChar, AttributedStyle.DEFAULT.foreground(r, g, b));
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public boolean onTick(long elapsedMs) {
        double newPosition = (elapsedMs % cycleDurationMs) / (double) cycleDurationMs;
        if (Math.abs(newPosition - position) > 0.001) {
            position = newPosition;
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
        private int width = 40;
        private int glowRadius = 10;
        private long cycleDurationMs = 2000;
        private char barChar = '\u2594'; // ▔ upper one-eighth block
        private int[] trackColor = {15, 23, 42}; // dark navy
        private int[] glowColor = {96, 165, 250}; // bright blue

        Builder() {}

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        /**
         * Radius of the gradient glow in character cells.
         * The glow fades smoothly from the center outward.
         */
        public Builder glowRadius(int glowRadius) {
            this.glowRadius = glowRadius;
            return this;
        }

        /**
         * Duration of one full sweep cycle (back and forth) in milliseconds.
         * Must be positive; values less than 1 are clamped to 1.
         */
        public Builder cycleDuration(long cycleDurationMs) {
            this.cycleDurationMs = Math.max(1, cycleDurationMs);
            return this;
        }

        public Builder barChar(char barChar) {
            this.barChar = barChar;
            return this;
        }

        /**
         * Set the dark track color (RGB).
         */
        public Builder trackColor(int r, int g, int b) {
            this.trackColor = new int[] {r, g, b};
            return this;
        }

        /**
         * Set the bright glow center color (RGB).
         */
        public Builder glowColor(int r, int g, int b) {
            this.glowColor = new int[] {r, g, b};
            return this;
        }

        public IndeterminateProgressBar build() {
            return new IndeterminateProgressBar(this);
        }
    }
}

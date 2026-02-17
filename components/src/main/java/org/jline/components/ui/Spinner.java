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
import org.jline.components.animation.SpinnerFrames;
import org.jline.components.layout.Size;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.WCWidth;

/**
 * Animated spinner component.
 */
public class Spinner extends AbstractComponent implements Animatable {

    private SpinnerFrames frames;
    private String label;
    private AttributedStyle spinnerStyle;
    private AttributedStyle labelStyle;
    private int currentFrame;

    private Spinner(Builder builder) {
        this.frames = builder.frames;
        this.label = builder.label;
        this.spinnerStyle = builder.spinnerStyle;
        this.labelStyle = builder.labelStyle;
        this.currentFrame = 0;
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setLabel(String label) {
        this.label = label;
        invalidate();
    }

    @Override
    public Size getPreferredSize() {
        int w = frames.maxWidth();
        if (label != null && !label.isEmpty()) {
            w += 1 + displayWidth(label); // space + label
        }
        return new Size(w, 1);
    }

    private static int displayWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int cw = WCWidth.wcwidth(cp);
            if (cw > 0) w += cw;
            i += Character.charCount(cp);
        }
        return w;
    }

    @Override
    public void render(Canvas canvas, int width, int height) {
        markClean();
        if (width <= 0 || height <= 0) return;

        String frame = frames.frame(currentFrame);
        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.style(spinnerStyle);
        sb.append(frame);

        if (label != null && !label.isEmpty()) {
            sb.style(labelStyle);
            sb.append(' ');
            sb.append(label);
        }

        canvas.text(0, 0, sb.toAttributedString());
    }

    @Override
    public boolean onTick(long elapsedMs) {
        int newFrame = (int) ((elapsedMs / frames.intervalMs()) % frames.frameCount());
        if (newFrame != currentFrame) {
            currentFrame = newFrame;
            invalidate();
            return true;
        }
        return false;
    }

    @Override
    public long getIntervalMs() {
        return frames.intervalMs();
    }

    public static class Builder {
        private SpinnerFrames frames = SpinnerFrames.DOTS;
        private String label = "";
        private AttributedStyle spinnerStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
        private AttributedStyle labelStyle = AttributedStyle.DEFAULT;

        Builder() {}

        public Builder frames(SpinnerFrames frames) {
            this.frames = frames;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder spinnerStyle(AttributedStyle style) {
            this.spinnerStyle = style;
            return this;
        }

        public Builder labelStyle(AttributedStyle style) {
            this.labelStyle = style;
            return this;
        }

        public Spinner build() {
            return new Spinner(this);
        }
    }
}

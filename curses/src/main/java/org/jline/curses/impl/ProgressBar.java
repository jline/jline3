/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.EnumSet;

import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * A progress bar component that displays a value from 0.0 to 1.0.
 */
public class ProgressBar extends AbstractComponent {

    private double value;
    private boolean showPercentage = true;

    public ProgressBar() {
        setBehaviors(EnumSet.of(Behavior.NoFocus));
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = Math.max(0.0, Math.min(1.0, value));
    }

    public boolean isShowPercentage() {
        return showPercentage;
    }

    public void setShowPercentage(boolean showPercentage) {
        this.showPercentage = showPercentage;
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        Position pos = getScreenPosition();
        if (size == null || pos == null) {
            return;
        }

        AttributedStyle filledStyle = resolveStyle(".progress.filled", AttributedStyle.DEFAULT.inverse());
        AttributedStyle emptyStyle = resolveStyle(".progress.empty", AttributedStyle.DEFAULT);
        AttributedStyle textStyle = resolveStyle(".progress.text", AttributedStyle.DEFAULT);

        int width = size.w();
        String percentText = showPercentage ? String.format(" %d%%", (int) (value * 100)) : "";
        int barWidth = width - percentText.length();

        if (barWidth <= 0) {
            return;
        }

        int filledWidth = (int) (barWidth * value);

        AttributedStringBuilder asb = new AttributedStringBuilder();
        for (int i = 0; i < barWidth; i++) {
            if (i < filledWidth) {
                asb.style(filledStyle);
                asb.append('\u2588'); // █
            } else {
                asb.style(emptyStyle);
                asb.append('\u2591'); // ░
            }
        }
        if (!percentText.isEmpty()) {
            asb.style(textStyle);
            asb.append(percentText);
        }

        screen.text(pos.x(), pos.y(), asb.toAttributedString());
    }

    @Override
    protected Size doGetPreferredSize() {
        return new Size(20, 1);
    }
}

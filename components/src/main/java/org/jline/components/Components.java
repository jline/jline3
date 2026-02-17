/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components;

import org.jline.components.animation.SpinnerFrames;
import org.jline.components.layout.FlexDirection;
import org.jline.components.layout.Insets;
import org.jline.components.ui.Box;
import org.jline.components.ui.Gradient;
import org.jline.components.ui.Hyperlink;
import org.jline.components.ui.IndeterminateProgressBar;
import org.jline.components.ui.ProgressBar;
import org.jline.components.ui.Separator;
import org.jline.components.ui.Spinner;
import org.jline.components.ui.StatusMessage;
import org.jline.components.ui.Text;
import org.jline.utils.AttributedStyle;

/**
 * Convenience factory for creating common component configurations.
 */
public final class Components {

    private Components() {}

    public static Text text(String text) {
        return Text.builder().text(text).build();
    }

    public static Text text(String text, AttributedStyle style) {
        return Text.builder().text(text).style(style).build();
    }

    public static Text boldText(String text) {
        return Text.builder().text(text).style(AttributedStyle.BOLD).build();
    }

    public static Spinner spinner(String label) {
        return Spinner.builder().label(label).build();
    }

    public static Spinner spinner(SpinnerFrames frames, String label) {
        return Spinner.builder().frames(frames).label(label).build();
    }

    public static ProgressBar progressBar() {
        return ProgressBar.builder().build();
    }

    public static ProgressBar progressBar(double progress) {
        return ProgressBar.builder().progress(progress).build();
    }

    public static IndeterminateProgressBar indeterminateProgressBar() {
        return IndeterminateProgressBar.builder().build();
    }

    public static IndeterminateProgressBar indeterminateProgressBar(int width) {
        return IndeterminateProgressBar.builder().width(width).build();
    }

    public static Separator separator() {
        return Separator.builder().build();
    }

    public static Separator separator(String title) {
        return Separator.builder().title(title).build();
    }

    public static StatusMessage success(String message) {
        return StatusMessage.success(message);
    }

    public static StatusMessage warning(String message) {
        return StatusMessage.warning(message);
    }

    public static StatusMessage error(String message) {
        return StatusMessage.error(message);
    }

    public static StatusMessage info(String message) {
        return StatusMessage.info(message);
    }

    public static Gradient gradient(String text, int[]... colors) {
        return Gradient.builder().text(text).colors(colors).build();
    }

    public static Hyperlink link(String url, String text) {
        return Hyperlink.builder().url(url).text(text).build();
    }

    public static Box vbox(Component... children) {
        return Box.builder().direction(FlexDirection.COLUMN).children(children).build();
    }

    public static Box hbox(Component... children) {
        return Box.builder().direction(FlexDirection.ROW).children(children).build();
    }

    public static Box borderedBox(Box.BorderStyle border, Component... children) {
        return Box.builder()
                .direction(FlexDirection.COLUMN)
                .borderStyle(border)
                .padding(Insets.of(0, 1))
                .children(children)
                .build();
    }
}

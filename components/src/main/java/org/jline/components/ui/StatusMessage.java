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
import org.jline.utils.WCWidth;

/**
 * Colored status message component with prefix icon.
 */
public class StatusMessage extends AbstractComponent {

    private final Type type;
    private final String message;

    private StatusMessage(Type type, String message) {
        this.type = type;
        this.message = message;
    }

    public static StatusMessage success(String message) {
        return new StatusMessage(Type.SUCCESS, message);
    }

    public static StatusMessage warning(String message) {
        return new StatusMessage(Type.WARNING, message);
    }

    public static StatusMessage error(String message) {
        return new StatusMessage(Type.ERROR, message);
    }

    public static StatusMessage info(String message) {
        return new StatusMessage(Type.INFO, message);
    }

    @Override
    public Size getPreferredSize() {
        // icon + space + message
        return new Size(displayWidth(type.icon) + 1 + message.length(), 1);
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

        AttributedStringBuilder sb = new AttributedStringBuilder();
        sb.style(type.style);
        sb.append(type.icon);
        sb.style(AttributedStyle.DEFAULT);
        sb.append(' ');
        sb.append(message);

        canvas.text(0, 0, sb.toAttributedString());
    }

    public enum Type {
        SUCCESS("\u2714", AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)),
        WARNING("\u26A0", AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)),
        ERROR("\u2716", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)),
        INFO("\u2139", AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE));

        final String icon;
        final AttributedStyle style;

        Type(String icon, AttributedStyle style) {
            this.icon = icon;
            this.style = style;
        }
    }
}

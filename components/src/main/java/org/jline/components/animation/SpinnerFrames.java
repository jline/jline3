/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.animation;

import org.jline.utils.WCWidth;

/**
 * Predefined spinner frame sets for terminal animations.
 */
public enum SpinnerFrames {

    // Classic spinners
    DOTS(80, "\u28F7", "\u28EF", "\u28DF", "\u287F", "\u28BF", "\u28FB", "\u28FD", "\u28FE"),
    DOTS2(80, "\u28FE", "\u28FD", "\u28FB", "\u28BF", "\u287F", "\u28DF", "\u28EF", "\u28F7"),
    DOTS3(
            80, "\u2804", "\u2806", "\u2807", "\u280B", "\u2819", "\u2838", "\u2830", "\u2820", "\u2830", "\u2838",
            "\u2819", "\u280B", "\u2807", "\u2806"),
    LINE(130, "-", "\\", "|", "/"),
    PIPE(100, "\u2524", "\u2518", "\u2534", "\u2514", "\u251C", "\u250C", "\u252C", "\u2510"),
    STAR(70, "\u2736", "\u2738", "\u2739", "\u273A", "\u2739", "\u2738"),
    FLIP(70, "_", "_", "_", "-", "`", "`", "'", "\u00B4", "-", "_", "_", "_"),
    BOUNCE(120, "\u2801", "\u2802", "\u2804", "\u2802"),
    ARC(100, "\u25DC", "\u25E0", "\u25DD", "\u25DE", "\u25E1", "\u25DF"),
    CIRCLE(120, "\u25E1", "\u2299", "\u25E0"),
    SQUARE_CORNERS(180, "\u25F0", "\u25F3", "\u25F2", "\u25F1"),
    TRIANGLE(50, "\u25E2", "\u25E3", "\u25E4", "\u25E5"),
    ARROW(100, "\u2190", "\u2196", "\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199"),
    BALLOON(140, " ", ".", "o", "O", "@", "*", " "),
    NOISE(100, "\u2593", "\u2592", "\u2591", "\u2592"),
    SIMPLE_DOTS(300, ".  ", ".. ", "...", "   "),
    SIMPLE_DOTS_SCROLLING(200, ".  ", ".. ", "...", " ..", "  .", "   "),
    GROW_VERTICAL(
            120, "\u2581", "\u2583", "\u2584", "\u2585", "\u2586", "\u2587", "\u2588", "\u2587", "\u2586", "\u2585",
            "\u2584", "\u2583"),
    GROW_HORIZONTAL(
            120, "\u258F", "\u258E", "\u258D", "\u258C", "\u258B", "\u258A", "\u2589", "\u2588", "\u2589", "\u258A",
            "\u258B", "\u258C", "\u258D", "\u258E"),
    TOGGLE(250, "\u22B6", "\u22B7"),
    ARROW2(
            100,
            "\u2B06\uFE0F",
            "\u2197\uFE0F",
            "\u27A1\uFE0F",
            "\u2198\uFE0F",
            "\u2B07\uFE0F",
            "\u2199\uFE0F",
            "\u2B05\uFE0F",
            "\u2196\uFE0F"),
    HAMBURGER(100, "\u2631", "\u2632", "\u2634"),
    POINT(
            125,
            "\u2219\u2219\u2219",
            "\u25CF\u2219\u2219",
            "\u2219\u25CF\u2219",
            "\u2219\u2219\u25CF",
            "\u2219\u2219\u2219"),
    LAYER(150, "-", "=", "\u2261"),
    CLASSIC(100, "|", "/", "-", "\\");

    private final long intervalMs;
    private final String[] frames;

    SpinnerFrames(long intervalMs, String... frames) {
        this.intervalMs = intervalMs;
        this.frames = frames;
    }

    public long intervalMs() {
        return intervalMs;
    }

    public String[] frames() {
        return frames;
    }

    public int frameCount() {
        return frames.length;
    }

    public String frame(int index) {
        return frames[index % frames.length];
    }

    /**
     * Returns the maximum display width of any frame.
     */
    public int maxWidth() {
        int max = 0;
        for (String f : frames) {
            int w = 0;
            for (int i = 0; i < f.length(); ) {
                int cp = f.codePointAt(i);
                int cw = WCWidth.wcwidth(cp);
                if (cw > 0) w += cw;
                i += Character.charCount(cp);
            }
            max = Math.max(max, w);
        }
        return max;
    }
}

/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.components.layout;

import java.util.List;

import org.jline.components.Canvas;
import org.jline.components.Component;

/**
 * Simplified flexbox layout engine operating on integer character cells.
 */
public final class LayoutEngine {

    private LayoutEngine() {}

    /**
     * Represents the computed layout position and size of a child.
     */
    public static final class ChildLayout {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public ChildLayout(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Layout children in a flex container and render them.
     *
     * @param canvas    the canvas to render into
     * @param children  the child components
     * @param width     available width
     * @param height    available height
     * @param direction flex direction
     * @param justify   main-axis justification
     * @param align     cross-axis alignment
     * @param gap       gap between children on main axis
     * @param padding   padding insets
     */
    public static void layout(
            Canvas canvas,
            List<Component> children,
            int width,
            int height,
            FlexDirection direction,
            FlexJustify justify,
            FlexAlign align,
            int gap,
            Insets padding) {

        if (children.isEmpty()) {
            return;
        }

        int innerWidth = width - padding.horizontal();
        int innerHeight = height - padding.vertical();
        if (innerWidth <= 0 || innerHeight <= 0) {
            return;
        }

        boolean horizontal = direction == FlexDirection.ROW;
        int mainAvailable = horizontal ? innerWidth : innerHeight;
        int crossAvailable = horizontal ? innerHeight : innerWidth;

        // Measure phase: query preferred sizes
        int n = children.size();
        int[] prefMain = new int[n];
        int[] prefCross = new int[n];
        int totalPrefMain = 0;

        for (int i = 0; i < n; i++) {
            Size pref = children.get(i).getPreferredSize();
            prefMain[i] = horizontal ? pref.width() : pref.height();
            prefCross[i] = horizontal ? pref.height() : pref.width();
            totalPrefMain += prefMain[i];
        }

        int totalGaps = (n - 1) * gap;
        int spaceForChildren = Math.max(0, mainAvailable - totalGaps);

        // Distribute main-axis sizes
        int[] childMain = new int[n];
        if (totalPrefMain <= spaceForChildren) {
            // Children fit — use preferred sizes (no auto-grow)
            for (int i = 0; i < n; i++) {
                childMain[i] = prefMain[i];
            }
        } else {
            // Shrink proportionally, distributing remainder to avoid lost pixels
            int allocated = 0;
            for (int i = 0; i < n; i++) {
                childMain[i] = totalPrefMain > 0 ? Math.max(0, spaceForChildren * prefMain[i] / totalPrefMain) : 0;
                allocated += childMain[i];
            }
            // Distribute remaining pixels to the first children
            int remainder = spaceForChildren - allocated;
            for (int i = 0; i < n && remainder > 0; i++) {
                if (prefMain[i] > 0) {
                    childMain[i]++;
                    remainder--;
                }
            }
        }

        // Cross-axis sizes
        int[] childCross = new int[n];
        for (int i = 0; i < n; i++) {
            childCross[i] = align == FlexAlign.STRETCH ? crossAvailable : Math.min(prefCross[i], crossAvailable);
        }

        // Justify: compute starting position and spacing
        int totalUsed = 0;
        for (int i = 0; i < n; i++) {
            totalUsed += childMain[i];
        }
        totalUsed += totalGaps;
        int freeSpace = Math.max(0, mainAvailable - totalUsed);

        int mainStart;
        int extraGap = 0;
        int outerGap = 0;

        switch (justify) {
            case CENTER:
                mainStart = freeSpace / 2;
                break;
            case END:
                mainStart = freeSpace;
                break;
            case SPACE_BETWEEN:
                mainStart = 0;
                if (n > 1) {
                    extraGap = freeSpace / (n - 1);
                }
                break;
            case SPACE_AROUND:
                if (n > 0) {
                    // CSS space-around: equal space on both sides of each child.
                    // Between adjacent children, the gap is 2x the outer margin.
                    outerGap = freeSpace / (n * 2);
                    extraGap = outerGap * 2;
                    mainStart = outerGap;
                } else {
                    mainStart = 0;
                }
                break;
            default: // START
                mainStart = 0;
                break;
        }

        // Position and render each child
        int mainPos = mainStart;
        for (int i = 0; i < n; i++) {
            int cm = childMain[i];
            int cc = childCross[i];

            // Cross-axis position
            int crossPos;
            switch (align) {
                case CENTER:
                    crossPos = (crossAvailable - cc) / 2;
                    break;
                case END:
                    crossPos = crossAvailable - cc;
                    break;
                default: // START, STRETCH
                    crossPos = 0;
                    break;
            }

            int x, y, w, h;
            if (horizontal) {
                x = padding.left() + mainPos;
                y = padding.top() + crossPos;
                w = cm;
                h = cc;
            } else {
                x = padding.left() + crossPos;
                y = padding.top() + mainPos;
                w = cc;
                h = cm;
            }

            Canvas sub = canvas.subRegion(x, y, w, h);
            children.get(i).render(sub, w, h);

            mainPos += cm + gap + extraGap;
        }
    }
}

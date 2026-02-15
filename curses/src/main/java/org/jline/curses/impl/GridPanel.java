/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.Map;

import org.jline.curses.*;

public class GridPanel extends AbstractPanel {

    @Override
    protected void layout() {
        Size size = getSize();
        if (size == null || components.isEmpty()) {
            return;
        }

        int maxRow = 0;
        int maxCol = 0;
        for (Map.Entry<Component, Constraint> entry : components.entrySet()) {
            Curses.GridConstraint gc = (Curses.GridConstraint) entry.getValue();
            maxRow = Math.max(maxRow, gc.row() + gc.rowSpan());
            maxCol = Math.max(maxCol, gc.col() + gc.colSpan());
        }

        if (maxRow == 0 || maxCol == 0) {
            return;
        }

        // Compute preferred widths/heights for each column/row (ignoring spans)
        int[] colWidths = new int[maxCol];
        int[] rowHeights = new int[maxRow];
        for (Map.Entry<Component, Constraint> entry : components.entrySet()) {
            Curses.GridConstraint gc = (Curses.GridConstraint) entry.getValue();
            Size pref = entry.getKey().getPreferredSize();
            if (pref == null) {
                continue;
            }
            if (gc.colSpan() == 1) {
                colWidths[gc.col()] = Math.max(colWidths[gc.col()], pref.w());
            }
            if (gc.rowSpan() == 1) {
                rowHeights[gc.row()] = Math.max(rowHeights[gc.row()], pref.h());
            }
        }

        // Distribute excess space evenly
        int totalPrefWidth = 0;
        for (int w : colWidths) totalPrefWidth += w;
        int totalPrefHeight = 0;
        for (int h : rowHeights) totalPrefHeight += h;

        int excessW = size.w() - totalPrefWidth;
        if (excessW > 0) {
            int perCol = excessW / maxCol;
            int remainder = excessW % maxCol;
            for (int c = 0; c < maxCol; c++) {
                colWidths[c] += perCol + (c < remainder ? 1 : 0);
            }
        }

        int excessH = size.h() - totalPrefHeight;
        if (excessH > 0) {
            int perRow = excessH / maxRow;
            int remainder = excessH % maxRow;
            for (int r = 0; r < maxRow; r++) {
                rowHeights[r] += perRow + (r < remainder ? 1 : 0);
            }
        }

        // Compute cumulative offsets
        int[] colOffsets = new int[maxCol + 1];
        for (int c = 0; c < maxCol; c++) {
            colOffsets[c + 1] = colOffsets[c] + colWidths[c];
        }
        int[] rowOffsets = new int[maxRow + 1];
        for (int r = 0; r < maxRow; r++) {
            rowOffsets[r + 1] = rowOffsets[r] + rowHeights[r];
        }

        // Position each component
        for (Map.Entry<Component, Constraint> entry : components.entrySet()) {
            Curses.GridConstraint gc = (Curses.GridConstraint) entry.getValue();
            int x = colOffsets[gc.col()];
            int y = rowOffsets[gc.row()];
            int w = colOffsets[Math.min(gc.col() + gc.colSpan(), maxCol)] - x;
            int h = rowOffsets[Math.min(gc.row() + gc.rowSpan(), maxRow)] - y;
            entry.getKey().setPosition(new Position(x, y));
            entry.getKey().setSize(new Size(Math.max(0, w), Math.max(0, h)));
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        if (components.isEmpty()) {
            return new Size(0, 0);
        }

        int maxRow = 0;
        int maxCol = 0;
        for (Constraint c : components.values()) {
            Curses.GridConstraint gc = (Curses.GridConstraint) c;
            maxRow = Math.max(maxRow, gc.row() + gc.rowSpan());
            maxCol = Math.max(maxCol, gc.col() + gc.colSpan());
        }

        int[] colWidths = new int[maxCol];
        int[] rowHeights = new int[maxRow];
        for (Map.Entry<Component, Constraint> entry : components.entrySet()) {
            Curses.GridConstraint gc = (Curses.GridConstraint) entry.getValue();
            Size pref = entry.getKey().getPreferredSize();
            if (pref == null) {
                continue;
            }
            if (gc.colSpan() == 1) {
                colWidths[gc.col()] = Math.max(colWidths[gc.col()], pref.w());
            }
            if (gc.rowSpan() == 1) {
                rowHeights[gc.row()] = Math.max(rowHeights[gc.row()], pref.h());
            }
        }

        int totalW = 0;
        for (int w : colWidths) totalW += w;
        int totalH = 0;
        for (int h : rowHeights) totalH += h;
        return new Size(totalW, totalH);
    }
}

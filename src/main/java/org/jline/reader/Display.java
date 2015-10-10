/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jline.Console;
import org.jline.utils.AnsiHelper;
import org.jline.utils.Curses;
import org.jline.utils.DiffHelper;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.WCWidth;

/**
 * Handle display and visual cursor.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class Display {

    protected final Console console;
    protected final boolean fullScreen;
    protected List<String> oldLines = Collections.emptyList();
    protected int cursorPos;
    protected boolean cursorOk;
    protected int columns;
    protected int rows;
    protected int tabWidth = ConsoleReaderImpl.TAB_WIDTH;
    protected boolean reset;

    protected final Map<Capability, Integer> cost = new HashMap<>();
    protected final boolean canScroll;
    protected final boolean noWrapAtEol;
    protected final boolean cursorDownIsNewLine;

    public Display(Console console, boolean fullscreen) {
        this.console = console;
        this.fullScreen = fullscreen;

        this.canScroll = can(Capability.insert_line, Capability.parm_insert_line)
                            && can(Capability.delete_line, Capability.parm_delete_line);
        this.noWrapAtEol = console.getBooleanCapability(Capability.auto_right_margin)
                            && console.getBooleanCapability(Capability.eat_newline_glitch);
        this.cursorDownIsNewLine = "\n".equals(tput(Capability.cursor_down));
    }

    public void resize(int rows, int columns) {
        if (this.rows != rows || this.columns != columns) {
            this.rows = rows;
            this.columns = columns;
            oldLines = AnsiHelper.splitLines(String.join("\n", oldLines), columns, tabWidth);
        }
    }

    public void setTabWidth(int tabWidth) {
        this.tabWidth = tabWidth;
    }

    public void reset() {
        oldLines = Collections.emptyList();
    }

    /**
     * Clears the whole screen.
     * Use this method only when using full-screen / application mode.
     */
    public void clear() {
        if (fullScreen) {
            reset = true;
        }
    }

    /**
     * Update the display according to the new lines
     */
    public void update(List<String> newLines, int targetCursorPos) {
        if (reset) {
            console.puts(Capability.clear_screen);
            oldLines.clear();
            cursorPos = 0;
            cursorOk = true;
            reset = false;
        }

        // Detect scrolling
        if (fullScreen && newLines.size() == oldLines.size() && canScroll) {
            int nbHeaders = 0;
            int nbFooters = 0;
            // Find common headers and footers
            int l = newLines.size();
            while (nbHeaders < l
                    && Objects.equals(newLines.get(nbHeaders), oldLines.get(nbHeaders))) {
                nbHeaders++;
            }
            while (nbFooters < l - nbHeaders - 1
                    && Objects.equals(newLines.get(newLines.size() - nbFooters - 1), oldLines.get(oldLines.size() - nbFooters - 1))) {
                nbFooters++;
            }
            List<String> o1 = newLines.subList(nbHeaders, newLines.size() - nbFooters);
            List<String> o2 = oldLines.subList(nbHeaders, oldLines.size() - nbFooters);
            int[] common = longestCommon(o1, o2);
            if (common != null) {
                int s1 = common[0];
                int s2 = common[1];
                int sl = common[2];
                if (sl > 1 && s1 < s2) {
                    moveVisualCursorTo((nbHeaders + s1) * columns);
                    int nb = s2 - s1;
                    deleteLines(nb);
                    for (int i = 0; i < nb; i++) {
                        oldLines.remove(nbHeaders + s1);
                    }
                    if (nbFooters > 0) {
                        moveVisualCursorTo((nbHeaders + s1 + sl) * columns);
                        insertLines(nb);
                        for (int i = 0; i < nb; i++) {
                            oldLines.add(nbHeaders + s1 + sl, "");
                        }
                    }
                } else if (sl > 1 && s1 > s2) {
                    int nb = s1 - s2;
                    if (nbFooters > 0) {
                        moveVisualCursorTo((nbHeaders + s2 + sl) * columns);
                        deleteLines(nb);
                        for (int i = 0; i < nb; i++) {
                            oldLines.remove(nbHeaders + s2 + sl);
                        }
                    }
                    moveVisualCursorTo((nbHeaders + s2) * columns);
                    insertLines(nb);
                    for (int i = 0; i < nb; i++) {
                        oldLines.add(nbHeaders + s2, "");
                    }
                }
            }
        }

        int lineIndex = 0;
        int currentPos = 0;
        while (lineIndex < Math.min(oldLines.size(), newLines.size())) {
            String oldLine = oldLines.get(lineIndex);
            String newLine = newLines.get(lineIndex);

            List<DiffHelper.Diff> diffs = DiffHelper.diff(oldLine, newLine);
            boolean ident = true;
            boolean cleared = false;
            int curCol = currentPos;
            for (int i = 0; i < diffs.size(); i++) {
                DiffHelper.Diff diff = diffs.get(i);
                int width = wcwidth(diff.text, currentPos % columns);
                switch (diff.operation) {
                    case EQUAL:
                        if (!ident) {
                            cursorPos = moveVisualCursorTo(currentPos);
                            rawPrint(diff.text);
                            cursorPos += width;
                            cursorOk = false;
                            currentPos = cursorPos;
                        } else {
                            currentPos += width;
                        }
                        break;
                    case INSERT:
                        if (i <= diffs.size() - 2
                                && diffs.get(i + 1).operation == DiffHelper.Operation.EQUAL) {
                            cursorPos = moveVisualCursorTo(currentPos);
                            if (insertChars(width)) {
                                rawPrint(diff.text);
                                cursorPos += width;
                                cursorOk = false;
                                currentPos = cursorPos;
                                break;
                            }
                        } else if (i <= diffs.size() - 2
                                && diffs.get(i + 1).operation == DiffHelper.Operation.DELETE
                                && width == wcwidth(diffs.get(i + 1).text, currentPos % columns)) {
                            moveVisualCursorTo(currentPos);
                            rawPrint(diff.text);
                            cursorPos += width;
                            cursorOk = false;
                            currentPos = cursorPos;
                            i++; // skip delete
                            break;
                        }
                        moveVisualCursorTo(currentPos);
                        rawPrint(diff.text);
                        cursorPos += width;
                        cursorOk = false;
                        currentPos = cursorPos;
                        ident = false;
                        break;
                    case DELETE:
                        if (cleared) {
                            continue;
                        }
                        if (currentPos - curCol >= columns) {
                            continue;
                        }
                        if (i <= diffs.size() - 2
                                && diffs.get(i + 1).operation == DiffHelper.Operation.EQUAL) {
                            if (currentPos + wcwidth(diffs.get(i + 1).text, currentPos % columns) < columns) {
                                moveVisualCursorTo(currentPos);
                                if (deleteChars(width)) {
                                    break;
                                }
                            }
                        }
                        int oldLen = wcwidth(oldLine);
                        int newLen = wcwidth(newLine);
                        int nb = Math.max(oldLen, newLen) - currentPos;
                        moveVisualCursorTo(currentPos);
                        if (!console.puts(Capability.clr_eol)) {
                            rawPrint(' ', nb);
                            cursorPos += nb;
                            cursorOk = false;
                        }
                        cleared = true;
                        ident = false;
                        break;
                }
            }
            lineIndex++;
            if (!cursorOk && noWrapAtEol && cursorPos == curCol + columns) {
                console.puts(Capability.carriage_return); // CR / not newline.
                cursorPos = curCol;
                cursorOk = true;
            }
            if (lineIndex < Math.max(oldLines.size(), newLines.size())) {
                currentPos = curCol + columns;
            } else {
                currentPos = curCol + wcwidth(newLine);
            }
        }
        while (lineIndex < Math.max(oldLines.size(), newLines.size())) {
            moveVisualCursorTo(currentPos);
            if (lineIndex < oldLines.size()) {
                if (console.getStringCapability(Capability.clr_eol) != null) {
                    console.puts(Capability.clr_eol);
                } else {
                    int nb = wcwidth(newLines.get(lineIndex));
                    rawPrint(' ', nb);
                    cursorPos += nb;
                    cursorOk = false;
                }
            } else {
                rawPrint(newLines.get(lineIndex));
                cursorPos += wcwidth(newLines.get(lineIndex));
                cursorOk = false;
            }
            if (!cursorOk && noWrapAtEol && cursorPos == currentPos + columns) {
                console.puts(Capability.carriage_return); // CR / not newline.
                cursorPos = currentPos;
                cursorOk = true;
            }
            lineIndex++;
            if (lineIndex < Math.max(oldLines.size(), newLines.size())) {
                currentPos = currentPos + columns;
            } else {
                currentPos = cursorPos;
            }
        }
        moveVisualCursorTo(targetCursorPos < 0 ? currentPos : targetCursorPos);
        oldLines = newLines;
    }

    protected boolean deleteLines(int nb) {
        return perform(Capability.delete_line, Capability.parm_delete_line, nb);
    }

    protected boolean insertLines(int nb) {
        return perform(Capability.insert_line, Capability.parm_insert_line, nb);
    }

    protected boolean insertChars(int nb) {
        return perform(Capability.insert_character, Capability.parm_ich, nb);
    }

    protected boolean deleteChars(int nb) {
        return perform(Capability.delete_character, Capability.parm_dch, nb);
    }

    protected boolean can(Capability single, Capability multi) {
        return console.getStringCapability(single) != null
                || console.getStringCapability(multi) != null;
    }

    protected boolean perform(Capability single, Capability multi, int nb) {
        boolean hasMulti = console.getStringCapability(multi) != null;
        boolean hasSingle = console.getStringCapability(single) != null;
        if (hasMulti && (!hasSingle || cost(single) * nb > cost(multi))) {
            console.puts(multi, nb);
            return true;
        } else if (hasSingle) {
            for (int i = 0; i < nb; i++) {
                console.puts(single);
            }
            return true;
        } else {
            return false;
        }
    }

    private int cost(Capability cap) {
        return cost.computeIfAbsent(cap, this::computeCost);
    }

    private int computeCost(Capability cap) {
        String s = tput(cap, 0);
        return s != null ? s.length() : Integer.MAX_VALUE;
    }

    private String tput(Capability cap, Object... params) {
        try {
            StringWriter sw = new StringWriter();
            String d = console.getStringCapability(cap);
            if (d != null) {
                Curses.tputs(sw, d, params);
                return sw.toString();
            }
            return null;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static int[] longestCommon(List<String> l1, List<String> l2) {
        int start1 = 0;
        int start2 = 0;
        int max = 0;
        for (int i = 0; i < l1.size(); i++) {
            for (int j = 0; j < l2.size(); j++) {
                int x = 0;
                while (Objects.equals(l1.get(i + x), l2.get(j + x))) {
                    x++;
                    if (((i + x) >= l1.size()) || ((j + x) >= l2.size())) break;
                }
                if (x > max) {
                    max = x;
                    start1 = i;
                    start2 = j;
                }
            }
        }
        return max != 0 ? new int[] { start1, start2, max } : null;
    }

    protected int moveVisualCursorTo(int i1) {
        int i0 = cursorPos;
        if (i0 == i1) return i1;
        int width = columns;
        int l0 = i0 / width;
        int c0 = i0 % width;
        int l1 = i1 / width;
        int c1 = i1 % width;
        if (l0 > l1) {
            perform(Capability.cursor_up, Capability.parm_up_cursor, l0 - l1);
        } else if (l0 < l1) {
            // TODO: clean the following
            if (fullScreen) {
                if (!console.puts(Capability.parm_down_cursor, l1 - l0)) {
                    for (int i = l0; i < l1; i++) {
                        console.puts(Capability.cursor_down);
                    }
                    if (cursorDownIsNewLine) {
                        c0 = 0;
                    }
                }
            } else {
                rawPrint('\n', l1 - l0);
                c0 = 0;
            }
        }
        if (c0 != 0 && c1 == 0) {
            console.puts(Capability.carriage_return);
        } else if (c0 < c1) {
            perform(Capability.cursor_right, Capability.parm_right_cursor, c1 - c0);
        } else if (c0 > c1) {
            perform(Capability.cursor_left, Capability.parm_left_cursor, c0 - c1);
        }
        cursorPos = i1;
        cursorOk = true;
        return i1;
    }

    void rawPrint(char c, int num) {
        for (int i = 0; i < num; i++) {
            rawPrint(c);
        }
    }

    void rawPrint(int c) {
        console.writer().write(c);
    }

    void rawPrint(String str) {
        console.writer().write(str);
    }

    public int wcwidth(String str) {
        return wcwidth(str, 0);
    }

    int wcwidth(CharSequence str, int pos) {
        String tr = AnsiHelper.strip(str);
        return wcwidth(tr, 0, tr.length(), pos);
    }

    int wcwidth(CharSequence str, int start, int end, int pos) {
        int cur = pos;
        for (int i = start; i < end; ) {
            int ucs;
            char c1 = str.charAt(i++);
            if (!Character.isHighSurrogate(c1) || i >= end) {
                ucs = c1;
            } else {
                char c2 = str.charAt(i);
                if (Character.isLowSurrogate(c2)) {
                    i++;
                    ucs = Character.toCodePoint(c1, c2);
                } else {
                    ucs = c1;
                }
            }
            cur += wcwidth(ucs, cur);
        }
        return cur - pos;
    }

    int wcwidth(int ucs, int pos) {
        if (ucs == '\t') {
            return nextTabStop(pos);
        } else if (ucs < 32) {
            return 2;
        } else {
            int w = WCWidth.wcwidth(ucs);
            return w > 0 ? w : 0;
        }
    }

    int nextTabStop(int pos) {
        int width = columns;
        int mod = (pos + tabWidth - 1) % tabWidth;
        int npos = pos + tabWidth - mod;
        return npos < width ? npos - pos : width - pos;
    }

}

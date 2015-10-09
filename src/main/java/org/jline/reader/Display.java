/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
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

    public Display(Console console, boolean fullscreen) {
        this.console = console;
        this.fullScreen = fullscreen;
    }

    public void resize(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        oldLines = AnsiHelper.splitLines(String.join("\n", oldLines), columns, tabWidth);
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
     *
     * TODO: use scrolling if appropriate
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
        boolean hasInsert = console.getStringCapability(Capability.parm_insert_line) != null
                         || console.getStringCapability(Capability.insert_line) != null;
        boolean hasDelete = console.getStringCapability(Capability.parm_delete_line) != null
                         || console.getStringCapability(Capability.delete_line) != null;
        if (fullScreen && newLines.size() == oldLines.size() && hasInsert && hasDelete) {
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
                            boolean hasIch = console.getStringCapability(Capability.parm_ich) != null;
                            boolean hasIch1 = console.getStringCapability(Capability.insert_character) != null;
                            if (hasIch) {
                                console.puts(Capability.parm_ich, width);
                                rawPrint(diff.text);
                                cursorPos += width;
                                cursorOk = false;
                                currentPos = cursorPos;
                                break;
                            } else if (hasIch1) {
                                for (int j = 0; j < width; j++) {
                                    console.puts(Capability.insert_character);
                                }
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
                                boolean hasDch = console.getStringCapability(Capability.parm_dch) != null;
                                boolean hasDch1 = console.getStringCapability(Capability.delete_character) != null;
                                if (hasDch) {
                                    console.puts(Capability.parm_dch, width);
                                    break;
                                } else if (hasDch1) {
                                    for (int j = 0; j < width; j++) {
                                        console.puts(Capability.delete_character);
                                    }
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
            if (!cursorOk
                    && console.getBooleanCapability(Capability.auto_right_margin)
                    && console.getBooleanCapability(Capability.eat_newline_glitch)
                    && cursorPos == curCol + columns) {
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
            if (!cursorOk
                    && console.getBooleanCapability(Capability.auto_right_margin)
                    && console.getBooleanCapability(Capability.eat_newline_glitch)
                    && cursorPos == currentPos + columns) {
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

    protected void deleteLines(int nb) {
        if (console.getStringCapability(Capability.parm_delete_line) != null
                && (nb > 1 || console.getStringCapability(Capability.delete_line) == null)) {
            console.puts(Capability.parm_delete_line, nb);
        } else {
            for (int i = 0; i < nb; i++) {
                console.puts(Capability.delete_line);
            }
        }
    }

    protected void insertLines(int nb) {
        if (console.getStringCapability(Capability.parm_insert_line) != null
                && (nb > 1 || console.getStringCapability(Capability.insert_line) == null)) {
            console.puts(Capability.parm_insert_line, nb);
        } else {
            for (int i = 0; i < nb; i++) {
                console.puts(Capability.insert_line);
            }
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

    private boolean cursorDownIsNewLine() {
        try {
            StringWriter sw = new StringWriter();
            String d = console.getStringCapability(Capability.cursor_down);
            if (d != null) {
                Curses.tputs(sw, d);
                return sw.toString().equals("\n");
            }
        } catch (IOException e) {
        }
        return false;
    }

    protected int moveVisualCursorTo(int i1) {
        int i0 = cursorPos;
        if (i0 == i1) return i1;
        int width = columns;
        int l0 = i0 / width;
        int c0 = i0 % width;
        int l1 = i1 / width;
        int c1 = i1 % width;
        if (l0 == l1 + 1) {
            if (!console.puts(Capability.cursor_up)) {
                console.puts(Capability.parm_up_cursor, 1);
            }
        } else if (l0 > l1) {
            if (!console.puts(Capability.parm_up_cursor, l0 - l1)) {
                for (int i = l1; i < l0; i++) {
                    console.puts(Capability.cursor_up);
                }
            }
        } else if (l0 < l1) {
            if (fullScreen) {
                if (!console.puts(Capability.parm_down_cursor, l1 - l0)) {
                    for (int i = l0; i < l1; i++) {
                        console.puts(Capability.cursor_down);
                    }
                    if (cursorDownIsNewLine()) {
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
        } else if (c0 == c1 - 1) {
            console.puts(Capability.cursor_right);
        } else if (c0 == c1 + 1) {
            console.puts(Capability.cursor_left);
        } else if (c0 < c1) {
            if (!console.puts(Capability.parm_right_cursor, c1 - c0)) {
                for (int i = c0; i < c1; i++) {
                    console.puts(Capability.cursor_right);
                }
            }
        } else if (c0 > c1) {
            if (!console.puts(Capability.parm_left_cursor, c0 - c1)) {
                for (int i = c1; i < c0; i++) {
                    console.puts(Capability.cursor_left);
                }
            }
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

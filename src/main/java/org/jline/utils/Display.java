/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

/**
 * Handle display and visual cursor.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class Display {

    protected final Terminal terminal;
    protected final boolean fullScreen;
    protected List<AttributedString> oldLines = Collections.emptyList();
    protected int cursorPos;
    protected boolean cursorOk;
    protected int columns;
    protected int rows;
    protected boolean reset;

    protected final Map<Capability, Integer> cost = new HashMap<>();
    protected final boolean canScroll;
    protected final boolean noWrapAtEol;
    protected final boolean cursorDownIsNewLine;

    public Display(Terminal terminal, boolean fullscreen) {
        this.terminal = terminal;
        this.fullScreen = fullscreen;

        this.canScroll = can(Capability.insert_line, Capability.parm_insert_line)
                            && can(Capability.delete_line, Capability.parm_delete_line);
        this.noWrapAtEol = terminal.getBooleanCapability(Capability.auto_right_margin)
                            && terminal.getBooleanCapability(Capability.eat_newline_glitch);
        this.cursorDownIsNewLine = "\n".equals(tput(Capability.cursor_down));
    }

    public void resize(int rows, int columns) {
        if (this.rows != rows || this.columns != columns) {
            this.rows = rows;
            this.columns = columns;
            oldLines = AttributedString.join(new AttributedString("\n"), oldLines).columnSplitLength(columns);
        }
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

    public void updateAnsi(List<String> newLines, int targetCursorPos) {
        update(newLines.stream().map(AttributedString::fromAnsi).collect(Collectors.toList()), targetCursorPos);
    }

    /**
     * Update the display according to the new lines
     */
    public void update(List<AttributedString> newLines, int targetCursorPos) {
        if (reset) {
            terminal.puts(Capability.clear_screen);
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
            List<AttributedString> o1 = newLines.subList(nbHeaders, newLines.size() - nbFooters);
            List<AttributedString> o2 = oldLines.subList(nbHeaders, oldLines.size() - nbFooters);
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
                            oldLines.add(nbHeaders + s1 + sl, new AttributedString(""));
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
                        oldLines.add(nbHeaders + s2, new AttributedString(""));
                    }
                }
            }
        }

        int lineIndex = 0;
        int currentPos = 0;
        while (lineIndex < Math.min(oldLines.size(), newLines.size())) {
            AttributedString oldLine = oldLines.get(lineIndex);
            AttributedString newLine = newLines.get(lineIndex);

            List<DiffHelper.Diff> diffs = DiffHelper.diff(oldLine, newLine);
            boolean ident = true;
            boolean cleared = false;
            int curCol = currentPos;
            for (int i = 0; i < diffs.size(); i++) {
                DiffHelper.Diff diff = diffs.get(i);
                int width = diff.text.columnLength();
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
                                && width == diffs.get(i + 1).text.columnLength()) {
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
                            if (currentPos + diffs.get(i + 1).text.columnLength() < columns) {
                                moveVisualCursorTo(currentPos);
                                if (deleteChars(width)) {
                                    break;
                                }
                            }
                        }
                        int oldLen = oldLine.columnLength();
                        int newLen = newLine.columnLength();
                        int nb = Math.max(oldLen, newLen) - currentPos;
                        moveVisualCursorTo(currentPos);
                        if (!terminal.puts(Capability.clr_eol)) {
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
                terminal.puts(Capability.carriage_return); // CR / not newline.
                cursorPos = curCol;
                cursorOk = true;
            }
            if (lineIndex < Math.max(oldLines.size(), newLines.size())) {
                currentPos = curCol + columns;
            } else {
                currentPos = curCol + newLine.columnLength();
            }
        }
        while (lineIndex < Math.max(oldLines.size(), newLines.size())) {
            moveVisualCursorTo(currentPos);
            if (lineIndex < oldLines.size()) {
                if (terminal.getStringCapability(Capability.clr_eol) != null) {
                    terminal.puts(Capability.clr_eol);
                } else {
                    int nb = newLines.get(lineIndex).columnLength();
                    rawPrint(' ', nb);
                    cursorPos += nb;
                    cursorOk = false;
                }
            } else {
                rawPrint(newLines.get(lineIndex));
                cursorPos += newLines.get(lineIndex).columnLength();
                cursorOk = false;
            }
            if (!cursorOk && noWrapAtEol && cursorPos == currentPos + columns) {
                terminal.puts(Capability.carriage_return); // CR / not newline.
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
        return terminal.getStringCapability(single) != null
                || terminal.getStringCapability(multi) != null;
    }

    protected boolean perform(Capability single, Capability multi, int nb) {
        boolean hasMulti = terminal.getStringCapability(multi) != null;
        boolean hasSingle = terminal.getStringCapability(single) != null;
        if (hasMulti && (!hasSingle || cost(single) * nb > cost(multi))) {
            terminal.puts(multi, nb);
            return true;
        } else if (hasSingle) {
            for (int i = 0; i < nb; i++) {
                terminal.puts(single);
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
            String d = terminal.getStringCapability(cap);
            if (d != null) {
                Curses.tputs(sw, d, params);
                return sw.toString();
            }
            return null;
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static int[] longestCommon(List<AttributedString> l1, List<AttributedString> l2) {
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
                if (!terminal.puts(Capability.parm_down_cursor, l1 - l0)) {
                    for (int i = l0; i < l1; i++) {
                        terminal.puts(Capability.cursor_down);
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
            terminal.puts(Capability.carriage_return);
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
        terminal.writer().write(c);
    }

    void rawPrint(AttributedString str) {
        terminal.writer().write(str.toAnsi(terminal));
    }

    public int wcwidth(String str) {
        return AttributedString.fromAnsi(str).columnLength();
    }

}

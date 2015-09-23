/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.util.Collections;
import java.util.List;

import org.jline.Console;
import org.jline.utils.AnsiHelper;
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
    protected List<String> oldLines = Collections.emptyList();
    protected int cursorPos;
    protected boolean cursorOk;
    protected int columns;
    protected int tabWidth = ConsoleReaderImpl.TAB_WIDTH;

    public Display(Console console) {
        this.console = console;
    }

    public void setColumns(int columns) {
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
     * Update the display according to the new lines
     *
     * TODO: use scrolling if appropriate
     */
    public void update(List<String> newLines, int targetCursorPos) {
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
                int width = wcwidth(AnsiHelper.strip(diff.text), currentPos);
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
                                && width == wcwidth(AnsiHelper.strip(diffs.get(i + 1).text), currentPos)) {
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
                            if (currentPos + wcwidth(diffs.get(i + 1).text, cursorPos) < columns) {
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
                        int oldLen = wcwidth(oldLine, 0);
                        int newLen = wcwidth(newLine, 0);
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
                    && lineIndex == Math.max(oldLines.size(), newLines.size()) - 1
                    && cursorPos > curCol && cursorPos % columns == 0) {
                rawPrint(' '); // move cursor to next line by printing dummy space
                console.puts(Capability.carriage_return); // CR / not newline.
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
                    int nb = wcwidth(AnsiHelper.strip(newLines.get(lineIndex)), cursorPos);
                    rawPrint(' ', nb);
                    cursorPos += nb;
                }
            } else {
                rawPrint(newLines.get(lineIndex));
                cursorPos += wcwidth(AnsiHelper.strip(newLines.get(lineIndex)), cursorPos);
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

    public int wcwidth(String str) {
        return wcwidth(AnsiHelper.strip(str), 0);
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
            console.puts(Capability.carriage_return);
            rawPrint('\n', l1 - l0);
            c0 = 0;
        }
        if (c0 == c1 - 1) {
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

    int wcwidth(CharSequence str, int pos) {
        return wcwidth(str, 0, str.length(), pos);
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

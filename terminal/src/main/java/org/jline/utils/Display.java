/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.io.IOError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jline.terminal.Sized;
import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

/**
 * Manages terminal display and efficient screen updates with cursor positioning.
 *
 * <p>
 * The Display class provides functionality for managing the display of content on
 * the terminal screen. It handles the complexities of cursor positioning, line wrapping,
 * and efficient screen updates to minimize the amount of data sent to the terminal.
 * </p>
 *
 * <p>
 * This class supports two main modes of operation:
 * </p>
 * <ul>
 *   <li><b>Full-screen mode</b> - Takes over the entire terminal screen</li>
 *   <li><b>Partial-screen mode</b> - Updates only a portion of the screen, preserving content above</li>
 * </ul>
 *
 * <p>
 * Key features include:
 * </p>
 * <ul>
 *   <li>Efficient screen updates using cursor positioning</li>
 *   <li>Support for multi-line content with proper wrapping</li>
 *   <li>Handling of ANSI-styled text (colors, attributes)</li>
 *   <li>Size-aware rendering that adapts to terminal dimensions</li>
 *   <li>Cursor positioning relative to the display area</li>
 * </ul>
 *
 * <p>
 * This class is used by various JLine components, such as LineReader, to provide
 * efficient terminal display management for features like command-line editing,
 * completion menus, and status messages.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * <b>This class is NOT thread-safe</b> and must be accessed from a single thread or with
 * external synchronization. The Display class maintains mutable state including cursor
 * position, screen content, and terminal dimensions that can be corrupted by concurrent access.
 * </p>
 * <p>
 * Components that use Display in multi-threaded environments (such as signal handlers for
 * window resize events) must provide their own synchronization. For example, the LineReader
 * uses a ReentrantLock to coordinate access between the main thread and signal handlers.
 * </p>
 * <p>
 * <b>Warning:</b> Concurrent access to Display methods may result in:
 * </p>
 * <ul>
 *   <li>ConcurrentModificationException</li>
 *   <li>Corrupted terminal output</li>
 *   <li>Inconsistent cursor positioning</li>
 *   <li>Race conditions in screen updates</li>
 * </ul>
 */
public class Display implements Sized {

    protected final Terminal terminal;
    protected final boolean fullScreen;
    protected List<AttributedString> oldLines = Collections.emptyList();
    protected int cursorPos;
    protected int columns;
    protected int columns1; // columns+1
    protected int rows;
    protected boolean reset;
    protected boolean delayLineWrap;

    protected final Map<Capability, Integer> cost = new HashMap<>();
    protected final boolean canScroll;
    protected final boolean terminalWrapAtEol;
    protected final boolean terminalDelayedWrapAtEol;
    protected boolean wrapAtEol;
    protected boolean delayedWrapAtEol;
    protected final boolean cursorDownIsNewLine;

    // Byte-mode fields: when the terminal uses UTF-8, we accumulate all output
    // in a ByteArrayBuilder and write directly to terminal.output(), bypassing
    // PrintWriter/OutputStreamWriter overhead.
    private ByteArrayBuilder byteBuilder;
    private Appendable byteAppendable;
    private boolean useByteMode;
    private int ansiColors;
    private AttributedCharSequence.ForceMode ansiForceMode;
    private ColorPalette ansiPalette;
    private String ansiAltIn;
    private String ansiAltOut;

    /**
     * Create a Display bound to the given Terminal and configured for either full-screen
     * or inline (partial-screen) usage.
     *
     * <p>Queries the terminal for capabilities and initializes internal flags that
     * control scrolling, wrap-at-end-of-line behavior, and cursor movement semantics.
     *
     * @param terminal the target terminal used for rendering
     * @param fullscreen true to enable full-screen (application-takes-over) mode, false for partial-screen mode
     */
    @SuppressWarnings("this-escape")
    public Display(Terminal terminal, boolean fullscreen) {
        this.terminal = terminal;
        this.fullScreen = fullscreen;

        this.canScroll = can(Capability.insert_line, Capability.parm_insert_line)
                && can(Capability.delete_line, Capability.parm_delete_line);
        this.terminalWrapAtEol = terminal.getBooleanCapability(Capability.auto_right_margin);
        this.terminalDelayedWrapAtEol =
                this.terminalWrapAtEol && terminal.getBooleanCapability(Capability.eat_newline_glitch);
        this.wrapAtEol = this.terminalWrapAtEol;
        this.delayedWrapAtEol = this.terminalDelayedWrapAtEol;
        this.cursorDownIsNewLine = "\n".equals(Curses.tputs(terminal.getStringCapability(Capability.cursor_down)));
    }

    /**
     * If cursor is at right margin, don't wrap immediately.
     * See <code>org.jline.reader.LineReader.Option#DELAY_LINE_WRAP</code>.
     * @return <code>true</code> if line wrap is delayed, <code>false</code> otherwise
     */
    public boolean delayLineWrap() {
        return delayLineWrap;
    }

    /**
     * Enable or disable delayed line wrapping when the cursor reaches the right margin.
     *
     * @param v `true` to delay wrapping at end-of-line, `false` to disable delayed wrapping
     */
    public void setDelayLineWrap(boolean v) {
        delayLineWrap = v;
    }

    /**
     * Resize the display to the dimensions specified by the given Size.
     *
     * @param sized the target display dimensions; its rows and columns are applied to the display
     */
    public void resize(Sized sized) {
        resize(sized.getRows(), sized.getColumns());
    }

    /**
     * Resize the display to the specified number of rows and columns.
     *
     * This updates the display geometry, rewraps previously rendered lines to the new
     * width, and adjusts wrap-at-EOL behavior based on the terminal's buffer width.
     * If either dimension is zero the method treats it as a special case (sets rows
     * to 1 and columns to a very large internal value) to avoid a zero-sized display.
     *
     * @param rows    the number of display rows
     * @param columns the number of display columns
     * @deprecated Use {@link #resize(Sized)} instead to avoid parameter order confusion.
     */
    @Deprecated
    @SuppressWarnings("java:S1133") // Intentional deprecation; removal planned for a future major version
    public void resize(int rows, int columns) {
        if (rows == 0 || columns == 0) {
            columns = Integer.MAX_VALUE - 1;
            rows = 1;
        }
        if (this.rows != rows || this.columns != columns) {
            this.rows = rows;
            this.columns = columns;
            this.columns1 = columns + 1;
            oldLines = AttributedString.join(AttributedString.EMPTY, oldLines)
                    .columnSplitLength(columns, true, delayLineWrap(), terminal);
        }
        // When the terminal buffer is wider than the visible window (e.g. Windows with
        // a wide screen buffer), auto-wrap occurs at the buffer width, not the visible
        // width. Disable wrap-at-eol reliance since content won't reach the buffer edge.
        int bufferColumns = terminal.getBufferSize().getColumns();
        if (bufferColumns > columns) {
            this.wrapAtEol = false;
            this.delayedWrapAtEol = false;
        } else {
            this.wrapAtEol = this.terminalWrapAtEol;
            this.delayedWrapAtEol = this.terminalDelayedWrapAtEol;
        }
    }

    /**
     * Get the current display width in character cells.
     *
     * @return the number of columns (display width)
     */
    @Override
    public int getColumns() {
        return columns;
    }

    /**
     * The current display height in character rows.
     *
     * @return the current number of rows
     */
    @Override
    public int getRows() {
        return rows;
    }

    /**
     * Clears the cached model of previously rendered lines.
     *
     * <p>The next {@link #update} call will treat all content as new and repaint
     * every line via the diff algorithm. This does <em>not</em> issue a terminal
     * {@code clear_screen}; to also clear the physical screen, call {@link #clear()}
     * before {@code reset()}.</p>
     */
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
     * Update the display according to the new lines and flushes the output.
     * @param newLines the lines to display
     * @param targetCursorPos desired cursor position - see Size.cursorPos.
     */
    public void update(List<AttributedString> newLines, int targetCursorPos) {
        update(newLines, targetCursorPos, true);
    }

    /**
     * Update the display according to the new lines.
     * @param newLines the lines to display
     * @param targetCursorPos desired cursor position - see Size.cursorPos.
     * @param flush whether the output should be flushed or not
     */
    public void update(List<AttributedString> newLines, int targetCursorPos, boolean flush) {
        // Set up byte mode: accumulate all output in a byte buffer for UTF-8 terminals.
        // This avoids String allocations and charset encoding for ANSI escape sequences.
        Integer cols = terminal.getNumericCapability(Capability.max_colors);
        useByteMode = (cols != null && cols >= 8) && StandardCharsets.UTF_8.equals(terminal.outputEncoding());
        if (useByteMode) {
            if (byteBuilder == null) {
                byteBuilder = new ByteArrayBuilder(4096);
                byteAppendable = byteBuilder.asAsciiAppendable();
            } else {
                byteBuilder.reset();
            }
            ansiColors = cols;
            ansiForceMode = AttributedCharSequence.ForceMode.None;
            ansiPalette = terminal.getPalette();
            if (!AttributedCharSequence.DISABLE_ALTERNATE_CHARSET) {
                ansiAltIn = Curses.tputs(terminal.getStringCapability(Capability.enter_alt_charset_mode));
                ansiAltOut = Curses.tputs(terminal.getStringCapability(Capability.exit_alt_charset_mode));
            } else {
                ansiAltIn = null;
                ansiAltOut = null;
            }
        }

        if (reset) {
            puts(Capability.clear_screen);
            oldLines.clear();
            cursorPos = 0;
            reset = false;
        }

        // If dumb display, get rid of ansi sequences now
        if (cols == null || cols < 8) {
            newLines = newLines.stream()
                    .map(s -> new AttributedString(s.toString()))
                    .collect(Collectors.toList());
        }

        // Detect scrolling
        if ((fullScreen || newLines.size() >= rows) && newLines.size() == oldLines.size() && canScroll) {
            int nbHeaders = 0;
            int nbFooters = 0;
            // Find common headers and footers
            int l = newLines.size();
            while (nbHeaders < l && Objects.equals(newLines.get(nbHeaders), oldLines.get(nbHeaders))) {
                nbHeaders++;
            }
            while (nbFooters < l - nbHeaders - 1
                    && Objects.equals(
                            newLines.get(newLines.size() - nbFooters - 1),
                            oldLines.get(oldLines.size() - nbFooters - 1))) {
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
                    moveVisualCursorTo((nbHeaders + s1) * columns1);
                    int nb = s2 - s1;
                    deleteLines(nb);
                    for (int i = 0; i < nb; i++) {
                        oldLines.remove(nbHeaders + s1);
                    }
                    if (nbFooters > 0) {
                        moveVisualCursorTo((nbHeaders + s1 + sl) * columns1);
                        insertLines(nb);
                        for (int i = 0; i < nb; i++) {
                            oldLines.add(nbHeaders + s1 + sl, new AttributedString(""));
                        }
                    }
                } else if (sl > 1 && s1 > s2) {
                    int nb = s1 - s2;
                    if (nbFooters > 0) {
                        moveVisualCursorTo((nbHeaders + s2 + sl) * columns1);
                        deleteLines(nb);
                        for (int i = 0; i < nb; i++) {
                            oldLines.remove(nbHeaders + s2 + sl);
                        }
                    }
                    moveVisualCursorTo((nbHeaders + s2) * columns1);
                    insertLines(nb);
                    for (int i = 0; i < nb; i++) {
                        oldLines.add(nbHeaders + s2, new AttributedString(""));
                    }
                }
            }
        }

        int lineIndex = 0;
        int currentPos = 0;
        int numLines = Math.min(rows, Math.max(oldLines.size(), newLines.size()));
        boolean wrapNeeded = false;
        while (lineIndex < numLines) {
            AttributedString oldLine = lineIndex < oldLines.size() ? oldLines.get(lineIndex) : AttributedString.EMPTY;
            AttributedString newLine = lineIndex < newLines.size() ? newLines.get(lineIndex) : AttributedString.EMPTY;
            currentPos = lineIndex * columns1;
            int curCol = currentPos;
            int oldLength = oldLine.length();
            int newLength = newLine.length();
            boolean oldNL = oldLength > 0 && oldLine.charAt(oldLength - 1) == '\n';
            boolean newNL = newLength > 0 && newLine.charAt(newLength - 1) == '\n';
            if (oldNL) {
                oldLength--;
                oldLine = oldLine.substring(0, oldLength);
            }
            if (newNL) {
                newLength--;
                newLine = newLine.substring(0, newLength);
            }
            if (wrapNeeded && lineIndex == (cursorPos + 1) / columns1 && lineIndex < newLines.size()) {
                // move from right margin to next line's left margin
                cursorPos++;
                if (newLength == 0 || newLine.isHidden(0)) {
                    // go to next line column zero
                    rawPrint(' ');
                    puts(Capability.cursor_left);
                } else {
                    AttributedString firstChar = newLine.substring(0, 1);
                    // go to next line column one
                    rawPrint(firstChar);
                    cursorPos += firstChar.columnLength(terminal); // normally 1
                    newLine = newLine.substring(1, newLength);
                    newLength--;
                    if (oldLength > 0) {
                        oldLine = oldLine.substring(1, oldLength);
                        oldLength--;
                    }
                    currentPos = cursorPos;
                }
            }
            // When grapheme cluster mode is active, the terminal may retroactively
            // combine or uncombine characters as ZWJ and other combining code points
            // are added incrementally. This invalidates cursor position tracking
            // based on char-level diffs. Force a full line repaint in this case.
            if (terminal.getGraphemeClusterMode() && !oldLine.equals(newLine)) {
                cursorPos = moveVisualCursorTo(currentPos);
                if (!puts(Capability.clr_eol)) {
                    int oldLen = oldLine.columnLength(terminal);
                    if (oldLen > 0) {
                        rawPrint(' ', oldLen);
                        cursorPos += oldLen;
                        cursorPos = moveVisualCursorTo(currentPos);
                    }
                }
                rawPrint(newLine);
                cursorPos += newLine.columnLength(terminal);
                currentPos = cursorPos;
                lineIndex++;
                boolean newWrap2 = !newNL && lineIndex < newLines.size();
                if (targetCursorPos + 1 == lineIndex * columns1 && (newWrap2 || !delayLineWrap)) targetCursorPos++;
                wrapNeeded = newWrap2;
                continue;
            }
            List<DiffHelper.Diff> diffs = DiffHelper.diff(oldLine, newLine);
            boolean ident = true;
            boolean cleared = false;
            for (int i = 0; i < diffs.size(); i++) {
                DiffHelper.Diff diff = diffs.get(i);
                int width = diff.text.columnLength(terminal);
                switch (diff.operation) {
                    case EQUAL:
                        if (!ident) {
                            cursorPos = moveVisualCursorTo(currentPos);
                            rawPrint(diff.text);
                            cursorPos += width;
                            currentPos = cursorPos;
                        } else {
                            currentPos += width;
                        }
                        break;
                    case INSERT:
                        if (i <= diffs.size() - 2 && diffs.get(i + 1).operation == DiffHelper.Operation.EQUAL) {
                            cursorPos = moveVisualCursorTo(currentPos);
                            if (insertChars(width)) {
                                rawPrint(diff.text);
                                cursorPos += width;
                                currentPos = cursorPos;
                                break;
                            }
                        } else if (i <= diffs.size() - 2
                                && diffs.get(i + 1).operation == DiffHelper.Operation.DELETE
                                && width == diffs.get(i + 1).text.columnLength(terminal)) {
                            moveVisualCursorTo(currentPos);
                            rawPrint(diff.text);
                            cursorPos += width;
                            currentPos = cursorPos;
                            i++; // skip delete
                            break;
                        }
                        moveVisualCursorTo(currentPos);
                        rawPrint(diff.text);
                        cursorPos += width;
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
                        if (i <= diffs.size() - 2 && diffs.get(i + 1).operation == DiffHelper.Operation.EQUAL) {
                            if (currentPos + diffs.get(i + 1).text.columnLength(terminal) < columns) {
                                moveVisualCursorTo(currentPos);
                                if (deleteChars(width)) {
                                    break;
                                }
                            }
                        }
                        int oldLen = oldLine.columnLength(terminal);
                        int newLen = newLine.columnLength(terminal);
                        int nb = Math.max(oldLen, newLen) - (currentPos - curCol);
                        moveVisualCursorTo(currentPos);
                        if (!puts(Capability.clr_eol)) {
                            rawPrint(' ', nb);
                            cursorPos += nb;
                        }
                        cleared = true;
                        ident = false;
                        break;
                }
            }
            lineIndex++;
            boolean newWrap = !newNL && lineIndex < newLines.size();
            if (targetCursorPos + 1 == lineIndex * columns1 && (newWrap || !delayLineWrap)) targetCursorPos++;
            boolean atRight = (cursorPos - curCol) % columns1 == columns;
            wrapNeeded = false;
            if (this.delayedWrapAtEol) {
                boolean oldWrap = !oldNL && lineIndex < oldLines.size();
                if (newWrap != oldWrap && !(oldWrap && cleared)) {
                    moveVisualCursorTo(lineIndex * columns1 - 1, newLines);
                    if (newWrap) wrapNeeded = true;
                    else puts(Capability.clr_eol);
                }
            } else if (atRight) {
                if (this.wrapAtEol) {
                    if (!fullScreen || (fullScreen && lineIndex < numLines)) {
                        rawPrint(' ');
                        puts(Capability.cursor_left);
                        cursorPos++;
                    }
                } else {
                    puts(Capability.carriage_return); // CR / not newline.
                    cursorPos = curCol;
                }
                currentPos = cursorPos;
            }
        }
        if (cursorPos != targetCursorPos) {
            moveVisualCursorTo(targetCursorPos < 0 ? currentPos : targetCursorPos, newLines);
        }
        oldLines = newLines;

        if (useByteMode && byteBuilder.length() > 0) {
            // Flush any pending writer data, then write accumulated bytes
            terminal.writer().flush();
            try {
                byteBuilder.writeTo(terminal.output());
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
        if (flush) {
            if (useByteMode) {
                try {
                    terminal.output().flush();
                } catch (IOException e) {
                    throw new IOError(e);
                }
            } else {
                terminal.flush();
            }
        }
        useByteMode = false;
    }

    /**
     * Emits terminal control sequences to delete the specified number of lines.
     *
     * @param nb the number of lines to delete
     * @return `true` if a delete-line capability was available and the operation was issued, `false` otherwise
     */
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
        return terminal.getStringCapability(single) != null || terminal.getStringCapability(multi) != null;
    }

    /**
     * Emits a terminal capability to affect a repeated action, using the parameterized (multi) form when available and preferable, otherwise repeating the single-capability.
     *
     * @param single the single-invocation capability to use repeatedly if a multi-parameter form is unavailable or not preferable
     * @param multi the multi-parameter capability that can perform the action for a specified count in one invocation
     * @param nb the number of times the action should be applied
     * @return {@code true} if a capability sequence was emitted to perform the action, {@code false} if neither capability is available
     */
    protected boolean perform(Capability single, Capability multi, int nb) {
        boolean hasMulti = terminal.getStringCapability(multi) != null;
        boolean hasSingle = terminal.getStringCapability(single) != null;
        if (hasMulti && (!hasSingle || cost(single) * nb > cost(multi))) {
            puts(multi, nb);
            return true;
        } else if (hasSingle) {
            for (int i = 0; i < nb; i++) {
                puts(single);
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
        String s = Curses.tputs(terminal.getStringCapability(cap), 0);
        return s != null ? s.length() : Integer.MAX_VALUE;
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
        return max != 0 ? new int[] {start1, start2, max} : null;
    }

    /*
     * Move cursor from cursorPos to argument, updating cursorPos
     * We're at the right margin if {@code (cursorPos % columns1) == columns}.
     * This method knows how to move both *from* and *to* the right margin.
     */
    protected void moveVisualCursorTo(int targetPos, List<AttributedString> newLines) {
        if (cursorPos != targetPos) {
            boolean atRight = (targetPos % columns1) == columns;
            moveVisualCursorTo(targetPos - (atRight ? 1 : 0));
            if (atRight) {
                // There is no portable way to move to the right margin
                // except by writing a character in the right-most column.
                int row = targetPos / columns1;
                AttributedString lastChar = row >= newLines.size()
                        ? AttributedString.EMPTY
                        : newLines.get(row).columnSubSequence(columns - 1, columns, terminal);
                if (lastChar.length() == 0) rawPrint((int) ' ');
                else rawPrint(lastChar);
                cursorPos++;
            }
        }
    }

    /**
     * Move the visual cursor to the specified wrapped-line position without allowing movement to a right-margin target.
     *
     * Moves the terminal cursor from the current visual position (stored in {@code cursorPos}) to {@code i1},
     * handling line and column transitions, and updating {@code cursorPos}. If the current position is at the right
     * margin a carriage return is emitted before further movement. The target position must not lie on a right-margin
     * column (i.e. {@code i1 % columns1 != columns}).
     *
     * @param i1 the target visual cursor position in wrapped-line coordinates
     * @return the updated cursor position (equal to {@code i1})
     */
    protected int moveVisualCursorTo(int i1) {
        int i0 = cursorPos;
        if (i0 == i1) return i1;
        int width = columns1;
        int l0 = i0 / width;
        int c0 = i0 % width;
        int l1 = i1 / width;
        int c1 = i1 % width;
        if (c0 == columns) { // at right margin
            puts(Capability.carriage_return);
            c0 = 0;
        }
        if (l0 > l1) {
            perform(Capability.cursor_up, Capability.parm_up_cursor, l0 - l1);
        } else if (l0 < l1) {
            // TODO: clean the following
            if (fullScreen) {
                if (!puts(Capability.parm_down_cursor, l1 - l0)) {
                    for (int i = l0; i < l1; i++) {
                        puts(Capability.cursor_down);
                    }
                    if (cursorDownIsNewLine) {
                        c0 = 0;
                    }
                }
            } else {
                puts(Capability.carriage_return);
                rawPrint('\n', l1 - l0);
                c0 = 0;
            }
        }
        if (c0 != 0 && c1 == 0) {
            puts(Capability.carriage_return);
        } else if (c0 < c1) {
            perform(Capability.cursor_right, Capability.parm_right_cursor, c1 - c0);
        } else if (c0 > c1) {
            perform(Capability.cursor_left, Capability.parm_left_cursor, c0 - c1);
        }
        cursorPos = i1;
        return i1;
    }

    /**
     * Prints the specified character to the terminal output the given number of times.
     *
     * @param c   the character to print
     * @param num the number of times to print {@code c}; if less than or equal to zero, nothing is printed
     */
    void rawPrint(char c, int num) {
        for (int i = 0; i < num; i++) {
            rawPrint(c);
        }
    }

    /**
     * Append or write a single Unicode code point to the terminal output buffer.
     *
     * If byte-mode is enabled, append the code point's UTF-8 bytes to the internal byte buffer;
     * otherwise write the code point to the terminal's writer.
     *
     * @param c the Unicode code point to output
     */
    void rawPrint(int c) {
        if (useByteMode) {
            byteBuilder.appendUtf8(c);
        } else {
            terminal.writer().write(c);
        }
    }

    /**
     * Writes the given attributed string to the display output.
     *
     * When byte-mode is enabled, the string is encoded as ANSI/UTF-8 bytes and appended to the internal output buffer; otherwise it is printed through the terminal's print path.
     *
     * @param str the attributed string to write (may contain styling/ANSI sequences)
     */
    void rawPrint(AttributedString str) {
        if (useByteMode) {
            str.toAnsiBytes(byteBuilder, ansiColors, ansiForceMode, ansiPalette, ansiAltIn, ansiAltOut);
        } else {
            str.print(terminal);
        }
    }

    /**
     * Emit the terminal control sequence for the given capability, using the byte buffer when byte mode is enabled.
     *
     * @param capability the terminal capability to emit
     * @param params parameters to format into the capability string, if applicable
     * @return `true` if the capability sequence was emitted; `false` if the capability string is unavailable
     */
    private boolean puts(Capability capability, Object... params) {
        if (useByteMode) {
            String str = terminal.getStringCapability(capability);
            if (str == null) {
                return false;
            }
            Curses.tputs(byteAppendable, str, params);
            return true;
        } else {
            return terminal.puts(capability, params);
        }
    }

    /**
     * Compute the number of terminal columns required to display a string, interpreting ANSI escape sequences.
     *
     * @param str the input string, which may contain ANSI escape sequences; if `null` it is treated as empty
     * @return the displayed column width of the string (0 if `str` is `null`)
     */
    public int wcwidth(String str) {
        return str != null ? AttributedString.fromAnsi(str).columnLength(terminal) : 0;
    }
}

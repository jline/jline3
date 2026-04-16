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
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jline.terminal.Size;
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
public class Display {

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

    // Pre-allocated reusable arrays for zero-allocation update path
    private final int[] diffResult = new int[2];
    private final int[] lcsResult = new int[3];
    // Style state: [0]=current style, [1]=alt charset (0/1)
    private final long[] ansiColorState = new long[2];
    private final BreakIterator graphemeBreakIterator;
    private final WCWidth.CharSequenceCharacterIterator graphemeCharIterator;
    private final boolean hasCursorAddress;
    private final boolean canSkipIntraLine;

    /*
     * Minimum number of unchanged characters within a changed region that justifies
     * a cursor-movement skip instead of re-emitting them.  A CUF escape (\e[nC)
     * costs 4-6 bytes, so gaps shorter than this are cheaper to re-emit inline.
     */
    private static final int INTRA_LINE_SKIP_THRESHOLD = 8;

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
        if (!AttributedCharSequence.DISABLE_ALTERNATE_CHARSET) {
            this.ansiAltIn = Curses.tputs(terminal.getStringCapability(Capability.enter_alt_charset_mode));
            this.ansiAltOut = Curses.tputs(terminal.getStringCapability(Capability.exit_alt_charset_mode));
        }
        if (WCWidth.HAS_JDK_GRAPHEME_SUPPORT) {
            this.graphemeBreakIterator = BreakIterator.getCharacterInstance();
            this.graphemeCharIterator = new WCWidth.CharSequenceCharacterIterator();
        } else {
            this.graphemeBreakIterator = null;
            this.graphemeCharIterator = null;
        }
        this.hasCursorAddress = fullScreen && terminal.getStringCapability(Capability.cursor_address) != null;
        this.canSkipIntraLine = terminal.getStringCapability(Capability.parm_right_cursor) != null;
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
     * @param size the target display dimensions; its rows and columns are applied to the display
     */
    public void resize(Size size) {
        resize(size.getRows(), size.getColumns());
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
     * @deprecated Use {@link #resize(Size)} instead to avoid parameter order confusion.
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
                    .columnSplitLength(terminal, columns, true, delayLineWrap());
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
    public int getColumns() {
        return columns;
    }

    /**
     * The current display height in character rows.
     *
     * @return the current number of rows
     */
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

    /**
     * Update the display with lines containing ANSI escape sequences and flush the output.
     *
     * <p>Each string in the list is parsed via {@link AttributedString#fromAnsi(String)}
     * to convert ANSI escape codes into styled {@link AttributedString}s, then delegated
     * to {@link #update(List, int)}.</p>
     *
     * @param newLines         the new lines to display, with embedded ANSI escape sequences
     * @param targetCursorPos  the desired cursor position after the update (0-based character offset
     *                         from the start of the first line, or -1 to leave the cursor at the end)
     */
    public void updateAnsi(List<String> newLines, int targetCursorPos) {
        List<AttributedString> attrLines = new ArrayList<>(newLines.size());
        for (int i = 0; i < newLines.size(); i++) {
            attrLines.add(AttributedString.fromAnsi(newLines.get(i)));
        }
        update(attrLines, targetCursorPos);
    }

    /**
     * Update the display according to the new lines and flushes the output.
     *
     * @param newLines the lines to display
     * @param targetCursorPos desired cursor position - see Size.cursorPos.
     */
    public void update(List<AttributedString> newLines, int targetCursorPos) {
        update(newLines, targetCursorPos, true);
    }

    /**
     * Update the display according to the new lines.
     *
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
            // ansiAltIn / ansiAltOut are cached from the constructor
            // Reset style state for this update cycle
            ansiColorState[0] = 0;
            ansiColorState[1] = 0;
        }

        if (reset) {
            puts(Capability.clear_screen);
            oldLines.clear();
            cursorPos = 0;
            reset = false;
        }

        // If dumb display, get rid of ansi sequences now
        if (cols == null || cols < 8) {
            List<AttributedString> stripped = new ArrayList<>(newLines.size());
            for (int idx = 0; idx < newLines.size(); idx++) {
                stripped.add(new AttributedString(newLines.get(idx).toString()));
            }
            newLines = stripped;
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
            int fromIndex = nbHeaders;
            int toIndex = newLines.size() - nbFooters;
            longestCommon(newLines, fromIndex, toIndex, oldLines, fromIndex, toIndex);
            if (lcsResult[2] > 0) {
                int s1 = lcsResult[0];
                int s2 = lcsResult[1];
                int sl = lcsResult[2];
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
                            oldLines.add(nbHeaders + s1 + sl, AttributedString.EMPTY);
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
                        oldLines.add(nbHeaders + s2, AttributedString.EMPTY);
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
            // Track effective ranges instead of creating substrings
            int oStart = 0;
            int oEnd = oldLine.length();
            int nStart = 0;
            int nEnd = newLine.length();
            boolean oldNL = oEnd > 0 && oldLine.charAt(oEnd - 1) == '\n';
            boolean newNL = nEnd > 0 && newLine.charAt(nEnd - 1) == '\n';
            if (oldNL) {
                oEnd--;
            }
            if (newNL) {
                nEnd--;
            }
            if (wrapNeeded && lineIndex == (cursorPos + 1) / columns1 && lineIndex < newLines.size()) {
                // move from right margin to next line's left margin
                cursorPos++;
                if (nEnd - nStart == 0 || newLine.isHidden(nStart)) {
                    // go to next line column zero
                    ensureDefaultAnsiStyle();
                    rawPrint(' ');
                    puts(Capability.cursor_left);
                } else {
                    // go to next line column one
                    rawPrint(newLine, nStart, nStart + 1);
                    cursorPos += newLine.columnLength(
                            terminal, graphemeBreakIterator, graphemeCharIterator, nStart, nStart + 1); // normally 1
                    nStart++;
                    if (oEnd - oStart > 0) {
                        oStart++;
                    }
                    currentPos = cursorPos;
                }
            }
            int oLen = oEnd - oStart;
            int nLen = nEnd - nStart;
            // When grapheme cluster mode is active, the terminal may retroactively
            // combine or uncombine characters as ZWJ and other combining code points
            // are added incrementally. This invalidates cursor position tracking
            // based on char-level diffs. Force a full line repaint in this case.
            if (terminal.getGraphemeClusterMode() && !equalsRange(oldLine, oStart, oEnd, newLine, nStart, nEnd)) {
                cursorPos = moveVisualCursorTo(currentPos);
                ensureDefaultAnsiStyle();
                if (!puts(Capability.clr_eol)) {
                    int oldLen =
                            oldLine.columnLength(terminal, graphemeBreakIterator, graphemeCharIterator, oStart, oEnd);
                    if (oldLen > 0) {
                        rawPrint(' ', oldLen);
                        cursorPos += oldLen;
                        cursorPos = moveVisualCursorTo(currentPos);
                    }
                }
                rawPrint(newLine, nStart, nEnd);
                cursorPos += newLine.columnLength(terminal, graphemeBreakIterator, graphemeCharIterator, nStart, nEnd);
                currentPos = cursorPos;
                lineIndex++;
                boolean newWrap2 = !newNL && lineIndex < newLines.size();
                if (targetCursorPos + 1 == lineIndex * columns1 && (newWrap2 || !delayLineWrap)) targetCursorPos++;
                wrapNeeded = newWrap2;
                continue;
            }
            // Inline diff: compute common prefix/suffix lengths without allocation
            DiffHelper.diff(oldLine, oStart, oEnd, newLine, nStart, nEnd, diffResult);
            int cs = diffResult[0]; // common prefix length
            int ce = diffResult[1]; // common suffix length
            boolean hasInsert = nLen > cs + ce;
            boolean hasDelete = oLen > cs + ce;
            boolean hasEqSuffix = ce > 0;
            boolean ident = true;
            boolean cleared = false;
            // EQUAL prefix
            if (cs > 0) {
                currentPos += oldLine.columnLength(
                        terminal, graphemeBreakIterator, graphemeCharIterator, oStart, oStart + cs);
            }
            // INSERT
            if (hasInsert) {
                int iStart = nStart + cs;
                int iEnd = nEnd - ce;
                int insertWidth =
                        newLine.columnLength(terminal, graphemeBreakIterator, graphemeCharIterator, iStart, iEnd);
                boolean insertHandled = false;
                // Optimization: if followed by EQUAL suffix (no DELETE), try insertChars
                if (hasEqSuffix && !hasDelete) {
                    cursorPos = moveVisualCursorTo(currentPos);
                    ensureDefaultAnsiStyle();
                    if (insertChars(insertWidth)) {
                        rawPrint(newLine, iStart, iEnd);
                        cursorPos += insertWidth;
                        currentPos = cursorPos;
                        insertHandled = true;
                    }
                }
                // Optimization: if followed by DELETE with same width, overwrite
                if (!insertHandled && hasDelete) {
                    int dStart = oStart + cs;
                    int dEnd = oEnd - ce;
                    int deleteWidth =
                            oldLine.columnLength(terminal, graphemeBreakIterator, graphemeCharIterator, dStart, dEnd);
                    if (insertWidth == deleteWidth) {
                        moveVisualCursorTo(currentPos);
                        if (canSkipIntraLine && (iEnd - iStart) == (dEnd - dStart)) {
                            emitOverwriteWithSkips(newLine, iStart, iEnd, oldLine, dStart);
                        } else {
                            rawPrint(newLine, iStart, iEnd);
                            cursorPos += insertWidth;
                        }
                        currentPos = cursorPos;
                        hasDelete = false; // skip DELETE processing
                        insertHandled = true;
                    }
                }
                // Default: just print
                if (!insertHandled) {
                    moveVisualCursorTo(currentPos);
                    rawPrint(newLine, iStart, iEnd);
                    cursorPos += insertWidth;
                    currentPos = cursorPos;
                    ident = false;
                }
            }
            // DELETE
            if (hasDelete && !cleared && currentPos - curCol < columns) {
                int dStart = oStart + cs;
                int dEnd = oEnd - ce;
                int deleteWidth =
                        oldLine.columnLength(terminal, graphemeBreakIterator, graphemeCharIterator, dStart, dEnd);
                boolean deleteHandled = false;
                // Optimization: if followed by EQUAL suffix, try deleteChars
                if (hasEqSuffix) {
                    int suffixWidth = oldLine.columnLength(
                            terminal, graphemeBreakIterator, graphemeCharIterator, oEnd - ce, oEnd);
                    if ((currentPos - curCol) + suffixWidth < columns) {
                        moveVisualCursorTo(currentPos);
                        ensureDefaultAnsiStyle();
                        if (deleteChars(deleteWidth)) {
                            deleteHandled = true;
                        }
                    }
                }
                if (!deleteHandled) {
                    int oldColLen =
                            oldLine.columnLength(terminal, graphemeBreakIterator, graphemeCharIterator, oStart, oEnd);
                    int newColLen =
                            newLine.columnLength(terminal, graphemeBreakIterator, graphemeCharIterator, nStart, nEnd);
                    int nb = Math.max(oldColLen, newColLen) - (currentPos - curCol);
                    moveVisualCursorTo(currentPos);
                    ensureDefaultAnsiStyle();
                    if (!puts(Capability.clr_eol)) {
                        rawPrint(' ', nb);
                        cursorPos += nb;
                    }
                    cleared = true;
                    ident = false;
                }
            }
            // EQUAL suffix
            if (hasEqSuffix) {
                int suffixWidth =
                        oldLine.columnLength(terminal, graphemeBreakIterator, graphemeCharIterator, oEnd - ce, oEnd);
                if (!ident) {
                    cursorPos = moveVisualCursorTo(currentPos);
                    rawPrint(newLine, nEnd - ce, nEnd);
                    cursorPos += suffixWidth;
                    currentPos = cursorPos;
                } else {
                    currentPos += suffixWidth;
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
                    else {
                        ensureDefaultAnsiStyle();
                        puts(Capability.clr_eol);
                    }
                }
            } else if (atRight) {
                if (this.wrapAtEol) {
                    if (!fullScreen || (fullScreen && lineIndex < numLines)) {
                        ensureDefaultAnsiStyle();
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
        ensureDefaultAnsiStyle();

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

    /*
     * Compare ranges of two attributed strings for equality without allocation.
     */
    private static boolean equalsRange(
            AttributedCharSequence a, int aStart, int aEnd, AttributedCharSequence b, int bStart, int bEnd) {
        if (aEnd - aStart != bEnd - bStart) return false;
        int offA = a.offset() + aStart;
        int offB = b.offset() + bStart;
        int endA = a.offset() + aEnd;
        return Arrays.equals(a.buffer(), offA, endA, b.buffer(), offB, offB + (aEnd - aStart))
                && Arrays.equals(a.styleBuffer(), offA, endA, b.styleBuffer(), offB, offB + (aEnd - aStart));
    }

    /*
     * Emit the changed region of a same-width overwrite, using cursor movement
     * to skip runs of unchanged characters that are at least
     * INTRA_LINE_SKIP_THRESHOLD characters long.
     *
     * Both regions must have the same character count (not just the same
     * display width).  When they differ, the caller should fall back to
     * rawPrint(newLine, iStart, iEnd).
     */
    private void emitOverwriteWithSkips(
            AttributedString newLine, int iStart, int iEnd, AttributedString oldLine, int dStart) {
        int len = iEnd - iStart;
        int pendingStart = -1; // start offset of segment to emit (-1 = none)
        int pos = 0;

        while (pos < len) {
            // Detect run of unchanged characters
            int runStart = pos;
            while (pos < len
                    && newLine.charAt(iStart + pos) == oldLine.charAt(dStart + pos)
                    && newLine.styleCodeAt(iStart + pos) == oldLine.styleCodeAt(dStart + pos)) {
                pos++;
            }
            int unchangedLen = pos - runStart;

            if (unchangedLen >= INTRA_LINE_SKIP_THRESHOLD) {
                // Gap is long enough to skip — first emit any pending content
                pendingStart = flushPending(newLine, iStart, pendingStart, runStart);
                // Skip the gap with cursor movement
                int gapWidth = newLine.columnLength(
                        terminal, graphemeBreakIterator, graphemeCharIterator, iStart + runStart, iStart + pos);
                moveVisualCursorTo(cursorPos + gapWidth);
            } else if (unchangedLen > 0 && pendingStart < 0) {
                // Gap too short or at end — include in pending segment
                pendingStart = runStart;
            }

            // Advance past the next changed character (if any)
            if (pos < len) {
                if (pendingStart < 0) pendingStart = pos;
                pos++;
            }
        }

        // Emit any remaining content
        if (pendingStart >= 0) {
            rawPrint(newLine, iStart + pendingStart, iEnd);
            cursorPos += newLine.columnLength(
                    terminal, graphemeBreakIterator, graphemeCharIterator, iStart + pendingStart, iEnd);
        }
    }

    /*
     * Emit the pending segment [pendingStart, segEnd) if any, and return -1.
     */
    private int flushPending(AttributedString line, int lineStart, int pendingStart, int segEnd) {
        if (pendingStart >= 0) {
            rawPrint(line, lineStart + pendingStart, lineStart + segEnd);
            cursorPos += line.columnLength(
                    terminal,
                    graphemeBreakIterator,
                    graphemeCharIterator,
                    lineStart + pendingStart,
                    lineStart + segEnd);
        }
        return -1;
    }

    private void longestCommon(
            List<AttributedString> l1, int from1, int to1, List<AttributedString> l2, int from2, int to2) {
        int start1 = 0;
        int start2 = 0;
        int max = 0;
        for (int i = from1; i < to1; i++) {
            for (int j = from2; j < to2; j++) {
                int x = 0;
                while (Objects.equals(l1.get(i + x), l2.get(j + x))) {
                    x++;
                    if (((i + x) >= to1) || ((j + x) >= to2)) break;
                }
                if (x > max) {
                    max = x;
                    start1 = i - from1;
                    start2 = j - from2;
                }
            }
        }
        lcsResult[0] = start1;
        lcsResult[1] = start2;
        lcsResult[2] = max;
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
                        : newLines.get(row).columnSubSequence(terminal, columns - 1, columns);
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
        // Use absolute CUP for diagonal moves (both row and column change).
        // CUP (\e[r;cH) is typically 6-10 bytes, which is almost always cheaper
        // than the combined vertical + horizontal relative sequences.
        if (hasCursorAddress && l0 != l1 && c0 != c1) {
            puts(Capability.cursor_address, l1, c1);
            cursorPos = i1;
            return i1;
        }
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
        rawPrint(str, 0, str.length());
    }

    /**
     * Writes a range of the given attributed string to the display output without
     * creating a substring. In byte mode, style state is tracked across calls via
     * {@code ansiColorState} to avoid redundant style resets and re-emissions.
     *
     * @param str   the attributed string to write
     * @param start start index (inclusive)
     * @param end   end index (exclusive)
     */
    void rawPrint(AttributedString str, int start, int end) {
        if (start >= end) return;
        if (useByteMode) {
            str.toAnsiBytes(
                    byteBuilder,
                    start,
                    end,
                    ansiColors,
                    ansiForceMode,
                    ansiPalette,
                    ansiAltIn,
                    ansiAltOut,
                    ansiColorState);
        } else {
            // Non-byte-mode path: fall back to subSequence (rare path for non-UTF-8 terminals)
            str.subSequence(start, end).print(terminal);
        }
    }

    /*
     * Ensure the terminal is in default ANSI style before emitting content that
     * must appear unstyled (blanking spaces, clr_eol, insert/delete chars).
     * Emits reset only when a non-default style is currently active.
     */
    private void ensureDefaultAnsiStyle() {
        if (!useByteMode) return;
        if (ansiColorState[1] != 0 && ansiAltOut != null) {
            byteBuilder.appendAscii(ansiAltOut);
            ansiColorState[1] = 0;
        }
        if (ansiColorState[0] != 0) {
            byteBuilder.csi().appendAscii("0m");
            ansiColorState[0] = 0;
        }
    }

    private static final Object[] EMPTY_PARAMS = new Object[0];

    /**
     * Emit the terminal control sequence for a parameter-less capability without
     * varargs allocation.
     *
     * @param capability the terminal capability to emit
     * @return `true` if the capability sequence was emitted; `false` if unavailable
     */
    private boolean puts(Capability capability) {
        if (useByteMode) {
            String str = terminal.getStringCapability(capability);
            if (str == null) {
                return false;
            }
            Curses.tputs(byteAppendable, str, EMPTY_PARAMS);
            return true;
        } else {
            return terminal.puts(capability, EMPTY_PARAMS);
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

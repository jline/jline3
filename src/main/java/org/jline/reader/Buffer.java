/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import java.util.Arrays;

import static org.jline.utils.Preconditions.checkNotNull;

/**
 * A holder for a {@link StringBuilder} that also contains the current cursor position.
 *
 * @author <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 * @author <a href="mailto:jason@planet57.com">Jason Dillon</a>
 * @since 2.0
 */
public class Buffer
{
    private int cursor = 0;
    private int cursorCol = -1;
    private int length = 0;
    private int[] buffer = new int[64];
    
    public Buffer copy () {
        Buffer that = new Buffer();
        that.setBuffer(this);
        return that;
    }

    public int cursor() {
        return cursor;
    }

    public int length() {
        return length;
    }

    public boolean currChar(int ch) {
        if (cursor == length) {
            return false;
        } else {
            buffer[cursor] = ch;
            return true;
        }
    }

    public int currChar() {
        if (cursor == length) {
            return 0;
        } else {
            return buffer[cursor];
        }
    }

    public int prevChar() {
        if (cursor <= 0) {
            return 0;
        }
        return buffer[cursor - 1];
    }

    public int nextChar() {
        if (cursor >= length - 1) {
            return 0;
        }

        return buffer[cursor + 1];
    }

    public int atChar(int i) {
        if (i < 0 || i >= length) {
            return 0;
        }
        return buffer[i];
    }

    /**
     * Write the specific character into the buffer, setting the cursor position
     * ahead one.
     *
     * @param c the character to insert
     */
    public void write(int c) {
        write(new int[] { c }, false);
    }

    /**
     * Write the specific character into the buffer, setting the cursor position
     * ahead one. The text may overwrite or insert based on the current setting
     * of {@code overTyping}.
     *
     * @param c the character to insert
     */
    public void write(int c, boolean overTyping) {
        write(new int[] { c }, overTyping);
    }

    /**
     * Insert the specified chars into the buffer, setting the cursor to the end of the insertion point.
     */
    public void write(CharSequence str) {
        checkNotNull(str);
        write(str.codePoints().toArray(), false);
    }

    public void write(CharSequence str, boolean overTyping) {
        checkNotNull(str);
        write(str.codePoints().toArray(), overTyping);
    }

    private void write(int[] ucps, boolean overTyping) {
        if (overTyping) {
            int len = cursor + ucps.length;
            int sz = buffer.length;
            if (sz < len) {
                while (sz < len) {
                    sz *= 2;
                }
                buffer = Arrays.copyOf(buffer, sz);
            }
            System.arraycopy(ucps, 0, buffer, cursor, ucps.length);
            length = len;
            cursor += ucps.length;
        } else {
            int len = length + ucps.length;
            int sz = buffer.length;
            if (sz < len) {
                while (sz < len) {
                    sz *= 2;
                }
                buffer = Arrays.copyOf(buffer, sz);
            }
            System.arraycopy(buffer, cursor, buffer, cursor + ucps.length, length - cursor);
            System.arraycopy(ucps, 0, buffer, cursor, ucps.length);
            cursor += ucps.length;
            length += ucps.length;
        }
        cursorCol = -1;
    }

    public boolean clear() {
        if (length == 0) {
            return false;
        }
        length = 0;
        cursor = 0;
        cursorCol = -1;
        return true;
    }

    public String substring(int start) {
        return substring(start, length);
    }

    public String substring(int start, int end) {
        if (start >= end || start < 0 || end > length) {
            return "";
        }
        return new String(buffer, start, end - start);
    }

    public String upToCursor() {
        return substring(0, cursor);
    }

    /**
     * Move the cursor position to the specified absolute index.
     */
    public boolean cursor(int position) {
        if (position == cursor) {
            return true;
        }
        return move(position - cursor) != 0;
    }

    /**
     * Move the cursor <i>where</i> characters.
     *
     * @param num   If less than 0, move abs(<i>where</i>) to the left, otherwise move <i>where</i> to the right.
     * @return      The number of spaces we moved
     */
    public int move(final int num) {
        int where = num;

        if ((cursor == 0) && (where <= 0)) {
            return 0;
        }

        if ((cursor == length) && (where >= 0)) {
            return 0;
        }

        if ((cursor + where) < 0) {
            where = -cursor;
        }
        else if ((cursor + where) > length) {
            where = length - cursor;
        }

        cursor += where;
        cursorCol = -1;

        return where;
    }

    public boolean up() {
        int col = getCursorCol();
        int pnl = cursor - 1;
        while (pnl >= 0 && buffer[pnl] != '\n') {
            pnl--;
        }
        if (pnl < 0) {
            return false;
        }
        int ppnl = pnl - 1;
        while (ppnl >= 0 && buffer[ppnl] != '\n') {
            ppnl--;
        }
        cursor = Math.min(ppnl + col + 1, pnl);
        return true;
    }

    public boolean down() {
        int col = getCursorCol();
        int nnl = cursor;
        while (nnl < length && buffer[nnl] != '\n') {
            nnl++;
        }
        if (nnl >= length) {
            return false;
        }
        int nnnl = nnl + 1;
        while (nnnl < length && buffer[nnnl] != '\n') {
            nnnl++;
        }
        cursor = Math.min(nnl + col + 1, nnnl);
        return true;
    }

    private int getCursorCol() {
        if (cursorCol < 0) {
            cursorCol = 0;
            int pnl = cursor - 1;
            while (pnl >= 0 && buffer[pnl] != '\n') {
                pnl--;
            }
            cursorCol = cursor - pnl - 1;
        }
        return cursorCol;
    }

    /**
     * Issue <em>num</em> backspaces.
     *
     * @return the number of characters backed up
     */
    public int backspace(final int num) {
        int count = Math.max(Math.min(cursor, num), 0);
        cursor -= count;
        System.arraycopy(buffer, cursor + count, buffer, cursor, length - cursor - count);
        length -= count;
        cursorCol = -1;
        return count;
    }

    /**
     * Issue a backspace.
     *
     * @return true if successful
     */
    public boolean backspace() {
        return backspace(1) == 1;
    }

    public int delete(int num) {
        int count = Math.max(Math.min(length - cursor, num), 0);
        System.arraycopy(buffer, cursor + count, buffer, cursor, length - cursor - count);
        length -= count;
        cursorCol = -1;
        return count;
    }

    public boolean delete() {
        return delete(1) == 1;
    }

    @Override
    public String toString() {
        return substring(0, length);
    }

    /**
     * Performs character transpose. The character prior to the cursor and the
     * character under the cursor are swapped and the cursor is advanced one
     * character unless you are already at the end of the line.
     */
    public boolean transpose() {
        if (cursor == 0 || cursor == length) {
            return false;
        }
        int tmp = buffer[cursor];
        buffer[cursor] = buffer[cursor - 1];
        buffer[cursor - 1] = tmp;
        cursor++;
        cursorCol = -1;
        return true;
    }

    public void setBuffer(Buffer that) {
        this.length = that.length;
        this.buffer = that.buffer.clone();
        this.cursor = that.cursor;
        this.cursorCol = that.cursorCol;
    }
}

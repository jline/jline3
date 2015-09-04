/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

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
    private boolean overTyping = false;

    private int cursor = 0;

    private final StringBuilder buffer = new StringBuilder();
    
    public Buffer copy () {
        Buffer that = new Buffer();
        that.overTyping = this.overTyping;
        that.cursor = this.cursor;
        that.buffer.append(this.buffer);
        return that;
    }

    public boolean overTyping() {
        return overTyping;
    }

    public void overTyping(boolean b) {
        overTyping = b;
    }

    public int cursor() {
        return cursor;
    }

    public int length() {
        return buffer.length();
    }

    public boolean currChar(char ch) {
        if (cursor == buffer.length()) {
            return false;
        } else {
            buffer.setCharAt(cursor, ch);
            return true;
        }
    }

    public char currChar() {
        if (cursor == buffer.length()) {
            return 0;
        } else {
            return buffer.charAt(cursor);
        }
    }

    public char prevChar() {
        if (cursor <= 0) {
            return 0;
        }

        return buffer.charAt(cursor - 1);
    }

    public char nextChar() {
        if (cursor >= length() - 1) {
            return 0;
        }

        return buffer.charAt(cursor + 1);
    }

    public char atChar(int i) {
        if (i < 0 || i >= length()) {
            return 0;
        }
        return buffer.charAt(i);
    }

    /**
     * Write the specific character into the buffer, setting the cursor position
     * ahead one. The text may overwrite or insert based on the current setting
     * of {@link #overTyping}.
     *
     * @param c the character to insert
     */
    public void write(final char c) {
        buffer.insert(cursor++, c);
        if (overTyping() && cursor < buffer.length()) {
            buffer.deleteCharAt(cursor);
        }
    }

    /**
     * Insert the specified chars into the buffer, setting the cursor to the end of the insertion point.
     */
    public void write(final CharSequence str) {
        checkNotNull(str);

        if (buffer.length() == 0) {
            buffer.append(str);
        }
        else {
            buffer.insert(cursor, str);
        }

        cursor += str.length();

        if (overTyping() && cursor < buffer.length()) {
            buffer.delete(cursor, (cursor + str.length()));
        }
    }

    public boolean clear() {
        if (buffer.length() == 0) {
            return false;
        }
        buffer.delete(0, buffer.length());
        cursor = 0;
        return true;
    }

    public String substring(int start) {
        return substring(start, length());
    }

    public String substring(int start, int end) {
        if (start >= end || start < 0 || end > length()) {
            return "";
        }
        return buffer.substring(start, end);
    }

    public String upToCursor() {
        return buffer.substring(0, cursor);
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
        // TODO: handle MBC
        int where = num;

        if ((cursor == 0) && (where <= 0)) {
            return 0;
        }

        if ((cursor == length()) && (where >= 0)) {
            return 0;
        }

        if ((cursor + where) < 0) {
            where = -cursor;
        }
        else if ((cursor + where) > length()) {
            where = length() - cursor;
        }

        cursor += where;

        return where;
    }

    /**
     * Issue <em>num</em> backspaces.
     *
     * @return the number of characters backed up
     */
    public int backspace(final int num) {
        int count = Math.min(cursor, num);
        cursor -= count;
        buffer.delete(cursor, cursor + count);
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
        int count = Math.min(length() - cursor, num);
        buffer.delete(cursor, cursor + count);
        return count;
    }

    public boolean delete() {
        return delete(1) == 1;
    }

    @Override
    public String toString() {
        return buffer.toString();
    }

    /**
     * Performs character transpose. The character prior to the cursor and the
     * character under the cursor are swapped and the cursor is advanced one
     * character unless you are already at the end of the line.
     */
    public boolean transpose() {
        if (cursor == 0 || cursor == length()) {
            return false;
        }
        char tmp = buffer.charAt(cursor);
        buffer.setCharAt(cursor, buffer.charAt(cursor - 1));
        buffer.setCharAt(cursor - 1, tmp);
        cursor++;
        return true;
    }

    public void setBuffer(Buffer buffer) {
        clear();
        write(buffer.buffer);
        cursor = buffer.cursor;
    }
}

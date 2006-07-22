/*
 * Copyright (c) 2002-2006, Marc Prud'hommeaux. All rights reserved.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 */
package jline;


/**
 *  A CursorBuffer is a holder for a {@link StringBuffer} that
 *  also contains the current cursor position.
 *
 *  @author  <a href="mailto:mwp1@cornell.edu">Marc Prud'hommeaux</a>
 */
public class CursorBuffer {
    public int cursor = 0;
    public final StringBuffer buffer = new StringBuffer();

    public int length() {
        return buffer.length();
    }

    public char current() {
        if (cursor <= 0) {
            return 0;
        }

        return buffer.charAt(cursor - 1);
    }

    /**
     *  Insert the specific character into the buffer, setting the
     *  cursor position ahead one.
     *
     *  @param  c  the character to insert
     */
    public void insert(final char c) {
        buffer.insert(cursor++, c);
    }

    /**
     *  Insert the specified {@link String} into the buffer, setting
     *  the cursor to the end of the insertion point.
     *
     *  @param  str  the String to insert. Must not be null.
     */
    public void insert(final String str) {
        if (buffer.length() == 0) {
            buffer.append(str);
        } else {
            buffer.insert(cursor, str);
        }

        cursor += str.length();
    }

    public String toString() {
        return buffer.toString();
    }
}

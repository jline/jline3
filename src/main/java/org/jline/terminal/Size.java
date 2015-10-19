/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal;

public class Size {

    private short ws_row;
    private short ws_col;

    public Size() {
    }

    public Size(int columns, int rows) {
        this();
        setColumns(columns);
        setRows(rows);
    }

    public int getColumns() {
        return ws_col;
    }

    public void setColumns(int columns) {
        ws_col = (short) columns;
    }

    public int getRows() {
        return ws_row;
    }

    public void setRows(int rows) {
        ws_row = (short) rows;
    }

    public void copy(Size size) {
        setColumns(size.getColumns());
        setRows(size.getRows());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Size size = (Size) o;

        if (ws_row != size.ws_row) return false;
        return ws_col == size.ws_col;

    }

    @Override
    public int hashCode() {
        int result = (int) ws_row;
        result = 31 * result + (int) ws_col;
        return result;
    }

    @Override
    public String toString() {
        return "Size[" +
                "cols=" + ws_col +
                ", rows=" + ws_row +
                ']';
    }
}

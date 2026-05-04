/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

/**
 * Represents the dimensions of a terminal in terms of rows and columns.
 *
 * <p>
 * The Size class encapsulates the dimensions of a terminal screen, providing methods to get
 * the number of rows and columns. Terminal dimensions are used for various operations such as
 * cursor positioning, screen clearing, and text layout calculations.
 * </p>
 *
 * <p>Prefer the {@link #of(int, int)} or {@link #of(Sized)} factory methods to create
 * new instances. The mutating methods ({@link #setColumns}, {@link #setRows}, {@link #copy})
 * are deprecated and will be removed in a future major version.</p>
 *
 * <p>
 * Terminal dimensions are typically measured in character cells, where:
 * </p>
 * <ul>
 *   <li><b>Columns</b> - The number of character cells in each row (width)</li>
 *   <li><b>Rows</b> - The number of character cells in each column (height)</li>
 * </ul>
 *
 * <p>
 * Size objects are typically obtained from a {@link Terminal} using {@link Terminal#getSize()},
 * and can be used to adjust display formatting or to set the terminal size using
 * {@link Terminal#setSize(Sized)}.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 * Terminal terminal = TerminalBuilder.terminal();
 *
 * // Get current terminal size
 * Size size = terminal.getSize();
 * System.out.println("Terminal dimensions: " + size.getColumns() + "x" + size.getRows());
 *
 * // Create a new size and set it
 * Size newSize = Size.of(80, 24);
 * terminal.setSize(newSize);
 * </pre>
 *
 * @see Terminal#getSize()
 * @see Terminal#setSize(Sized)
 */
public class Size implements Sized {

    private int rows;
    private int cols;

    /**
     * Creates a new Size with the specified number of columns and rows.
     *
     * @param columns the number of columns (width)
     * @param rows the number of rows (height)
     * @return a new Size instance
     */
    public static Size of(int columns, int rows) {
        return new Size(columns, rows);
    }

    /**
     * Creates a new Size with the same columns and rows as the given source.
     *
     * @param sized the source from which to copy columns and rows
     * @return a new Size instance
     */
    public static Size of(Sized sized) {
        // Always copy: Size is still mutable via deprecated setters
        return new Size(sized.getColumns(), sized.getRows());
    }

    /**
     * Creates a new Size instance with default dimensions (0 rows and 0 columns).
     *
     * @deprecated Size is now immutable. Use {@link #of(int, int)} instead.
     */
    @Deprecated
    @SuppressWarnings("java:S1133")
    public Size() {
        this(0, 0);
    }

    /**
     * Constructs a Size with the specified number of columns and rows.
     *
     * @param columns the number of columns (width)
     * @param rows the number of rows (height)
     * @deprecated Use {@link #of(int, int)} instead.
     */
    @Deprecated
    @SuppressWarnings("java:S1133")
    public Size(int columns, int rows) {
        this.cols = columns;
        this.rows = rows;
    }

    /**
     * Constructs a new Size with the same columns and rows as the given size.
     *
     * @param sized the source Size from which to copy columns and rows
     * @deprecated Use {@link #of(Sized)} instead.
     */
    @Deprecated
    @SuppressWarnings("java:S1133")
    public Size(Sized sized) {
        this(sized.getColumns(), sized.getRows());
    }

    /**
     * Returns the number of columns (width) in this terminal size.
     *
     * @return the number of columns
     */
    @Override
    public int getColumns() {
        return cols;
    }

    /**
     * Sets the number of columns (width) for this terminal size.
     *
     * @param columns the number of columns to set
     * @deprecated Use {@link #of(int, int)} to create a new instance instead.
     */
    @Deprecated
    @SuppressWarnings("java:S1133")
    public void setColumns(int columns) {
        this.cols = columns;
    }

    /**
     * Returns the number of rows (height) in this terminal size.
     *
     * @return the number of rows
     */
    @Override
    public int getRows() {
        return rows;
    }

    /**
     * Sets the number of rows (height) for this terminal size.
     *
     * @param rows the number of rows to set
     * @deprecated Use {@link #of(int, int)} to create a new instance instead.
     */
    @Deprecated
    @SuppressWarnings("java:S1133")
    public void setRows(int rows) {
        this.rows = rows;
    }

    /**
     * A cursor position combines a row number with a column position.
     * <p>
     * Note each row has {@code col+1} different column positions,
     * including the right margin.
     * </p>
     *
     * @param col the new column
     * @param row the new row
     * @return the cursor position
     */
    public int cursorPos(int row, int col) {
        return row * (cols + 1) + col;
    }

    /**
     * Copies the dimensions from another Size object to this one.
     *
     * @param size the Size object to copy dimensions from
     * @deprecated Use {@link #of(Sized)} to create a new instance instead.
     */
    @Deprecated
    @SuppressWarnings("java:S1133")
    public void copy(Size size) {
        this.rows = size.rows;
        this.cols = size.cols;
    }

    /**
     * Copies the dimensions from another Sized object to this one.
     *
     * @param size the Sized object to copy dimensions from
     * @deprecated Use {@link #of(Sized)} to create a new instance instead.
     */
    @Deprecated
    @SuppressWarnings("java:S1133")
    public void copy(Sized size) {
        this.rows = size.getRows();
        this.cols = size.getColumns();
    }

    /**
     * Compares this Size object with another object for equality.
     *
     * <p>
     * Two Size objects are considered equal if they have the same number of
     * rows and columns.
     * </p>
     *
     * @param o the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Size) {
            Size size = (Size) o;
            return rows == size.rows && cols == size.cols;
        } else {
            return false;
        }
    }

    /**
     * Returns a hash code for this Size object.
     *
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return rows * 31 + cols;
    }

    /**
     * Returns a string representation of this Size object.
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "Size[" + "cols=" + cols + ", rows=" + rows + ']';
    }
}

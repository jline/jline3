/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.curses.Theme;
import org.jline.keymap.KeyMap;
import org.jline.terminal.KeyEvent;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;

/**
 * A table component for displaying tabular data.
 *
 * <p>Table provides a data table with support for:
 * <ul>
 * <li>Column headers and data rows</li>
 * <li>Row selection (single and multiple)</li>
 * <li>Column sorting</li>
 * <li>Scrolling for large datasets</li>
 * <li>Customizable column widths</li>
 * <li>Custom cell renderers</li>
 * </ul>
 * </p>
 *
 * @param <T> the type of data objects in the table
 */
public class Table<T> extends AbstractComponent {

    /**
     * Represents a table column.
     */
    public static class Column<T> {
        private final String header;
        private final Function<T, String> cellRenderer;
        private int width;
        private boolean sortable = true;
        private Comparator<T> comparator;

        public Column(String header, Function<T, String> cellRenderer) {
            this(header, cellRenderer, -1);
        }

        public Column(String header, Function<T, String> cellRenderer, int width) {
            this.header = header != null ? header : "";
            this.cellRenderer = cellRenderer != null ? cellRenderer : Object::toString;
            this.width = width;
        }

        public String getHeader() {
            return header;
        }

        public Function<T, String> getCellRenderer() {
            return cellRenderer;
        }

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width;
        }

        public boolean isSortable() {
            return sortable;
        }

        public void setSortable(boolean sortable) {
            this.sortable = sortable;
        }

        public Comparator<T> getComparator() {
            return comparator;
        }

        public void setComparator(Comparator<T> comparator) {
            this.comparator = comparator;
        }
    }

    /**
     * Selection mode for the table.
     */
    public enum SelectionMode {
        SINGLE,
        MULTIPLE
    }

    /**
     * Actions for keyboard navigation.
     */
    enum Action {
        Up,
        Down,
        Left,
        Right,
        PageUp,
        PageDown,
        Home,
        End,
        Select,
        ToggleSelect
    }

    private final java.util.List<Column<T>> columns = new ArrayList<>();
    private final java.util.List<T> data = new ArrayList<>();
    private final java.util.List<T> filteredData = new ArrayList<>();
    private final Set<Integer> selectedRows = new HashSet<>();

    private int focusedRow = -1;
    private int scrollRow = 0;
    private int scrollCol = 0;
    private SelectionMode selectionMode = SelectionMode.SINGLE;
    private boolean showHeaders = true;
    private int sortColumn = -1;
    private boolean sortAscending = true;

    // Event listeners
    private final java.util.List<Runnable> selectionChangeListeners = new ArrayList<>();

    // Input handling
    private KeyMap<Action> keyMap;

    // Styling - will be initialized from theme in setTheme()
    private AttributedStyle normalStyle = AttributedStyle.DEFAULT;
    private AttributedStyle headerStyle = AttributedStyle.DEFAULT.bold();
    private AttributedStyle selectedStyle = AttributedStyle.DEFAULT.background(AttributedStyle.BLUE);
    private AttributedStyle focusedStyle = AttributedStyle.DEFAULT.inverse();
    private AttributedStyle selectedFocusedStyle =
            AttributedStyle.DEFAULT.background(AttributedStyle.BLUE).inverse();
    private AttributedStyle borderStyle = AttributedStyle.DEFAULT;

    public Table() {}

    @Override
    public void setTheme(Theme theme) {
        super.setTheme(theme);
        if (theme != null) {
            // Initialize styles from theme
            normalStyle = theme.getStyle(".table.normal");
            headerStyle = theme.getStyle(".table.header");
            selectedStyle = theme.getStyle(".table.selected");
            focusedStyle = theme.getStyle(".table.focused");
            selectedFocusedStyle = theme.getStyle(".table.selected.focused");
        }
    }

    /**
     * Adds a column to the table.
     *
     * @param column the column to add
     */
    public void addColumn(Column<T> column) {
        if (column != null) {
            columns.add(column);
            updateLayout();
        }
    }

    /**
     * Adds a column to the table.
     *
     * @param header the column header
     * @param cellRenderer the cell renderer function
     */
    public void addColumn(String header, Function<T, String> cellRenderer) {
        addColumn(new Column<>(header, cellRenderer));
    }

    /**
     * Adds a column to the table with specified width.
     *
     * @param header the column header
     * @param cellRenderer the cell renderer function
     * @param width the column width
     */
    public void addColumn(String header, Function<T, String> cellRenderer, int width) {
        addColumn(new Column<>(header, cellRenderer, width));
    }

    /**
     * Removes a column from the table.
     *
     * @param index the column index to remove
     */
    public void removeColumn(int index) {
        if (index >= 0 && index < columns.size()) {
            columns.remove(index);
            updateLayout();
        }
    }

    /**
     * Gets the table columns.
     *
     * @return an unmodifiable view of the columns
     */
    public java.util.List<Column<T>> getColumns() {
        return Collections.unmodifiableList(columns);
    }

    /**
     * Sets the table data.
     *
     * @param data the data to set
     */
    public void setData(java.util.List<T> data) {
        this.data.clear();
        selectedRows.clear();

        if (data != null) {
            this.data.addAll(data);
        }

        updateFilteredData();
        focusedRow = this.data.isEmpty() ? -1 : 0;
        scrollRow = 0;
        notifySelectionChange();
    }

    /**
     * Adds a data row to the table.
     *
     * @param item the item to add
     */
    public void addData(T item) {
        if (item != null) {
            data.add(item);
            updateFilteredData();
            if (focusedRow == -1) {
                focusedRow = 0;
            }
        }
    }

    /**
     * Removes a data row from the table.
     *
     * @param item the item to remove
     * @return true if the item was removed
     */
    public boolean removeData(T item) {
        int index = data.indexOf(item);
        if (index != -1) {
            data.remove(index);
            updateFilteredData();

            // Adjust selected rows
            Set<Integer> newSelected = new HashSet<>();
            for (int selected : selectedRows) {
                if (selected < index) {
                    newSelected.add(selected);
                } else if (selected > index) {
                    newSelected.add(selected - 1);
                }
            }
            selectedRows.clear();
            selectedRows.addAll(newSelected);

            // Adjust focused row
            if (focusedRow >= filteredData.size()) {
                focusedRow = filteredData.size() - 1;
            }
            if (focusedRow < 0 && !filteredData.isEmpty()) {
                focusedRow = 0;
            }

            ensureFocusedVisible();
            notifySelectionChange();
            return true;
        }
        return false;
    }

    /**
     * Clears all data from the table.
     */
    public void clearData() {
        data.clear();
        filteredData.clear();
        selectedRows.clear();
        focusedRow = -1;
        scrollRow = 0;
        notifySelectionChange();
    }

    /**
     * Gets the table data.
     *
     * @return an unmodifiable view of the data
     */
    public java.util.List<T> getData() {
        return Collections.unmodifiableList(data);
    }

    /**
     * Gets the filtered/sorted data currently displayed.
     *
     * @return an unmodifiable view of the filtered data
     */
    public java.util.List<T> getFilteredData() {
        return Collections.unmodifiableList(filteredData);
    }

    /**
     * Gets the selection mode.
     *
     * @return the selection mode
     */
    public SelectionMode getSelectionMode() {
        return selectionMode;
    }

    /**
     * Sets the selection mode.
     *
     * @param selectionMode the selection mode to set
     */
    public void setSelectionMode(SelectionMode selectionMode) {
        this.selectionMode = selectionMode != null ? selectionMode : SelectionMode.SINGLE;

        if (this.selectionMode == SelectionMode.SINGLE && selectedRows.size() > 1) {
            int firstSelected = selectedRows.iterator().next();
            selectedRows.clear();
            selectedRows.add(firstSelected);
            notifySelectionChange();
        }
    }

    /**
     * Gets whether headers are shown.
     *
     * @return true if headers are shown
     */
    public boolean isShowHeaders() {
        return showHeaders;
    }

    /**
     * Sets whether headers are shown.
     *
     * @param showHeaders true to show headers
     */
    public void setShowHeaders(boolean showHeaders) {
        this.showHeaders = showHeaders;
    }

    /**
     * Gets the focused row index.
     *
     * @return the focused row index, or -1 if no row is focused
     */
    public int getFocusedRow() {
        return focusedRow;
    }

    /**
     * Sets the focused row index.
     *
     * @param row the row index to focus
     */
    public void setFocusedRow(int row) {
        if (row >= -1 && row < filteredData.size()) {
            focusedRow = row;
            ensureFocusedVisible();
        }
    }

    /**
     * Gets the focused row data.
     *
     * @return the focused row data, or null if no row is focused
     */
    public T getFocusedRowData() {
        return (focusedRow >= 0 && focusedRow < filteredData.size()) ? filteredData.get(focusedRow) : null;
    }

    /**
     * Gets the selected row indices.
     *
     * @return an unmodifiable view of the selected row indices
     */
    public Set<Integer> getSelectedRows() {
        return Collections.unmodifiableSet(selectedRows);
    }

    /**
     * Gets the selected row data.
     *
     * @return a list of selected row data
     */
    public java.util.List<T> getSelectedRowData() {
        java.util.List<T> selected = new ArrayList<>();
        for (int index : selectedRows) {
            if (index >= 0 && index < filteredData.size()) {
                selected.add(filteredData.get(index));
            }
        }
        return selected;
    }

    /**
     * Sets the selected row.
     *
     * @param row the row index to select
     */
    public void setSelectedRow(int row) {
        selectedRows.clear();
        if (row >= 0 && row < filteredData.size()) {
            selectedRows.add(row);
        }
        notifySelectionChange();
    }

    /**
     * Adds a row to the selection.
     *
     * @param row the row index to add to selection
     */
    public void addToSelection(int row) {
        if (row >= 0 && row < filteredData.size()) {
            if (selectionMode == SelectionMode.SINGLE) {
                selectedRows.clear();
            }
            selectedRows.add(row);
            notifySelectionChange();
        }
    }

    /**
     * Toggles the selection of a row.
     *
     * @param row the row index to toggle
     */
    public void toggleSelection(int row) {
        if (selectedRows.contains(row)) {
            selectedRows.remove(row);
        } else {
            addToSelection(row);
        }
        notifySelectionChange();
    }

    /**
     * Clears the selection.
     */
    public void clearSelection() {
        if (!selectedRows.isEmpty()) {
            selectedRows.clear();
            notifySelectionChange();
        }
    }

    /**
     * Sorts the table by the specified column.
     *
     * @param columnIndex the column index to sort by
     * @param ascending true for ascending sort, false for descending
     */
    public void sortByColumn(int columnIndex, boolean ascending) {
        if (columnIndex >= 0 && columnIndex < columns.size()) {
            Column<T> column = columns.get(columnIndex);
            if (column.isSortable()) {
                sortColumn = columnIndex;
                sortAscending = ascending;
                updateFilteredData();
            }
        }
    }

    /**
     * Moves the focus up by one row.
     */
    public void moveFocusUp() {
        if (focusedRow > 0) {
            focusedRow--;
            ensureFocusedVisible();
        }
    }

    /**
     * Moves the focus down by one row.
     */
    public void moveFocusDown() {
        if (focusedRow < filteredData.size() - 1) {
            focusedRow++;
            ensureFocusedVisible();
        }
    }

    /**
     * Scrolls the table up by the specified number of rows.
     *
     * @param rows the number of rows to scroll up
     */
    public void scrollUp(int rows) {
        scrollRow = Math.max(0, scrollRow - rows);
    }

    /**
     * Scrolls the table down by the specified number of rows.
     *
     * @param rows the number of rows to scroll down
     */
    public void scrollDown(int rows) {
        Size size = getSize();
        if (size != null) {
            int dataRows = filteredData.size();
            int headerRows = showHeaders ? 1 : 0;
            int maxScroll = Math.max(0, dataRows - (size.h() - headerRows));
            scrollRow = Math.min(maxScroll, scrollRow + rows);
        }
    }

    /**
     * Adds a selection change listener.
     *
     * @param listener the listener to add
     */
    public void addSelectionChangeListener(Runnable listener) {
        if (listener != null) {
            selectionChangeListeners.add(listener);
        }
    }

    /**
     * Removes a selection change listener.
     *
     * @param listener the listener to remove
     */
    public void removeSelectionChangeListener(Runnable listener) {
        selectionChangeListeners.remove(listener);
    }

    /**
     * Updates the filtered and sorted data.
     */
    private void updateFilteredData() {
        filteredData.clear();
        filteredData.addAll(data);

        // Apply sorting if specified
        if (sortColumn >= 0 && sortColumn < columns.size()) {
            Column<T> column = columns.get(sortColumn);
            Comparator<T> comparator = column.getComparator();

            if (comparator == null) {
                // Create default comparator based on string representation
                comparator =
                        Comparator.comparing(item -> column.getCellRenderer().apply(item));
            }

            if (!sortAscending) {
                comparator = comparator.reversed();
            }

            filteredData.sort(comparator);
        }
    }

    /**
     * Updates the column layout by calculating widths.
     */
    private void updateLayout() {
        Size size = getSize();
        if (size == null || columns.isEmpty()) {
            return;
        }

        int totalWidth = size.w();
        int fixedWidth = 0;
        int flexColumns = 0;

        // Calculate fixed width and count flexible columns
        for (Column<T> column : columns) {
            if (column.getWidth() > 0) {
                fixedWidth += column.getWidth();
            } else {
                flexColumns++;
            }
        }

        // Distribute remaining width among flexible columns
        if (flexColumns > 0) {
            int remainingWidth = Math.max(0, totalWidth - fixedWidth);
            int flexWidth = remainingWidth / flexColumns;

            for (Column<T> column : columns) {
                if (column.getWidth() <= 0) {
                    column.setWidth(Math.max(1, flexWidth));
                }
            }
        }
    }

    /**
     * Ensures the focused row is visible by adjusting scroll position.
     */
    private void ensureFocusedVisible() {
        if (focusedRow < 0) {
            return;
        }

        Size size = getSize();
        if (size == null) {
            return;
        }

        int height = size.h();
        int headerRows = showHeaders ? 1 : 0;
        int dataHeight = height - headerRows;

        if (dataHeight <= 0) {
            return;
        }

        if (focusedRow < scrollRow) {
            scrollRow = focusedRow;
        } else if (focusedRow >= scrollRow + dataHeight) {
            scrollRow = focusedRow - dataHeight + 1;
        }
    }

    /**
     * Notifies all selection change listeners.
     */
    private void notifySelectionChange() {
        for (Runnable listener : selectionChangeListeners) {
            try {
                listener.run();
            } catch (Exception e) {
                System.err.println("Error in table selection change listener: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean handleMouse(MouseEvent event) {
        if (event.getType() == MouseEvent.Type.Pressed) {
            Position pos = getScreenPosition();
            if (pos == null) {
                return false;
            }
            int row = event.getY() - pos.y();
            // Account for header row
            if (showHeaders) {
                row -= 1;
            }
            int dataIndex = scrollRow + row;
            if (dataIndex >= 0 && dataIndex < data.size()) {
                focusedRow = dataIndex;
                return true;
            }
        }
        return false;
    }

    private void resolveStyles() {
        normalStyle = resolveStyle(".table.normal", normalStyle);
        headerStyle = resolveStyle(".table.header", headerStyle);
        selectedStyle = resolveStyle(".table.selected", selectedStyle);
        focusedStyle = resolveStyle(".table.focused", focusedStyle);
        selectedFocusedStyle = resolveStyle(".table.selected.focused", selectedFocusedStyle);
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        if (size == null) {
            return;
        }

        Position pos = getScreenPosition();
        if (pos == null) {
            return;
        }

        resolveStyles();

        int width = size.w();
        int height = size.h();

        updateLayout();

        // Clear the table area
        screen.fill(pos.x(), pos.y(), width, height, normalStyle);

        int currentRow = 0;

        // Draw headers if enabled
        if (showHeaders && currentRow < height) {
            drawHeaders(screen, currentRow, width, pos);
            currentRow++;
        }

        // Draw data rows
        for (int row = 0; row < height - currentRow && (scrollRow + row) < filteredData.size(); row++) {
            int dataIndex = scrollRow + row;
            T rowData = filteredData.get(dataIndex);

            drawDataRow(screen, currentRow + row, width, dataIndex, rowData, pos);
        }
    }

    /**
     * Draws the table headers.
     */
    private void drawHeaders(Screen screen, int row, int width, Position pos) {
        int x = 0;

        for (int col = 0; col < columns.size() && x < width; col++) {
            Column<T> column = columns.get(col);
            int colWidth = Math.min(column.getWidth(), width - x);

            if (colWidth <= 0) {
                break;
            }

            // Fill header background
            screen.fill(pos.x() + x, pos.y() + row, colWidth, 1, headerStyle);

            // Draw header text
            String headerText = column.getHeader();
            if (headerText.length() > colWidth) {
                headerText = headerText.substring(0, Math.max(0, colWidth - 3)) + "...";
            }

            // Add sort indicator
            if (col == sortColumn) {
                String indicator = sortAscending ? " ↑" : " ↓";
                if (headerText.length() + indicator.length() <= colWidth) {
                    headerText += indicator;
                }
            }

            if (!headerText.isEmpty()) {
                AttributedString attributedHeader = new AttributedString(headerText, headerStyle);
                screen.text(pos.x() + x, pos.y() + row, attributedHeader);
            }

            x += colWidth;
        }
    }

    /**
     * Draws a data row.
     */
    private void drawDataRow(Screen screen, int row, int width, int dataIndex, T rowData, Position pos) {
        boolean isSelected = selectedRows.contains(dataIndex);
        // Only show focus highlight when the component has focus
        boolean isFocused = (dataIndex == focusedRow) && isFocused();

        AttributedStyle rowStyle = normalStyle;
        if (isSelected && isFocused) {
            rowStyle = selectedFocusedStyle;
        } else if (isSelected) {
            rowStyle = selectedStyle;
        } else if (isFocused) {
            rowStyle = focusedStyle;
        }

        // Fill row background
        screen.fill(pos.x(), pos.y() + row, width, 1, rowStyle);

        int x = 0;
        for (int col = 0; col < columns.size() && x < width; col++) {
            Column<T> column = columns.get(col);
            int colWidth = Math.min(column.getWidth(), width - x);

            if (colWidth <= 0) {
                break;
            }

            // Get cell text
            String cellText = column.getCellRenderer().apply(rowData);
            if (cellText.length() > colWidth) {
                cellText = cellText.substring(0, Math.max(0, colWidth - 3)) + "...";
            }

            // Draw cell text
            if (!cellText.isEmpty()) {
                AttributedString attributedCell = new AttributedString(cellText, rowStyle);
                screen.text(pos.x() + x, pos.y() + row, attributedCell);
            }

            x += colWidth;
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        if (columns.isEmpty()) {
            return new Size(20, 5);
        }

        // Calculate preferred width based on column headers and data
        int preferredWidth = 0;
        for (Column<T> column : columns) {
            int colWidth = Math.max(column.getHeader().length(), 10);

            // Sample some data to estimate column width
            for (int i = 0; i < Math.min(5, filteredData.size()); i++) {
                String cellText = column.getCellRenderer().apply(filteredData.get(i));
                colWidth = Math.max(colWidth, cellText.length());
            }

            preferredWidth += Math.min(colWidth, 30); // Cap column width
        }

        // Preferred height includes headers and some data rows
        int headerRows = showHeaders ? 1 : 0;
        int dataRows = Math.min(filteredData.size(), 15);
        int preferredHeight = headerRows + Math.max(3, dataRows);

        return new Size(Math.max(20, preferredWidth), preferredHeight);
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        Action action = null;

        // Handle key events directly based on KeyEvent type
        if (event.getType() == KeyEvent.Type.Arrow) {
            switch (event.getArrow()) {
                case Up:
                    action = Action.Up;
                    break;
                case Down:
                    action = Action.Down;
                    break;
                case Left:
                    action = Action.Left;
                    break;
                case Right:
                    action = Action.Right;
                    break;
            }
        } else if (event.getType() == KeyEvent.Type.Special) {
            switch (event.getSpecial()) {
                case Enter:
                    action = Action.Select;
                    break;
                case Tab:
                    action = Action.ToggleSelect;
                    break;
                case PageUp:
                    action = Action.PageUp;
                    break;
                case PageDown:
                    action = Action.PageDown;
                    break;
                case Home:
                    action = Action.Home;
                    break;
                case End:
                    action = Action.End;
                    break;
            }
        } else if (event.getType() == KeyEvent.Type.Character) {
            char ch = event.getCharacter();
            if (ch == ' ' || ch == '\n' || ch == '\r') {
                action = Action.Select;
            } else if (ch == '\t') {
                action = Action.ToggleSelect;
            }
        }

        if (action != null) {
            handleAction(action);
            return true;
        }
        return false;
    }

    private void initializeKeyMap() {
        Terminal terminal = getWindow().getGUI().getTerminal();
        keyMap = new KeyMap<>();

        // Arrow keys
        keyMap.bind(Action.Up, KeyMap.key(terminal, InfoCmp.Capability.key_up));
        keyMap.bind(Action.Down, KeyMap.key(terminal, InfoCmp.Capability.key_down));
        keyMap.bind(Action.Left, KeyMap.key(terminal, InfoCmp.Capability.key_left));
        keyMap.bind(Action.Right, KeyMap.key(terminal, InfoCmp.Capability.key_right));

        // Page navigation
        keyMap.bind(Action.PageUp, KeyMap.key(terminal, InfoCmp.Capability.key_ppage));
        keyMap.bind(Action.PageDown, KeyMap.key(terminal, InfoCmp.Capability.key_npage));

        // Home/End
        keyMap.bind(Action.Home, KeyMap.key(terminal, InfoCmp.Capability.key_home));
        keyMap.bind(Action.End, KeyMap.key(terminal, InfoCmp.Capability.key_end));

        // Selection
        keyMap.bind(Action.Select, KeyMap.key(terminal, InfoCmp.Capability.key_enter), " ", "\n", "\r");
        keyMap.bind(Action.ToggleSelect, "\t");
    }

    private void handleAction(Action action) {
        switch (action) {
            case Up:
                moveFocusUp();
                break;
            case Down:
                moveFocusDown();
                break;
            case Left:
                scrollLeft();
                break;
            case Right:
                scrollRight();
                break;
            case PageUp:
                pageUp();
                break;
            case PageDown:
                pageDown();
                break;
            case Home:
                moveFocusToFirst();
                break;
            case End:
                moveFocusToLast();
                break;
            case Select:
                if (focusedRow >= 0 && focusedRow < filteredData.size()) {
                    if (selectionMode == SelectionMode.SINGLE) {
                        selectedRows.clear();
                        selectedRows.add(focusedRow);
                    } else {
                        if (selectedRows.contains(focusedRow)) {
                            selectedRows.remove(focusedRow);
                        } else {
                            selectedRows.add(focusedRow);
                        }
                    }
                    notifySelectionChange();
                }
                break;
            case ToggleSelect:
                if (focusedRow >= 0 && focusedRow < filteredData.size() && selectionMode == SelectionMode.MULTIPLE) {
                    if (selectedRows.contains(focusedRow)) {
                        selectedRows.remove(focusedRow);
                    } else {
                        selectedRows.add(focusedRow);
                    }
                    notifySelectionChange();
                }
                break;
        }
    }

    private void pageUp() {
        Size size = getSize();
        if (size != null) {
            int pageSize = Math.max(1, size.h() - (showHeaders ? 2 : 1));
            for (int i = 0; i < pageSize && focusedRow > 0; i++) {
                moveFocusUp();
            }
        }
    }

    private void pageDown() {
        Size size = getSize();
        if (size != null) {
            int pageSize = Math.max(1, size.h() - (showHeaders ? 2 : 1));
            for (int i = 0; i < pageSize && focusedRow < filteredData.size() - 1; i++) {
                moveFocusDown();
            }
        }
    }

    private void scrollLeft() {
        if (scrollCol > 0) {
            scrollCol--;
        }
    }

    private void scrollRight() {
        if (scrollCol < columns.size() - 1) {
            scrollCol++;
        }
    }

    private void moveFocusToFirst() {
        if (!filteredData.isEmpty()) {
            focusedRow = 0;
            ensureFocusedVisible();
        }
    }

    private void moveFocusToLast() {
        if (!filteredData.isEmpty()) {
            focusedRow = filteredData.size() - 1;
            ensureFocusedVisible();
        }
    }
}

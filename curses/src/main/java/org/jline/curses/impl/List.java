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
import java.util.Collection;
import java.util.Collections;
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
 * A list component for displaying and selecting items.
 *
 * <p>List provides a scrollable list with support for:
 * <ul>
 * <li>Single and multiple selection modes</li>
 * <li>Keyboard navigation</li>
 * <li>Custom item rendering</li>
 * <li>Scrolling for large lists</li>
 * <li>Selection change events</li>
 * </ul>
 * </p>
 *
 * @param <T> the type of items in the list
 */
public class List<T> extends AbstractComponent {

    /**
     * Selection mode for the list.
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
        PageUp,
        PageDown,
        Home,
        End,
        Select,
        ToggleSelect
    }

    private final java.util.List<T> items = new ArrayList<>();
    private final Set<Integer> selectedIndices = new HashSet<>();
    private int focusedIndex = -1;
    private int scrollOffset = 0;
    private SelectionMode selectionMode = SelectionMode.SINGLE;
    private Function<T, String> itemRenderer = Object::toString;

    // Event listeners
    private final java.util.List<Runnable> selectionChangeListeners = new ArrayList<>();

    // Input handling
    private KeyMap<Action> keyMap;

    // Styling - will be initialized from theme in setTheme()
    private AttributedStyle normalStyle = AttributedStyle.DEFAULT;
    private AttributedStyle selectedStyle = AttributedStyle.DEFAULT.background(AttributedStyle.BLUE);
    private AttributedStyle focusedStyle = AttributedStyle.DEFAULT.inverse();
    private AttributedStyle selectedFocusedStyle =
            AttributedStyle.DEFAULT.background(AttributedStyle.BLUE).inverse();

    public List() {}

    public List(Collection<T> items) {
        if (items != null) {
            this.items.addAll(items);
            if (!this.items.isEmpty()) {
                focusedIndex = 0;
            }
        }
    }

    @Override
    public void setTheme(Theme theme) {
        super.setTheme(theme);
        if (theme != null) {
            // Initialize styles from theme
            normalStyle = theme.getStyle(".list.normal");
            selectedStyle = theme.getStyle(".list.selected");
            focusedStyle = theme.getStyle(".list.focused");
            selectedFocusedStyle = theme.getStyle(".list.selected.focused");
        }
    }

    /**
     * Gets the list items.
     *
     * @return an unmodifiable view of the items
     */
    public java.util.List<T> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Sets the list items.
     *
     * @param items the items to set
     */
    public void setItems(Collection<T> items) {
        this.items.clear();
        selectedIndices.clear();

        if (items != null) {
            this.items.addAll(items);
        }

        focusedIndex = this.items.isEmpty() ? -1 : 0;
        scrollOffset = 0;
        notifySelectionChange();
    }

    /**
     * Adds an item to the list.
     *
     * @param item the item to add
     */
    public void addItem(T item) {
        items.add(item);
        if (focusedIndex == -1) {
            focusedIndex = 0;
        }
    }

    /**
     * Removes an item from the list.
     *
     * @param item the item to remove
     * @return true if the item was removed
     */
    public boolean removeItem(T item) {
        int index = items.indexOf(item);
        if (index != -1) {
            return removeItem(index);
        }
        return false;
    }

    /**
     * Removes an item at the specified index.
     *
     * @param index the index of the item to remove
     * @return true if the item was removed
     */
    public boolean removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);

            // Adjust selected indices
            Set<Integer> newSelected = new HashSet<>();
            for (int selected : selectedIndices) {
                if (selected < index) {
                    newSelected.add(selected);
                } else if (selected > index) {
                    newSelected.add(selected - 1);
                }
                // Skip the removed index
            }
            selectedIndices.clear();
            selectedIndices.addAll(newSelected);

            // Adjust focused index
            if (focusedIndex >= items.size()) {
                focusedIndex = items.size() - 1;
            }
            if (focusedIndex < 0 && !items.isEmpty()) {
                focusedIndex = 0;
            }

            ensureFocusedVisible();
            notifySelectionChange();
            return true;
        }
        return false;
    }

    /**
     * Clears all items from the list.
     */
    public void clear() {
        items.clear();
        selectedIndices.clear();
        focusedIndex = -1;
        scrollOffset = 0;
        notifySelectionChange();
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

        // If switching to single selection, keep only the first selected item
        if (this.selectionMode == SelectionMode.SINGLE && selectedIndices.size() > 1) {
            int firstSelected = selectedIndices.iterator().next();
            selectedIndices.clear();
            selectedIndices.add(firstSelected);
            notifySelectionChange();
        }
    }

    /**
     * Gets the item renderer function.
     *
     * @return the item renderer
     */
    public Function<T, String> getItemRenderer() {
        return itemRenderer;
    }

    /**
     * Sets the item renderer function.
     *
     * @param itemRenderer the item renderer to set
     */
    public void setItemRenderer(Function<T, String> itemRenderer) {
        this.itemRenderer = itemRenderer != null ? itemRenderer : Object::toString;
    }

    /**
     * Gets the focused item index.
     *
     * @return the focused index, or -1 if no item is focused
     */
    public int getFocusedIndex() {
        return focusedIndex;
    }

    /**
     * Sets the focused item index.
     *
     * @param index the index to focus
     */
    public void setFocusedIndex(int index) {
        if (index >= -1 && index < items.size() && focusedIndex != index) {
            focusedIndex = index;
            ensureFocusedVisible();
            invalidate(); // Trigger repaint when focus changes
        }
    }

    /**
     * Gets the focused item.
     *
     * @return the focused item, or null if no item is focused
     */
    public T getFocusedItem() {
        return (focusedIndex >= 0 && focusedIndex < items.size()) ? items.get(focusedIndex) : null;
    }

    /**
     * Gets the selected item indices.
     *
     * @return an unmodifiable view of the selected indices
     */
    public Set<Integer> getSelectedIndices() {
        return Collections.unmodifiableSet(selectedIndices);
    }

    /**
     * Gets the selected items.
     *
     * @return a list of selected items
     */
    public java.util.List<T> getSelectedItems() {
        java.util.List<T> selected = new ArrayList<>();
        for (int index : selectedIndices) {
            if (index >= 0 && index < items.size()) {
                selected.add(items.get(index));
            }
        }
        return selected;
    }

    /**
     * Gets the first selected item.
     *
     * @return the first selected item, or null if no item is selected
     */
    public T getSelectedItem() {
        if (selectedIndices.isEmpty()) {
            return null;
        }
        int index = selectedIndices.iterator().next();
        return (index >= 0 && index < items.size()) ? items.get(index) : null;
    }

    /**
     * Sets the selected item index.
     *
     * @param index the index to select
     */
    public void setSelectedIndex(int index) {
        selectedIndices.clear();
        if (index >= 0 && index < items.size()) {
            selectedIndices.add(index);
        }
        invalidate(); // Trigger repaint when selection changes
        notifySelectionChange();
    }

    /**
     * Adds an index to the selection.
     *
     * @param index the index to add to selection
     */
    public void addToSelection(int index) {
        if (index >= 0 && index < items.size()) {
            if (selectionMode == SelectionMode.SINGLE) {
                selectedIndices.clear();
            }
            selectedIndices.add(index);
            notifySelectionChange();
        }
    }

    /**
     * Removes an index from the selection.
     *
     * @param index the index to remove from selection
     */
    public void removeFromSelection(int index) {
        if (selectedIndices.remove(index)) {
            notifySelectionChange();
        }
    }

    /**
     * Toggles the selection of an index.
     *
     * @param index the index to toggle
     */
    public void toggleSelection(int index) {
        if (selectedIndices.contains(index)) {
            removeFromSelection(index);
        } else {
            addToSelection(index);
        }
    }

    /**
     * Clears the selection.
     */
    public void clearSelection() {
        if (!selectedIndices.isEmpty()) {
            selectedIndices.clear();
            notifySelectionChange();
        }
    }

    /**
     * Selects all items (only in multiple selection mode).
     */
    public void selectAll() {
        if (selectionMode == SelectionMode.MULTIPLE) {
            selectedIndices.clear();
            for (int i = 0; i < items.size(); i++) {
                selectedIndices.add(i);
            }
            notifySelectionChange();
        }
    }

    /**
     * Moves the focus up by one item.
     */
    public void moveFocusUp() {
        if (focusedIndex > 0) {
            focusedIndex--;
            ensureFocusedVisible();
            invalidate(); // Trigger repaint when focus changes
        }
    }

    /**
     * Moves the focus down by one item.
     */
    public void moveFocusDown() {
        if (focusedIndex < items.size() - 1) {
            focusedIndex++;
            ensureFocusedVisible();
            invalidate(); // Trigger repaint when focus changes
        }
    }

    /**
     * Moves the focus to the first item.
     */
    public void moveFocusToFirst() {
        if (!items.isEmpty() && focusedIndex != 0) {
            focusedIndex = 0;
            ensureFocusedVisible();
            invalidate(); // Trigger repaint when focus changes
        }
    }

    /**
     * Moves the focus to the last item.
     */
    public void moveFocusToLast() {
        if (!items.isEmpty()) {
            int lastIndex = items.size() - 1;
            if (focusedIndex != lastIndex) {
                focusedIndex = lastIndex;
                ensureFocusedVisible();
                invalidate(); // Trigger repaint when focus changes
            }
        }
    }

    /**
     * Scrolls the list up by the specified number of items.
     *
     * @param items the number of items to scroll up
     */
    public void scrollUp(int items) {
        scrollOffset = Math.max(0, scrollOffset - items);
    }

    /**
     * Scrolls the list down by the specified number of items.
     *
     * @param items the number of items to scroll down
     */
    public void scrollDown(int items) {
        Size size = getSize();
        if (size != null) {
            int maxScroll = Math.max(0, this.items.size() - size.h());
            scrollOffset = Math.min(maxScroll, scrollOffset + items);
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
     * Ensures the focused item is visible by adjusting scroll offset.
     */
    private void ensureFocusedVisible() {
        if (focusedIndex < 0) {
            return;
        }

        Size size = getSize();
        if (size == null) {
            return;
        }

        int height = size.h();
        if (height <= 0) {
            return;
        }

        if (focusedIndex < scrollOffset) {
            scrollOffset = focusedIndex;
        } else if (focusedIndex >= scrollOffset + height) {
            scrollOffset = focusedIndex - height + 1;
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
                System.err.println("Error in list selection change listener: " + e.getMessage());
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
            int itemIndex = scrollOffset + row;
            if (itemIndex >= 0 && itemIndex < items.size()) {
                focusedIndex = itemIndex;
                return true;
            }
        }
        return false;
    }

    private void resolveStyles() {
        normalStyle = resolveStyle(".list.normal", normalStyle);
        selectedStyle = resolveStyle(".list.selected", selectedStyle);
        focusedStyle = resolveStyle(".list.focused", focusedStyle);
        selectedFocusedStyle = resolveStyle(".list.selected.focused", selectedFocusedStyle);
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

        int height = size.h();
        boolean needsScrollbar = items.size() > height;
        int scrollbarWidth = needsScrollbar ? 1 : 0;
        int width = size.w() - scrollbarWidth;

        // Clear the list area
        screen.fill(pos.x(), pos.y(), size.w(), height, normalStyle);

        // Draw visible items
        for (int row = 0; row < height && (scrollOffset + row) < items.size(); row++) {
            int itemIndex = scrollOffset + row;
            T item = items.get(itemIndex);
            String itemText = itemRenderer.apply(item);

            // Truncate text if too long
            if (itemText.length() > width) {
                itemText = itemText.substring(0, Math.max(0, width - 3)) + "...";
            }

            // Determine style - only show focus highlight when the component has focus
            AttributedStyle style = normalStyle;
            boolean isSelected = selectedIndices.contains(itemIndex);
            boolean isFocusedItem = (itemIndex == focusedIndex) && isFocused();

            if (isSelected && isFocusedItem) {
                style = selectedFocusedStyle;
            } else if (isSelected) {
                style = selectedStyle;
            } else if (isFocusedItem) {
                style = focusedStyle;
            }

            // Fill the entire row with the background style
            screen.fill(pos.x(), pos.y() + row, width, 1, style);

            // Draw the item text
            if (!itemText.isEmpty()) {
                AttributedString attributedText = new AttributedString(itemText, style);
                screen.text(pos.x(), pos.y() + row, attributedText);
            }
        }

        // Draw vertical scrollbar
        if (needsScrollbar) {
            int scrollX = pos.x() + size.w() - 1;
            int totalItems = items.size();
            int thumbSize = Math.max(1, (int) ((double) height * height / totalItems));
            int thumbPos = (int) ((double) scrollOffset * (height - thumbSize) / Math.max(1, totalItems - height));
            thumbPos = Math.max(0, Math.min(thumbPos, height - thumbSize));

            for (int row = 0; row < height; row++) {
                boolean isThumb = row >= thumbPos && row < thumbPos + thumbSize;
                char ch = isThumb ? '\u2588' : '\u2591';
                screen.text(
                        scrollX,
                        pos.y() + row,
                        new AttributedString(String.valueOf(ch), isThumb ? focusedStyle : normalStyle));
            }
        }
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
                if (focusedIndex >= 0 && focusedIndex < items.size()) {
                    if (selectionMode == SelectionMode.SINGLE) {
                        selectedIndices.clear();
                        selectedIndices.add(focusedIndex);
                    } else {
                        if (selectedIndices.contains(focusedIndex)) {
                            selectedIndices.remove(focusedIndex);
                        } else {
                            selectedIndices.add(focusedIndex);
                        }
                    }
                    notifySelectionChange();
                }
                break;
            case ToggleSelect:
                if (focusedIndex >= 0 && focusedIndex < items.size() && selectionMode == SelectionMode.MULTIPLE) {
                    if (selectedIndices.contains(focusedIndex)) {
                        selectedIndices.remove(focusedIndex);
                    } else {
                        selectedIndices.add(focusedIndex);
                    }
                    notifySelectionChange();
                }
                break;
        }
    }

    private void pageUp() {
        Size size = getSize();
        if (size != null) {
            int pageSize = Math.max(1, size.h() - 1);
            for (int i = 0; i < pageSize && focusedIndex > 0; i++) {
                moveFocusUp();
            }
        }
    }

    private void pageDown() {
        Size size = getSize();
        if (size != null) {
            int pageSize = Math.max(1, size.h() - 1);
            for (int i = 0; i < pageSize && focusedIndex < items.size() - 1; i++) {
                moveFocusDown();
            }
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        if (items.isEmpty()) {
            return new Size(10, 3);
        }

        // Calculate preferred width based on longest item
        int maxWidth = 0;
        for (T item : items) {
            String text = itemRenderer.apply(item);
            maxWidth = Math.max(maxWidth, text.length());
        }

        // Preferred height is number of items, but capped at reasonable maximum
        int preferredHeight = Math.min(items.size(), 15);

        return new Size(Math.max(10, maxWidth), Math.max(3, preferredHeight));
    }
}

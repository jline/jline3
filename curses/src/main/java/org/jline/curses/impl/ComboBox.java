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
import java.util.List;
import java.util.function.Function;

import org.jline.curses.Position;
import org.jline.curses.Screen;
import org.jline.curses.Size;
import org.jline.terminal.KeyEvent;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;

/**
 * A combo box (dropdown) component.
 *
 * @param <T> the type of items in the combo box
 */
public class ComboBox<T> extends AbstractComponent {

    private final List<T> items = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean open;
    private int dropdownFocusedIndex;
    private Function<T, String> itemRenderer = Object::toString;
    private final List<Runnable> changeListeners = new ArrayList<>();

    public ComboBox() {}

    public List<T> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void setItems(List<T> items) {
        this.items.clear();
        if (items != null) {
            this.items.addAll(items);
        }
        if (selectedIndex >= this.items.size()) {
            selectedIndex = this.items.isEmpty() ? -1 : 0;
        }
    }

    public void addItem(T item) {
        items.add(item);
        if (selectedIndex < 0) {
            selectedIndex = 0;
        }
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= -1 && index < items.size()) {
            int old = selectedIndex;
            selectedIndex = index;
            if (old != selectedIndex) {
                fireChangeListeners();
            }
        }
    }

    public T getSelectedItem() {
        return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null;
    }

    public boolean isOpen() {
        return open;
    }

    public void setItemRenderer(Function<T, String> renderer) {
        this.itemRenderer = renderer != null ? renderer : Object::toString;
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    private void fireChangeListeners() {
        for (Runnable listener : changeListeners) {
            listener.run();
        }
    }

    private String renderItem(int index) {
        if (index >= 0 && index < items.size()) {
            return itemRenderer.apply(items.get(index));
        }
        return "";
    }

    @Override
    public boolean handleKey(KeyEvent event) {
        if (open) {
            return handleKeyOpen(event);
        } else {
            return handleKeyClosed(event);
        }
    }

    private boolean handleKeyClosed(KeyEvent event) {
        if (event.getType() == KeyEvent.Type.Special && event.getSpecial() == KeyEvent.Special.Enter) {
            openDropdown();
            return true;
        }
        if (event.getType() == KeyEvent.Type.Character && event.getCharacter() == ' ') {
            openDropdown();
            return true;
        }
        if (event.getType() == KeyEvent.Type.Arrow) {
            if (event.getArrow() == KeyEvent.Arrow.Down && selectedIndex < items.size() - 1) {
                setSelectedIndex(selectedIndex + 1);
                return true;
            }
            if (event.getArrow() == KeyEvent.Arrow.Up && selectedIndex > 0) {
                setSelectedIndex(selectedIndex - 1);
                return true;
            }
        }
        return false;
    }

    private boolean handleKeyOpen(KeyEvent event) {
        if (event.getType() == KeyEvent.Type.Arrow) {
            if (event.getArrow() == KeyEvent.Arrow.Down) {
                if (dropdownFocusedIndex < items.size() - 1) {
                    dropdownFocusedIndex++;
                }
                return true;
            }
            if (event.getArrow() == KeyEvent.Arrow.Up) {
                if (dropdownFocusedIndex > 0) {
                    dropdownFocusedIndex--;
                }
                return true;
            }
        }
        if (event.getType() == KeyEvent.Type.Special) {
            if (event.getSpecial() == KeyEvent.Special.Enter) {
                setSelectedIndex(dropdownFocusedIndex);
                closeDropdown();
                return true;
            }
            if (event.getSpecial() == KeyEvent.Special.Escape) {
                closeDropdown();
                return true;
            }
        }
        if (event.getType() == KeyEvent.Type.Character && event.getCharacter() == ' ') {
            setSelectedIndex(dropdownFocusedIndex);
            closeDropdown();
            return true;
        }
        return false;
    }

    private void openDropdown() {
        open = true;
        dropdownFocusedIndex = Math.max(0, selectedIndex);
    }

    private void closeDropdown() {
        open = false;
    }

    @Override
    protected void doDraw(Screen screen) {
        Size size = getSize();
        Position pos = getScreenPosition();
        if (size == null || pos == null) {
            return;
        }

        AttributedStyle normalStyle = resolveStyle(".combo.normal", AttributedStyle.DEFAULT);
        AttributedStyle focusedStyle = resolveStyle(".combo.focused", AttributedStyle.DEFAULT.inverse());
        AttributedStyle dropdownStyle = resolveStyle(".combo.dropdown", AttributedStyle.DEFAULT);
        AttributedStyle dropdownSelectedStyle =
                resolveStyle(".combo.dropdown.selected", AttributedStyle.DEFAULT.inverse());

        AttributedStyle style = isFocused() ? focusedStyle : normalStyle;

        // Draw closed combo box
        screen.fill(pos.x(), pos.y(), size.w(), 1, style);
        String selectedText = renderItem(selectedIndex);
        int textWidth = size.w() - 2; // reserve space for ▼
        if (selectedText.length() > textWidth) {
            selectedText = selectedText.substring(0, textWidth);
        }
        StringBuilder display = new StringBuilder(selectedText);
        while (display.length() < textWidth) {
            display.append(' ');
        }
        display.append(" \u25bc"); // ▼
        screen.text(pos.x(), pos.y(), new AttributedString(display.toString(), style));

        // Draw dropdown if open
        if (open && !items.isEmpty()) {
            int dropdownY = pos.y() + 1;
            int maxVisible = Math.min(items.size(), 8);
            for (int i = 0; i < maxVisible; i++) {
                AttributedStyle itemStyle = (i == dropdownFocusedIndex) ? dropdownSelectedStyle : dropdownStyle;
                screen.fill(pos.x(), dropdownY + i, size.w(), 1, itemStyle);
                String itemText = renderItem(i);
                if (itemText.length() > size.w()) {
                    itemText = itemText.substring(0, size.w());
                }
                screen.text(pos.x(), dropdownY + i, new AttributedString(itemText, itemStyle));
            }
        }
    }

    @Override
    protected Size doGetPreferredSize() {
        int maxWidth = 10;
        for (int i = 0; i < items.size(); i++) {
            maxWidth = Math.max(maxWidth, renderItem(i).length());
        }
        return new Size(maxWidth + 2, 1); // +2 for ▼ indicator
    }
}

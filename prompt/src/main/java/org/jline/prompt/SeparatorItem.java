/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

/**
 * Interface for separator items in lists and checkboxes.
 * Separators are non-selectable items that provide visual grouping.
 */
public interface SeparatorItem extends ListItem, CheckboxItem {

    /**
     * Get the separator text.
     *
     * @return the separator text
     */
    String getText();

    /**
     * Separators are never selectable.
     *
     * @return always false
     */
    @Override
    default boolean isSelectable() {
        return false;
    }

    /**
     * Separators don't have names for selection.
     *
     * @return always null
     */
    @Override
    default String getName() {
        return null;
    }

    /**
     * Separators are never disabled.
     *
     * @return always false
     */
    @Override
    default boolean isDisabled() {
        return false;
    }

    /**
     * Separators don't have disabled text.
     *
     * @return always null
     */
    @Override
    default String getDisabledText() {
        return null;
    }

    /**
     * Separators are never initially checked.
     *
     * @return always false
     */
    @Override
    default boolean isInitiallyChecked() {
        return false;
    }
}

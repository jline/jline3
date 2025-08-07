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
 * Builder for list prompts.
 */
public interface ListBuilder extends BaseBuilder<ListBuilder> {

    /**
     * Create a new item with the given ID.
     *
     * @param id the ID
     * @return this builder
     */
    ListBuilder newItem(String id);

    /**
     * Set the text for the current item.
     *
     * @param text the text
     * @return this builder
     */
    ListBuilder text(String text);

    /**
     * Set whether the current item is disabled.
     *
     * @param disabled whether the item is disabled
     * @return this builder
     */
    ListBuilder disabled(boolean disabled);

    /**
     * Set the disabled text for the current item.
     *
     * @param disabledText the disabled text
     * @return this builder
     */
    ListBuilder disabledText(String disabledText);

    /**
     * Add the current item to the list.
     *
     * @return this builder
     */
    ListBuilder add();

    /**
     * Convenience method to add a simple list item with name and text.
     *
     * @param name the item name/id
     * @param text the display text
     * @return this builder
     */
    default ListBuilder add(String name, String text) {
        return newItem(name).text(text).add();
    }

    /**
     * Convenience method to add a simple list item with name, text, and disabled state.
     *
     * @param name the item name/id
     * @param text the display text
     * @param disabled whether the item is disabled
     * @return this builder
     */
    default ListBuilder add(String name, String text, boolean disabled) {
        return newItem(name).text(text).disabled(disabled).add();
    }

    /**
     * Convenience method to add a simple list item with name, text, disabled state, and disabled text.
     *
     * @param name the item name/id
     * @param text the display text
     * @param disabled whether the item is disabled
     * @param disabledText the text to show when disabled
     * @return this builder
     */
    default ListBuilder add(String name, String text, boolean disabled, String disabledText) {
        return newItem(name)
                .text(text)
                .disabled(disabled)
                .disabledText(disabledText)
                .add();
    }

    /**
     * Set the page size for pagination.
     *
     * @param pageSize the page size, or 0 for no pagination
     * @return this builder
     */
    ListBuilder pageSize(int pageSize);

    /**
     * Set whether to show page indicators.
     *
     * @param showPageIndicator true to show page indicators
     * @return this builder
     */
    ListBuilder showPageIndicator(boolean showPageIndicator);

    /**
     * Create a new separator with no text.
     *
     * @return a separator builder
     */
    ListSeparatorBuilder newSeparator();

    /**
     * Create a new separator with the given text.
     *
     * @param text the separator text
     * @return a separator builder
     */
    ListSeparatorBuilder newSeparator(String text);
}

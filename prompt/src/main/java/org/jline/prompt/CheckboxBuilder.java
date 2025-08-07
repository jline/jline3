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
 * Builder for checkbox prompts.
 */
public interface CheckboxBuilder extends BaseBuilder<CheckboxBuilder> {

    /**
     * Create a new item with the given ID.
     *
     * @param id the ID
     * @return this builder
     */
    CheckboxBuilder newItem(String id);

    /**
     * Set the text for the current item.
     *
     * @param text the text
     * @return this builder
     */
    CheckboxBuilder text(String text);

    /**
     * Set whether the current item is checked.
     *
     * @param checked whether the item is checked
     * @return this builder
     */
    CheckboxBuilder checked(boolean checked);

    /**
     * Set whether the current item is disabled.
     *
     * @param disabled whether the item is disabled
     * @return this builder
     */
    CheckboxBuilder disabled(boolean disabled);

    /**
     * Set the disabled text for the current item.
     *
     * @param disabledText the disabled text
     * @return this builder
     */
    CheckboxBuilder disabledText(String disabledText);

    /**
     * Add the current item to the list.
     *
     * @return this builder
     */
    CheckboxBuilder add();

    /**
     * Convenience method to add a simple checkbox item with name and text.
     *
     * @param name the item name/id
     * @param text the display text
     * @return this builder
     */
    default CheckboxBuilder add(String name, String text) {
        return newItem(name).text(text).add();
    }

    /**
     * Convenience method to add a simple checkbox item with name, text, and checked state.
     *
     * @param name the item name/id
     * @param text the display text
     * @param checked whether the item is initially checked
     * @return this builder
     */
    default CheckboxBuilder add(String name, String text, boolean checked) {
        return newItem(name).text(text).checked(checked).add();
    }

    /**
     * Convenience method to add a checkbox item with all options.
     *
     * @param name the item name/id
     * @param text the display text
     * @param checked whether the item is initially checked
     * @param disabled whether the item is disabled
     * @return this builder
     */
    default CheckboxBuilder add(String name, String text, boolean checked, boolean disabled) {
        return newItem(name).text(text).checked(checked).disabled(disabled).add();
    }

    /**
     * Set the page size for pagination.
     *
     * @param pageSize the page size, or 0 for no pagination
     * @return this builder
     */
    CheckboxBuilder pageSize(int pageSize);

    /**
     * Set whether to show page indicators.
     *
     * @param showPageIndicator true to show page indicators
     * @return this builder
     */
    CheckboxBuilder showPageIndicator(boolean showPageIndicator);

    /**
     * Create a new separator with no text.
     *
     * @return a separator builder
     */
    CheckboxSeparatorBuilder newSeparator();

    /**
     * Create a new separator with the given text.
     *
     * @param text the separator text
     * @return a separator builder
     */
    CheckboxSeparatorBuilder newSeparator(String text);
}

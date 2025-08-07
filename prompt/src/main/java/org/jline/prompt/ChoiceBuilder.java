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
 * Builder for choice prompts.
 */
public interface ChoiceBuilder extends BaseBuilder<ChoiceBuilder> {

    /**
     * Create a new choice with the given ID.
     *
     * @param id the ID
     * @return this builder
     */
    ChoiceBuilder newChoice(String id);

    /**
     * Set the key for the current choice.
     *
     * @param key the key
     * @return this builder
     */
    ChoiceBuilder key(char key);

    /**
     * Set the text for the current choice.
     *
     * @param text the text
     * @return this builder
     */
    ChoiceBuilder text(String text);

    /**
     * Set the help text for the current choice.
     *
     * @param helpText the help text
     * @return this builder
     */
    ChoiceBuilder helpText(String helpText);

    /**
     * Set whether the current choice is the default.
     *
     * @param defaultChoice whether the choice is the default
     * @return this builder
     */
    ChoiceBuilder defaultChoice(boolean defaultChoice);

    /**
     * Add the current choice to the list.
     *
     * @return this builder
     */
    ChoiceBuilder add();

    /**
     * Convenience method to add a simple choice item with name, text, and key.
     *
     * @param name the item name/id
     * @param text the display text
     * @param key the key to select this choice
     * @return this builder
     */
    default ChoiceBuilder add(String name, String text, char key) {
        return newChoice(name).text(text).key(key).add();
    }

    /**
     * Convenience method to add a choice item with name, text, key, and default state.
     *
     * @param name the item name/id
     * @param text the display text
     * @param key the key to select this choice
     * @param defaultChoice whether this is the default choice
     * @return this builder
     */
    default ChoiceBuilder add(String name, String text, char key, boolean defaultChoice) {
        return newChoice(name).text(text).key(key).defaultChoice(defaultChoice).add();
    }

    /**
     * Convenience method to add a choice item with all options.
     *
     * @param name the item name/id
     * @param text the display text
     * @param key the key to select this choice
     * @param helpText the help text for this choice
     * @param defaultChoice whether this is the default choice
     * @return this builder
     */
    default ChoiceBuilder add(String name, String text, char key, String helpText, boolean defaultChoice) {
        return newChoice(name)
                .text(text)
                .key(key)
                .helpText(helpText)
                .defaultChoice(defaultChoice)
                .add();
    }
}

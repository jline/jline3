package org.jline.consoleui.elements.items;

/**
 * User: Andreas Wegmann
 * Date: 13.01.16
 */
public interface ChoiceItemIF extends ConsoleUIItemIF, ListItemIF {
    default boolean isDefaultChoice() {
        return false;
    }

    default Character getKey() {
        return ' ';
    }
}

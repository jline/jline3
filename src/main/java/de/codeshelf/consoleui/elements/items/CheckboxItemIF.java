package de.codeshelf.consoleui.elements.items;

/**
 * User: Andreas Wegmann
 * Date: 01.01.16
 */
public interface CheckboxItemIF extends ConsoleUIItemIF {
    default boolean isChecked() {
        return false;
    }
}

package de.codeshelf.consoleui.elements.items;

/**
 * User: Andreas Wegmann
 * Date: 01.01.16
 */
public interface ConsoleUIItemIF {
  boolean isSelectable();

  String getName();

  default boolean isDisabled() {
    return false;
  }

  String getText();

  default String getDisabledText() {
    return "";
  }
}

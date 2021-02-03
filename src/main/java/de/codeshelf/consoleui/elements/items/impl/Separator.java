package de.codeshelf.consoleui.elements.items.impl;

import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.ChoiceItemIF;
import de.codeshelf.consoleui.elements.items.ListItemIF;

/**
 * User: Andreas Wegmann
 * Date: 01.01.16
 */
public class Separator implements CheckboxItemIF, ListItemIF, ChoiceItemIF {
  private String message;

  public Separator(String message) {
    this.message = message;
  }

  public Separator() {
  }

  public String getMessage() {
    return message;
  }

  public String getText() {
    return message;
  }

  public boolean isSelectable() {
    return false;
  }

  @Override
  public String getName() {
    return null;
  }

}

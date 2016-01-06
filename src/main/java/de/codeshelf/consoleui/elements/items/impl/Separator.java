package de.codeshelf.consoleui.elements.items.impl;

import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.ListItemIF;

/**
 * User: andy
 * Date: 01.01.16
 */
public class Separator implements CheckboxItemIF, ListItemIF {
  private String message;

  public Separator(String message) {
    this.message = message;
  }

  public Separator() {
  }

  public String getMessage() {
    return message;
  }

  public boolean isSelectable() {
    return false;
  }

}

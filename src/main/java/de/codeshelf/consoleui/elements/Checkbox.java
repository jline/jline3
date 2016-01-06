package de.codeshelf.consoleui.elements;

import de.codeshelf.consoleui.elements.items.CheckboxItemIF;

import java.util.List;

/**
 * User: andy
 * Date: 01.01.16
 */
public class Checkbox extends AbstractPromptableElement {

  private List<CheckboxItemIF> checkboxItemList;

  public Checkbox(String message, String name, List<CheckboxItemIF> checkboxItemList) {
    super(message,name);
    this.checkboxItemList = checkboxItemList;
  }

  public String getMessage() {
    return message;
  }

  public List<CheckboxItemIF> getCheckboxItemList() {
    return checkboxItemList;
  }
}

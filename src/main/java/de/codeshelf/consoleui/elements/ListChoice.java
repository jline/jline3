package de.codeshelf.consoleui.elements;

import de.codeshelf.consoleui.elements.items.ListItemIF;

import java.util.List;

/**
 * User: andy
 * Date: 04.01.16
 */
public class ListChoice extends AbstractPromptableElement {

  private List<ListItemIF> listItemList;

  public ListChoice(String message, String name, List<ListItemIF> listItemList) {
    super(message,name);
    this.listItemList = listItemList;
  }

  public String getMessage() {
    return message;
  }

  public List<ListItemIF> getListItemList() {
    return listItemList;
  }
}

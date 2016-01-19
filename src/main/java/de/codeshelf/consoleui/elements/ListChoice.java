package de.codeshelf.consoleui.elements;

import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import de.codeshelf.consoleui.elements.items.ListItemIF;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Andreas Wegmann
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

  public ArrayList<ConsoleUIItemIF> getListItemList() {
    return new ArrayList<ConsoleUIItemIF>(listItemList);
  }
}

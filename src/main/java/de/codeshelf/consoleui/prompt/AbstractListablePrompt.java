package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import de.codeshelf.consoleui.elements.items.ListItemIF;

import java.io.IOException;
import java.util.ArrayList;

/**
 * User: Andreas Wegmann
 * Date: 19.01.16
 */
public abstract class AbstractListablePrompt extends AbstractPrompt {

  protected int selectedItemIndex;
  ArrayList<? extends ConsoleUIItemIF> itemList;

  public AbstractListablePrompt() throws IOException {
  }

  protected int getNextSelectableItemIndex() {
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex + 1 + i) % itemList.size();
      ConsoleUIItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  protected int getPreviousSelectableItemIndex() {
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex - 1 - i + itemList.size()) % itemList.size();
      ConsoleUIItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  protected int getFirstSelectableItemIndex() {
    int index = 0;
    for (ConsoleUIItemIF item : itemList) {
      if (item.isSelectable())
        return index;
      index++;
    }
    throw new IllegalStateException("no selectable item in list");
  }
}

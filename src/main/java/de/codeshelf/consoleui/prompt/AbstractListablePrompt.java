package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Abstract base class for all listable prompts (checkbox, list and expandable choice).
 * This class contains some helper methods for the list prompts.
 *
 * User: Andreas Wegmann
 * Date: 19.01.16
 */
public abstract class AbstractListablePrompt extends AbstractPrompt {

  // holds the index of the selected item (of course)
  protected int selectedItemIndex;

  // the item list of the prompt
  protected ArrayList<? extends ConsoleUIItemIF> itemList;

  /**
   * Empty default constructor.
   * @throws IOException  may be thrown by super class
   */
  public AbstractListablePrompt() throws IOException {
    super();
  }

  /**
   * Find the next selectable Item (user pressed 'down').
   *
   * @return index of the next selectable item.
   */
  protected int getNextSelectableItemIndex() {
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex + 1 + i) % itemList.size();
      ConsoleUIItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  /**
   * Find the previous selectable item (user pressed 'up').
   *
   * @return index of the previous selectable item.
   */
  protected int getPreviousSelectableItemIndex() {
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex - 1 - i + itemList.size()) % itemList.size();
      ConsoleUIItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  /**
   * Find the first selectable item of the item list.
   *
   * @return index of the first selectable item.
   * @throws IllegalStateException if no item is selectable.
   */
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

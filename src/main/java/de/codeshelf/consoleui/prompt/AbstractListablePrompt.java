package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.PageSizeType;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import jline.Terminal;
import jline.TerminalFactory;

import java.io.IOException;
import java.util.ArrayList;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Abstract base class for all listable prompts (checkbox, list and expandable choice).
 * This class contains some helper methods for the list prompts.
 * <p>
 * User: Andreas Wegmann
 * Date: 19.01.16
 */
public abstract class AbstractListablePrompt extends AbstractPrompt {

  // holds the index of the selected item (of course)
  protected int selectedItemIndex;

  // the item list of the prompt
  protected ArrayList<? extends ConsoleUIItemIF> itemList;
  protected Terminal terminal;
  /**
   * first item in view
   */
  protected int topDisplayedItem;
  protected int nearBottomMargin = 3;
  protected int viewPortHeight;
  protected Rollover rollOverMode;

  /**
   * Empty default constructor.
   *
   * @throws IOException may be thrown by super class
   */
  public AbstractListablePrompt() throws IOException {
    super();
    terminal = TerminalFactory.get();
  }

  /**
   * total height of screen
   * @return number of lines on terminal.
   */
  protected int screenHeight() {
    return terminal.getHeight();
  }

  /**
   * calculate renderHeight by relative or absolute type, screen size and item count.
   */
  protected void initRendering() {
    topDisplayedItem = 0;
    if (getPageSizeType() == PageSizeType.ABSOLUTE)
      // requested absolute page size plus header plus baseline, but not bigger than screen size
      renderHeight = Math.min(screenHeight(), getPageSize() + 2);
    else {
      // relative page size for complete list plus header plus baseline
      renderHeight = (screenHeight()) * getPageSize() / 100;
    }

    // if less items than renderHeight, we reduce the height
    renderHeight = Math.min(renderHeight, getItemSize() + 2);

    // renderHeight must be at least 3, for a single list item. may be smaller with relative or absolute
    // settings, so we correct this to at least 3.
    renderHeight = Math.max(3, renderHeight);

    // viewPortHeight is the height of the list items itself, without header and baseline
    viewPortHeight = renderHeight - 2;

    // if list size is bigger than viewPort, then we disable the rollover feature.
    if (viewPortHeight == getItemSize())
      rollOverMode = Rollover.ALLOWED;
    else
      rollOverMode = Rollover.HALT_AT_LIST_END;
  }

  abstract protected int getPageSize();

  abstract protected PageSizeType getPageSizeType();

  protected void gotoRenderTop() {
    System.out.println(ansi().cursorUp(renderHeight));
    //System.out.println(ansi().cursor(0, screenHeight() - renderHeight));
  }

  enum Rollover {ALLOWED, HALT_AT_LIST_END}

  /**
   * Find the next selectable Item (user pressed 'down').
   *
   * @param rollover enables or disables rollover feature when searching for next item
   * @return index of the next selectable item.
   */
  protected int getNextSelectableItemIndex(Rollover rollover) {
    if (rollover == Rollover.ALLOWED)
      return getNextSelectableItemIndexWithRollover();
    else
      return getNextSelectableItemIndexWithoutRollover();
  }

  /**
   * Find the next selectable Item (user pressed 'down').
   *
   * @return index of the next selectable item.
   */
  private int getNextSelectableItemIndexWithRollover() {
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex + 1 + i) % itemList.size();
      ConsoleUIItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  /**
   * Find the next selectable Item (user pressed 'down'), but does not start at the beginning
   * if end of list is reached.
   *
   * @return index of the next selectable item.
   */
  private int getNextSelectableItemIndexWithoutRollover() {
    for (int newIndex = selectedItemIndex + 1; newIndex < itemList.size(); newIndex++) {
      ConsoleUIItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  /**
   * Find the previous selectable item (user pressed 'up').
   *
   * @param rollover enables or disables rollover feature when searching for previous item
   * @return index of the previous selectable item.
   */
  protected int getPreviousSelectableItemIndex(Rollover rollover) {
    if (rollover == Rollover.ALLOWED)
      return getPreviousSelectableItemIndexWithRollover();
    else
      return getPreviousSelectableItemIndexWithoutRollover();
  }

  /**
   * Find the previous selectable item (user pressed 'up').
   *
   * @return index of the previous selectable item.
   */
  private int getPreviousSelectableItemIndexWithRollover() {
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex - 1 - i + itemList.size()) % itemList.size();
      ConsoleUIItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  /**
   * Find the previous selectable item (user pressed 'up'), but does not start at end of list if
   * beginning of list is reached.
   *
   * @return index of the previous selectable item.
   */
  private int getPreviousSelectableItemIndexWithoutRollover() {
    for (int newIndex = selectedItemIndex - 1; newIndex >= 0; newIndex--) {
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

  protected void recalculateViewWindow(boolean upward, boolean downward) {
    if (viewPortHeight < getItemSize()) {
      if (downward && itemsBelowEnd() && selectedItemNearBottom())
        topDisplayedItem++;
      if (upward && itemsAboveTop() && selectedItemNearTop())
        topDisplayedItem--;
    }
  }

  private boolean selectedItemNearTop() {
    return topDisplayedItem + nearBottomMargin - 1 > selectedItemIndex;
  }

  private boolean itemsAboveTop() {
    return topDisplayedItem > 0;
  }

  private boolean selectedItemNearBottom() {
    return selectedItemIndex + nearBottomMargin > topDisplayedItem + viewPortHeight;
  }

  private boolean itemsBelowEnd() {
    return topDisplayedItem + viewPortHeight < getItemSize();
  }

  abstract int getItemSize();
}

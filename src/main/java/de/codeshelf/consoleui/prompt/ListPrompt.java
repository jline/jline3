package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.ListChoice;
import de.codeshelf.consoleui.elements.PageSizeType;
import de.codeshelf.consoleui.elements.items.impl.ListItem;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;

import java.io.IOException;

/**
 * ListPrompt implements the list choice handling.
 * <p>
 * User: Andreas Wegmann
 * Date: 01.01.16
 */
public class ListPrompt extends AbstractListablePrompt implements PromptIF<ListChoice, ListResult> {

  // the list to let the user choose from
  protected ListChoice listChoice;

  /**
   * helper class with render functionality.
   */
  CUIRenderer itemRenderer = CUIRenderer.getRenderer();

  /**
   * Empty default constructor.
   *
   * @throws IOException may be thrown by super class
   */
  public ListPrompt() throws IOException {
    super();
  }

  @Override
  protected int getPageSize() {
    return listChoice.getPageSize();
  }

  @Override
  protected PageSizeType getPageSizeType() {
    return listChoice.getPageSizeType();
  }

  @Override
  protected int getItemSize() {
    return itemList.size();
  }

  /**
   * Prompt the user for selecting zero to many choices from a checkbox.
   *
   * @param listChoice list with items to choose from.
   * @return {@link ListResult} which holds the users choices.
   * @throws IOException may be thrown by console reader
   */
  public ListResult prompt(ListChoice listChoice) throws IOException {
    this.listChoice = listChoice;
    itemList = listChoice.getListItemList();
    if (reader == null) {
      reader = new ConsoleReaderImpl();
    }
    reader.addAllowedPrintableKey('j');
    reader.addAllowedPrintableKey('k');
    reader.addAllowedPrintableKey(' ');
    reader.addAllowedSpecialKey(ReaderIF.SpecialKey.DOWN);
    reader.addAllowedSpecialKey(ReaderIF.SpecialKey.UP);
    reader.addAllowedSpecialKey(ReaderIF.SpecialKey.ENTER);

    selectedItemIndex = getFirstSelectableItemIndex();

    initRendering();
    render();
    ReaderIF.ReaderInput readerInput = reader.read();
    while (readerInput.getSpecialKey() != ReaderIF.SpecialKey.ENTER) {
      boolean downward = false;
      boolean upward = false;

      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.PRINTABLE_KEY) {
        if (readerInput.getPrintableKey().equals('j')) {
          selectedItemIndex = getNextSelectableItemIndex(rollOverMode);
          downward = true;
        } else if (readerInput.getPrintableKey().equals('k')) {
          selectedItemIndex = getPreviousSelectableItemIndex(rollOverMode);
          upward = true;
        }
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.DOWN) {
        selectedItemIndex = getNextSelectableItemIndex(rollOverMode);
        downward = true;
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.UP) {
        selectedItemIndex = getPreviousSelectableItemIndex(rollOverMode);
        upward = true;
      }
      if (upward || downward)
        recalculateViewWindow(upward, downward);
      gotoRenderTop();
      render();
      readerInput = reader.read();
    }

    ListItem listItem = (ListItem) itemList.get(selectedItemIndex);
    ListResult selection = new ListResult(listItem.getName());
    renderMessagePromptAndResult(listChoice.getMessage(), ((ListItem) itemList.get(selectedItemIndex)).getText());
    return selection;
  }

  private void render() {
    int itemNumber;
    int renderedLines = 0;

    System.out.println(renderMessagePrompt(listChoice.getMessage()));
    for (itemNumber = topDisplayedItem; renderedLines < viewPortHeight; itemNumber++, renderedLines++) {
      String renderedItem = itemRenderer.render(itemList.get(itemNumber), (selectedItemIndex == itemNumber));
      System.out.println(renderedItem);
    }
  }
}

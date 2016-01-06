package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.ListChoice;
import de.codeshelf.consoleui.elements.items.ListItemIF;
import de.codeshelf.consoleui.elements.items.impl.ListItem;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: andy
 * Date: 01.01.16
 */
public class ListPrompt extends AbstractPrompt implements PromptIF<ListChoice> {
  private ListChoice listChoice;

  ReaderIF reader;

  public void setReader(ReaderIF reader) {
    this.reader = reader;
  }

  int selectedItemIndex;
  CUIRenderer itemRenderer = CUIRenderer.getRenderer();

  public ListPrompt() {
  }


  private void render() {
    int itemNumber = 0;
    List<ListItemIF> itemList = listChoice.getListItemList();

    if (renderHeight == 0) {
      renderHeight = 2 + itemList.size();
    } else {
      System.out.println(ansi().cursorUp(renderHeight));
    }

    System.out.println(renderMessagePrompt(listChoice.getMessage()));
    for (ListItemIF listItem : itemList) {
      String renderedItem = itemRenderer.render(listItem,(selectedItemIndex == itemNumber));
      System.out.println(renderedItem);
      itemNumber++;
    }
  }

  public LinkedHashSet<String> prompt(ListChoice listChoice) throws IOException {
    this.listChoice = listChoice;
    List<ListItemIF> itemList = listChoice.getListItemList();
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

    render();
    ReaderIF.ReaderInput readerInput = reader.read();
    while (readerInput.getSpecialKey() != ReaderIF.SpecialKey.ENTER) {
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.PRINTABLE_KEY) {
        if (readerInput.getPrintableKey().equals('j')) {
          selectedItemIndex = getNextSelectableItemIndex();
        } else if (readerInput.getPrintableKey().equals('k')) {
          selectedItemIndex = getPreviousSelectableItemIndex();
        }
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.DOWN) {
        selectedItemIndex = getNextSelectableItemIndex();
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.UP) {
        selectedItemIndex = getPreviousSelectableItemIndex();
      }

      render();
      readerInput = reader.read();
    }

    LinkedHashSet<String> selection = new LinkedHashSet<String>();

    ListItem listItem = (ListItem) itemList.get(selectedItemIndex);
    selection.add(listItem.getName());
    renderMessagePromptAndResult(listChoice.getMessage(),selection.toString());
    return selection ;
  }


  private int getNextSelectableItemIndex() {
    List<ListItemIF> itemList = listChoice.getListItemList();
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex + 1 + i) % itemList.size();
      ListItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  private int getPreviousSelectableItemIndex() {
    List<ListItemIF> itemList = listChoice.getListItemList();
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex - 1 - i + itemList.size()) % itemList.size();
      ListItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  private int getFirstSelectableItemIndex() {
    int index = 0;
    List<ListItemIF> itemList = listChoice.getListItemList();
    for (ListItemIF item : itemList) {
      if (item.isSelectable())
        return index;
      index++;
    }
    throw new IllegalStateException("no selectable item in list");
  }


}

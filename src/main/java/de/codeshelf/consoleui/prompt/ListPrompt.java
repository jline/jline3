package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.ListChoice;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import de.codeshelf.consoleui.elements.items.impl.ListItem;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;

import java.io.IOException;
import java.util.LinkedHashSet;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 01.01.16
 */
public class ListPrompt extends AbstractListablePrompt implements PromptIF<ListChoice,ListResult> {
  private ListChoice listChoice;

  ReaderIF reader;

  public void setReader(ReaderIF reader) {
    this.reader = reader;
  }

  CUIRenderer itemRenderer = CUIRenderer.getRenderer();

  public ListPrompt() throws IOException {
    super();
  }


  private void render() {
    int itemNumber = 0;

    if (renderHeight == 0) {
      renderHeight = 2 + itemList.size();
    } else {
      System.out.println(ansi().cursorUp(renderHeight));
    }

    System.out.println(renderMessagePrompt(listChoice.getMessage()));
    for (ConsoleUIItemIF listItem : itemList) {
      String renderedItem = itemRenderer.render(listItem,(selectedItemIndex == itemNumber));
      System.out.println(renderedItem);
      itemNumber++;
    }
  }

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


    ListItem listItem = (ListItem) itemList.get(selectedItemIndex);
    ListResult selection=new ListResult(listItem.getName());
    renderMessagePromptAndResult(listChoice.getMessage(),selection.toString());
    return selection ;
  }


}

package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.Checkbox;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import de.codeshelf.consoleui.elements.items.impl.CheckboxItem;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.LinkedHashSet;

public class CheckboxPrompt extends AbstractListablePrompt implements PromptIF<Checkbox> {
  private Checkbox checkbox;
  ReaderIF reader;

  public void setReader(ReaderIF reader) {
    this.reader = reader;
  }

  CUIRenderer itemRenderer = CUIRenderer.getRenderer();

  private void render() {
    int itemNumber = 0;

    if (this.renderHeight == 0) {
      this.renderHeight = (2 + itemList.size());
    } else {
      System.out.println(Ansi.ansi().cursorUp(this.renderHeight));
    }
    System.out.println(renderMessagePrompt(this.checkbox.getMessage()));
    for (ConsoleUIItemIF checkboxItem : itemList) {
      String renderedItem = this.itemRenderer.render(checkboxItem, this.selectedItemIndex == itemNumber);
      System.out.println(renderedItem);
      itemNumber++;
    }
  }

  public LinkedHashSet<String> prompt(Checkbox checkbox)
          throws IOException {
    this.checkbox = checkbox;
    itemList = this.checkbox.getCheckboxItemList();

    if (this.reader == null) {
      this.reader = new ConsoleReaderImpl();
    }
    this.reader.addAllowedPrintableKey('j');
    this.reader.addAllowedPrintableKey('k');
    this.reader.addAllowedPrintableKey(' ');
    this.reader.addAllowedSpecialKey(ReaderIF.SpecialKey.DOWN);
    this.reader.addAllowedSpecialKey(ReaderIF.SpecialKey.UP);
    this.reader.addAllowedSpecialKey(ReaderIF.SpecialKey.ENTER);

    this.selectedItemIndex = getFirstSelectableItemIndex();

    render();
    ReaderIF.ReaderInput readerInput = this.reader.read();
    while (readerInput.getSpecialKey() != ReaderIF.SpecialKey.ENTER) {
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.PRINTABLE_KEY) {
        if (readerInput.getPrintableKey().equals(' ')) {
          toggleSelection();
        } else if (readerInput.getPrintableKey().equals('j')) {
          this.selectedItemIndex = getNextSelectableItemIndex();
        } else if (readerInput.getPrintableKey().equals('k')) {
          this.selectedItemIndex = getPreviousSelectableItemIndex();
        }
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.DOWN) {
        this.selectedItemIndex = getNextSelectableItemIndex();
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.UP) {
        this.selectedItemIndex = getPreviousSelectableItemIndex();
      }
      render();
      readerInput = this.reader.read();
    }
    LinkedHashSet<String> selections = new LinkedHashSet<String>();
    for (ConsoleUIItemIF item : itemList) {
      if ((item instanceof CheckboxItem)) {
        CheckboxItem checkboxItem = (CheckboxItem) item;
        if (checkboxItem.isChecked()) {
          selections.add(checkboxItem.getName());
        }
      }
    }
    renderMessagePromptAndResult(checkbox.getMessage(), selections.toString());
    return selections;
  }

  private void toggleSelection() {
    CheckboxItem checkboxItem = (CheckboxItem) itemList.get(this.selectedItemIndex);
    checkboxItem.setChecked(!checkboxItem.isChecked());
  }
}

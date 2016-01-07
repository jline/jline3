package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.Checkbox;
import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.impl.CheckboxItem;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

public class CheckboxPrompt extends AbstractPrompt implements PromptIF<Checkbox> {
  private Checkbox checkbox;
  ReaderIF reader;
  int selectedItemIndex;

  public void setReader(ReaderIF reader) {
    this.reader = reader;
  }

  CUIRenderer itemRenderer = CUIRenderer.getRenderer();

  private void render() {
    int itemNumber = 0;
    List<CheckboxItemIF> itemList = this.checkbox.getCheckboxItemList();
    if (this.renderHeight == 0) {
      this.renderHeight = (2 + itemList.size());
    } else {
      System.out.println(Ansi.ansi().cursorUp(this.renderHeight));
    }
    System.out.println(renderMessagePrompt(this.checkbox.getMessage()));
    for (CheckboxItemIF checkboxItem : itemList) {
      String renderedItem = this.itemRenderer.render(checkboxItem, this.selectedItemIndex == itemNumber);
      System.out.println(renderedItem);
      itemNumber++;
    }
  }

  public LinkedHashSet<String> prompt(Checkbox checkbox)
          throws IOException {
    this.checkbox = checkbox;
    List<CheckboxItemIF> itemList = checkbox.getCheckboxItemList();
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
    for (CheckboxItemIF item : itemList) {
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

  private int getNextSelectableItemIndex() {
    List<CheckboxItemIF> itemList = this.checkbox.getCheckboxItemList();
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (this.selectedItemIndex + 1 + i) % itemList.size();
      CheckboxItemIF item = itemList.get(newIndex);
      if (item.isSelectable()) {
        return newIndex;
      }
    }
    return this.selectedItemIndex;
  }

  private int getPreviousSelectableItemIndex() {
    List<CheckboxItemIF> itemList = this.checkbox.getCheckboxItemList();
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (this.selectedItemIndex - 1 - i + itemList.size()) % itemList.size();
      CheckboxItemIF item = itemList.get(newIndex);
      if (item.isSelectable()) {
        return newIndex;
      }
    }
    return this.selectedItemIndex;
  }

  private int getFirstSelectableItemIndex() {
    int index = 0;
    List<CheckboxItemIF> itemList = this.checkbox.getCheckboxItemList();
    for (CheckboxItemIF item : itemList) {
      if (item.isSelectable()) {
        return index;
      }
      index++;
    }
    throw new IllegalStateException("no selectable item in list");
  }

  private void toggleSelection() {
    List<CheckboxItemIF> itemList = this.checkbox.getCheckboxItemList();
    CheckboxItem checkboxItem = (CheckboxItem) itemList.get(this.selectedItemIndex);
    checkboxItem.setChecked(!checkboxItem.isChecked());
  }
}

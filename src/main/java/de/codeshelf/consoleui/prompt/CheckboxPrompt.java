package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.Checkbox;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import de.codeshelf.consoleui.elements.items.impl.CheckboxItem;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;

/**
 * CheckboxPrompt implements the checkbox choice handling.
 */
public class CheckboxPrompt extends AbstractListablePrompt implements PromptIF<Checkbox,CheckboxResult> {

  // checkbox object to prompt the user for.
  private Checkbox checkbox;

  /**
   * helper class with render functionality.
   */
  CUIRenderer itemRenderer = CUIRenderer.getRenderer();

  /**
   * Empty default constructor.
   * @throws IOException  may be thrown by super class
   */
  public CheckboxPrompt() throws IOException {
    super();
  }


  /**
   * render the checkbox on the terminal.
   */
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

  /**
   * Prompt the user for selecting zero to many choices from a checkbox.
   *
   * @param checkbox checkbox with items to choose from.
   * @return {@link CheckboxResult} which holds the users choices.
   *
   * @throws IOException  may be thrown by console reader
   */
  public CheckboxResult prompt(Checkbox checkbox)
          throws IOException {
    this.checkbox = checkbox;
    itemList = this.checkbox.getCheckboxItemList();

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

    LinkedHashSet<String> selections = new LinkedHashSet<>();

    for (ConsoleUIItemIF item : itemList) {
      if ((item instanceof CheckboxItem)) {
        CheckboxItem checkboxItem = (CheckboxItem) item;
        if (checkboxItem.isChecked()) {
          selections.add(checkboxItem.getName());
        }
      }
    }
    renderMessagePromptAndResult(checkbox.getMessage(), selections.toString());
    return new CheckboxResult(selections);
  }

  /**
   * Toggles the selection of the currently selected checkbox item.
   */
  private void toggleSelection() {
    CheckboxItem checkboxItem = (CheckboxItem) itemList.get(this.selectedItemIndex);
    checkboxItem.setChecked(!checkboxItem.isChecked());
  }
}

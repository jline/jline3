package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.ExpandableChoice;
import de.codeshelf.consoleui.elements.items.impl.ChoiceItem;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.LinkedHashSet;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: andy
 * Date: 07.01.16
 */
public class ExpandableChoicePrompt extends AbstractPrompt implements PromptIF<ExpandableChoice> {
  private ConsoleReaderImpl reader;
  private ExpandableChoice expandableChoice;
  CUIRenderer itemRenderer = CUIRenderer.getRenderer();
  private int selectedItemIndex;
  ChoiceItem choosenItem;
  ChoiceItem defaultItem;
  private ChoiceItem errorMessageItem = new ChoiceItem(' ', "error", "Please enter a valid command");

  enum RenderState {
    FOLDED,
    FOLDED_ANSWERED,
    EXPANDED
  }

  RenderState renderState = RenderState.FOLDED;
  LinkedHashSet<ChoiceItem> choiceItems;
  String promptString;

  private void render() {
    if (renderState == RenderState.EXPANDED) {
      int itemNumber = 0;
      LinkedHashSet<ChoiceItem> itemList = expandableChoice.getChoiceItems();

      if (renderHeight == 0) {
        renderHeight = 2 + itemList.size();
      } else {
        System.out.println(ansi().cursorUp(renderHeight));
      }

      for (ChoiceItem choiceItem : itemList) {
        String renderedItem = itemRenderer.render(choiceItem, (selectedItemIndex == itemNumber));
        System.out.println(renderedItem);
        itemNumber++;
      }
    } else if (renderState == RenderState.FOLDED) {
      System.out.println("");
      System.out.println(ansi().eraseLine().cursorUp(2));
      System.out.print(renderMessagePrompt(expandableChoice.getMessage()) + " (" + promptString + ") ");
      System.out.flush();
      renderHeight = 1;
    } else if (renderState == RenderState.FOLDED_ANSWERED) {
      System.out.println("");
      System.out.println(ansi().fg(Ansi.Color.CYAN).a(">> ").reset().a(choosenItem.getMessage()));
      System.out.print(ansi().cursorUp(2));
      System.out.print(renderMessagePrompt(expandableChoice.getMessage()) + " (" + promptString + ") ");
      System.out.flush();
      renderHeight = 1;
    }
  }


  public LinkedHashSet<String>

  prompt(ExpandableChoice expandableChoice) throws IOException {
    this.expandableChoice = expandableChoice;
    if (reader == null) {
      reader = new ConsoleReaderImpl();
    }

    choiceItems = expandableChoice.getChoiceItems();
    promptString = "";

    for (ChoiceItem choiceItem : choiceItems) {
      if (choiceItem.getKey() == 'h') {
        throw new IllegalStateException("expandableChoice may not use the reserved key 'h' for an element.");
      }
      if (defaultItem == null) {
        defaultItem = choiceItem;
      }
      reader.addAllowedPrintableKey(choiceItem.getKey());
      promptString += choiceItem.getKey();
    }

    choiceItems.add(new ChoiceItem('h',"help","Help, list all options"));
    reader.addAllowedPrintableKey('h');
    promptString += "h";
    System.out.println("promptString = " + promptString);
    reader.addAllowedSpecialKey(ReaderIF.SpecialKey.ENTER);
    reader.addAllowedSpecialKey(ReaderIF.SpecialKey.BACKSPACE);
    renderState = RenderState.FOLDED;

    // first render call, we don't need to position the cursor up
    renderHeight = 1;
    render();

    ReaderIF.ReaderInput readerInput = this.reader.read();
    while (true) {
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.ENTER) {
        if (choosenItem != null && choosenItem.getKey() == 'h') {
          renderState = RenderState.EXPANDED;
          render();
          readerInput = this.reader.read();
        } else {
          LinkedHashSet<String> hashSet = new LinkedHashSet<String>();
          System.out.println("");
          if (choosenItem != null) {
            renderMessagePromptAndResult(expandableChoice.getMessage(),choosenItem.getMessage());
            hashSet.add(choosenItem.getName());
          } else {
            renderMessagePromptAndResult(expandableChoice.getMessage(),defaultItem.getMessage());
            hashSet.add(defaultItem.getName());
          }
          return hashSet;
        }
      }
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.PRINTABLE_KEY) {
        Character pressedKey = readerInput.getPrintableKey();
        if (promptString.toLowerCase().contains("" + pressedKey)) {
          // find the new choosen item
          for (ChoiceItem choiceItem : choiceItems) {
            if (choiceItem.getKey() == pressedKey) {
              choosenItem = choiceItem;
              break;
            }
          }
        } else {
          // not in valid choices
          choosenItem = errorMessageItem;
        }
        renderState = RenderState.FOLDED_ANSWERED;
        render();
        readerInput = this.reader.read();
      }
    }
  }
}
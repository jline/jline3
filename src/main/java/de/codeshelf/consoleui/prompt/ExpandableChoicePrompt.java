package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.ExpandableChoice;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import de.codeshelf.consoleui.elements.items.impl.ChoiceItem;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.ArrayList;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Implementation of the expandable choice. The user is asked a question to be answered with
 * a single key. Each key represents a choice from a given set of items.
 *
 * Items with the key 'h' are not allowed. This key is reserved for the help message. With the
 * help message the prompt can be expanded to a list with the answers.
 *
 * User: Andreas Wegmann
 * Date: 07.01.16
 */
public class ExpandableChoicePrompt extends AbstractListablePrompt implements PromptIF<ExpandableChoice, ExpandableChoiceResult> {
  private ExpandableChoice expandableChoice;
  CUIRenderer itemRenderer = CUIRenderer.getRenderer();
  ChoiceItem chosenItem;
  ChoiceItem defaultItem;
  private ChoiceItem errorMessageItem = new ChoiceItem(' ', "error", resourceBundle.getString("please.enter.a.valid.command"), false);

  public ExpandableChoicePrompt() throws IOException {
  }

  enum RenderState {
    FOLDED,
    FOLDED_ANSWERED,
    EXPANDED
  }

  RenderState renderState = RenderState.FOLDED;
  ArrayList<ConsoleUIItemIF> choiceItems;
  String promptString;

  private void render() {
    if (renderState == RenderState.EXPANDED) {
      renderList();
    } else if (renderState == RenderState.FOLDED) {
      System.out.println("");
      System.out.println(ansi().eraseLine().cursorUp(2));
      System.out.print(renderMessagePrompt(expandableChoice.getMessage()) + " (" + promptString + ") ");
      System.out.flush();
      renderHeight = 1;
    } else if (renderState == RenderState.FOLDED_ANSWERED) {
      System.out.println("");
      System.out.println(ansi().fg(Ansi.Color.CYAN).a(">> ").reset().a(chosenItem.getMessage()).eraseLine());
      System.out.print(ansi().cursorUp(2));
      System.out.print(renderMessagePrompt(expandableChoice.getMessage()) + " (" + promptString + ") ");
      System.out.flush();
      renderHeight = 1;
    }
  }

  private void renderList() {
    if (renderHeight == 1) {
      // first time we expand the list...
      renderHeight = 1 + itemList.size();
      System.out.println("");
      System.out.println(ansi().eraseLine().cursorUp(2).a(renderMessagePrompt(expandableChoice.getMessage())).eraseLine(Ansi.Erase.FORWARD));
      System.out.flush();
    } else {
      System.out.println(ansi().cursorUp(renderHeight));
    }

    int itemNumber = 0;
    for (ConsoleUIItemIF choiceItem : itemList) {
      String renderedItem = itemRenderer.render(choiceItem, (selectedItemIndex == itemNumber));
      System.out.println(renderedItem + ansi().eraseLine(Ansi.Erase.FORWARD));
      itemNumber++;
    }
  }

  public ExpandableChoiceResult prompt(ExpandableChoice expandableChoice) throws IOException {
    this.expandableChoice = expandableChoice;

    choiceItems = expandableChoice.getChoiceItems();
    promptString = "";

    for (ConsoleUIItemIF choiceItem : choiceItems) {
      if (choiceItem instanceof ChoiceItem) {
        ChoiceItem item = (ChoiceItem) choiceItem;

        if (item.getKey() == 'h') {
          throw new IllegalStateException("you may not use the reserved key 'h' for an element of expandableChoice.");
        }
        if (defaultItem == null) {
          defaultItem = item;
        }
        reader.addAllowedPrintableKey(item.getKey());
        promptString += item.isDefaultChoice() ? item.getKey().toString().toUpperCase() : item.getKey();
      }
    }

    choiceItems.add(new ChoiceItem('h', "help", resourceBundle.getString("help.list.all.options"), false));
    reader.addAllowedPrintableKey('h');
    promptString += "h";

    reader.addAllowedSpecialKey(ReaderIF.SpecialKey.ENTER);
    reader.addAllowedSpecialKey(ReaderIF.SpecialKey.BACKSPACE);
    renderState = RenderState.FOLDED;

    // first render call, we don't need to position the cursor up
    renderHeight = 1;
    render();

    ReaderIF.ReaderInput readerInput = this.reader.read();
    while (true) {
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.ENTER) {
        // if ENTER pressed
        if (chosenItem != null && chosenItem.getKey() == 'h') {
          renderState = RenderState.EXPANDED;

          itemList = expandableChoice.getChoiceItems();

          selectedItemIndex = getFirstSelectableItemIndex();
          render();
          reader.addAllowedSpecialKey(ReaderIF.SpecialKey.UP);
          reader.addAllowedSpecialKey(ReaderIF.SpecialKey.DOWN);

          readerInput = this.reader.read();
        } else {
          if (renderState != RenderState.EXPANDED) {
            System.out.println("");
          } else {
            renderHeight++;
          }
          if (chosenItem != null) {
            renderMessagePromptAndResult(expandableChoice.getMessage(), chosenItem.getMessage());
            return new ExpandableChoiceResult(chosenItem.getName());
          } else {
            renderMessagePromptAndResult(expandableChoice.getMessage(), defaultItem.getMessage());
            return new ExpandableChoiceResult(defaultItem.getName());
          }
        }
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.UP) {
        selectedItemIndex = getPreviousSelectableItemIndex();
        chosenItem = (ChoiceItem) itemList.get(selectedItemIndex);
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.DOWN) {
        selectedItemIndex = getNextSelectableItemIndex();
        chosenItem = (ChoiceItem) itemList.get(selectedItemIndex);
      }
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.PRINTABLE_KEY) {
        Character pressedKey = readerInput.getPrintableKey();
        if (promptString.toLowerCase().contains("" + pressedKey)) {
          // find the new chosen item
          selectedItemIndex = 0;
          for (ConsoleUIItemIF choiceItem : choiceItems) {
            if (choiceItem instanceof ChoiceItem) {
              ChoiceItem item = (ChoiceItem) choiceItem;
              if (item.getKey().equals(pressedKey)) {
                chosenItem = item;
                break;
              }
              selectedItemIndex++;
            }
          }
          if (renderState == RenderState.FOLDED) {
            renderState = RenderState.FOLDED_ANSWERED;
          }
        } else {
          // not in valid choices
          chosenItem = errorMessageItem;
        }
      }
      render();
      readerInput = this.reader.read();
    }
  }
}
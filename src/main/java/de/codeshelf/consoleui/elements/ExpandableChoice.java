package de.codeshelf.consoleui.elements;

import de.codeshelf.consoleui.elements.items.impl.ChoiceItem;

import java.util.LinkedHashSet;

/**
 * User: andy
 * Date: 07.01.16
 */
public class ExpandableChoice extends AbstractPromptableElement {

  private LinkedHashSet<ChoiceItem> choiceItems;

  public ExpandableChoice(String message, String name, LinkedHashSet<ChoiceItem> choiceItems) {
    super(message, name);
    this.choiceItems = choiceItems;
  }

  public LinkedHashSet<ChoiceItem> getChoiceItems() {
    return choiceItems;
  }
}

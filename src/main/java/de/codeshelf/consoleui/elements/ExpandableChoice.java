package de.codeshelf.consoleui.elements;

import de.codeshelf.consoleui.elements.items.ChoiceItemIF;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * User: Andreas Wegmann
 * Date: 07.01.16
 */
public class ExpandableChoice extends AbstractPromptableElement {

  private List<ChoiceItemIF> choiceItems;

  public ExpandableChoice(String message, String name, List<ChoiceItemIF> choiceItems) {
    super(message, name);
    this.choiceItems = choiceItems;
  }

  public List<ChoiceItemIF> getChoiceItems() {
    return choiceItems;
  }
}

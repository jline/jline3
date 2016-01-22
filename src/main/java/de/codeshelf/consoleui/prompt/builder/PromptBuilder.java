package de.codeshelf.consoleui.prompt.builder;

import de.codeshelf.consoleui.elements.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andreas Wegmann
 * on 20.01.16.
 */
public class PromptBuilder {
  List<PromptableElementIF> promptList = new ArrayList<PromptableElementIF>();

  public List<PromptableElementIF> build() {
    return promptList;
  }

  public void addPrompt(PromptableElementIF promptableElement) {
    promptList.add(promptableElement);
  }

  public InputValueBuilder createInputPrompt() {
    return new InputValueBuilder(this);
  }

  public ListPromptBuilder createListPrompt() {
    return new ListPromptBuilder(this);
  }

  public ExpandableChoicePromptBuilder createChoicePrompt() {
    return new ExpandableChoicePromptBuilder(this);
  }

  public CheckboxPromptBuilder createCheckboxPrompt() {
    return new CheckboxPromptBuilder(this);
  }


}

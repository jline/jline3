package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.*;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by Andreas Wegmann on 20.01.16.
 */
public class ConsolePrompt {

  InputPrompt inputPrompt;
  ExpandableChoicePrompt expandableChoicePrompt;
  CheckboxPrompt checkboxPrompt;
  ListPrompt listPrompt = new ListPrompt();
  ConfirmPrompt confirmPrompt = new ConfirmPrompt();

  public ConsolePrompt() throws IOException {
    inputPrompt = new InputPrompt();
    checkboxPrompt = new CheckboxPrompt();
    expandableChoicePrompt = new ExpandableChoicePrompt();
  }


  public HashMap<String, ? extends PromtResultItemIF> prompt(List<PromptableElementIF> promptableElementList) throws IOException {
    HashMap<String, PromtResultItemIF> resultMap = new HashMap<String, PromtResultItemIF>();

    for (int i = 0; i < promptableElementList.size(); i++) {
      PromptableElementIF promptableElement = promptableElementList.get(i);
      if (promptableElement instanceof ListChoice) {
        ListResult result = doPrompt((ListChoice) promptableElement);
        resultMap.put(promptableElement.getName(),result);
      } else if (promptableElement instanceof InputValue) {
        InputResult result = doPrompt((InputValue) promptableElement);
        resultMap.put(promptableElement.getName(),result);
      } else if (promptableElement instanceof ExpandableChoice) {
        ExpandableChoiceResult result = doPrompt((ExpandableChoice) promptableElement);
        resultMap.put(promptableElement.getName(),result);
      } else if (promptableElement instanceof Checkbox) {
        CheckboxResult result = doPrompt((Checkbox) promptableElement);
        resultMap.put(promptableElement.getName(),result);
      } else if (promptableElement instanceof ConfirmChoice) {
        ConfirmResult result = doPrompt((ConfirmChoice) promptableElement);
        resultMap.put(promptableElement.getName(),result);
      } else {
        throw new IllegalArgumentException("wrong type of promptable element");
      }
    }
    return resultMap;
  }

  private ConfirmResult doPrompt(ConfirmChoice confirmChoice) throws IOException {
    return confirmPrompt.prompt(confirmChoice);
  }

  private ListResult doPrompt(ListChoice listChoice) throws IOException {
    return listPrompt.prompt(listChoice);
  }

  private InputResult doPrompt(InputValue listChoice) throws IOException {
    return inputPrompt.prompt(listChoice);
  }

  private CheckboxResult doPrompt(Checkbox listChoice) throws IOException {
    return checkboxPrompt.prompt(listChoice);
  }

  private ExpandableChoiceResult doPrompt(ExpandableChoice listChoice) throws IOException {
    return expandableChoicePrompt.prompt(listChoice);
  }


  public PromptBuilder getPromptBuilder() {
    return new PromptBuilder();
  }
}

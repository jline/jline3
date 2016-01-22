package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Created by Andreas Wegmann on 20.01.16.
 */
public class ConsolePrompt {

  InputPrompt inputPrompt = new InputPrompt();
  ExpandableChoicePrompt expandableChoicePrompt = new ExpandableChoicePrompt();
  CheckboxPrompt checkboxPrompt = new CheckboxPrompt();
  ListPrompt listPrompt = new ListPrompt();


  public HashMap<String, Object> prompt(List<PromptableElementIF> promptableElementList) throws IOException {
    HashMap<String, Object> resultMap = new HashMap<String, Object>();

    for (int i = 0; i < promptableElementList.size(); i++) {
      PromptableElementIF promptableElement = promptableElementList.get(i);
      LinkedHashSet<String> result=null;
      if (promptableElement instanceof ListChoice) {
        result = doPrompt((ListChoice) promptableElement);
      } else if (promptableElement instanceof InputValue) {
        result = doPrompt((InputValue) promptableElement);
      } else if (promptableElement instanceof ExpandableChoice) {
        result = doPrompt((ExpandableChoice) promptableElement);
      } else if (promptableElement instanceof Checkbox) {
        result = doPrompt((Checkbox) promptableElement);
      } else {
        throw new IllegalArgumentException("wrong type of promptable element");
      }
      resultMap.put(promptableElement.getName(),result);
    }
    return resultMap;
  }

  private LinkedHashSet<String> doPrompt(ListChoice listChoice) throws IOException {
    return listPrompt.prompt(listChoice);
  }

  private LinkedHashSet<String> doPrompt(InputValue listChoice) throws IOException {
    return inputPrompt.prompt(listChoice);
  }

  private LinkedHashSet<String> doPrompt(Checkbox listChoice) throws IOException {
    return checkboxPrompt.prompt(listChoice);
  }

  private LinkedHashSet<String> doPrompt(ExpandableChoice listChoice) throws IOException {
    return expandableChoicePrompt.prompt(listChoice);
  }


  public PromptBuilder getPromptBuilder() {
    return new PromptBuilder();
  }
}

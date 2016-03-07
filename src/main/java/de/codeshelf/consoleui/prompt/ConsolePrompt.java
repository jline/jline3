package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.*;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * ConsolePrompt encapsulates the prompting of a list of input questions for the user.
 * <p>
 * Created by Andreas Wegmann on 20.01.16.
 */
public class ConsolePrompt {
  // input prompt implementation
  private InputPrompt inputPrompt;

  // expandable choice prompt implementation
  private ExpandableChoicePrompt expandableChoicePrompt;

  // checkbox prompt implementation
  private CheckboxPrompt checkboxPrompt;

  // list box prompt implementation
  private ListPrompt listPrompt;

  // confirmation prompt implementation
  private ConfirmPrompt confirmPrompt;

  /* Lazy getter for input prompt */
  private InputPrompt getInputPrompt() throws IOException {
    if (inputPrompt == null) {
      inputPrompt = new InputPrompt();
    }
    return inputPrompt;
  }

  /* Lazy getter for expandable choice prompt */
  private ExpandableChoicePrompt getExpandableChoicePrompt() throws IOException {
    if (expandableChoicePrompt == null) {
      expandableChoicePrompt = new ExpandableChoicePrompt();
    }
    return expandableChoicePrompt;
  }

  /* Lazy getter for checkbox prompt */
  private CheckboxPrompt getCheckboxPrompt() throws IOException {
    if (checkboxPrompt == null) {
      checkboxPrompt = new CheckboxPrompt();
    }
    return checkboxPrompt;
  }

  /* Lazy getter for list prompt */
  private ListPrompt getListPrompt() throws IOException {
    if (listPrompt == null) {
      listPrompt = new ListPrompt();
    }
    return listPrompt;
  }

  /* Lazy getter for confirm prompt */
  private ConfirmPrompt getConfirmPrompt() throws IOException {
    if (confirmPrompt == null) {
      confirmPrompt = new ConfirmPrompt();
    }
    return confirmPrompt;
  }

  /**
   * Default constructor for this class.
   */
  public ConsolePrompt() {
  }

  /**
   * Prompt a list of choices (questions). This method takes a list of promptable elements, typically
   * created with {@link PromptBuilder}. Each of the elements is processed and the user entries and
   * answers are filled in to the result map. The result map contains the key of each promtable element
   * and the user entry as an object implementing {@link PromtResultItemIF}.
   *
   * @param promptableElementList the list of questions / promts to ask the user for.
   * @return a map containing a result for each element of promptableElementList
   * @throws IOException  may be thrown by console reader
   */
  public HashMap<String, ? extends PromtResultItemIF> prompt(List<PromptableElementIF> promptableElementList)
          throws IOException {
    HashMap<String, PromtResultItemIF> resultMap = new HashMap<String, PromtResultItemIF>();

    for (int i = 0; i < promptableElementList.size(); i++) {
      PromptableElementIF promptableElement = promptableElementList.get(i);
      if (promptableElement instanceof ListChoice) {
        ListResult result = doPrompt((ListChoice) promptableElement);
        resultMap.put(promptableElement.getName(), result);
      } else if (promptableElement instanceof InputValue) {
        InputResult result = doPrompt((InputValue) promptableElement);
        resultMap.put(promptableElement.getName(), result);
      } else if (promptableElement instanceof ExpandableChoice) {
        ExpandableChoiceResult result = doPrompt((ExpandableChoice) promptableElement);
        resultMap.put(promptableElement.getName(), result);
      } else if (promptableElement instanceof Checkbox) {
        CheckboxResult result = doPrompt((Checkbox) promptableElement);
        resultMap.put(promptableElement.getName(), result);
      } else if (promptableElement instanceof ConfirmChoice) {
        ConfirmResult result = doPrompt((ConfirmChoice) promptableElement);
        resultMap.put(promptableElement.getName(), result);
      } else {
        throw new IllegalArgumentException("wrong type of promptable element");
      }
    }
    return resultMap;
  }

  /**
   * Process a {@link ConfirmChoice}.
   *
   * @param confirmChoice the confirmation to ask the user for.
   * @return Object of type {@link ConfirmResult} holding the users answer
   * @throws IOException may be thrown by console reader
   */
  private ConfirmResult doPrompt(ConfirmChoice confirmChoice) throws IOException {
    return getConfirmPrompt().prompt(confirmChoice);
  }

  /**
   * Process a {@link ListChoice}.
   *
   * @param listChoice the list to let the user choose an item from.
   * @return Object of type {@link ListResult} holding the uses choice.
   * @throws IOException may be thrown by console reader
   */
  private ListResult doPrompt(ListChoice listChoice) throws IOException {
    return getListPrompt().prompt(listChoice);
  }

  /**
   * Process a {@link InputValue}.
   *
   * @param inputValue the input value to ask the user for.
   * @return Object of type {@link InputResult} holding the uses input.
   * @throws IOException may be thrown by console reader
   */
  private InputResult doPrompt(InputValue inputValue) throws IOException {
    return getInputPrompt().prompt(inputValue);
  }

  /**
   * Process a {@link Checkbox}.
   *
   * @param checkbox the checkbox displayed where the user can check values.
   * @return Object of type {@link CheckboxResult} holding the uses choice.
   * @throws IOException may be thrown by console reader
   */
  private CheckboxResult doPrompt(Checkbox checkbox) throws IOException {
    return getCheckboxPrompt().prompt(checkbox);
  }

  /**
   * Process a {@link ExpandableChoice}.
   *
   * @param expandableChoice the expandable choice displayed where the user can select a value from.
   * @return Object of type {@link ExpandableChoiceResult} holding the uses choice.
   * @throws IOException may be thrown by console reader
   */
  private ExpandableChoiceResult doPrompt(ExpandableChoice expandableChoice) throws IOException {
    return getExpandableChoicePrompt().prompt(expandableChoice);
  }

  /**
   * Creates a {@link PromptBuilder}.
   *
   * @return a new prompt builder object.
   */
  public PromptBuilder getPromptBuilder() {
    return new PromptBuilder();
  }
}

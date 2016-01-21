package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.PromptableElementIF;

import java.util.List;

/**
 * Created by andy on 20.01.16.
 */
public class PromptBuilder {



  public List<PromptableElementIF> build() {
    return null;
  }

  public InputValueBuilder createInputPrompt() {
    return null;
  }

  public ListPromptBuilder createListPrompt() {
    return null;
  }

  public ExpandableChoicePromptBuilder createChoicePrompt() {
    return null;
  }

  public CheckboxPromptBuilder createCheckboxPrompt() {
    return null;
  }

  public class InputValueBuilder {
    public InputValueBuilder name(String name) {
      return null;
    }

    public InputValueBuilder defaultValue(String defaultValue) {
      return null;
    }

    public InputValueBuilder message(String message) {
      return null;
    }

    public PromptBuilder add() {

      return null;
    }
  }

  public class ListPromptBuilder {
    public ListPromptBuilder name(String name) {
      return null;
    }

    public ListPromptBuilder message(String message) {
      return null;
    }

    public ListItemBuilder getItemBuilder() {
      return null;
    }

    public class ListItemBuilder {
      public ListItemBuilder name(String name) {
        return null;
      }

      public ListItemBuilder text(String text) {
        return null;
      }

      public ListItemBuilder add() {
        return null;
      }
    }
  }


  public class ExpandableChoicePromptBuilder {
    public ExpandableChoicePromptBuilder name(String name) {
      return null;
    }

    public ExpandableChoicePromptBuilder text(String text) {
      return null;
    }

    public ExpandableChoicePromptBuilder message(String message) {
      return null;
    }

    public ExpandableChoiceItemBuilder getItemBuilder() {
      return null;
    }

    public class ExpandableChoiceItemBuilder {
      public ExpandableChoiceItemBuilder name(String name1) {
        return null;
      }

      public ExpandableChoiceItemBuilder message(String message) {
        return null;
      }

      public ExpandableChoiceItemBuilder key(char key) {
        return null;
      }

      public ExpandableChoiceItemBuilder add() {
        return null;
      }

      public ExpandableChoiceItemBuilder asDefault() {
        return null;
      }
    }
  }

  public class CheckboxPromptBuilder {
    public CheckboxPromptBuilder name(String name) {
      return null;
    }

    public CheckboxPromptBuilder message(String message) {
      return null;
    }

    public CheckboxItemBuilder getItemBuilder() {
      return null;
    }

    public class CheckboxItemBuilder {
      private boolean checked;

      public CheckboxItemBuilder name(String name) {
        return null;
      }

      public CheckboxItemBuilder text(String text) {
        return null;
      }

      public CheckboxItemBuilder add() {
        return null;
      }

      public CheckboxItemBuilder disabledText(String disabledText) {
        return null;
      }

      public CheckboxItemBuilder check() {
        return null;
      }

      public CheckboxItemBuilder checked(boolean checked) {
        this.checked = checked;
        return this;
      }

      public CheckboxItemBuilder separator() {
        return null;
      }

      public CheckboxItemBuilder separator(String text) {
        return null;
      }
    }
  }
}

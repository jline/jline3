package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.PromptableElementIF;
import jline.console.completer.Completer;

import java.awt.peer.ChoicePeer;
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

    public InputValueBuilder addCompleter(Completer completer) {
      return null;
    }

    public PromptBuilder addPrompt() {

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

    public ListItemBuilder newItem() {
      return null;
    }

    public ListItemBuilder newItem(String name) {
      return null;
    }

    public PromptBuilder addPrompt() {

      return null;
    }

    public class ListItemBuilder {
      public ListItemBuilder text(String text) {
        return null;
      }

      public ListItemBuilder name(String name) {
        return null;
      }

      public ListPromptBuilder add() {
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

    public ExpandableChoiceItemBuilder newItem() {
      return null;
    }

    public ExpandableChoiceItemBuilder newItem(String name) {
      return null;
    }

    public PromptBuilder addPrompt() {

      return null;
    }

    public ExpandableChoiceItemBuilder newSeparator(String text) {
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

      public ExpandableChoicePromptBuilder add() {
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

    public CheckboxItemBuilder newItem() {
      return null;
    }

    public CheckboxItemBuilder newItem(String name) {
      return null;
    }

    public PromptBuilder addPrompt() {
      return null;
    }

    public CheckboxItemBuilder newSeparator() {
      return null;
    }

    public CheckboxItemBuilder newSeparator(String text) {
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

      public CheckboxPromptBuilder add() {
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
    }
  }
}

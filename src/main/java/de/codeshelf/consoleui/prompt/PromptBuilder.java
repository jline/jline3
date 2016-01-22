package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.*;
import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.ChoiceItemIF;
import de.codeshelf.consoleui.elements.items.ListItemIF;
import de.codeshelf.consoleui.elements.items.impl.CheckboxItem;
import de.codeshelf.consoleui.elements.items.impl.ChoiceItem;
import de.codeshelf.consoleui.elements.items.impl.ListItem;
import de.codeshelf.consoleui.elements.items.impl.Separator;
import jline.console.completer.Completer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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

  private void addPrompt(PromptableElementIF promptableElement) {
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

  public class InputValueBuilder {
    private final PromptBuilder promptBuilder;
    private String name;
    private String defaultValue;
    private String message;
    private ArrayList<Completer> completers;

    public InputValueBuilder(PromptBuilder promptBuilder) {
      this.promptBuilder = promptBuilder;
    }

    public InputValueBuilder name(String name) {
      this.name = name;
      return this;
    }

    public InputValueBuilder defaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public InputValueBuilder message(String message) {
      this.message = message;
      return this;
    }

    public InputValueBuilder addCompleter(Completer completer) {
      if (completers == null) {
        completers = new ArrayList<Completer>();
      }
      this.completers.add(completer);
      return this;
    }

    public PromptBuilder addPrompt() {
      InputValue inputValue = new InputValue(name, message, null, defaultValue);
      if (completers != null) {
        inputValue.setCompleter(completers);
      }
      promptBuilder.addPrompt(inputValue);
      return promptBuilder;
    }
  }

  public class ListPromptBuilder {
    private final PromptBuilder promptBuilder;
    private String name;
    private String message;
    private List<ListItemIF> itemList = new ArrayList<ListItemIF>();

    public ListPromptBuilder(PromptBuilder promptBuilder) {
      this.promptBuilder = promptBuilder;
    }

    public ListPromptBuilder name(String name) {
      this.name = name;
      return this;
    }

    public ListPromptBuilder message(String message) {
      this.message = message;
      return this;
    }

    public ListItemBuilder newItem() {
      return new ListItemBuilder(this);
    }

    public ListItemBuilder newItem(String name) {
      ListItemBuilder listItemBuilder = new ListItemBuilder(this);
      return listItemBuilder.name(name);
    }

    public PromptBuilder addPrompt() {
      ListChoice listChoice = new ListChoice(name, message, itemList);
      promptBuilder.addPrompt(listChoice);
      return promptBuilder;
    }

    private void addItem(ListItem listItem) {
      this.itemList.add(listItem);
    }

    public class ListItemBuilder {
      private final ListPromptBuilder listPromptBuilder;
      private String text;
      private String name;

      public ListItemBuilder(ListPromptBuilder listPromptBuilder) {
        this.listPromptBuilder = listPromptBuilder;
      }

      public ListItemBuilder text(String text) {
        this.text = text;
        return this;
      }

      public ListItemBuilder name(String name) {
        this.name = name;
        return this;
      }

      public ListPromptBuilder add() {
        listPromptBuilder.addItem(new ListItem(text, name));
        return listPromptBuilder;
      }
    }

  }


  public class ExpandableChoicePromptBuilder {
    private final PromptBuilder promptBuilder;
    private String name;
    private String text;
    private String message;
    private LinkedHashSet<ChoiceItemIF> itemList;

    public ExpandableChoicePromptBuilder(PromptBuilder promptBuilder) {
      this.promptBuilder = promptBuilder;
      this.itemList = new LinkedHashSet<ChoiceItemIF>();
    }

    private void addItem(ChoiceItemIF choiceItem) {
      this.itemList.add(choiceItem);
    }

    public ExpandableChoicePromptBuilder name(String name) {
      this.name = name;
      return this;
    }

    public ExpandableChoicePromptBuilder text(String text) {
      this.text = text;
      return this;
    }

    public ExpandableChoicePromptBuilder message(String message) {
      this.message = message;
      return this;
    }

    public ExpandableChoiceItemBuilder newItem() {
      return new ExpandableChoiceItemBuilder(this);
    }

    public ExpandableChoiceItemBuilder newItem(String name) {
      ExpandableChoiceItemBuilder expandableChoiceItemBuilder = new ExpandableChoiceItemBuilder(this);
      return expandableChoiceItemBuilder.name(name);
    }

    public PromptBuilder addPrompt() {
      ExpandableChoice expandableChoice = new ExpandableChoice(message, name, itemList);
      promptBuilder.addPrompt(expandableChoice);
      return promptBuilder;
    }

    public ExpandableChoiceItemBuilder newSeparator(String text) {
      return null;
    }

    public class ExpandableChoiceItemBuilder {
      private final ExpandableChoicePromptBuilder choicePromptBuilder;
      private String name;
      private String message;
      private Character key;

      public ExpandableChoiceItemBuilder(ExpandableChoicePromptBuilder choicePromptBuilder) {
        this.choicePromptBuilder = choicePromptBuilder;
      }

      public ExpandableChoiceItemBuilder name(String name) {
        this.name = name;
        return this;
      }

      public ExpandableChoiceItemBuilder message(String message) {
        this.message = message;
        return this;
      }

      public ExpandableChoiceItemBuilder key(char key) {
        this.key = key;
        return this;
      }

      public ExpandableChoicePromptBuilder add() {
        ChoiceItem choiceItem = new ChoiceItem(key, name, message);
        choicePromptBuilder.addItem(choiceItem);
        return choicePromptBuilder;
      }

      public ExpandableChoiceItemBuilder asDefault() {
        return null;
      }
    }

  }

  public class CheckboxPromptBuilder {
    private final PromptBuilder promptBuilder;
    private String name;
    private String message;
    private List<CheckboxItemIF> itemList;

    public CheckboxPromptBuilder(PromptBuilder promptBuilder) {
      this.promptBuilder = promptBuilder;
      itemList = new ArrayList<CheckboxItemIF>();
    }

    private void addItem(CheckboxItemIF checkboxItem) {
      itemList.add(checkboxItem);
    }

    public CheckboxPromptBuilder name(String name) {
      this.name = name;
      return this;
    }

    public CheckboxPromptBuilder message(String message) {
      this.message = message;
      return this;
    }

    public CheckboxItemBuilder newItem() {
      return new CheckboxItemBuilder(this);
    }

    public CheckboxItemBuilder newItem(String name) {
      CheckboxItemBuilder checkboxItemBuilder = new CheckboxItemBuilder(this);
      return checkboxItemBuilder.name(name);
    }

    public PromptBuilder addPrompt() {
      Checkbox checkbox = new Checkbox(name, message, itemList);
      promptBuilder.addPrompt(checkbox);
      return promptBuilder;
    }

    public CheckboxSeperatorBuilder newSeparator() {
      return new CheckboxSeperatorBuilder(this);
    }

    public CheckboxSeperatorBuilder newSeparator(String text) {
      CheckboxSeperatorBuilder checkboxSeperatorBuilder = new CheckboxSeperatorBuilder(this);
      return checkboxSeperatorBuilder.text(text);
    }

    public class CheckboxItemBuilder {
      private final CheckboxPromptBuilder checkboxPromptBuilder;
      private boolean checked;
      private String name;
      private String text;
      private String disabledText;

      public CheckboxItemBuilder(CheckboxPromptBuilder checkboxPromptBuilder) {
        this.checkboxPromptBuilder = checkboxPromptBuilder;
      }

      public CheckboxItemBuilder name(String name) {
        this.name = name;
        return this;
      }

      public CheckboxItemBuilder text(String text) {
        this.text = text;
        return this;
      }

      public CheckboxPromptBuilder add() {
        CheckboxItemIF item = new CheckboxItem(checked, text, disabledText, name);
        checkboxPromptBuilder.addItem(item);
        return checkboxPromptBuilder;
      }

      public CheckboxItemBuilder disabledText(String disabledText) {
        this.disabledText = disabledText;
        return this;
      }

      public CheckboxItemBuilder check() {
        this.checked = true;
        return this;
      }

      public CheckboxItemBuilder checked(boolean checked) {
        this.checked = checked;
        return this;
      }
    }


    public class CheckboxSeperatorBuilder {
      private final CheckboxPromptBuilder promptBuilder;
      private String text;

      public CheckboxSeperatorBuilder(CheckboxPromptBuilder checkboxPromptBuilder) {
        this.promptBuilder = checkboxPromptBuilder;
      }

      public CheckboxPromptBuilder add() {
        Separator separator = new Separator(text);
        promptBuilder.addItem(separator);

        return promptBuilder;
      }

      public CheckboxSeperatorBuilder text(String text) {
        this.text = text;
        return this;
      }
    }

  }
}

package de.codeshelf.consoleui.prompt.builder;

import de.codeshelf.consoleui.elements.ExpandableChoice;
import de.codeshelf.consoleui.elements.items.ChoiceItemIF;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andy on 22.01.16.
 */
public class ExpandableChoicePromptBuilder {
  private final PromptBuilder promptBuilder;
  private String name;
  private String message;
  private List<ChoiceItemIF> itemList;

  public ExpandableChoicePromptBuilder(PromptBuilder promptBuilder) {
    this.promptBuilder = promptBuilder;
    this.itemList = new ArrayList<>();
  }

  void addItem(ChoiceItemIF choiceItem) {
    this.itemList.add(choiceItem);
  }

  public ExpandableChoicePromptBuilder name(String name) {
    this.name = name;
    if (message == null) {
      message = name;
    }
    return this;
  }

  public ExpandableChoicePromptBuilder message(String message) {
    this.message = message;
    if (name == null) {
      name = message;
    }
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

  public ExpandableChoiceSeparatorBuilder newSeparator(String text) {
    ExpandableChoiceSeparatorBuilder expandableChoiceSeparatorBuilder = new ExpandableChoiceSeparatorBuilder(this);
    return expandableChoiceSeparatorBuilder.text(text);
  }

}

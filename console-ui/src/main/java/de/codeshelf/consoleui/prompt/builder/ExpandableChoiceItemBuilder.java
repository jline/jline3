package de.codeshelf.consoleui.prompt.builder;

import de.codeshelf.consoleui.elements.items.impl.ChoiceItem;

/**
 * Created by andy on 22.01.16.
 */
public class ExpandableChoiceItemBuilder {
  private final ExpandableChoicePromptBuilder choicePromptBuilder;
  private String name;
  private String message;
  private Character key;
  private boolean asDefault;

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
    ChoiceItem choiceItem = new ChoiceItem(key, name, message, asDefault);
    choicePromptBuilder.addItem(choiceItem);
    return choicePromptBuilder;
  }

  public ExpandableChoiceItemBuilder asDefault() {
    this.asDefault = true;
    return this;
  }
}

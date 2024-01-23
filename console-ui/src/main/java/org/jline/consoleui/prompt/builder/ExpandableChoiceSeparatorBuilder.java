package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.items.impl.Separator;

/**
 * Created by andy on 22.01.16.
 */
public class ExpandableChoiceSeparatorBuilder {
  private final ExpandableChoicePromptBuilder expandableChoicePromptBuilder;
  private String text;

  public ExpandableChoiceSeparatorBuilder(ExpandableChoicePromptBuilder expandableChoicePromptBuilder) {
    this.expandableChoicePromptBuilder = expandableChoicePromptBuilder;
  }

  public ExpandableChoiceSeparatorBuilder text(String text) {
    this.text = text;
    return this;
  }

  public ExpandableChoicePromptBuilder add() {
    Separator separator = new Separator(text);
    expandableChoicePromptBuilder.addItem(separator);

    return expandableChoicePromptBuilder;
  }
}

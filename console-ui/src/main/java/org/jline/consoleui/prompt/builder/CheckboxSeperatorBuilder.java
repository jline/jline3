package org.jline.consoleui.prompt.builder;

import org.jline.consoleui.elements.items.impl.Separator;

/**
 * Created by andy on 22.01.16.
 */
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

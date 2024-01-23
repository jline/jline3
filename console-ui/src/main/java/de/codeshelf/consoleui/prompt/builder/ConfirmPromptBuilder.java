package de.codeshelf.consoleui.prompt.builder;

import de.codeshelf.consoleui.elements.ConfirmChoice;

/**
 * User: Andreas Wegmann
 * Date: 24.01.16
 */
public class ConfirmPromptBuilder {
  private final PromptBuilder promptBuilder;
  private String name;
  private String message;
  private ConfirmChoice.ConfirmationValue defaultConfirmationValue;

  public ConfirmPromptBuilder(PromptBuilder promptBuilder) {
    this.promptBuilder = promptBuilder;
  }

  public ConfirmPromptBuilder name(String name) {
    this.name = name;
    if (message == null) {
      message = name;
    }
    return this;
  }

  public ConfirmPromptBuilder message(String message) {
    this.message = message;
    if (name == null) {
      name = message;
    }
    return this;
  }

  public ConfirmPromptBuilder defaultValue(ConfirmChoice.ConfirmationValue confirmationValue) {
    this.defaultConfirmationValue = confirmationValue;
    return this;
  }

  public PromptBuilder addPrompt() {
    promptBuilder.addPrompt(new ConfirmChoice(message, name, defaultConfirmationValue));
    return promptBuilder;
  }
}

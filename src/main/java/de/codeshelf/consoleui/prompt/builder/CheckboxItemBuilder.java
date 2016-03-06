package de.codeshelf.consoleui.prompt.builder;

import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.impl.CheckboxItem;

/**
 * Created by andy on 22.01.16.
 */
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
    if (text == null) {
      text = name;
    }
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

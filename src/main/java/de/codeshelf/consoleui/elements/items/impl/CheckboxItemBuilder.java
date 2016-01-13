package de.codeshelf.consoleui.elements.items.impl;

import de.codeshelf.consoleui.util.BuilderIF;

/**
 * User: Andreas Wegmann
 * Date: 04.01.16
 */
public class CheckboxItemBuilder implements BuilderIF<CheckboxItem> {

  private String text;
  private String disabledText;
  private String name;
  private boolean checked;

  private CheckboxItemBuilder() {
  }

  public static CheckboxItemBuilder create() {
    return new CheckboxItemBuilder();
  }

  public CheckboxItemBuilder text(String text) {
    this.text = text;
    if (this.name == null) {
      this.name = text;
    }

    return this;
  }

  public CheckboxItemBuilder name(String name) {
    this.name = name;
    return this;
  }

  public CheckboxItemBuilder disabledText(String text) {
    this.disabledText = text;
    return this;
  }

  public CheckboxItemBuilder check() {
    this.checked = true;
    return this;
  }

  public CheckboxItemBuilder uncheck() {
    this.checked = false;
    return this;
  }

  public CheckboxItemBuilder checked(boolean checked) {
    this.checked = checked;
    return this;
  }


  public CheckboxItem build() {
    CheckboxItem checkboxItem = new CheckboxItem(checked, text, disabledText, name);
    this.name = null;
    return checkboxItem;
  }
}

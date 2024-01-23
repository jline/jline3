package de.codeshelf.consoleui.elements.items.impl;

import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;

/**
 * User: Andreas Wegmann
 * Date: 07.12.15
 */
public class CheckboxItem implements CheckboxItemIF, ConsoleUIItemIF {
  boolean checked;
  String text;
  String disabledText;
  String name;

  public CheckboxItem(boolean checked, String text, String disabledText, String name) {
    this.checked = checked;
    this.text = text;
    this.disabledText = disabledText;
    this.name = name;
  }

  public CheckboxItem(boolean checked, String text) {
    this(checked, text, null, text);
  }

  public CheckboxItem(String text) {
    this(false, text, null, text);
  }

  public CheckboxItem(String text, String disabledText) {
    this(false, text, disabledText, text);
  }

  public CheckboxItem() {
    this(false, null, null, null);
  }

  public boolean isChecked() {
    return checked;
  }

  public void setChecked(boolean checked) {
    this.checked = checked;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public boolean isSelectable() {
    return isEnabled();
  }

  public void setDisabled() {
    disabledText = "disabled";
  }

  public void setDisabled(String disabledText) {
    this.disabledText = disabledText;
  }

  public void setEnabled() {
    disabledText = null;
  }

  public boolean isDisabled() {
    return disabledText != null;
  }

  public String getDisabledText() {
    return disabledText;
  }

  public boolean isEnabled() {
    return disabledText == null;
  }
}

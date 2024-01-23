package de.codeshelf.consoleui.elements;

import org.jline.reader.Completer;

/**
 * User: Andreas Wegmann
 * Date: 06.01.16
 */
public class InputValue extends AbstractPromptableElement {
  private String value;
  private String defaultValue;
  private Character mask;
  private Completer completer;

  public InputValue(String name, String message) {
    super(message, name);
    this.value = null;
    this.defaultValue = null;
  }

  public InputValue(String name, String message, String value, String defaultValue) {
    super(message, name);
    //this.value = value;
    if (value!=null)
      throw new IllegalStateException("pre filled values for InputValue are not supported at the moment.");
    this.defaultValue = defaultValue;
  }

  public String getValue() {
    return value;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public void setMask(Character mask) {
    this.mask = mask;
  }

  public Character getMask() {
    return mask;
  }

  public void setCompleter(Completer completer) {
    this.completer = completer;
  }

  public Completer getCompleter() {
    return completer;
  }

}

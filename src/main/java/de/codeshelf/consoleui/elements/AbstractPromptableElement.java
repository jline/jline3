package de.codeshelf.consoleui.elements;

/**
 * User: Andreas Wegmann
 * Date: 06.01.16
 */
public class AbstractPromptableElement implements PromptableElementIF {

  protected String message;
  protected String name;

  public AbstractPromptableElement(String message, String name) {
    this.message = message;
    this.name = name;
  }

  public String getMessage() {
    return message;
  }

  public String getName() {
    return name;
  }
}

package de.codeshelf.consoleui.prompt;

/**
 * User: ${FULL_NAME}
 * Date: 03.02.16
 */
public class InputResult implements PromtResultItemIF {
  String input;

  public InputResult(String input) {
    this.input = input;
  }

  public String getInput() {
    return input;
  }

  @Override
  public String toString() {
    return "InputResult{" +
            "input='" + input + '\'' +
            '}';
  }
}

package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.ConfirmChoice;

/**
 * User: ${FULL_NAME}
 * Date: 03.02.16
 */
public class ConfirmResult implements PromtResultItemIF {
  ConfirmChoice.ConfirmationValue confirmed;

  public ConfirmResult(ConfirmChoice.ConfirmationValue confirm) {
    this.confirmed = confirm;
  }

  public ConfirmChoice.ConfirmationValue getConfirmed() {
    return confirmed;
  }

  @Override
  public String toString() {
    return "ConfirmResult{" +
            "confirmed=" + confirmed +
            '}';
  }
}

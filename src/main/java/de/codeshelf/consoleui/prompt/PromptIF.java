package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.PromptableElementIF;

import java.io.IOException;

/**
 * Interface for all prompt implementation.
 *
 * User: Andreas Wegmann
 * Date: 01.01.16
 */
public interface PromptIF<T extends PromptableElementIF, R extends PromptResultItemIF> {
  /**
   * Prompt the user for an imput.
   *
   * @param promptableElement prompt definition
   * @return the prompt result
   * @throws IOException may be thrown by getting the users input.
   */
  R prompt(T promptableElement) throws IOException;
}

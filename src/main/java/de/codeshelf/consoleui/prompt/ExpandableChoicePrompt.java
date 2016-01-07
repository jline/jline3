package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.ExpandableChoice;

import java.io.IOException;
import java.util.LinkedHashSet;

/**
 * User: andy
 * Date: 07.01.16
 */
public class ExpandableChoicePrompt extends AbstractPrompt implements PromptIF<ExpandableChoice> {
  enum RenderState { FOLDED, EXPANDED };

  RenderState renderState = RenderState.FOLDED;

  public LinkedHashSet<String> prompt(ExpandableChoice promptableElement) throws IOException {
    return null;
  }
}

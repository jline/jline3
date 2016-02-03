package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.PromptableElementIF;

import java.io.IOException;
import java.util.LinkedHashSet;

/**
 * User: Andreas Wegmann
 * Date: 01.01.16
 */
public interface PromptIF<T extends PromptableElementIF, R extends PromtResultItemIF> {
  R prompt(T promptableElement) throws IOException;
}

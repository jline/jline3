package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.PromptableElementIF;

import java.io.IOException;
import java.util.LinkedHashSet;

/**
 * User: andy
 * Date: 01.01.16
 */
public interface PromptIF<T extends PromptableElementIF> {
  LinkedHashSet<String> prompt(T promptableElement) throws IOException;
}

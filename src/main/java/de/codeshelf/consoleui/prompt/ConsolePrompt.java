package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.PromptableElementIF;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Andreas Wegmann on 20.01.16.
 */
public class ConsolePrompt {

  public HashMap<String,Object> prompt(List<PromptableElementIF> promptableElementList) throws IOException
  {

    return null;
  }


  public PromptBuilder getPromptBuilder() {
    return new PromptBuilder();
  }
}

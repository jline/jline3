package de.codeshelf.consoleui.examples;

import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.ConsolePrompt.UiConfig;
import de.codeshelf.consoleui.prompt.PromptResultItemIF;
import de.codeshelf.consoleui.prompt.builder.CheckboxPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.ListPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: Andreas Wegmann
 * Date: 29.11.15
 */
public class LongList {

  public static void main(String[] args) {
    List<AttributedString> header = new ArrayList<>();
    header.add(new AttributedStringBuilder()
            .append("This is a demonstration of ConsoleUI java library. It provides a simple console interface").toAttributedString());
    header.add(new AttributedStringBuilder()
            .append("for querying information from the user. ConsoleUI is inspired by Inquirer.js which is written").toAttributedString());
    header.add(new AttributedStringBuilder()
            .append("in JavaScript.").toAttributedString());

    try (Terminal terminal = TerminalBuilder.builder().build()) {
      UiConfig config = new UiConfig(">", "( )", "(x)", "( )");
      ConsolePrompt prompt = new ConsolePrompt(terminal, config);
      PromptBuilder promptBuilder = prompt.getPromptBuilder();

      ListPromptBuilder listPrompt = promptBuilder.createListPrompt();
      listPrompt.name("longlist").message("What's your favourite Letter?").relativePageSize(66);

      for (char letter = 'A'; letter <= 'C'; letter++)
        for (char letter2 = 'A'; letter2 <= 'Z'; letter2++)
          listPrompt.newItem().text("" + letter + letter2).add();
      listPrompt.addPrompt();

      CheckboxPromptBuilder checkboxPrompt = promptBuilder.createCheckboxPrompt();
      checkboxPrompt.name("longcheckbox").message("What's your favourite Letter? Select all you want...").relativePageSize(66);

      for (char letter = 'A'; letter <= 'C'; letter++)
        for (char letter2 = 'A'; letter2 <= 'Z'; letter2++)
          checkboxPrompt.newItem().text("" + letter + letter2).add();
      checkboxPrompt.addPrompt();

      Map<String, PromptResultItemIF> result = prompt.prompt(header, promptBuilder.build());
      System.out.println("result = " + result);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

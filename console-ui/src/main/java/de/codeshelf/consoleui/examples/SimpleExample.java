package de.codeshelf.consoleui.examples;

import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.PromptResultItemIF;
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
 * Date: 12.08.2020
 */
public class SimpleExample {

  public static void main(String[] args) {
    List<AttributedString> header = new ArrayList<>();
    header.add(new AttributedStringBuilder().append("Simple list example:").toAttributedString());

    try (Terminal terminal = TerminalBuilder.builder().build()) {
      ConsolePrompt prompt = new ConsolePrompt(terminal);
      PromptBuilder promptBuilder = prompt.getPromptBuilder();

      promptBuilder.createListPrompt()
              .name("pizzatype")
              .message("Which pizza do you want?")
              .newItem().text("Margherita").add()  // without name (name defaults to text)
              .newItem("veneziana").text("Veneziana").add()
              .newItem("hawai").text("Hawai").add()
              .newItem("quattro").text("Quattro Stagioni").add()
              .addPrompt();

      Map<String, PromptResultItemIF> result = prompt.prompt(header, promptBuilder.build());
      System.out.println("result = " + result);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

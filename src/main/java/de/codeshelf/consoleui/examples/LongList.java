package de.codeshelf.consoleui.examples;

import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.PromtResultItemIF;
import de.codeshelf.consoleui.prompt.builder.CheckboxPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.ListPromptBuilder;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import jline.TerminalFactory;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.util.HashMap;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 29.11.15
 */
public class LongList {

  public static void main(String[] args) throws InterruptedException {
    AnsiConsole.systemInstall();
    System.out.println(ansi().eraseScreen().render("@|red,italic Hello|@ @|green World|@\n@|reset " +
            "This is a demonstration of ConsoleUI java library. It provides a simple console interface\n" +
            "for querying information from the user. ConsoleUI is inspired by Inquirer.js which is written\n" +
            "in JavaScript.|@"));


    try {
      ConsolePrompt prompt = new ConsolePrompt();
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

      HashMap<String, ? extends PromtResultItemIF> result = prompt.prompt(promptBuilder.build());
      System.out.println("result = " + result);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        TerminalFactory.get().restore();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }
}

package de.codeshelf.consoleui.examples;

import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.PromtResultItemIF;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import jline.TerminalFactory;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.util.HashMap;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 12.08.2020
 */
public class SimpleExample {

  public static void main(String[] args) throws InterruptedException {
    AnsiConsole.systemInstall();
    System.out.println(ansi().eraseScreen().render("Simple list example:"));

    try {
      ConsolePrompt prompt = new ConsolePrompt();
      PromptBuilder promptBuilder = prompt.getPromptBuilder();

      promptBuilder.createListPrompt()
              .name("pizzatype")
              .message("Which pizza do you want?")
              .newItem().text("Margherita").add()  // without name (name defaults to text)
              .newItem("veneziana").text("Veneziana").add()
              .newItem("hawai").text("Hawai").add()
              .newItem("quattro").text("Quattro Stagioni").add()
              .addPrompt();
      
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
  }}

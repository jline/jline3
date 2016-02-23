package de.codeshelf.consoleui;

import de.codeshelf.consoleui.elements.ConfirmChoice;
import de.codeshelf.consoleui.prompt.ConfirmResult;
import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.PromtResultItemIF;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import jline.TerminalFactory;
import jline.console.completer.StringsCompleter;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.util.HashMap;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 29.11.15
 */
public class Basic {

  public static void main(String[] args) throws InterruptedException {
    AnsiConsole.systemInstall();
    System.out.println(ansi().eraseScreen().render("@|red,italic Hello|@ @|green World|@\n@|reset " +
            "This is a demonstration of ConsoleUI java library. It provides a simple console interface\n" +
            "for querying information from the user. ConsoleUI is inspired by Inquirer.js which is written\n" +
            "in JavaScrpt.|@"));


    try {
      ConsolePrompt prompt = new ConsolePrompt();
      PromptBuilder promptBuilder = prompt.getPromptBuilder();


      promptBuilder.createInputPrompt()
              .name("name")
              .message("Please enter your name")
              .defaultValue("John Doe")
              .addCompleter(new StringsCompleter("Jim", "Jack", "John"))
              .addPrompt();

      promptBuilder.createListPrompt()
              .name("pizzatype")
              .message("Which pizza do you want?")
              .newItem().text("Margherita").add()  // without name (name defaults to text)
              .newItem("veneziana").text("Veneziana").add()
              .newItem("hawai").text("Hawai").add()
              .newItem("quattro").text("Quattro Stagioni").add()
              .addPrompt();

      promptBuilder.createCheckboxPrompt()
              .name("topping")
              .message("Please select additional toppings:")

              .newSeparator("standard toppings")
              .add()

              .newItem().name("cheese").text("Cheese").add()
              .newItem("bacon").text("Bacon").add()
              .newItem("onions").text("Onions").disabledText("Sorry. Out of stock.").add()

              .newSeparator().text("special toppings").add()

              .newItem("salami").text("Very hot salami").check().add()
              .newItem("salmon").text("Smoked Salmon").add()

              .newSeparator("and our speciality...").add()

              .newItem("special").text("Anchovies, and olives").checked(true).add()
              .addPrompt();

      promptBuilder.createChoicePrompt()
              .name("payment")
              .message("How do you want to pay?")

              .newItem().name("cash").message("Cash").key('c').asDefault().add()
              .newItem("visa").message("Visa Card").key('v').add()
              .newItem("master").message("Master Card").key('m').add()
              .newSeparator("online payment").add()
              .newItem("paypal").message("Paypal").key('p').add()
              .addPrompt();

      promptBuilder.createConfirmPromp()
              .name("delivery")
              .message("Is this pizza for delivery?")
              .defaultValue(ConfirmChoice.ConfirmationValue.YES)
              .addPrompt();

      HashMap<String, ? extends PromtResultItemIF> result = prompt.prompt(promptBuilder.build());
      System.out.println("result = " + result);

      ConfirmResult delivery = (ConfirmResult) result.get("delivery");
      if (delivery.getConfirmed()== ConfirmChoice.ConfirmationValue.YES) {
        System.out.println("We will deliver the pizza in 5 minutes");
      }
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

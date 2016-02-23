package de.codeshelf.consoleui;

import de.codeshelf.consoleui.elements.*;
import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.ChoiceItemIF;
import de.codeshelf.consoleui.elements.items.ListItemIF;
import de.codeshelf.consoleui.elements.items.impl.*;
import de.codeshelf.consoleui.prompt.*;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.Operation;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

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

  }

  private static void listChoiceDemo() throws IOException {
    ListPrompt listPrompt = new ListPrompt();
    List<ListItemIF> list= new ArrayList<ListItemIF>();

    ListItemBuilder listItemBuilder = ListItemBuilder.create();
    list.add(new ListItem("One"));
    list.add(new ListItem("Two"));
    list.add(listItemBuilder.name("3").text("Three").build());
    list.add(new Separator("some extra items"));
    list.add(new ListItem("Four"));
    list.add(new Separator());
    list.add(listItemBuilder.text("Five").build());
    ListChoice listChoice = new ListChoice("my first list choice", null, list);
    ListResult result = listPrompt.prompt(listChoice);
    System.out.println("result = " + result);
  }

  private static void checkBoxDemo() throws IOException {
    CheckboxPrompt checkboxPrompt = new CheckboxPrompt();
    List<CheckboxItemIF> list=new ArrayList<CheckboxItemIF>();


    list.add(new CheckboxItem("One"));
    list.add(new CheckboxItem(true,"Two"));
    list.add(CheckboxItemBuilder.create().name("3").text("Three").disabledText("always in").check().build());
    list.add(new Separator("some extra items"));
    list.add(new CheckboxItem("Four"));
    list.add(new Separator());
    list.add(new CheckboxItem(true,"Five"));
    Checkbox checkbox = new Checkbox("my first checkbox", null, list);
    CheckboxResult result = checkboxPrompt.prompt(checkbox);
    System.out.println("result = " + result);
  }

  private static void inputDemo() throws IOException {
    InputResult result;
    InputPrompt inputPrompt = new InputPrompt();

    result =inputPrompt.prompt(new InputValue("newItem", "enter your newItem"));
    System.out.println("result = " + result);

    result =inputPrompt.prompt(new InputValue("firstname","enter your first newItem",null,"John"));
    System.out.println("result = " + result);

    InputValue branch = new InputValue("branch", "enter a branch newItem", null, null);
    branch.addCompleter(new StringsCompleter("consoleui_1","consoleui_1_412_1","consoleui_1_769_2","simplegui_4_32"));
    branch.addCompleter(new FileNameCompleter());
    result = inputPrompt.prompt(branch);
    System.out.println("result = " + result);
  }

  private static void exandableChoiceDemo() throws IOException {
    ExpandableChoicePrompt expandableChoicePrompt = new ExpandableChoicePrompt();
    LinkedHashSet<ChoiceItemIF> choiceItems=new LinkedHashSet<ChoiceItemIF>();
    choiceItems.add(new ChoiceItem('o',"overwrite","Overwrite", false));
    choiceItems.add(new ChoiceItem('a',"overwriteAll","Overwrite this one and all next", false));
    choiceItems.add(new ChoiceItem('d',"diff","Show diff", false));
    choiceItems.add(new ChoiceItem('x',"abort","Abort", false));
    ExpandableChoice expChoice=new ExpandableChoice("conflict in 'MyBestClass.java'", "conflict", choiceItems);
    ExpandableChoiceResult result = expandableChoicePrompt.prompt(expChoice);
    System.out.println("result = " + result);
  }
}

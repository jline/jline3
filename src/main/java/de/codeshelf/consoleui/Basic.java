package de.codeshelf.consoleui;

import de.codeshelf.consoleui.elements.Checkbox;
import de.codeshelf.consoleui.elements.ExpandableChoice;
import de.codeshelf.consoleui.elements.InputValue;
import de.codeshelf.consoleui.elements.ListChoice;
import de.codeshelf.consoleui.elements.items.CheckboxItemIF;
import de.codeshelf.consoleui.elements.items.ListItemIF;
import de.codeshelf.consoleui.elements.items.impl.*;
import de.codeshelf.consoleui.prompt.CheckboxPrompt;
import de.codeshelf.consoleui.prompt.ExpandableChoicePrompt;
import de.codeshelf.consoleui.prompt.InputPrompt;
import de.codeshelf.consoleui.prompt.ListPrompt;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.Operation;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;
import org.fusesource.jansi.AnsiConsole;

import java.io.IOException;
import java.util.ArrayList;
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
      checkBoxDemo();
      listChoiceDemo();
      inputDemo();
      exandableChoiceDemo();
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
    LinkedHashSet<String> result = listPrompt.prompt(listChoice);
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
    LinkedHashSet<String> result = checkboxPrompt.prompt(checkbox);
    System.out.println("result = " + result);
  }

  private static void inputDemo() throws IOException {
    LinkedHashSet<String> result;
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
    LinkedHashSet<ChoiceItem> choiceItems=new LinkedHashSet<ChoiceItem>();
    choiceItems.add(new ChoiceItem('o',"overwrite","Overwrite"));
    choiceItems.add(new ChoiceItem('a',"overwriteAll","Overwrite this one and all next"));
    choiceItems.add(new ChoiceItem('d',"diff","Show diff"));
    choiceItems.add(new ChoiceItem('x',"abort","Abort"));
    ExpandableChoice expChoice=new ExpandableChoice("conflict in 'MyBestClass.java'", "conflict", choiceItems);
    LinkedHashSet<String> result = expandableChoicePrompt.prompt(expChoice);
    System.out.println("result = " + result);
  }

  private static void readBindingsDemo(ConsoleReader console) throws IOException {
    Object o = console.readBinding(console.getKeys());
    System.out.println("o = " + o);
  }

  private static void readCharsDemo(ConsoleReader console) throws IOException {
    int c=0;
    while (c != -1) {
      Operation o = (Operation) console.readBinding(console.getKeys());
      System.out.println("o = " + o + " "+o.getClass().getName());
      String lastBinding = console.getLastBinding();
      System.out.println(lastBinding);
      System.out.println("after read");
    }
    //System.out.println("finished");
  }
}

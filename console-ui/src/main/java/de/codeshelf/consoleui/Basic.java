package de.codeshelf.consoleui;

import de.codeshelf.consoleui.elements.ConfirmChoice;
import de.codeshelf.consoleui.prompt.ConfirmResult;
import de.codeshelf.consoleui.prompt.ConsolePrompt;
import de.codeshelf.consoleui.prompt.ConsolePrompt.UiConfig;
import de.codeshelf.consoleui.prompt.PromptResultItemIF;
import de.codeshelf.consoleui.prompt.builder.PromptBuilder;
import org.jline.builtins.Completers;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.OSUtils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: Andreas Wegmann
 * Date: 29.11.15
 */
public class Basic {

  private static void addInHeader(List<AttributedString> header, String text) {
    addInHeader(header, AttributedStyle.DEFAULT, text);
  }

  private static void addInHeader(List<AttributedString> header, AttributedStyle style, String text) {
    AttributedStringBuilder asb = new AttributedStringBuilder();
    asb.style(style).append(text);
    header.add(asb.toAttributedString());
  }

  public static void main(String[] args) {
    List<AttributedString> header = new ArrayList<>();
    AttributedStyle style = new AttributedStyle();
    addInHeader(header, style.italic().foreground(2), "Hello World!");
    addInHeader(header, "This is a demonstration of ConsoleUI java library. It provides a simple console interface");
    addInHeader(header, "for querying information from the user. ConsoleUI is inspired by Inquirer.js which is written");
    addInHeader(header, "in JavaScript.");
    try (Terminal terminal = TerminalBuilder.builder().build()) {
      UiConfig config;
      if (terminal.getType().equals(Terminal.TYPE_DUMB) || terminal.getType().equals(Terminal.TYPE_DUMB_COLOR)) {
        System.out.println(terminal.getName() + ": " + terminal.getType());
        throw new IllegalStateException("Dumb terminal detected.\nConsoleUi requires real terminal to work!\n"
                                      + "Note: On Windows Jansi or JNA library must be included in classpath.");
      } else if (OSUtils.IS_WINDOWS) {
        config = new UiConfig(">", "( )", "(x)", "( )");
      } else {
        config = new UiConfig("\u276F", "\u25EF ", "\u25C9 ", "\u25EF ");
      }
      //
      // LineReader is needed only if you are adding JLine Completers in your prompts.
      // If you are not using Completers you do not need to create LineReader.
      //
      LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
      ConsolePrompt prompt = new ConsolePrompt(reader, terminal, config);
      PromptBuilder promptBuilder = prompt.getPromptBuilder();


      promptBuilder.createInputPrompt()
              .name("name")
              .message("Please enter your name")
              .defaultValue("John Doe")
              //.mask('*')
              .addCompleter(new Completers.FilesCompleter(() -> Paths.get(System.getProperty("user.dir"))))
//              .addCompleter(new StringsCompleter("Jim", "Jack", "John"))
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

      Map<String, ? extends PromptResultItemIF> result = prompt.prompt(header, promptBuilder.build());
      System.out.println("result = " + result);

      ConfirmResult delivery = (ConfirmResult) result.get("delivery");
      if (delivery.getConfirmed() == ConfirmChoice.ConfirmationValue.YES) {
        System.out.println("We will deliver the pizza in 5 minutes");
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }}

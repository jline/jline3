/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.examples;

import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.consoleui.elements.ConfirmChoice;
import org.jline.consoleui.prompt.ConfirmResult;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.OSUtils;

public class BasicDynamic {

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
        addInHeader(header, style.italic().foreground(2), "Hello Dynamic World!");
        addInHeader(
                header, "This is a demonstration of ConsoleUI java library. It provides a simple console interface");
        addInHeader(
                header,
                "for querying information from the user. ConsoleUI is inspired by Inquirer.js which is written");
        addInHeader(header, "in JavaScript.");
        try (Terminal terminal = TerminalBuilder.builder().build()) {
            Thread executeThread = Thread.currentThread();
            terminal.handle(Terminal.Signal.INT, signal -> executeThread.interrupt());
            ConsolePrompt.UiConfig config;
            if (terminal.getType().equals(Terminal.TYPE_DUMB)
                    || terminal.getType().equals(Terminal.TYPE_DUMB_COLOR)) {
                System.out.println(terminal.getName() + ": " + terminal.getType());
                throw new IllegalStateException("Dumb terminal detected.\nConsoleUi requires real terminal to work!\n"
                        + "Note: On Windows Jansi or JNA library must be included in classpath.");
            } else if (OSUtils.IS_WINDOWS) {
                config = new ConsolePrompt.UiConfig(">", "( )", "(x)", "( )");
            } else {
                config = new ConsolePrompt.UiConfig("\u276F", "\u25EF ", "\u25C9 ", "\u25EF ");
            }
            config.setCancellableFirstPrompt(true);
            //
            // LineReader is needed only if you are adding JLine Completers in your prompts.
            // If you are not using Completers you do not need to create LineReader.
            //
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            ConsolePrompt prompt = new ConsolePrompt(reader, terminal, config);

            Map<String, ? extends PromptResultItemIF> result1 = null, result2 = null, result3 = null;
            while (result2 == null) {
                List<AttributedString> header1 = new ArrayList<>(header);
                result1 = prompt.prompt(header1, pizzaOrHamburgerPrompt(prompt).build());
                if (result1 == null) {
                    System.out.println("User cancelled order.");
                    return;
                }
                while (result3 == null) {
                    if ("Pizza".equals(result1.get("product").getResult())) {
                        result2 = prompt.prompt(pizzaPrompt(prompt).build());
                    } else {
                        result2 = prompt.prompt(hamburgerPrompt(prompt).build());
                    }
                    if (result2 == null) {
                        break;
                    }
                    result3 = prompt.prompt(finalPrompt(prompt).build());
                }
            }

            Map<String, PromptResultItemIF> result = new HashMap<>();
            result.putAll(result1);
            result.putAll(result2);
            result.putAll(result3);
            System.out.println("result = " + result);

            ConfirmResult delivery = (ConfirmResult) result.get("delivery");
            if (delivery.getConfirmed() == ConfirmChoice.ConfirmationValue.YES) {
                System.out.println("We will deliver the order in 5 minutes");
            }

        } catch (InterruptedIOException e) {
            System.out.println("Exiting application.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static PromptBuilder pizzaOrHamburgerPrompt(ConsolePrompt prompt) {
        PromptBuilder promptBuilder = prompt.getPromptBuilder();
        promptBuilder
                .createInputPrompt()
                .name("name")
                .message("Please enter your name")
                .defaultValue("John Doe")
                .addCompleter(new StringsCompleter("Jim", "Jack", "John", "Donald", "Dock"))
                .addPrompt();
        promptBuilder
                .createListPrompt()
                .name("product")
                .message("Which do you want to order?")
                .newItem()
                .text("Pizza")
                .add() // without name (name defaults to text)
                .newItem()
                .text("Hamburger")
                .add()
                .addPrompt();
        return promptBuilder;
    }

    static PromptBuilder pizzaPrompt(ConsolePrompt prompt) {
        PromptBuilder promptBuilder = prompt.getPromptBuilder();
        promptBuilder
                .createListPrompt()
                .name("pizzatype")
                .message("Which pizza do you want?")
                .newItem()
                .text("Margherita")
                .add() // without name (name defaults to text)
                .newItem("veneziana")
                .text("Veneziana")
                .add()
                .newItem("hawai")
                .text("Hawai")
                .add()
                .newItem("quattro")
                .text("Quattro Stagioni")
                .add()
                .addPrompt();
        promptBuilder
                .createCheckboxPrompt()
                .name("topping")
                .message("Please select additional toppings:")
                .newSeparator("standard toppings")
                .add()
                .newItem()
                .name("cheese")
                .text("Cheese")
                .add()
                .newItem("bacon")
                .text("Bacon")
                .add()
                .newItem("onions")
                .text("Onions")
                .disabledText("Sorry. Out of stock.")
                .add()
                .newSeparator()
                .text("special toppings")
                .add()
                .newItem("salami")
                .text("Very hot salami")
                .check()
                .add()
                .newItem("salmon")
                .text("Smoked Salmon")
                .add()
                .newSeparator("and our speciality...")
                .add()
                .newItem("special")
                .text("Anchovies, and olives")
                .checked(true)
                .add()
                .addPrompt();
        return promptBuilder;
    }

    static PromptBuilder hamburgerPrompt(ConsolePrompt prompt) {
        PromptBuilder promptBuilder = prompt.getPromptBuilder();
        promptBuilder
                .createListPrompt()
                .name("hamburgertype")
                .message("Which hamburger do you want?")
                .newItem()
                .text("Cheeseburger")
                .add() // without name (name defaults to text)
                .newItem("chickenburger")
                .text("Chickenburger")
                .add()
                .newItem("veggieburger")
                .text("Veggieburger")
                .add()
                .addPrompt();
        promptBuilder
                .createCheckboxPrompt()
                .name("ingredients")
                .message("Please select additional ingredients:")
                .newSeparator("standard ingredients")
                .add()
                .newItem()
                .name("tomato")
                .text("Tomato")
                .add()
                .newItem("lettuce")
                .text("Lettuce")
                .add()
                .newItem("onions")
                .text("Onions")
                .disabledText("Sorry. Out of stock.")
                .add()
                .newSeparator()
                .text("special ingredients")
                .add()
                .newItem("crispybacon")
                .text("Crispy Bacon")
                .check()
                .add()
                .addPrompt();
        return promptBuilder;
    }

    static PromptBuilder finalPrompt(ConsolePrompt prompt) {
        PromptBuilder promptBuilder = prompt.getPromptBuilder();
        promptBuilder
                .createChoicePrompt()
                .name("payment")
                .message("How do you want to pay?")
                .newItem()
                .name("cash")
                .message("Cash")
                .key('c')
                .asDefault()
                .add()
                .newItem("visa")
                .message("Visa Card")
                .key('v')
                .add()
                .newItem("master")
                .message("Master Card")
                .key('m')
                .add()
                .newSeparator("online payment")
                .add()
                .newItem("paypal")
                .message("Paypal")
                .key('p')
                .add()
                .addPrompt();
        promptBuilder
                .createConfirmPromp()
                .name("delivery")
                .message("Is this order for delivery?")
                .defaultValue(ConfirmChoice.ConfirmationValue.YES)
                .addPrompt();
        return promptBuilder;
    }
}

/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.util.Map;

import org.jline.consoleui.elements.ConfirmChoice;
import org.jline.consoleui.prompt.ConsolePrompt;
import org.jline.consoleui.prompt.PromptResultItemIF;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Example demonstrating basic ConsoleUI usage.
 */
@SuppressWarnings("deprecation")
public class ConsoleUIExample {

    // SNIPPET_START: ConsoleUIBasicExample
    public static void main(String[] args) throws IOException {
        // Create a terminal
        Terminal terminal = TerminalBuilder.builder().build();

        // Create a ConsolePrompt with the terminal
        ConsolePrompt prompt = new ConsolePrompt(terminal);

        // Get a PromptBuilder to create UI elements
        PromptBuilder builder = prompt.getPromptBuilder();

        // Create a simple yes/no prompt
        builder.createConfirmPromp()
                .name("continue")
                .message("Do you want to continue?")
                .defaultValue(ConfirmChoice.ConfirmationValue.YES)
                .addPrompt();

        try {
            // Display the prompt and get the result
            Map<String, PromptResultItemIF> result = prompt.prompt(builder.build());
            System.out.println("You chose: " + result.get("continue"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            terminal.close();
        }
    }
    // SNIPPET_END: ConsoleUIBasicExample

    // SNIPPET_START: ConsoleUIInputExample
    public void demonstrateInputPrompt(Terminal terminal) {
        ConsolePrompt prompt = new ConsolePrompt(terminal);
        PromptBuilder builder = prompt.getPromptBuilder();

        // Create an input prompt for text entry
        builder.createInputPrompt()
                .name("username")
                .message("Enter your username")
                .defaultValue("admin")
                .addPrompt();

        // Create a masked input prompt for password entry
        builder.createInputPrompt()
                .name("password")
                .message("Enter your password")
                .mask('*')
                .addPrompt();

        try {
            Map<String, PromptResultItemIF> result = prompt.prompt(builder.build());
            String username = result.get("username").getResult();
            String password = result.get("password").getResult();
            System.out.println("Logged in as: " + username);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: ConsoleUIInputExample

    // SNIPPET_START: ConsoleUIListExample
    public void demonstrateListPrompt(Terminal terminal) {
        ConsolePrompt prompt = new ConsolePrompt(terminal);
        PromptBuilder builder = prompt.getPromptBuilder();

        // Create a list prompt for single selection
        builder.createListPrompt()
                .name("color")
                .message("Choose your favorite color")
                .newItem()
                .text("Red")
                .add()
                .newItem("green")
                .text("Green")
                .add()
                .newItem("blue")
                .text("Blue")
                .add()
                .newItem("yellow")
                .text("Yellow")
                .add()
                .pageSize(3) // Show 3 items at a time
                .addPrompt();

        try {
            Map<String, PromptResultItemIF> result = prompt.prompt(builder.build());
            System.out.println("Selected color: " + result.get("color").getResult());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: ConsoleUIListExample

    // SNIPPET_START: ConsoleUICheckboxExample
    public void demonstrateCheckboxPrompt(Terminal terminal) {
        ConsolePrompt prompt = new ConsolePrompt(terminal);
        PromptBuilder builder = prompt.getPromptBuilder();

        // Create a checkbox prompt for multiple selection
        builder.createCheckboxPrompt()
                .name("toppings")
                .message("Select pizza toppings")
                .newSeparator("Vegetables")
                .add()
                .newItem("tomato")
                .text("Tomato")
                .add()
                .newItem("onion")
                .text("Onion")
                .add()
                .newItem("pepper")
                .text("Bell Pepper")
                .add()
                .newSeparator("Meats")
                .add()
                .newItem("pepperoni")
                .text("Pepperoni")
                .check()
                .add()
                .newItem("sausage")
                .text("Sausage")
                .add()
                .newItem("bacon")
                .text("Bacon")
                .add()
                .newSeparator("Others")
                .add()
                .newItem("extra_cheese")
                .text("Extra Cheese")
                .check()
                .add()
                .newItem("pineapple")
                .text("Pineapple")
                .disabledText("Temporarily unavailable")
                .add()
                .addPrompt();

        try {
            Map<String, PromptResultItemIF> result = prompt.prompt(builder.build());
            System.out.println("Selected toppings: " + result.get("toppings").getResult());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: ConsoleUICheckboxExample

    // SNIPPET_START: ConsoleUIChoiceExample
    public void demonstrateChoicePrompt(Terminal terminal) {
        ConsolePrompt prompt = new ConsolePrompt(terminal);
        PromptBuilder builder = prompt.getPromptBuilder();

        // Create an expandable choice prompt
        builder.createChoicePrompt()
                .name("payment")
                .message("How would you like to pay?")
                .newItem()
                .name("cash")
                .message("Cash")
                .key('c')
                .asDefault()
                .add()
                .newItem("credit")
                .message("Credit Card")
                .key('r')
                .add()
                .newItem("debit")
                .message("Debit Card")
                .key('d')
                .add()
                .newSeparator("Online Options")
                .add()
                .newItem("paypal")
                .message("PayPal")
                .key('p')
                .add()
                .newItem("crypto")
                .message("Cryptocurrency")
                .key('y')
                .add()
                .addPrompt();

        try {
            Map<String, PromptResultItemIF> result = prompt.prompt(builder.build());
            System.out.println(
                    "Selected payment method: " + result.get("payment").getResult());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // SNIPPET_END: ConsoleUIChoiceExample
}

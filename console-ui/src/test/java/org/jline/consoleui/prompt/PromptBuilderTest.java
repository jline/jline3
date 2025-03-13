/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.consoleui.prompt;

import java.io.IOException;
import java.util.List;

import org.jline.consoleui.elements.ConfirmChoice;
import org.jline.consoleui.elements.PromptableElementIF;
import org.jline.consoleui.prompt.builder.PromptBuilder;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PromptBuilderTest {

    @Test
    public void testBuilder() throws IOException {
        ConsolePrompt prompt = new ConsolePrompt(TerminalBuilder.builder().build());
        PromptBuilder promptBuilder = prompt.getPromptBuilder();

        promptBuilder
                .createConfirmPromp()
                .name("wantapizza")
                .message("Do you want to order a pizza?")
                .defaultValue(ConfirmChoice.ConfirmationValue.YES)
                .addPrompt();

        promptBuilder
                .createInputPrompt()
                .name("name")
                .message("Please enter your name")
                .defaultValue("John Doe")
                .addPrompt();

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

        assertNotNull(promptBuilder);
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

        List<PromptableElementIF> promptableElementList = promptBuilder.build();

        // only for test. reset the default reader to a test reader to automate the input
        // promptableElementList.get(0)

        // HashMap<String, Object> result = prompt.prompt(promptableElementList);
    }
}

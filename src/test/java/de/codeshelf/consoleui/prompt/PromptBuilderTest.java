package de.codeshelf.consoleui.prompt;

import org.junit.Test;

/**
 * User: Andreas Wegmann
 * Date: 20.01.16
 */
public class PromptBuilderTest {

  @Test
  public void testBuilder() throws Exception {
    ConsolePrompt prompt = new ConsolePrompt();
    PromptBuilder promptBuilder = prompt.getPromptBuilder();

    promptBuilder.createInputPrompt()
            .name("name")
            .message("Please enter your name")
            .defaultValue("John Doe")
            .addPrompt();

    promptBuilder.createListPrompt()
            .name("pizzatype")
            .message("Which pizza do you want?")
            .newItem().text("Margherita").add()  // without name (name defaults to text)
            .newItem("veneziana").text("Veniziana").add()
            .newItem("hawai").text("Hawai").add()
            .newItem("quattro").text("Quattro Stagioni").add()
            .addPrompt();

    promptBuilder.createCheckboxPrompt()
            .name("topping")
            .message("Please select additional toppings:")

            .newSeparator("standard toppings").add()

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

    prompt.prompt(promptBuilder.build());
  }


}
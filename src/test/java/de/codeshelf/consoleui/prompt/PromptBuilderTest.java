package de.codeshelf.consoleui.prompt;

import org.junit.Test;

/**
 * User: ${FULL_NAME}
 * Date: 20.01.16
 */
public class PromptBuilderTest {

  @Test
  public void testBuilder() throws Exception {
    ConsolePrompt prompt = new ConsolePrompt();
    PromptBuilder promptBuilder = prompt.getPromptBuilder();

    promptBuilder.createInputPrompt()
            .name("name")
            .message("message")
            .defaultValue("defaultValue")
            .add();

    promptBuilder.createListPrompt()
            .name("name")
            .message("message")
            .getItemBuilder()
            .name("item1.name").text("item1 text").add()
            .name("item2.name").text("item2 text").add()
            .name("item3.name").text("item3 text").add()
            .name("item4.name").text("item4 text").add();

    promptBuilder.createChoicePrompt()
            .name("choicePrompt")
            .message("choice Message")
            .getItemBuilder()
            .name("name1").message("message a").key('a').add()
            .name("name2").message("message b").key('b').add()
            .name("name3").message("message c").key('c').asDefault().add();

    promptBuilder.createCheckboxPrompt()
            .name("name")
            .message("message")
            .getItemBuilder()
            .name("item1.name").text("item1 text").add()
            .name("item2.name").text("item2 text").disabledText("I'm disabled").add()
            .separator().add()
            .name("item3.name").text("item3 text").check().add()
            .name("item3.name").text("item3 text").add()
            .separator("and the last...").add()
            .name("item4.name").text("item4 text").checked(true).add();



  }
}
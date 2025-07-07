/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jline.prompt.impl.DefaultConfirmPrompt;
import org.jline.prompt.impl.DefaultInputPrompt;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for dynamic prompting functionality.
 */
public class DynamicPromptTest {

    @Test
    public void testDynamicPromptLogic() throws Exception {
        // Create a terminal (in test mode)
        Terminal terminal = TerminalBuilder.builder().system(false).build();

        // Create a prompter
        Prompter prompter = PrompterFactory.create(terminal);
        assertNotNull(prompter);

        // Test the dynamic prompt provider logic
        List<String> promptSequence = new ArrayList<>();

        // Create a dynamic prompt provider that creates different prompts based on previous answers
        Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>> promptProvider =
                (Map<String, ? extends PromptResult<? extends Prompt>> results) -> {
                    List<Prompt> prompts = new ArrayList<>();

                    if (results.isEmpty()) {
                        // First prompt: ask for user's name
                        promptSequence.add("First prompt: name");
                        prompts.add(new DefaultInputPrompt("name", "What is your name?", null, null, null, null, null));

                    } else if (results.containsKey("name") && !results.containsKey("age")) {
                        // Second prompt: ask for age (depends on having a name)
                        promptSequence.add("Second prompt: age");
                        String name = results.get("name").getResult();
                        prompts.add(new DefaultInputPrompt(
                                "age", "Hello " + name + ", what is your age?", null, null, null, null, null));

                    } else if (results.containsKey("age") && !results.containsKey("confirm")) {
                        // Third prompt: confirm information (depends on having both name and age)
                        promptSequence.add("Third prompt: confirm");
                        String name = results.get("name").getResult();
                        String age = results.get("age").getResult();
                        prompts.add(new DefaultConfirmPrompt(
                                "confirm", "Is this correct? Name: " + name + ", Age: " + age, false));

                    } else {
                        // No more prompts
                        promptSequence.add("No more prompts");
                        return null;
                    }

                    return prompts;
                };

        // Test that the provider logic works correctly

        // First call - empty results should return name prompt
        List<? extends Prompt> firstPrompts = promptProvider.apply(Collections.emptyMap());
        assertNotNull(firstPrompts);
        assertEquals(1, firstPrompts.size());
        assertEquals("name", firstPrompts.get(0).getName());
        assertEquals("What is your name?", firstPrompts.get(0).getMessage());

        // Simulate name result
        Map<String, PromptResult<? extends Prompt>> nameResult = new HashMap<>();
        nameResult.put("name", new MockInputResult("John", firstPrompts.get(0)));

        // Second call - with name should return age prompt
        List<? extends Prompt> secondPrompts = promptProvider.apply(nameResult);
        assertNotNull(secondPrompts);
        assertEquals(1, secondPrompts.size());
        assertEquals("age", secondPrompts.get(0).getName());
        assertTrue(secondPrompts.get(0).getMessage().contains("Hello John"));

        // Simulate age result
        Map<String, PromptResult<? extends Prompt>> nameAndAgeResults = new HashMap<>();
        nameAndAgeResults.put("name", new MockInputResult("John", firstPrompts.get(0)));
        nameAndAgeResults.put("age", new MockInputResult("25", secondPrompts.get(0)));

        // Third call - with name and age should return confirm prompt
        List<? extends Prompt> thirdPrompts = promptProvider.apply(nameAndAgeResults);
        assertNotNull(thirdPrompts);
        assertEquals(1, thirdPrompts.size());
        assertEquals("confirm", thirdPrompts.get(0).getName());
        assertTrue(thirdPrompts.get(0).getMessage().contains("John"));
        assertTrue(thirdPrompts.get(0).getMessage().contains("25"));

        // Simulate confirm result
        Map<String, PromptResult<? extends Prompt>> allResults = new HashMap<>();
        allResults.put("name", new MockInputResult("John", firstPrompts.get(0)));
        allResults.put("age", new MockInputResult("25", secondPrompts.get(0)));
        allResults.put("confirm", new MockConfirmResult(true, thirdPrompts.get(0)));

        // Fourth call - with all results should return null (no more prompts)
        List<? extends Prompt> fourthPrompts = promptProvider.apply(allResults);
        assertNull(fourthPrompts);

        // Verify the sequence was called correctly
        assertEquals(4, promptSequence.size());
        assertEquals("First prompt: name", promptSequence.get(0));
        assertEquals("Second prompt: age", promptSequence.get(1));
        assertEquals("Third prompt: confirm", promptSequence.get(2));
        assertEquals("No more prompts", promptSequence.get(3));
    }

    // Mock implementations for testing
    private static class MockInputResult implements InputResult {
        private final String result;
        private final Prompt prompt;

        public MockInputResult(String result, Prompt prompt) {
            this.result = result;
            this.prompt = prompt;
        }

        @Override
        public String getResult() {
            return result;
        }

        @Override
        public String getDisplayResult() {
            return result;
        }

        @Override
        public String getInput() {
            return result;
        }

        @Override
        public InputPrompt getPrompt() {
            return (InputPrompt) prompt;
        }
    }

    private static class MockConfirmResult implements ConfirmResult {
        private final boolean result;
        private final Prompt prompt;

        public MockConfirmResult(boolean result, Prompt prompt) {
            this.result = result;
            this.prompt = prompt;
        }

        @Override
        public String getResult() {
            return result ? "YES" : "NO";
        }

        @Override
        public String getDisplayResult() {
            return result ? "Yes" : "No";
        }

        @Override
        public boolean isConfirmed() {
            return result;
        }

        @Override
        public ConfirmationValue getConfirmed() {
            return result ? ConfirmationValue.YES : ConfirmationValue.NO;
        }

        @Override
        public ConfirmPrompt getPrompt() {
            return (ConfirmPrompt) prompt;
        }
    }
}

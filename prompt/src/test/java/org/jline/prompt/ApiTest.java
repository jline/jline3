/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the API structure is correct.
 */
public class ApiTest {

    @Test
    public void testApiStructure() {
        // Test that all interfaces exist and can be loaded
        assertNotNull(Prompter.class);
        assertNotNull(Prompt.class);
        assertNotNull(PromptResult.class);
        assertNotNull(PromptBuilder.class);
        assertNotNull(PromptItem.class);

        // Test prompt interfaces
        assertNotNull(CheckboxPrompt.class);
        assertNotNull(ListPrompt.class);
        assertNotNull(ChoicePrompt.class);
        assertNotNull(InputPrompt.class);
        assertNotNull(ConfirmPrompt.class);
        assertNotNull(TextPrompt.class);
        assertNotNull(PasswordPrompt.class);
        assertNotNull(NumberPrompt.class);
        assertNotNull(SearchPrompt.class);
        assertNotNull(EditorPrompt.class);

        // Test item interfaces
        assertNotNull(ListItem.class);
        assertNotNull(CheckboxItem.class);
        assertNotNull(ChoiceItem.class);
        assertNotNull(ConfirmItem.class);

        // Test result interfaces
        assertNotNull(ListResult.class);
        assertNotNull(CheckboxResult.class);
        assertNotNull(ChoiceResult.class);
        assertNotNull(InputResult.class);
        assertNotNull(ConfirmResult.class);
        assertNotNull(NoResult.class);
        assertNotNull(SearchResult.class);
        assertNotNull(EditorResult.class);

        // Test factory
        assertNotNull(PrompterFactory.class);
    }

    @Test
    public void testInheritanceStructure() {
        // Test that prompt interfaces extend Prompt
        assertTrue(Prompt.class.isAssignableFrom(CheckboxPrompt.class));
        assertTrue(Prompt.class.isAssignableFrom(ListPrompt.class));
        assertTrue(Prompt.class.isAssignableFrom(ChoicePrompt.class));
        assertTrue(Prompt.class.isAssignableFrom(InputPrompt.class));
        assertTrue(Prompt.class.isAssignableFrom(ConfirmPrompt.class));
        assertTrue(Prompt.class.isAssignableFrom(TextPrompt.class));
        assertTrue(Prompt.class.isAssignableFrom(PasswordPrompt.class));
        assertTrue(Prompt.class.isAssignableFrom(NumberPrompt.class));
        assertTrue(Prompt.class.isAssignableFrom(SearchPrompt.class));
        assertTrue(Prompt.class.isAssignableFrom(EditorPrompt.class));

        // Test that PasswordPrompt extends InputPrompt
        assertTrue(InputPrompt.class.isAssignableFrom(PasswordPrompt.class));
        // Test that NumberPrompt extends InputPrompt
        assertTrue(InputPrompt.class.isAssignableFrom(NumberPrompt.class));

        // Test that item interfaces extend PromptItem
        assertTrue(PromptItem.class.isAssignableFrom(ListItem.class));
        assertTrue(PromptItem.class.isAssignableFrom(CheckboxItem.class));
        assertTrue(PromptItem.class.isAssignableFrom(ChoiceItem.class));
        assertTrue(PromptItem.class.isAssignableFrom(ConfirmItem.class));

        // Test that result interfaces extend PromptResult
        assertTrue(PromptResult.class.isAssignableFrom(ListResult.class));
        assertTrue(PromptResult.class.isAssignableFrom(CheckboxResult.class));
        assertTrue(PromptResult.class.isAssignableFrom(ChoiceResult.class));
        assertTrue(PromptResult.class.isAssignableFrom(InputResult.class));
        assertTrue(PromptResult.class.isAssignableFrom(ConfirmResult.class));
        assertTrue(PromptResult.class.isAssignableFrom(NoResult.class));
        assertTrue(PromptResult.class.isAssignableFrom(SearchResult.class));
        assertTrue(PromptResult.class.isAssignableFrom(EditorResult.class));
    }

    @Test
    public void testBuilderInterfaces() {
        // Test that builder interfaces exist
        assertNotNull(InputBuilder.class);
        assertNotNull(ListBuilder.class);
        assertNotNull(CheckboxBuilder.class);
        assertNotNull(ChoiceBuilder.class);
        assertNotNull(ConfirmBuilder.class);
        assertNotNull(TextBuilder.class);
        assertNotNull(PasswordBuilder.class);
        assertNotNull(NumberBuilder.class);
        assertNotNull(SearchBuilder.class);
        assertNotNull(EditorBuilder.class);

        // Test that all builders extend BaseBuilder
        assertTrue(BaseBuilder.class.isAssignableFrom(InputBuilder.class));
        assertTrue(BaseBuilder.class.isAssignableFrom(ListBuilder.class));
        assertTrue(BaseBuilder.class.isAssignableFrom(CheckboxBuilder.class));
        assertTrue(BaseBuilder.class.isAssignableFrom(ChoiceBuilder.class));
        assertTrue(BaseBuilder.class.isAssignableFrom(ConfirmBuilder.class));
        assertTrue(BaseBuilder.class.isAssignableFrom(TextBuilder.class));
        assertTrue(BaseBuilder.class.isAssignableFrom(PasswordBuilder.class));
        assertTrue(BaseBuilder.class.isAssignableFrom(NumberBuilder.class));
        assertTrue(BaseBuilder.class.isAssignableFrom(SearchBuilder.class));
        assertTrue(BaseBuilder.class.isAssignableFrom(EditorBuilder.class));
    }

    @Test
    public void testNoResultSingleton() {
        // Test that NoResult has a singleton instance
        assertNotNull(NoResult.INSTANCE);
        assertTrue(NoResult.INSTANCE instanceof NoResult);
    }

    @Test
    public void testConfirmResultEnum() {
        // Test that ConfirmResult has the expected enum values
        assertEquals(2, ConfirmResult.ConfirmationValue.values().length);
        assertNotNull(ConfirmResult.ConfirmationValue.YES);
        assertNotNull(ConfirmResult.ConfirmationValue.NO);
    }
}

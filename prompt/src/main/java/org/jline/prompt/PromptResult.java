/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

/**
 * Parameterized interface representing the result of a user prompt interaction.
 *
 * <p>
 * {@code PromptResult} provides a type-safe way to access the results of user interactions
 * with various prompt types. Each result is associated with the specific prompt that generated it,
 * enabling access to both the user's response and the original prompt configuration.
 * </p>
 *
 * <h3>Type Safety</h3>
 * <p>
 * The interface is parameterized with the specific prompt type, ensuring compile-time
 * type safety when accessing prompt-specific information:
 * </p>
 * <ul>
 *   <li>{@link ListResult} extends {@code PromptResult<ListPrompt>}</li>
 *   <li>{@link CheckboxResult} extends {@code PromptResult<CheckboxPrompt>}</li>
 *   <li>{@link ChoiceResult} extends {@code PromptResult<ChoicePrompt>}</li>
 *   <li>{@link InputResult} extends {@code PromptResult<InputPrompt>}</li>
 *   <li>{@link ConfirmResult} extends {@code PromptResult<ConfirmPrompt>}</li>
 * </ul>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Basic result access
 * Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(...);
 *
 * // Type-safe casting and access
 * ListResult listResult = (ListResult) results.get("choice");
 * String selectedId = listResult.getSelectedId();
 *
 * // Access the original prompt
 * ListPrompt originalPrompt = listResult.getPrompt();
 * if (originalPrompt != null) {
 *     List<ListItem> availableItems = originalPrompt.getItems();
 * }
 *
 * // Generic result access
 * String displayValue = listResult.getDisplayResult();
 * }</pre>
 *
 * <h3>Immutability</h3>
 * <p>
 * All prompt results are immutable value objects. Once created, their state cannot be modified.
 * This ensures thread safety and prevents accidental modification of user responses.
 * </p>
 *
 * @param <T> the specific type of prompt that generated this result
 * @see Prompt
 * @see Prompter#prompt(List, List)
 * @since 3.30.0
 */
public interface PromptResult<T extends Prompt> {

    /**
     * Get the raw result value as a string representation.
     *
     * <p>
     * This method returns the user's response in its most basic string form.
     * For selection-based prompts (list, checkbox, choice), this typically returns
     * the ID(s) of selected items. For input prompts, this returns the entered text.
     * </p>
     *
     * <h4>Examples:</h4>
     * <ul>
     *   <li>List prompt: {@code "option1"}</li>
     *   <li>Checkbox prompt: {@code "[option1, option3]"}</li>
     *   <li>Input prompt: {@code "user entered text"}</li>
     *   <li>Confirm prompt: {@code "YES"} or {@code "NO"}</li>
     * </ul>
     *
     * @return the result as a string, never {@code null}
     */
    String getResult();

    /**
     * Get a human-readable display representation of the result.
     *
     * <p>
     * This method returns a formatted version of the result that is suitable for
     * display to users. It may differ from {@link #getResult()} by providing
     * more descriptive text or better formatting.
     * </p>
     *
     * <p>
     * The default implementation returns the same value as {@link #getResult()}.
     * Specific result types may override this to provide more user-friendly formatting.
     * </p>
     *
     * @return a display-friendly representation of the result, never {@code null}
     */
    default String getDisplayResult() {
        return getResult();
    }

    /**
     * Get the prompt that generated this result.
     *
     * <p>
     * This method provides access to the original prompt configuration, allowing
     * inspection of the prompt's properties such as available items, validation rules,
     * or other configuration details.
     * </p>
     *
     * <h4>Example Usage:</h4>
     * <pre>{@code
     * ListResult result = (ListResult) results.get("choice");
     * ListPrompt prompt = result.getPrompt();
     *
     * if (prompt != null) {
     *     // Access prompt configuration
     *     String promptMessage = prompt.getMessage();
     *     List<ListItem> availableItems = prompt.getItems();
     *
     *     // Validate selection against available options
     *     String selectedId = result.getSelectedId();
     *     boolean isValidSelection = availableItems.stream()
     *         .anyMatch(item -> item.getName().equals(selectedId));
     * }
     * }</pre>
     *
     * @return the prompt that generated this result, or {@code null} if not available
     * @see Prompt
     */
    default T getPrompt() {
        return null;
    }
}

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
 * Base interface for all prompt builders providing common configuration methods.
 *
 * <p>
 * {@code BaseBuilder} defines the fundamental methods that all prompt builders share,
 * including name and message configuration. It uses a parameterized self-type pattern
 * to enable fluent method chaining while maintaining type safety across different builder types.
 * </p>
 *
 * <h3>Fluent Interface Pattern</h3>
 * <p>
 * The interface uses the "curiously recurring template pattern" where each builder
 * extends {@code BaseBuilder<ConcreteBuilderType>}. This ensures that method chaining
 * returns the correct concrete type, enabling IDE auto-completion and type safety.
 * </p>
 *
 * <h3>Common Usage Pattern</h3>
 * <pre>{@code
 * // All builders follow this pattern
 * builder.createListPrompt()
 *     .name("choice")           // From BaseBuilder
 *     .message("Select option:") // From BaseBuilder
 *     .newItem("opt1")          // Specific to ListBuilder
 *     .text("Option 1")         // Specific to ListBuilder
 *     .add()                    // Specific to ListBuilder
 *     .addPrompt();             // From BaseBuilder
 * }</pre>
 *
 * <h3>Implementation Requirements</h3>
 * <p>
 * Concrete builders must:
 * </p>
 * <ul>
 *   <li>Extend this interface with their own type as the parameter</li>
 *   <li>Return {@code this} from all builder methods for fluent chaining</li>
 *   <li>Validate that required fields (name, message) are set before building</li>
 * </ul>
 *
 * @param <T> the concrete builder type (enables fluent interface with proper return types)
 * @see PromptBuilder
 * @see InputBuilder
 * @see ListBuilder
 * @see CheckboxBuilder
 * @since 3.30.0
 */
public interface BaseBuilder<T extends BaseBuilder<T>> {

    /**
     * Set the unique identifier for this prompt.
     *
     * <p>
     * The name serves as the key in the result map returned by {@link Prompter#prompt}.
     * It must be unique within a single prompt session to avoid conflicts.
     * </p>
     *
     * <h4>Naming Guidelines:</h4>
     * <ul>
     *   <li>Use descriptive, lowercase names with underscores: {@code "user_name"}, {@code "file_path"}</li>
     *   <li>Avoid spaces and special characters</li>
     *   <li>Keep names concise but meaningful</li>
     *   <li>Use consistent naming conventions across your application</li>
     * </ul>
     *
     * <h4>Example:</h4>
     * <pre>{@code
     * builder.createInputPrompt()
     *     .name("email_address")  // Used as key in results map
     *     .message("Enter your email:")
     *     .addPrompt();
     *
     * // Later access the result
     * InputResult emailResult = (InputResult) results.get("email_address");
     * }</pre>
     *
     * @param name the unique identifier for this prompt (required, non-null, non-empty)
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if name is null or empty
     */
    T name(String name);

    /**
     * Set the message displayed to the user for this prompt.
     *
     * <p>
     * The message is the primary text that explains what the user should do.
     * It should be clear, concise, and provide sufficient context for the user
     * to understand what input is expected.
     * </p>
     *
     * <h4>Message Guidelines:</h4>
     * <ul>
     *   <li>Use clear, actionable language: "Select your preferred option"</li>
     *   <li>End with appropriate punctuation (colon for selections, question mark for questions)</li>
     *   <li>Keep messages concise but informative</li>
     *   <li>Consider the terminal width for longer messages</li>
     * </ul>
     *
     * <h4>Examples:</h4>
     * <ul>
     *   <li>List prompt: {@code "Choose your preferred IDE:"}</li>
     *   <li>Input prompt: {@code "Enter your full name:"}</li>
     *   <li>Confirm prompt: {@code "Do you want to continue?"}</li>
     *   <li>Checkbox prompt: {@code "Select features to enable:"}</li>
     * </ul>
     *
     * @param message the message to display to the user (required, non-null, non-empty)
     * @return this builder instance for method chaining
     * @throws IllegalArgumentException if message is null or empty
     */
    T message(String message);

    /**
     * Complete the configuration of this prompt and add it to the parent builder.
     *
     * <p>
     * This method finalizes the current prompt configuration, validates that all
     * required fields are set, creates the prompt instance, and adds it to the
     * parent {@link PromptBuilder}. After calling this method, you can continue
     * adding more prompts or call {@link PromptBuilder#build()} to create the final list.
     * </p>
     *
     * <h4>Validation:</h4>
     * <p>
     * This method typically validates that:
     * </p>
     * <ul>
     *   <li>The prompt name is set and unique</li>
     *   <li>The message is set and non-empty</li>
     *   <li>Any prompt-specific requirements are met (e.g., list items are added)</li>
     * </ul>
     *
     * <h4>Example Usage:</h4>
     * <pre>{@code
     * PromptBuilder builder = prompter.newBuilder();
     *
     * builder.createListPrompt()
     *     .name("color")
     *     .message("Choose a color:")
     *     .newItem("red").text("Red").add()
     *     .newItem("blue").text("Blue").add()
     *     .addPrompt()  // Completes this prompt and returns to builder
     *     .createConfirmPrompt()
     *     .name("confirm")
     *     .message("Are you sure?")
     *     .addPrompt(); // Completes second prompt
     *
     * List<Prompt> prompts = builder.build();
     * }</pre>
     *
     * @return the parent {@link PromptBuilder} for continued prompt configuration
     * @throws IllegalStateException if required fields are not set or validation fails
     * @see PromptBuilder#build()
     */
    PromptBuilder addPrompt();
}

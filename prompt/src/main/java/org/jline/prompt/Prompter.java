/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

/**
 * Main interface for the JLine Prompt API.
 *
 * <p>
 * The {@code Prompter} is the primary entry point for creating and executing interactive console prompts.
 * It provides a fluent, type-safe API for building various types of user input prompts including
 * text input, lists, checkboxes, choices, and confirmations.
 * </p>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * // Create a prompter
 * Terminal terminal = TerminalBuilder.builder().build();
 * Prompter prompter = PrompterFactory.create(terminal);
 *
 * // Build and execute prompts
 * PromptBuilder builder = prompter.newBuilder();
 * builder.createListPrompt()
 *     .name("choice")
 *     .message("Select an option:")
 *     .newItem("option1").text("Option 1").add()
 *     .newItem("option2").text("Option 2").add()
 *     .addPrompt();
 *
 * Map<String, ? extends PromptResult<? extends Prompt>> results =
 *     prompter.prompt(Collections.emptyList(), builder.build());
 * }</pre>
 *
 * <h3>Dynamic Prompts</h3>
 * <p>
 * The prompter supports dynamic prompt generation where subsequent prompts can be created
 * based on the results of previous prompts:
 * </p>
 * <pre>{@code
 * Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(
 *     header,
 *     previousResults -> {
 *         // Create prompts based on previous answers
 *         if (shouldShowMorePrompts(previousResults)) {
 *             return createAdditionalPrompts();
 *         }
 *         return null; // No more prompts
 *     }
 * );
 * }</pre>
 *
 * <h3>Thread Safety</h3>
 * <p>
 * Prompter instances are thread-safe for reading configuration, but prompt execution
 * should be performed on a single thread due to terminal I/O constraints.
 * </p>
 *
 * @see PrompterFactory
 * @see PromptBuilder
 * @see PromptResult
 * @since 3.30.0
 */
public interface Prompter {

    /**
     * Get a builder for creating prompts.
     *
     * @return a prompt builder
     */
    PromptBuilder newBuilder();

    /**
     * Execute a list of prompts and collect user responses.
     *
     * <p>
     * This method presents the given prompts to the user in sequence and collects their responses.
     * Each prompt is identified by its name, which serves as the key in the returned result map.
     * </p>
     *
     * <h4>Example:</h4>
     * <pre>{@code
     * List<AttributedString> header = Arrays.asList(
     *     new AttributedString("Welcome to the setup wizard")
     * );
     *
     * List<Prompt> prompts = builder.build();
     * Map<String, ? extends PromptResult<? extends Prompt>> results =
     *     prompter.prompt(header, prompts);
     *
     * // Access specific results
     * ListResult choice = (ListResult) results.get("choice");
     * String selectedId = choice.getSelectedId();
     * }</pre>
     *
     * @param header header information to display before the prompts (may be empty)
     * @param prompts the list of prompts to present to the user in sequence
     * @return a map containing results for each prompt, keyed by prompt name
     * @throws IOException if an I/O error occurs during prompt execution
     * @throws UserInterruptException if user interrupt handling is enabled and the user types the interrupt character (Ctrl+C)
     * @see PromptBuilder#build()
     * @see PromptResult
     */
    Map<String, ? extends PromptResult<? extends Prompt>> prompt(
            List<AttributedString> header, List<? extends Prompt> prompts) throws IOException, UserInterruptException;

    /**
     * Execute prompts dynamically based on previous user responses.
     *
     * <p>
     * This method enables conditional prompting where subsequent prompts are generated
     * based on the user's previous answers. The {@code promptsProvider} function is called
     * repeatedly with the accumulated results until it returns {@code null}, indicating
     * no more prompts should be shown.
     * </p>
     *
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li>Conditional prompts based on user choices</li>
     *   <li>Multi-step wizards with branching logic</li>
     *   <li>Progressive disclosure of options</li>
     *   <li>Validation-dependent follow-up questions</li>
     * </ul>
     *
     * <h4>Example:</h4>
     * <pre>{@code
     * Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(
     *     header,
     *     previousResults -> {
     *         if (previousResults.isEmpty()) {
     *             // First call - show initial prompts
     *             return Arrays.asList(
     *                 builder.createConfirmPrompt()
     *                     .name("advanced")
     *                     .message("Show advanced options?")
     *                     .addPrompt()
     *             );
     *         } else if (previousResults.containsKey("advanced")) {
     *             ConfirmResult advanced = (ConfirmResult) previousResults.get("advanced");
     *             if (advanced.isConfirmed()) {
     *                 // Show advanced prompts
     *                 return Arrays.asList(
     *                     builder.createInputPrompt()
     *                         .name("config")
     *                         .message("Enter configuration:")
     *                         .addPrompt()
     *                 );
     *             }
     *         }
     *         return null; // No more prompts
     *     }
     * );
     * }</pre>
     *
     * @param header header information to display before the first set of prompts (may be empty)
     * @param promptsProvider a function that receives previous results and returns the next list of prompts,
     *                       or {@code null} to indicate no more prompts should be shown
     * @return a map containing results for all executed prompts, keyed by prompt name
     * @throws IOException if an I/O error occurs during prompt execution
     * @see PromptResult
     * @see PromptBuilder
     */
    Map<String, ? extends PromptResult<? extends Prompt>> prompt(
            List<AttributedString> header,
            Function<Map<String, ? extends PromptResult<? extends Prompt>>, List<? extends Prompt>> promptsProvider)
            throws IOException;

    /**
     * Get the terminal associated with this Prompter.
     *
     * @return the terminal
     */
    Terminal getTerminal();

    /**
     * Get the line reader associated with this Prompter, if any.
     *
     * @return the line reader, or null if none is associated
     */
    LineReader getLineReader();
}

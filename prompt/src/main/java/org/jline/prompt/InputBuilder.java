/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import java.util.function.Function;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;

/**
 * Builder for creating text input prompts with advanced features.
 *
 * <p>
 * {@code InputBuilder} creates prompts that allow users to enter free-form text input.
 * It supports various advanced features including input masking (for passwords),
 * auto-completion, validation, and custom line readers.
 * </p>
 *
 * <h3>Features</h3>
 * <ul>
 *   <li><strong>Text Input</strong> - Free-form text entry with full editing capabilities</li>
 *   <li><strong>Input Masking</strong> - Hide sensitive input like passwords</li>
 *   <li><strong>Auto-completion</strong> - Provide completion suggestions as user types</li>
 *   <li><strong>Validation</strong> - Validate input before accepting</li>
 *   <li><strong>Default Values</strong> - Pre-populate input with default text</li>
 *   <li><strong>Custom Line Readers</strong> - Use specialized line readers for advanced scenarios</li>
 * </ul>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * builder.createInputPrompt()
 *     .name("username")
 *     .message("Enter your username:")
 *     .defaultValue("admin")
 *     .addPrompt();
 * }</pre>
 *
 * <h3>Password Input</h3>
 * <pre>{@code
 * builder.createInputPrompt()
 *     .name("password")
 *     .message("Enter your password:")
 *     .mask('*')  // Hide input with asterisks
 *     .addPrompt();
 * }</pre>
 *
 * <h3>Input with Validation</h3>
 * <pre>{@code
 * builder.createInputPrompt()
 *     .name("email")
 *     .message("Enter your email address:")
 *     .validator(input -> input.contains("@") && input.contains("."))
 *     .addPrompt();
 * }</pre>
 *
 * <h3>Input with Completion</h3>
 * <pre>{@code
 * Completer fileCompleter = new FileNameCompleter();
 *
 * builder.createInputPrompt()
 *     .name("filepath")
 *     .message("Enter file path:")
 *     .completer(fileCompleter)
 *     .addPrompt();
 * }</pre>
 *
 * @see BaseBuilder
 * @see InputResult
 * @see org.jline.reader.Completer
 * @since 3.30.0
 */
public interface InputBuilder extends BaseBuilder<InputBuilder> {

    /**
     * Set the default value that will be pre-filled in the input field.
     *
     * <p>
     * The default value appears in the input field when the prompt is displayed,
     * allowing users to accept it by pressing Enter or modify it as needed.
     * This is useful for providing sensible defaults or previously entered values.
     * </p>
     *
     * <h4>Example:</h4>
     * <pre>{@code
     * builder.createInputPrompt()
     *     .name("port")
     *     .message("Enter port number:")
     *     .defaultValue("8080")  // User can accept or change this
     *     .addPrompt();
     * }</pre>
     *
     * @param defaultValue the default text to pre-fill (may be null for no default)
     * @return this builder instance for method chaining
     */
    InputBuilder defaultValue(String defaultValue);

    /**
     * Set the character used to mask input for sensitive data.
     *
     * <p>
     * When a mask character is set, the actual input characters are hidden and
     * replaced with the mask character in the display. This is essential for
     * password input and other sensitive data entry.
     * </p>
     *
     * <h4>Common Mask Characters:</h4>
     * <ul>
     *   <li>{@code '*'} - Traditional asterisk masking</li>
     *   <li>{@code '•'} - Bullet point (modern style)</li>
     *   <li>{@code '●'} - Filled circle</li>
     *   <li>{@code ' '} - Space (completely hidden)</li>
     * </ul>
     *
     * <h4>Example:</h4>
     * <pre>{@code
     * builder.createInputPrompt()
     *     .name("password")
     *     .message("Enter password:")
     *     .mask('*')  // Input appears as: ****
     *     .addPrompt();
     * }</pre>
     *
     * @param mask the character to display instead of actual input, or {@code null} to disable masking
     * @return this builder instance for method chaining
     */
    InputBuilder mask(Character mask);

    /**
     * Set the completer to provide auto-completion suggestions.
     *
     * <p>
     * The completer is invoked as the user types to provide completion suggestions.
     * Users can typically press Tab to trigger completion or navigate through suggestions.
     * JLine provides several built-in completers for common use cases.
     * </p>
     *
     * <h4>Built-in Completers:</h4>
     * <ul>
     *   <li>{@code FileNameCompleter} - File and directory completion</li>
     *   <li>{@code StringsCompleter} - Completion from a predefined list</li>
     *   <li>{@code ArgumentCompleter} - Multi-argument completion</li>
     *   <li>{@code AggregateCompleter} - Combines multiple completers</li>
     * </ul>
     *
     * <h4>Example:</h4>
     * <pre>{@code
     * Completer completer = new StringsCompleter("option1", "option2", "option3");
     *
     * builder.createInputPrompt()
     *     .name("choice")
     *     .message("Enter option:")
     *     .completer(completer)
     *     .addPrompt();
     * }</pre>
     *
     * @param completer the completer to use for auto-completion (may be null for no completion)
     * @return this builder instance for method chaining
     * @see org.jline.reader.impl.completer.FileNameCompleter
     * @see org.jline.reader.impl.completer.StringsCompleter
     */
    InputBuilder completer(Completer completer);

    /**
     * Set a custom line reader for advanced input handling.
     *
     * <p>
     * By default, the prompter uses its own line reader configuration. This method
     * allows you to provide a custom line reader with specific settings, key bindings,
     * or behaviors that differ from the default configuration.
     * </p>
     *
     * <h4>Use Cases:</h4>
     * <ul>
     *   <li>Custom key bindings for specific input scenarios</li>
     *   <li>Specialized editing modes (vi vs emacs)</li>
     *   <li>Custom history management</li>
     *   <li>Integration with existing line reader configurations</li>
     * </ul>
     *
     * <h4>Example:</h4>
     * <pre>{@code
     * LineReader customReader = LineReaderBuilder.builder()
     *     .terminal(terminal)
     *     .option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
     *     .build();
     *
     * builder.createInputPrompt()
     *     .name("command")
     *     .message("Enter command:")
     *     .lineReader(customReader)
     *     .addPrompt();
     * }</pre>
     *
     * @param lineReader the custom line reader to use (may be null to use default)
     * @return this builder instance for method chaining
     * @see org.jline.reader.LineReaderBuilder
     */
    InputBuilder lineReader(LineReader lineReader);

    /**
     * Set a validator function to validate user input before accepting it.
     *
     * <p>
     * The validator function is called with the user's input and should return
     * {@code true} if the input is valid, {@code false} otherwise. If validation
     * fails, the user is prompted to enter the input again.
     * </p>
     *
     * <h4>Validation Examples:</h4>
     * <pre>{@code
     * // Email validation
     * builder.createInputPrompt()
     *     .name("email")
     *     .message("Enter email:")
     *     .validator(input -> input.matches("^[^@]+@[^@]+\\.[^@]+$"))
     *     .addPrompt();
     *
     * // Number validation
     * builder.createInputPrompt()
     *     .name("port")
     *     .message("Enter port (1-65535):")
     *     .validator(input -> {
     *         try {
     *             int port = Integer.parseInt(input);
     *             return port >= 1 && port <= 65535;
     *         } catch (NumberFormatException e) {
     *             return false;
     *         }
     *     })
     *     .addPrompt();
     *
     * // Non-empty validation
     * builder.createInputPrompt()
     *     .name("name")
     *     .message("Enter your name:")
     *     .validator(input -> !input.trim().isEmpty())
     *     .addPrompt();
     * }</pre>
     *
     * @param validator function that returns {@code true} for valid input, {@code false} otherwise
     * @return this builder instance for method chaining
     */
    InputBuilder validator(Function<String, Boolean> validator);
}

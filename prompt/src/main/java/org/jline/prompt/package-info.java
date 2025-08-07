/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Prompt API - Interactive Console Prompts for Java Applications.
 *
 * <h2>Overview</h2>
 * <p>
 * The JLine Prompt API provides a modern, type-safe interface for creating interactive console prompts.
 * Inspired by Inquirer.js, it offers a fluent builder pattern for constructing various types of user input prompts
 * with full support for keyboard navigation, validation, and customization.
 * </p>
 *
 * <h2>Supported Prompt Types</h2>
 * <p>
 * The API supports the following prompt types:
 * </p>
 * <ul>
 *   <li><strong>Input Prompts</strong> - Text input with optional masking, completion, and validation</li>
 *   <li><strong>List Prompts</strong> - Single selection from a list of options with keyboard navigation</li>
 *   <li><strong>Checkbox Prompts</strong> - Multiple selection from a list with checkboxes</li>
 *   <li><strong>Choice Prompts</strong> - Single selection with keyboard shortcuts and help text</li>
 *   <li><strong>Confirmation Prompts</strong> - Yes/No confirmation dialogs</li>
 *   <li><strong>Text Displays</strong> - Static text display without user input</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Type Safety</strong> - Parameterized interfaces ensure compile-time type checking</li>
 *   <li><strong>Fluent API</strong> - Builder pattern for intuitive prompt construction</li>
 *   <li><strong>Cross-Platform</strong> - Automatic platform-specific UI configuration</li>
 *   <li><strong>Extensible</strong> - Interface-based design allows custom implementations</li>
 *   <li><strong>Immutable</strong> - Thread-safe prompt and result objects</li>
 * </ul>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Create a Prompter instance
 * Terminal terminal = TerminalBuilder.builder().build();
 * Prompter prompter = PrompterFactory.create(terminal);
 *
 * // Build prompts using the fluent API
 * PromptBuilder builder = prompter.newBuilder();
 *
 * builder.createListPrompt()
 *     .name("color")
 *     .message("What is your favorite color?")
 *     .newItem("red").text("Red").add()
 *     .newItem("green").text("Green").add()
 *     .newItem("blue").text("Blue").add()
 *     .addPrompt();
 *
 * // Execute prompts and get results
 * Map<String, ? extends PromptResult<? extends Prompt>> results =
 *     prompter.prompt(Collections.emptyList(), builder.build());
 *
 * // Access typed results
 * ListResult colorChoice = (ListResult) results.get("color");
 * System.out.println("Selected: " + colorChoice.getSelectedId());
 * }</pre>
 *
 * <h2>Advanced Usage</h2>
 * <h3>Dynamic Prompts</h3>
 * <pre>{@code
 * // Create prompts based on previous answers
 * Map<String, ? extends PromptResult<? extends Prompt>> results = prompter.prompt(
 *     Collections.emptyList(),
 *     previousResults -> {
 *         if (previousResults.containsKey("advanced")) {
 *             ConfirmResult advanced = (ConfirmResult) previousResults.get("advanced");
 *             if (advanced.isConfirmed()) {
 *                 return Arrays.asList(
 *                     builder.createInputPrompt()
 *                         .name("details")
 *                         .message("Enter additional details:")
 *                         .addPrompt()
 *                 );
 *             }
 *         }
 *         return null; // No more prompts
 *     }
 * );
 * }</pre>
 *
 * <h3>Custom Configuration</h3>
 * <pre>{@code
 * // Create custom configuration
 * Prompter.Config config = new DefaultPrompter.DefaultConfig(
 *     "❯",     // indicator
 *     "◯ ",    // unchecked box
 *     "◉ ",    // checked box
 *     "◯ "     // unavailable item
 * );
 *
 * Prompter prompter = PrompterFactory.create(terminal, config);
 * }</pre>
 *
 * <h2>Architecture</h2>
 * <p>
 * The API is built around several key interfaces:
 * </p>
 * <ul>
 *   <li>{@link Prompter} - Main entry point for creating and executing prompts</li>
 *   <li>{@link PromptBuilder} - Factory for creating different types of prompt builders</li>
 *   <li>{@link Prompt} - Base interface for all prompt types</li>
 *   <li>{@link PromptResult} - Parameterized interface for type-safe results</li>
 *   <li>{@link PromptItem} - Interface for individual items within prompts</li>
 * </ul>
 *
 * <h2>Migration from jline-console-ui</h2>
 * <p>
 * This module replaces the deprecated {@code jline-console-ui} module. Key improvements include:
 * </p>
 * <ul>
 *   <li>Clean interface-based design with better separation of concerns</li>
 *   <li>Parameterized types for compile-time type safety</li>
 *   <li>Record-style accessor methods without 'get' prefixes</li>
 *   <li>Immutable prompt and result objects</li>
 *   <li>Comprehensive documentation and examples</li>
 * </ul>
 *
 * @see Prompter
 * @see PrompterFactory
 * @see PromptBuilder
 * @see PromptResult
 * @see Prompt
 * @since 3.30.0
 */
package org.jline.prompt;

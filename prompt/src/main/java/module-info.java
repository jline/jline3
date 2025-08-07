/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Prompt Module.
 * <p>
 * This module provides a modern, interface-based API for creating interactive console prompts.
 * It is inspired by Inquirer.js and provides a clean, type-safe way to build console applications
 * that need to collect user input through various prompt types.
 * <p>
 * The prompt module supports various prompt types including:
 * <ul>
 * <li>Text input with completion and validation</li>
 * <li>Password input with masking</li>
 * <li>Single and multiple selection lists</li>
 * <li>Checkbox prompts for multiple selections</li>
 * <li>Choice prompts with keyboard shortcuts</li>
 * <li>Confirmation prompts (Yes/No)</li>
 * <li>Number input with validation</li>
 * <li>Search prompts with filtering</li>
 * <li>Editor prompts for multi-line text</li>
 * </ul>
 * <p>
 * This module replaces the deprecated {@code jline-console-ui} module and provides
 * a more modern, interface-based API with better type safety and documentation.
 */
module org.jline.prompt {
    // Core Java platform
    requires java.base;
    requires java.logging;

    // JLine dependencies
    requires transitive org.jline.terminal;
    requires transitive org.jline.reader;
    requires transitive org.jline.builtins;

    // Export public API
    exports org.jline.prompt;

    // Export examples for documentation and learning
    exports org.jline.prompt.examples;
}

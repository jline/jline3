/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import org.jline.prompt.impl.DefaultPrompterConfig;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStyle;
import org.jline.utils.StyleResolver;

/**
 * Configuration interface for customizing the visual appearance and behavior of prompts.
 *
 * <p>
 * The configuration controls various visual elements used in the prompt display,
 * including indicators, checkbox symbols, and interaction behavior. The API provides
 * platform-specific defaults that work well on different operating systems.
 * </p>
 *
 * <h4>Default Configurations:</h4>
 * <ul>
 *   <li><strong>Windows:</strong> {@code ">"}, {@code "( )"}, {@code "(x)"}, {@code "( )"}</li>
 *   <li><strong>Unix/Linux/macOS:</strong> {@code "❯"}, {@code "◯ "}, {@code "◉ "}, {@code "◯ "}</li>
 * </ul>
 *
 * <h4>Example Custom Configuration:</h4>
 * <pre>{@code
 * Prompter.Config config = new DefaultPrompter.DefaultConfig(
 *     "→",     // indicator
 *     "☐ ",    // unchecked box
 *     "☑ ",    // checked box
 *     "☐ "     // unavailable item
 * );
 *
 * Prompter prompter = PrompterFactory.create(terminal, config);
 * }</pre>
 *
 * @see PrompterFactory#create(Terminal, PrompterConfig)
 * @since 3.30.0
 */
public interface PrompterConfig {

    /**
     * Get the indicator character/string.
     *
     * @return the indicator
     */
    String indicator();

    /**
     * Get the unchecked box character/string.
     *
     * @return the unchecked box
     */
    String uncheckedBox();

    /**
     * Get the checked box character/string.
     *
     * @return the checked box
     */
    String checkedBox();

    /**
     * Get the unavailable item character/string.
     *
     * @return the unavailable item
     */
    String unavailable();

    /**
     * Get the style resolver for this configuration.
     *
     * @return the style resolver, or null if not available
     */
    default StyleResolver styleResolver() {
        return null;
    }

    /**
     * Whether the first prompt can be cancelled.
     *
     * @return true if the first prompt can be cancelled
     */
    boolean cancellableFirstPrompt();

    default PrompterConfig withCancellableFirstPrompt(boolean cancellable) {
        return custom(indicator(), uncheckedBox(), checkedBox(), unavailable(), null, cancellable);
    }

    /**
     * Create a configuration with platform-specific defaults.
     *
     * @return a configuration with platform defaults
     */
    static PrompterConfig defaults() {
        return DefaultPrompterConfig.defaults();
    }

    /**
     * Create a configuration with Windows-specific defaults.
     *
     * @return a configuration with Windows defaults
     */
    static PrompterConfig windows() {
        return DefaultPrompterConfig.windows();
    }

    /**
     * Create a configuration with Unix/Linux-specific defaults.
     *
     * @return a configuration with Unix defaults
     */
    static PrompterConfig unix() {
        return DefaultPrompterConfig.unix();
    }

    /**
     * Create a custom configuration with style resolver support.
     *
     * @param indicator the indicator character/string
     * @param uncheckedBox the unchecked box character/string
     * @param checkedBox the checked box character/string
     * @param unavailable the unavailable item character/string
     * @param styleResolver the style resolver for applying styles
     * @return a custom configuration with styling support
     */
    static PrompterConfig custom(
            String indicator,
            String uncheckedBox,
            String checkedBox,
            String unavailable,
            StyleResolver styleResolver,
            boolean cancellableFirstPrompt) {
        return DefaultPrompterConfig.custom(
                indicator, uncheckedBox, checkedBox, unavailable, styleResolver, cancellableFirstPrompt);
    }

    // Style constants for use with StyleResolver
    /** Cursor/selection indicator style */
    String CURSOR = ".cu";
    /** Prompt text style */
    String PR = ".pr";
    /** Message text style */
    String ME = ".me";
    /** Answer text style */
    String AN = ".an";
    /** Box enabled style */
    String BE = ".be";
    /** Box disabled style */
    String BD = ".bd";
    /** Selected item style */
    String SE = ".se";
    /** Checkbox style */
    String CB = ".cb";
    /** Error message style */
    String ERROR = ".er";

    default AttributedStyle style(String style) {
        return styleResolver().resolve(style);
    }
}

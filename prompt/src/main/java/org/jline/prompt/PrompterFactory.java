/*
 * Copyright (c) 2024, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.prompt;

import org.jline.prompt.impl.DefaultPrompter;
import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

/**
 * Factory for creating Prompter instances.
 */
public final class PrompterFactory {

    private PrompterFactory() {
        // Prevent instantiation
    }

    /**
     * Create a new Prompter instance with the given terminal.
     *
     * @param terminal the terminal to use
     * @return a new Prompter instance
     */
    public static Prompter create(Terminal terminal) {
        return create(terminal, PrompterConfig.defaults());
    }

    /**
     * Create a new Prompter instance with the given terminal and configuration.
     *
     * @param terminal the terminal to use
     * @param config the configuration to use
     * @return a new Prompter instance
     */
    public static Prompter create(Terminal terminal, PrompterConfig config) {
        return new DefaultPrompter(terminal, config);
    }

    /**
     * Create a new Prompter instance with the given line reader, terminal, and configuration.
     *
     * @param reader the line reader to use
     * @param terminal the terminal to use
     * @param config the configuration to use
     * @return a new Prompter instance
     */
    public static Prompter create(LineReader reader, Terminal terminal, PrompterConfig config) {
        return new DefaultPrompter(reader, terminal, config);
    }
}

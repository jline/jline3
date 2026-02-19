/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import org.jline.console.ConsoleEngine;
import org.jline.shell.CommandSession;
import org.jline.shell.LineExpander;

/**
 * A {@link LineExpander} backed by a {@link ConsoleEngine}'s parameter expansion.
 * <p>
 * This expander supports full {@code ${expression}} syntax with Groovy evaluation
 * (or whatever script engine the ConsoleEngine is configured with), in addition
 * to simple {@code $variable} expansion.
 * <p>
 * It delegates to {@link ConsoleEngine#expandCommandLine(String)} for the
 * actual expansion logic, which supports script engine evaluation.
 * <p>
 * Example:
 * <pre>
 * ConsoleEngine engine = ...;
 * LineExpander expander = new ConsoleLineExpander(engine);
 * Shell.builder()
 *     .lineExpander(expander)
 *     .build();
 * </pre>
 *
 * @since 4.0
 */
public class ConsoleLineExpander implements LineExpander {

    private final ConsoleEngine engine;

    /**
     * Creates a new ConsoleLineExpander backed by the given engine.
     *
     * @param engine the console engine for parameter expansion
     */
    public ConsoleLineExpander(ConsoleEngine engine) {
        this.engine = engine;
    }

    @Override
    public String expand(String line, CommandSession session) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        try {
            return engine.expandCommandLine(line);
        } catch (Exception e) {
            // If expansion fails, return the original line
            return line;
        }
    }
}

/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jline.shell.CommandDispatcher;
import org.jline.shell.CommandSession;
import org.jline.shell.ScriptRunner;

/**
 * Default implementation of {@link ScriptRunner} that reads a script file
 * line by line and executes each line through the dispatcher.
 * <p>
 * Features:
 * <ul>
 *   <li>Skips blank lines and lines starting with {@code #} (comments)</li>
 *   <li>Supports {@code \} line continuation (backslash at end of line
 *       joins with the next line)</li>
 *   <li>Each complete line is fed to {@link CommandDispatcher#execute(String)}</li>
 * </ul>
 *
 * @see ScriptRunner
 * @since 4.0
 */
public class DefaultScriptRunner implements ScriptRunner {

    /**
     * Creates a new DefaultScriptRunner.
     */
    public DefaultScriptRunner() {}

    @Override
    public void execute(Path script, CommandSession session, CommandDispatcher dispatcher) throws Exception {
        if (script == null || !Files.exists(script)) {
            throw new IllegalArgumentException("Script file not found: " + script);
        }

        List<String> lines = Files.readAllLines(script);
        StringBuilder pending = new StringBuilder();

        for (String line : lines) {
            // Handle line continuation
            if (pending.length() > 0) {
                pending.append(line);
            } else {
                // Skip comments and blank lines
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                pending.append(line);
            }

            // Check for line continuation
            if (pending.length() > 0 && pending.charAt(pending.length() - 1) == '\\') {
                pending.setLength(pending.length() - 1); // Remove trailing backslash
                continue;
            }

            // Execute the complete line
            String completeLine = pending.toString().trim();
            pending.setLength(0);

            if (!completeLine.isEmpty()) {
                dispatcher.execute(completeLine);
            }
        }

        // Execute any remaining pending line (unterminated continuation)
        if (pending.length() > 0) {
            String remaining = pending.toString().trim();
            if (!remaining.isEmpty()) {
                dispatcher.execute(remaining);
            }
        }
    }
}

/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.nio.file.Path;

/**
 * Executes a script file line by line through a {@link CommandDispatcher}.
 * <p>
 * A {@code ScriptRunner} reads lines from a script file and feeds them
 * to the dispatcher for execution. The exact behavior (comment handling,
 * line continuation, error handling) is determined by the implementation.
 * <p>
 * Example:
 * <pre>
 * ScriptRunner runner = (script, session, dispatcher) -&gt; {
 *     Files.readAllLines(script).forEach(line -&gt; dispatcher.execute(line));
 * };
 * </pre>
 *
 * @see org.jline.shell.impl.DefaultScriptRunner
 * @see ShellBuilder#scriptRunner(ScriptRunner)
 * @since 4.0
 */
@FunctionalInterface
public interface ScriptRunner {

    /**
     * Executes a script file.
     *
     * @param script the path to the script file
     * @param session the current command session
     * @param dispatcher the command dispatcher to execute lines through
     * @throws Exception if script execution fails
     */
    void execute(Path script, CommandSession session, CommandDispatcher dispatcher) throws Exception;
}

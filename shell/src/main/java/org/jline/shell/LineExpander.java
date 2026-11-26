/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

/**
 * Expands variables and other expressions in a command line.
 * <p>
 * A {@code LineExpander} operates on a command line string (not per-word
 * like {@link org.jline.reader.Expander}). It is called after alias expansion
 * and after pipeline parsing, once for the command line of each pipeline stage
 * and once for each redirection target, so that an expanded value cannot
 * introduce pipeline operators or redirections.
 * <p>
 * The exact expansion syntax is controlled by the implementation. Users can
 * provide their own {@code LineExpander} to support custom variable syntax,
 * glob expansion, or other transformations.
 * <p>
 * Example:
 * <pre>
 * LineExpander expander = (line, session) -&gt; line.replace("$HOME", "/home/user");
 * </pre>
 *
 * @see org.jline.shell.impl.DefaultLineExpander
 * @see ShellBuilder#lineExpander(LineExpander)
 * @since 4.0
 */
@FunctionalInterface
public interface LineExpander {

    /**
     * Expands variables and expressions in the given command line.
     *
     * @param line the command line to expand
     * @param session the current command session (for variable lookup)
     * @return the expanded command line
     */
    String expand(String line, CommandSession session);
}

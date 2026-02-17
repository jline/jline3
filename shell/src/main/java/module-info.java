/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Shell Module.
 * <p>
 * This module provides the shell API for building interactive command-line
 * applications with JLine. It defines the core abstractions for commands, command
 * groups, command dispatching, pipelines, and job management.
 * <p>
 * The shell module sits between {@code jline-reader} and {@code jline-console}
 * in the module dependency graph:
 * <pre>
 * jline-terminal
 *   ^
 * jline-reader
 *   ^
 * jline-shell  (this module)
 *   ^
 * jline-console
 * </pre>
 *
 * <h2>Public API Packages</h2>
 * <ul>
 * <li>{@code org.jline.shell} - Core shell interfaces: {@link org.jline.shell.Command},
 *     {@link org.jline.shell.CommandGroup}, {@link org.jline.shell.CommandDispatcher},
 *     {@link org.jline.shell.Shell}</li>
 * <li>{@code org.jline.shell.impl} - Default implementations including
 *     {@link org.jline.shell.impl.DefaultCommandDispatcher} and
 *     {@link org.jline.shell.impl.AbstractCommand}</li>
 * <li>{@code org.jline.shell.widget} - Interactive widgets including
 *     {@link org.jline.shell.widget.CommandTailTipWidgets}</li>
 * </ul>
 *
 * @since 4.0
 */
module org.jline.shell {
    requires java.base;
    requires transitive org.jline.terminal;
    requires transitive org.jline.reader;

    exports org.jline.shell;
    exports org.jline.shell.impl;
    exports org.jline.shell.widget;
}

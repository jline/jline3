/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * Clean shell API for building interactive command-line applications with JLine.
 * <p>
 * This package provides the core abstractions for commands, command groups,
 * command dispatching, pipelines, and job management.
 *
 * <h2>Architecture</h2>
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
 * <h2>Core Types</h2>
 * <ul>
 * <li>{@link org.jline.shell.Command} - a single executable command</li>
 * <li>{@link org.jline.shell.CommandGroup} - a named group of commands</li>
 * <li>{@link org.jline.shell.CommandDispatcher} - resolves and executes commands</li>
 * <li>{@link org.jline.shell.CommandSession} - execution context (terminal, I/O, variables)</li>
 * <li>{@link org.jline.shell.Shell} - the REPL loop</li>
 * <li>{@link org.jline.shell.Pipeline} - parsed pipeline of commands</li>
 * <li>{@link org.jline.shell.Job} - job control</li>
 * </ul>
 *
 * <h2>Quick Start</h2>
 * <pre>
 * Shell shell = Shell.builder()
 *     .prompt("myapp&gt; ")
 *     .groups(myGroup)
 *     .build();
 * shell.run();
 * </pre>
 *
 * @since 4.0
 */
package org.jline.shell;

/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * JLine Console package provides a framework for building interactive command-line applications.
 * <p>
 * This package contains classes and interfaces for:
 * <ul>
 *   <li>Command registration and execution</li>
 *   <li>Command argument parsing and description</li>
 *   <li>Command output formatting and printing</li>
 *   <li>Script execution and variable management</li>
 *   <li>System registry for command execution</li>
 * </ul>
 * <p>
 * Key components include:
 * <ul>
 *   <li>{@link org.jline.console.CommandRegistry} - Interface for registering and executing commands</li>
 *   <li>{@link org.jline.console.ConsoleEngine} - Interface for managing console variables, commands, and script execution</li>
 *   <li>{@link org.jline.console.SystemRegistry} - Interface for executing commands and managing the console environment</li>
 *   <li>{@link org.jline.console.Printer} - Interface for printing objects to the console with various formatting options</li>
 *   <li>{@link org.jline.console.CmdDesc} - Class for describing commands and their arguments</li>
 *   <li>{@link org.jline.console.ArgDesc} - Class for describing command arguments</li>
 *   <li>{@link org.jline.console.CmdLine} - Class for representing a parsed command line</li>
 *   <li>{@link org.jline.console.CommandInput} - Class for encapsulating command input and output streams</li>
 * </ul>
 * <p>
 * The console package is designed to be used with the JLine reader package to create
 * interactive command-line applications with features like command completion,
 * command history, and command help.
 */
package org.jline.console;

/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */

/**
 * Enhanced Picocli integration for JLine console applications.
 * <p>
 * This package provides seamless integration between Picocli's command-line parsing
 * framework and JLine's interactive terminal features. It builds upon the foundation
 * of {@code picocli-shell-jline3} but provides a cleaner, more integrated API that
 * works naturally with JLine's console system.
 * <p>
 * Key features:
 * <ul>
 *   <li><strong>Clean Context API</strong> - Framework-agnostic {@link org.jline.console.CommandContext} 
 *       that provides access to terminal, I/O streams, and environment without framework-specific dependencies</li>
 *   <li><strong>Automatic Context Injection</strong> - Commands can receive context through constructor injection,
 *       field injection, or method parameter injection</li>
 *   <li><strong>Rich Completion</strong> - Leverages Picocli's built-in completion capabilities with JLine's
 *       interactive features</li>
 *   <li><strong>Smart Help Generation</strong> - Automatically generates rich help and command descriptions
 *       from Picocli annotations</li>
 *   <li><strong>Widget Integration</strong> - Works seamlessly with JLine widgets like TailTip and AutoSuggestion</li>
 * </ul>
 * <p>
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * // Create command context
 * CommandContext context = CommandContext.builder()
 *     .terminal(terminal)
 *     .currentDir(Paths.get("."))
 *     .build();
 * 
 * // Create registry and register commands
 * PicocliCommandRegistry registry = new PicocliCommandRegistry(context)
 *     .register(new MyCommand())
 *     .register(AnotherCommand.class);
 * 
 * // Use with SystemRegistry
 * SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, configPath);
 * systemRegistry.setCommandRegistries(registry);
 * }</pre>
 * <p>
 * <h3>Context Injection Patterns</h3>
 * Commands can receive the CommandContext in several ways:
 * <p>
 * <strong>Constructor Injection:</strong>
 * <pre>{@code
 * @Command(name = "mycommand")
 * public class MyCommand implements Callable<Integer> {
 *     private final CommandContext context;
 *     
 *     public MyCommand(CommandContext context) {
 *         this.context = context;
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>Field Injection:</strong>
 * <pre>{@code
 * @Command(name = "mycommand")
 * public class MyCommand implements Callable<Integer> {
 *     private CommandContext context; // Automatically injected
 * }
 * }</pre>
 * <p>
 * <strong>Method Parameter Injection:</strong>
 * <pre>{@code
 * @Command(name = "mycommand")
 * public class MyCommand implements Callable<Integer> {
 *     public Integer call(CommandContext context) {
 *         // Use context
 *         return 0;
 *     }
 * }
 * }</pre>
 * <p>
 * <h3>Integration with Existing Systems</h3>
 * The CommandContext is designed to be compatible with existing command frameworks:
 * <ul>
 *   <li>Can be converted to/from {@link org.jline.builtins.PosixCommands.Context}</li>
 *   <li>Works alongside other CommandRegistry implementations</li>
 *   <li>Integrates with JLine's widget system</li>
 * </ul>
 * <p>
 * This design provides a clean separation between command logic and framework-specific
 * concerns, making it easy to write portable commands that work well in interactive
 * terminal environments.
 *
 * @see org.jline.console.CommandContext
 * @see org.jline.console.picocli.PicocliCommandRegistry
 * @see org.jline.console.SystemRegistry
 */
package org.jline.console.picocli;

/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jline.terminal.Terminal;

/**
 * Registry for POSIX commands that provides a convenient way to register and execute
 * POSIX commands in JLine applications.
 * <p>
 * This class acts as a bridge between command frameworks and the PosixCommands implementations,
 * making it easy to integrate POSIX commands into any command-line application.
 * </p>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * Terminal terminal = TerminalBuilder.builder().build();
 * Path currentDir = Paths.get(".");
 * 
 * PosixCommandsRegistry registry = new PosixCommandsRegistry(
 *     terminal.input(), 
 *     new PrintStream(terminal.output()),
 *     new PrintStream(terminal.output()),
 *     currentDir,
 *     terminal
 * );
 * 
 * // Execute a command
 * registry.execute("ls", new String[]{"ls", "-l"});
 * }</pre>
 */
public class PosixCommandsRegistry {
    
    private final PosixCommands.Context context;
    private final Map<String, BiConsumer<PosixCommands.Context, String[]>> commands;
    
    /**
     * Create a new POSIX commands registry.
     * 
     * @param in input stream for commands
     * @param out output stream for commands
     * @param err error stream for commands
     * @param currentDir current working directory
     * @param terminal terminal instance (can be null for non-interactive use)
     */
    public PosixCommandsRegistry(InputStream in, PrintStream out, PrintStream err, 
                                Path currentDir, Terminal terminal) {
        this.context = new PosixCommands.Context(in, out, err, currentDir, terminal);
        this.commands = new HashMap<>();
        registerDefaultCommands();
    }
    
    /**
     * Create a new POSIX commands registry with a pre-built context.
     * 
     * @param context the execution context
     */
    public PosixCommandsRegistry(PosixCommands.Context context) {
        this.context = context;
        this.commands = new HashMap<>();
        registerDefaultCommands();
    }
    
    /**
     * Register all default POSIX commands.
     */
    private void registerDefaultCommands() {
        register("cat", PosixCommands::cat);
        register("echo", PosixCommands::echo);
        register("grep", PosixCommands::grep);
        register("ls", PosixCommands::ls);
        register("pwd", PosixCommands::pwd);
        register("head", PosixCommands::head);
        register("tail", PosixCommands::tail);
        register("wc", PosixCommands::wc);
        register("date", PosixCommands::date);
        register("sleep", PosixCommands::sleep);
        register("sort", PosixCommands::sort);
        register("clear", PosixCommands::clear);
    }
    
    /**
     * Register a command with the registry.
     * 
     * @param name command name
     * @param command command implementation
     */
    public void register(String name, BiConsumer<PosixCommands.Context, String[]> command) {
        commands.put(name, command);
    }
    
    /**
     * Unregister a command from the registry.
     * 
     * @param name command name to unregister
     */
    public void unregister(String name) {
        commands.remove(name);
    }
    
    /**
     * Check if a command is registered.
     * 
     * @param name command name
     * @return true if the command is registered
     */
    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }
    
    /**
     * Get all registered command names.
     * 
     * @return array of command names
     */
    public String[] getCommandNames() {
        return commands.keySet().toArray(new String[0]);
    }
    
    /**
     * Execute a command.
     * 
     * @param name command name
     * @param argv command arguments (including command name as argv[0])
     * @throws Exception if command execution fails
     * @throws IllegalArgumentException if command is not registered
     */
    public void execute(String name, String[] argv) throws Exception {
        BiConsumer<PosixCommands.Context, String[]> command = commands.get(name);
        if (command == null) {
            throw new IllegalArgumentException("Unknown command: " + name);
        }
        command.accept(context, argv);
    }
    
    /**
     * Execute a command with a command line string.
     * This is a convenience method that splits the command line and calls execute.
     * 
     * @param commandLine command line string (e.g., "ls -l /tmp")
     * @throws Exception if command execution fails
     */
    public void execute(String commandLine) throws Exception {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return;
        }
        
        String[] parts = commandLine.trim().split("\\s+");
        execute(parts[0], parts);
    }
    
    /**
     * Get the execution context.
     * 
     * @return the execution context
     */
    public PosixCommands.Context getContext() {
        return context;
    }
    
    /**
     * Create a new registry with a different current directory.
     * This is useful for commands that need to operate in different directories.
     * 
     * @param newCurrentDir the new current directory
     * @return a new registry with the updated directory
     */
    public PosixCommandsRegistry withCurrentDirectory(Path newCurrentDir) {
        PosixCommands.Context newContext = new PosixCommands.Context(
            context.in(), 
            context.out(), 
            context.err(), 
            newCurrentDir, 
            context.terminal()
        );
        return new PosixCommandsRegistry(newContext);
    }
    
    /**
     * Print help for all available commands.
     */
    public void printHelp() {
        context.out().println("Available POSIX commands:");
        String[] names = getCommandNames();
        java.util.Arrays.sort(names);
        for (String name : names) {
            context.out().println("  " + name);
        }
        context.out().println();
        context.out().println("Use '<command> --help' for detailed help on each command.");
    }
    
    /**
     * Print help for a specific command.
     * 
     * @param commandName the command to get help for
     * @throws Exception if getting help fails
     */
    public void printHelp(String commandName) throws Exception {
        if (!hasCommand(commandName)) {
            context.err().println("Unknown command: " + commandName);
            return;
        }
        execute(commandName, new String[]{commandName, "--help"});
    }
}

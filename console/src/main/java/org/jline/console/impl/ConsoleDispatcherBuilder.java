/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

import org.jline.console.*;
import org.jline.reader.Completer;
import org.jline.reader.Parser;
import org.jline.shell.*;
import org.jline.shell.Pipeline;
import org.jline.terminal.Terminal;

/**
 * Factory that creates a {@link CommandDispatcher} backed by the legacy console
 * infrastructure ({@link SystemRegistryImpl}, {@link Builtins}, and optionally
 * {@link ConsoleEngineImpl}).
 * <p>
 * This builder replaces the complex wiring that was previously in the old Shell class.
 * It creates a dispatcher that uses the old console API internally while presenting
 * the new {@link CommandDispatcher} interface.
 * <p>
 * Example:
 * <pre>
 * CommandDispatcher dispatcher = ConsoleDispatcherBuilder.builder()
 *     .terminal(terminal)
 *     .parser(parser)
 *     .builtins(builtins)
 *     .commands(myRegistry)
 *     .build();
 *
 * Shell shell = Shell.builder()
 *     .dispatcher(dispatcher)
 *     .build();
 * shell.run();
 * </pre>
 *
 * @see Shell
 * @since 4.0
 */
public class ConsoleDispatcherBuilder {

    private Terminal terminal;
    private Parser parser;
    private Builtins builtins;
    private final List<CommandRegistry> commandRegistries = new ArrayList<>();
    private final List<CommandGroup> commandGroups = new ArrayList<>();
    private Supplier<Path> workDir;
    private ConsoleEngine consoleEngine;
    private ScriptEngine scriptEngine;
    private File initScript;

    private ConsoleDispatcherBuilder() {}

    /**
     * Creates a new builder.
     *
     * @return a new builder
     */
    public static ConsoleDispatcherBuilder builder() {
        return new ConsoleDispatcherBuilder();
    }

    /**
     * Sets the terminal.
     *
     * @param terminal the terminal
     * @return this builder
     */
    public ConsoleDispatcherBuilder terminal(Terminal terminal) {
        this.terminal = terminal;
        return this;
    }

    /**
     * Sets the parser.
     *
     * @param parser the parser
     * @return this builder
     */
    public ConsoleDispatcherBuilder parser(Parser parser) {
        this.parser = parser;
        return this;
    }

    /**
     * Sets the builtins registry.
     *
     * @param builtins the builtins
     * @return this builder
     */
    public ConsoleDispatcherBuilder builtins(Builtins builtins) {
        this.builtins = builtins;
        return this;
    }

    /**
     * Adds old-style command registries.
     *
     * @param registries the command registries
     * @return this builder
     */
    public ConsoleDispatcherBuilder commands(CommandRegistry... registries) {
        Collections.addAll(this.commandRegistries, registries);
        return this;
    }

    /**
     * Adds new-style command groups (they will be adapted to CommandRegistry).
     *
     * @param groups the command groups
     * @return this builder
     */
    public ConsoleDispatcherBuilder groups(CommandGroup... groups) {
        Collections.addAll(this.commandGroups, groups);
        return this;
    }

    /**
     * Sets the working directory supplier.
     *
     * @param workDir the working directory supplier
     * @return this builder
     */
    public ConsoleDispatcherBuilder workDir(Supplier<Path> workDir) {
        this.workDir = workDir;
        return this;
    }

    /**
     * Sets the console engine for scripting support.
     *
     * @param consoleEngine the console engine
     * @return this builder
     */
    public ConsoleDispatcherBuilder consoleEngine(ConsoleEngine consoleEngine) {
        this.consoleEngine = consoleEngine;
        return this;
    }

    /**
     * Sets the script engine.
     *
     * @param scriptEngine the script engine
     * @return this builder
     */
    public ConsoleDispatcherBuilder scriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
        return this;
    }

    /**
     * Sets the initialization script.
     *
     * @param initScript the init script
     * @return this builder
     */
    public ConsoleDispatcherBuilder initScript(File initScript) {
        this.initScript = initScript;
        return this;
    }

    /**
     * Builds the dispatcher.
     * <p>
     * Creates a {@link SystemRegistryImpl} backed dispatcher that wraps
     * the old console API with the new {@link CommandDispatcher} interface.
     *
     * @return the command dispatcher
     */
    public CommandDispatcher build() {
        Objects.requireNonNull(terminal, "terminal is required");

        // Adapt new-style groups to old CommandRegistry
        List<CommandRegistry> allRegistries = new ArrayList<>();
        if (builtins != null) {
            allRegistries.add(builtins);
        }
        allRegistries.addAll(commandRegistries);
        for (CommandGroup group : commandGroups) {
            allRegistries.add(new CommandRegistryAdapter(group));
        }

        Supplier<Path> wd = workDir != null ? workDir : () -> Path.of(System.getProperty("user.dir"));

        SystemRegistryImpl systemRegistry = new SystemRegistryImpl(parser, terminal, wd, null);
        systemRegistry.setCommandRegistries(allRegistries.toArray(new CommandRegistry[0]));

        return new SystemRegistryDispatcher(systemRegistry, terminal, initScript);
    }

    /**
     * A {@link CommandDispatcher} that delegates to a {@link SystemRegistry}.
     */
    private static class SystemRegistryDispatcher implements CommandDispatcher {

        private final SystemRegistry systemRegistry;
        private final Terminal terminal;
        private final File initScript;
        private final List<CommandGroup> groups = new ArrayList<>();

        SystemRegistryDispatcher(SystemRegistry systemRegistry, Terminal terminal, File initScript) {
            this.systemRegistry = systemRegistry;
            this.terminal = terminal;
            this.initScript = initScript;
        }

        @Override
        public void addGroup(CommandGroup group) {
            groups.add(group);
            // Also register with system registry via adapter
            systemRegistry.setCommandRegistries(new CommandRegistryAdapter(group));
        }

        @Override
        public List<CommandGroup> groups() {
            return Collections.unmodifiableList(groups);
        }

        @Override
        public Command findCommand(String name) {
            for (CommandGroup group : groups) {
                Command cmd = group.command(name);
                if (cmd != null) {
                    return cmd;
                }
            }
            return null;
        }

        @Override
        public Object execute(String line) throws Exception {
            return systemRegistry.execute(line);
        }

        @Override
        public Object execute(Pipeline pipeline) throws Exception {
            // Reconstruct command line from pipeline and execute via system registry
            return systemRegistry.execute(pipeline.source());
        }

        @Override
        public Completer completer() {
            return systemRegistry.completer();
        }

        @Override
        public CommandDescription describe(org.jline.shell.CommandLine commandLine) {
            CmdLine cmdLine = DescriptionAdapter.toCmdLine(commandLine);
            CmdDesc desc = systemRegistry.commandDescription(cmdLine);
            return DescriptionAdapter.toCommandDescription(desc);
        }

        @Override
        public Terminal terminal() {
            return terminal;
        }

        @Override
        public void initialize(File script) {
            File s = script != null ? script : initScript;
            if (s != null) {
                systemRegistry.initialize(s);
            }
        }

        @Override
        public void cleanUp() {
            systemRegistry.cleanUp();
        }

        @Override
        public void trace(Throwable exception) {
            systemRegistry.trace(exception);
        }

        @Override
        public void close() {
            systemRegistry.close();
        }
    }
}

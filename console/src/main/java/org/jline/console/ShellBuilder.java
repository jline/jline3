/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader.Option;
import org.jline.reader.Parser;
import org.jline.terminal.Terminal;

/**
 * Fluent builder for creating {@link Shell} instances.
 * <p>
 * ShellBuilder eliminates the boilerplate typically required to set up a JLine REPL.
 * It provides a fluent API for configuring the terminal, parser, commands, highlighting,
 * and other options.
 * <p>
 * Example usage:
 * <pre>
 * Shell shell = Shell.builder()
 *     .terminal(terminal)
 *     .prompt("myapp&gt; ")
 *     .commands(registry1, registry2)
 *     .build();
 * shell.run();
 * </pre>
 *
 * @see Shell
 */
public class ShellBuilder {

    Terminal terminal;
    String prompt;
    Supplier<String> promptSupplier;
    final List<CommandRegistry> commandRegistries = new ArrayList<>();
    ScriptEngine scriptEngine;
    Highlighter highlighter;
    Parser parser;
    Path historyFile;
    File initScript;
    Supplier<Path> workDir;
    final Map<String, Object> variables = new LinkedHashMap<>();
    final Map<Option, Boolean> options = new LinkedHashMap<>();
    boolean tailTipWidgets = true;
    JobManager jobManager;

    ShellBuilder() {}

    /**
     * Sets the terminal to use. If not set, a terminal will be auto-created.
     *
     * @param terminal the terminal
     * @return this builder
     */
    public ShellBuilder terminal(Terminal terminal) {
        this.terminal = terminal;
        return this;
    }

    /**
     * Sets a static prompt string.
     *
     * @param prompt the prompt string
     * @return this builder
     */
    public ShellBuilder prompt(String prompt) {
        this.prompt = prompt;
        this.promptSupplier = null;
        return this;
    }

    /**
     * Sets a dynamic prompt supplier.
     *
     * @param promptSupplier supplier that provides the prompt string
     * @return this builder
     */
    public ShellBuilder prompt(Supplier<String> promptSupplier) {
        this.promptSupplier = promptSupplier;
        this.prompt = null;
        return this;
    }

    /**
     * Adds command registries.
     *
     * @param registries the command registries to add
     * @return this builder
     */
    public ShellBuilder commands(CommandRegistry... registries) {
        for (CommandRegistry r : registries) {
            commandRegistries.add(r);
        }
        return this;
    }

    /**
     * Sets the script engine for scripting support.
     * When a script engine is provided, a full {@code ConsoleEngineImpl} and
     * {@code SystemRegistryImpl} are created. Without a script engine,
     * a {@code SimpleSystemRegistryImpl} is used.
     *
     * @param scriptEngine the script engine
     * @return this builder
     */
    public ShellBuilder scriptEngine(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
        return this;
    }

    /**
     * Sets the highlighter for command-line syntax highlighting.
     *
     * @param highlighter the highlighter
     * @return this builder
     */
    public ShellBuilder highlighter(Highlighter highlighter) {
        this.highlighter = highlighter;
        return this;
    }

    /**
     * Sets the parser. If not set, a {@code DefaultParser} with bracket and quote
     * defaults is created.
     *
     * @param parser the parser
     * @return this builder
     */
    public ShellBuilder parser(Parser parser) {
        this.parser = parser;
        return this;
    }

    /**
     * Sets the history file path.
     *
     * @param historyFile the path to the history file
     * @return this builder
     */
    public ShellBuilder historyFile(Path historyFile) {
        this.historyFile = historyFile;
        return this;
    }

    /**
     * Sets the initialization script to execute on startup.
     *
     * @param initScript the init script file
     * @return this builder
     */
    public ShellBuilder initScript(File initScript) {
        this.initScript = initScript;
        return this;
    }

    /**
     * Sets the working directory supplier.
     *
     * @param workDir supplier providing the current working directory
     * @return this builder
     */
    public ShellBuilder workDir(Supplier<Path> workDir) {
        this.workDir = workDir;
        return this;
    }

    /**
     * Sets a LineReader variable.
     *
     * @param name the variable name
     * @param value the variable value
     * @return this builder
     */
    public ShellBuilder variable(String name, Object value) {
        this.variables.put(name, value);
        return this;
    }

    /**
     * Sets a LineReader option.
     *
     * @param option the option to set
     * @param value the value for the option
     * @return this builder
     */
    public ShellBuilder option(Option option, boolean value) {
        this.options.put(option, value);
        return this;
    }

    /**
     * Enables or disables TailTipWidgets. Enabled by default.
     *
     * @param enabled whether to enable TailTipWidgets
     * @return this builder
     */
    public ShellBuilder tailTipWidgets(boolean enabled) {
        this.tailTipWidgets = enabled;
        return this;
    }

    /**
     * Sets the job manager for job control support.
     *
     * @param jobManager the job manager
     * @return this builder
     */
    public ShellBuilder jobManager(JobManager jobManager) {
        this.jobManager = jobManager;
        return this;
    }

    /**
     * Builds a new {@link Shell} instance with the configured settings.
     *
     * @return a new Shell instance
     * @throws Exception if an error occurs during construction
     */
    public Shell build() throws Exception {
        return new Shell(this);
    }
}

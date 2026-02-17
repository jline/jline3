/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.reader.impl.DefaultParser;
import org.jline.shell.impl.DefaultCommandDispatcher;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Builder for creating {@link Shell} instances.
 * <p>
 * The builder provides a fluent API for configuring the shell:
 * <pre>
 * Shell shell = Shell.builder()
 *     .terminal(terminal)              // optional, auto-created if null
 *     .prompt("myapp&gt; ")               // or .prompt(() -&gt; dynamicPrompt)
 *     .dispatcher(dispatcher)          // optional, DefaultCommandDispatcher if null
 *     .groups(group1, group2)          // added to dispatcher
 *     .parser(parser)                  // optional, DefaultParser by default
 *     .historyFile(path)               // optional
 *     .variable(name, value)           // forwarded to LineReader
 *     .option(Option.X, true)          // forwarded to LineReader
 *     .onReaderReady(reader -&gt; { ... })// optional callback
 *     .build();
 * </pre>
 *
 * @see Shell
 * @since 4.0
 */
public class ShellBuilder {

    private Terminal terminal;
    private CommandDispatcher dispatcher;
    private final List<CommandGroup> groups = new ArrayList<>();
    private Parser parser;
    private Path historyFile;
    private Supplier<String> promptSupplier;
    private Supplier<String> rightPromptSupplier;
    private final Map<String, Object> variables = new LinkedHashMap<>();
    private final Map<Option, Boolean> options = new LinkedHashMap<>();
    private Consumer<LineReader> onReaderReady;
    private File initScript;
    private JobManager jobManager;

    ShellBuilder() {}

    /**
     * Sets the terminal for the shell. If not specified, a system terminal is created automatically.
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
        this.promptSupplier = () -> prompt;
        return this;
    }

    /**
     * Sets a dynamic prompt supplier, called before each line read.
     *
     * @param promptSupplier the prompt supplier
     * @return this builder
     */
    public ShellBuilder prompt(Supplier<String> promptSupplier) {
        this.promptSupplier = promptSupplier;
        return this;
    }

    /**
     * Sets a static right prompt string.
     *
     * @param rightPrompt the right prompt string
     * @return this builder
     */
    public ShellBuilder rightPrompt(String rightPrompt) {
        this.rightPromptSupplier = () -> rightPrompt;
        return this;
    }

    /**
     * Sets a dynamic right prompt supplier, called before each line read.
     *
     * @param rightPromptSupplier the right prompt supplier
     * @return this builder
     */
    public ShellBuilder rightPrompt(Supplier<String> rightPromptSupplier) {
        this.rightPromptSupplier = rightPromptSupplier;
        return this;
    }

    /**
     * Sets the command dispatcher. If not specified, a {@link DefaultCommandDispatcher} is created.
     *
     * @param dispatcher the command dispatcher
     * @return this builder
     */
    public ShellBuilder dispatcher(CommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        return this;
    }

    /**
     * Adds command groups to the dispatcher.
     *
     * @param groups the command groups
     * @return this builder
     */
    public ShellBuilder groups(CommandGroup... groups) {
        Collections.addAll(this.groups, groups);
        return this;
    }

    /**
     * Sets the line parser. If not specified, a {@link DefaultParser} is used.
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
     * @param option the option
     * @param value the option value
     * @return this builder
     */
    public ShellBuilder option(Option option, boolean value) {
        this.options.put(option, value);
        return this;
    }

    /**
     * Sets a callback invoked after the LineReader is created but before {@link Shell#run()}.
     * <p>
     * This is useful for setting up TailTipWidgets or other post-reader customizations.
     *
     * @param onReaderReady the callback
     * @return this builder
     */
    public ShellBuilder onReaderReady(Consumer<LineReader> onReaderReady) {
        this.onReaderReady = onReaderReady;
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
     * Sets the job manager for the shell.
     *
     * @param jobManager the job manager
     * @return this builder
     */
    public ShellBuilder jobManager(JobManager jobManager) {
        this.jobManager = jobManager;
        return this;
    }

    /**
     * Builds the {@link Shell} instance.
     * <p>
     * If no terminal is provided, a system terminal is created. If no dispatcher
     * is provided, a {@link DefaultCommandDispatcher} is created. All configured
     * groups are added to the dispatcher.
     *
     * @return a new Shell
     * @throws IOException if terminal creation fails
     */
    public Shell build() throws IOException {
        boolean ownTerminal = false;
        Terminal term = this.terminal;
        if (term == null) {
            term = TerminalBuilder.builder().build();
            ownTerminal = true;
        }

        // Create or use provided dispatcher
        CommandDispatcher disp = this.dispatcher;
        if (disp == null) {
            disp = new DefaultCommandDispatcher(term);
        }

        // Add groups to dispatcher
        for (CommandGroup group : groups) {
            disp.addGroup(group);
        }

        // Create parser
        Parser p = this.parser;
        if (p == null) {
            p = new DefaultParser();
        }

        // Build LineReader
        LineReaderBuilder readerBuilder =
                LineReaderBuilder.builder().terminal(term).parser(p).completer(disp.completer());

        if (historyFile != null) {
            readerBuilder.variable(LineReader.HISTORY_FILE, historyFile);
        }

        LineReader reader = readerBuilder.build();

        // Apply variables
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            reader.setVariable(entry.getKey(), entry.getValue());
        }

        // Apply options
        for (Map.Entry<Option, Boolean> entry : options.entrySet()) {
            if (entry.getValue()) {
                reader.setOpt(entry.getKey());
            } else {
                reader.unsetOpt(entry.getKey());
            }
        }

        // Default prompt
        Supplier<String> prompt = this.promptSupplier;
        if (prompt == null) {
            prompt = () -> "> ";
        }

        // Callback
        if (onReaderReady != null) {
            onReaderReady.accept(reader);
        }

        return new Shell(term, ownTerminal, reader, disp, prompt, rightPromptSupplier, initScript);
    }
}

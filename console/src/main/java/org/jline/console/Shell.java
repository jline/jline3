/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Supplier;

import org.jline.console.impl.Builtins;
import org.jline.console.impl.ConsoleEngineImpl;
import org.jline.console.impl.DefaultJob;
import org.jline.console.impl.DefaultJobManager;
import org.jline.console.impl.DefaultPrinter;
import org.jline.console.impl.SimpleSystemRegistryImpl;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.Option;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.DefaultParser.Bracket;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.TailTipWidgets;
import org.jline.widget.TailTipWidgets.TipType;

/**
 * High-level REPL shell that encapsulates JLine setup and the read-eval-print loop.
 * <p>
 * Shell eliminates the boilerplate typically required to set up a JLine REPL.
 * Use {@link #builder()} to create instances with a fluent API.
 * <p>
 * Example usage:
 * <pre>
 * Shell shell = Shell.builder()
 *     .prompt("myapp&gt; ")
 *     .commands(myCommands)
 *     .build();
 * shell.run();
 * </pre>
 *
 * @see ShellBuilder
 */
public class Shell implements AutoCloseable {

    private final Terminal terminal;
    private final boolean terminalOwned;
    private final SystemRegistry systemRegistry;
    private final ConsoleEngine consoleEngine;
    private final LineReader reader;
    private final String prompt;
    private final Supplier<String> promptSupplier;
    private final JobManager jobManager;

    Shell(ShellBuilder builder) throws Exception {
        // Work directory
        Supplier<Path> workDir = builder.workDir;
        if (workDir == null) {
            workDir = () -> Paths.get(System.getProperty("user.dir"));
        }

        // Parser
        Parser parser = builder.parser;
        if (parser == null) {
            DefaultParser defaultParser = new DefaultParser();
            defaultParser.setEofOnUnclosedBracket(Bracket.CURLY, Bracket.ROUND, Bracket.SQUARE);
            defaultParser.setEofOnUnclosedQuote(true);
            defaultParser.setRegexCommand("[:]{0,1}[a-zA-Z!]{1,}\\S*");
            parser = defaultParser;
        }

        // Terminal
        if (builder.terminal != null) {
            this.terminal = builder.terminal;
            this.terminalOwned = false;
        } else {
            this.terminal = TerminalBuilder.builder().build();
            this.terminalOwned = true;
        }

        // Signal handling is set up in run() based on whether a job manager is present

        // ConfigurationPath (use current dir as default)
        org.jline.builtins.ConfigurationPath configPath =
                new org.jline.builtins.ConfigurationPath(workDir.get(), workDir.get());

        // System registry and console engine setup
        if (builder.scriptEngine != null) {
            // Full scripting mode with ConsoleEngineImpl
            Printer printer = new DefaultPrinter(builder.scriptEngine, configPath);
            ConsoleEngineImpl ce = new ConsoleEngineImpl(builder.scriptEngine, printer, workDir, configPath);
            this.consoleEngine = ce;

            Builtins builtins =
                    new Builtins(workDir, configPath, (String fun) -> new ConsoleEngine.WidgetCreator(ce, fun));

            SystemRegistryImpl sysReg = new SystemRegistryImpl(parser, terminal, workDir, configPath);
            CommandRegistry[] allRegistries = new CommandRegistry[builder.commandRegistries.size() + 2];
            allRegistries[0] = ce;
            allRegistries[1] = builtins;
            for (int i = 0; i < builder.commandRegistries.size(); i++) {
                allRegistries[i + 2] = builder.commandRegistries.get(i);
            }
            sysReg.setCommandRegistries(allRegistries);
            this.systemRegistry = sysReg;
        } else {
            // Simple mode without scripting
            this.consoleEngine = null;

            Builtins builtins = new Builtins(workDir, configPath, null);

            SimpleSystemRegistryImpl sysReg = new SimpleSystemRegistryImpl(parser, terminal, workDir, configPath);
            CommandRegistry[] allRegistries = new CommandRegistry[builder.commandRegistries.size() + 1];
            allRegistries[0] = builtins;
            for (int i = 0; i < builder.commandRegistries.size(); i++) {
                allRegistries[i + 1] = builder.commandRegistries.get(i);
            }
            sysReg.setCommandRegistries(allRegistries);
            this.systemRegistry = sysReg;
        }

        // LineReader
        LineReaderBuilder readerBuilder = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(systemRegistry.completer())
                .parser(parser);

        if (builder.highlighter != null) {
            readerBuilder.highlighter(builder.highlighter);
        }

        // Default variables
        readerBuilder.variable(LineReader.SECONDARY_PROMPT_PATTERN, "%M%P > ");
        readerBuilder.variable(LineReader.INDENTATION, 2);
        readerBuilder.variable(LineReader.LIST_MAX, 100);

        if (builder.historyFile != null) {
            readerBuilder.variable(LineReader.HISTORY_FILE, builder.historyFile);
        }

        // User-provided variables (can override defaults)
        for (Map.Entry<String, Object> entry : builder.variables.entrySet()) {
            readerBuilder.variable(entry.getKey(), entry.getValue());
        }

        // User-provided options
        for (Map.Entry<Option, Boolean> entry : builder.options.entrySet()) {
            readerBuilder.option(entry.getKey(), entry.getValue());
        }

        this.reader = readerBuilder.build();

        // Set line reader on registries
        if (systemRegistry instanceof SimpleSystemRegistryImpl) {
            ((SimpleSystemRegistryImpl) systemRegistry).setLineReader(reader);
        }
        if (consoleEngine instanceof ConsoleEngineImpl) {
            ((ConsoleEngineImpl) consoleEngine).setLineReader(reader);
        }
        for (CommandRegistry registry : builder.commandRegistries) {
            registry.setLineReader(reader);
        }

        // TailTipWidgets
        if (builder.tailTipWidgets) {
            new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TipType.COMPLETER);
        }

        // Init script
        if (builder.initScript != null) {
            systemRegistry.initialize(builder.initScript);
        }

        // Prompt
        this.prompt = builder.prompt;
        this.promptSupplier = builder.promptSupplier;

        // Job manager
        this.jobManager = builder.jobManager;
    }

    /**
     * Creates a new {@link ShellBuilder}.
     *
     * @return a new ShellBuilder
     */
    public static ShellBuilder builder() {
        return new ShellBuilder();
    }

    /**
     * Returns the terminal used by this shell.
     *
     * @return the terminal
     */
    public Terminal terminal() {
        return terminal;
    }

    /**
     * Returns the line reader used by this shell.
     *
     * @return the line reader
     */
    public LineReader reader() {
        return reader;
    }

    /**
     * Returns the system registry used by this shell.
     *
     * @return the system registry
     */
    public SystemRegistry systemRegistry() {
        return systemRegistry;
    }

    /**
     * Runs the REPL loop. This method blocks until the user exits (Ctrl-D/EOF).
     */
    public void run() {
        // Set up signal handling
        DefaultJobManager djm = (jobManager instanceof DefaultJobManager) ? (DefaultJobManager) jobManager : null;
        if (djm != null) {
            terminal.handle(Signal.INT, signal -> {
                Job fg = jobManager.foregroundJob();
                if (fg != null) {
                    fg.interrupt();
                }
            });
            terminal.handle(Signal.TSTP, signal -> {
                Job fg = jobManager.foregroundJob();
                if (fg != null) {
                    fg.suspend();
                }
            });
        } else {
            Thread executeThread = Thread.currentThread();
            terminal.handle(Signal.INT, signal -> executeThread.interrupt());
        }

        PrintStream out = new PrintStream(terminal.output());
        while (true) {
            try {
                systemRegistry.cleanUp();
                String currentPrompt = promptSupplier != null ? promptSupplier.get() : (prompt != null ? prompt : "> ");
                String line = reader.readLine(currentPrompt);

                DefaultJob job = null;
                if (djm != null) {
                    job = djm.createJob(line, Thread.currentThread());
                }
                try {
                    Object result = systemRegistry.execute(line);
                    if (result != null) {
                        if (consoleEngine != null) {
                            consoleEngine.println(result);
                        } else {
                            out.println(result);
                        }
                    }
                } finally {
                    if (djm != null && job != null) {
                        djm.completeJob(job);
                    }
                }
            } catch (UserInterruptException e) {
                // Ignore
            } catch (EndOfFileException e) {
                String pl = e.getPartialLine();
                if (pl != null) {
                    try {
                        Object result = systemRegistry.execute(pl);
                        if (result != null) {
                            if (consoleEngine != null) {
                                consoleEngine.println(result);
                            } else {
                                out.println(result);
                            }
                        }
                    } catch (Exception e2) {
                        systemRegistry.trace(e2);
                    }
                }
                break;
            } catch (Exception | Error e) {
                systemRegistry.trace(e);
            }
        }
    }

    /**
     * Closes this shell, releasing all resources.
     */
    @Override
    public void close() {
        systemRegistry.close();
        if (terminalOwned) {
            try {
                terminal.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}

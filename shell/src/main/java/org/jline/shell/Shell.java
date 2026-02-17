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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jline.reader.*;
import org.jline.shell.impl.DefaultJobManager;
import org.jline.terminal.Terminal;

/**
 * An interactive shell that runs a read-eval-print loop (REPL)
 * using a {@link CommandDispatcher} for command execution.
 * <p>
 * The shell is a thin REPL loop. The dispatcher is injected or auto-created
 * as a {@link org.jline.shell.impl.DefaultCommandDispatcher}.
 * <p>
 * Use {@link #builder()} to create a shell:
 * <pre>
 * Shell shell = Shell.builder()
 *     .terminal(terminal)
 *     .prompt("myapp&gt; ")
 *     .dispatcher(dispatcher)
 *     .groups(group1, group2)
 *     .build();
 * shell.run();
 * </pre>
 *
 * @see ShellBuilder
 * @see CommandDispatcher
 * @since 4.0
 */
public class Shell implements AutoCloseable {

    private final Terminal terminal;
    private final boolean ownTerminal;
    private final LineReader reader;
    private final CommandDispatcher dispatcher;
    private final Supplier<String> promptSupplier;
    private final Supplier<String> rightPromptSupplier;
    private final File initScript;
    private final JobManager jobManager;
    private volatile boolean running;

    Shell(
            Terminal terminal,
            boolean ownTerminal,
            LineReader reader,
            CommandDispatcher dispatcher,
            Supplier<String> promptSupplier,
            Supplier<String> rightPromptSupplier,
            File initScript,
            JobManager jobManager) {
        this.terminal = terminal;
        this.ownTerminal = ownTerminal;
        this.reader = reader;
        this.dispatcher = dispatcher;
        this.promptSupplier = promptSupplier;
        this.rightPromptSupplier = rightPromptSupplier;
        this.initScript = initScript;
        this.jobManager = jobManager;
    }

    /**
     * Creates a new {@link ShellBuilder}.
     *
     * @return a new shell builder
     */
    public static ShellBuilder builder() {
        return new ShellBuilder();
    }

    /**
     * Runs the interactive REPL loop.
     * <p>
     * This method blocks until the user exits (via EOF or an "exit"/"quit" command),
     * or until {@link #stop()} is called.
     *
     * @throws Exception if initialization or execution fails
     */
    public void run() throws Exception {
        running = true;
        try {
            dispatcher.initialize(initScript);

            while (running) {
                // Print completed background job notifications
                printCompletedJobs();

                String line;
                try {
                    String prompt = promptSupplier.get();
                    String rightPrompt = rightPromptSupplier != null ? rightPromptSupplier.get() : null;
                    line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                } catch (UserInterruptException e) {
                    // Ctrl-C: clear line, continue
                    continue;
                } catch (EndOfFileException e) {
                    // Ctrl-D: exit
                    break;
                }

                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Built-in exit/quit
                if ("exit".equals(line) || "quit".equals(line)) {
                    break;
                }

                try {
                    dispatcher.execute(line);
                } catch (Exception e) {
                    dispatcher.trace(e);
                } finally {
                    dispatcher.cleanUp();
                }
            }
        } finally {
            running = false;
        }
    }

    /**
     * Prints notifications for completed background jobs and removes them from the job list.
     */
    private void printCompletedJobs() {
        if (jobManager == null) {
            return;
        }
        List<Job> doneJobs = new ArrayList<>();
        for (Job job : jobManager.jobs()) {
            if (job.status() == Job.Status.Done) {
                doneJobs.add(job);
            }
        }
        if (!doneJobs.isEmpty()) {
            PrintStream out = new PrintStream(terminal.output());
            for (Job job : doneJobs) {
                out.println("[" + job.id() + "]  Done           " + job.command());
            }
            // Remove done jobs after notification
            if (jobManager instanceof DefaultJobManager) {
                DefaultJobManager djm = (DefaultJobManager) jobManager;
                for (Job job : doneJobs) {
                    djm.removeJob(job);
                }
            }
        }
    }

    /**
     * Stops the REPL loop. The current or next iteration will exit.
     */
    public void stop() {
        running = false;
    }

    /**
     * Returns whether the shell is currently running.
     *
     * @return true if running
     */
    public boolean isRunning() {
        return running;
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
     * Returns the command dispatcher used by this shell.
     *
     * @return the command dispatcher
     */
    public CommandDispatcher dispatcher() {
        return dispatcher;
    }

    /**
     * Closes this shell, releasing the dispatcher and optionally the terminal.
     */
    @Override
    public void close() {
        dispatcher.close();
        if (ownTerminal) {
            try {
                terminal.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}

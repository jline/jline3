/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.NullCompleter;
import org.jline.reader.impl.completer.SystemCompleter;
import org.jline.shell.*;
import org.jline.shell.Pipeline.Operator;
import org.jline.terminal.Terminal;

/**
 * Default implementation of {@link CommandDispatcher} that supports pipeline execution.
 * <p>
 * This dispatcher parses command lines using {@link PipelineParser}, resolves commands
 * from registered {@link CommandGroup}s, and executes pipeline stages with proper
 * pipe/redirect/conditional handling.
 * <p>
 * When a {@link JobManager} is configured, the dispatcher supports:
 * <ul>
 *   <li>Background execution via trailing {@code &}</li>
 *   <li>Foreground job tracking</li>
 *   <li>Built-in {@code jobs}, {@code fg}, {@code bg} commands</li>
 * </ul>
 *
 * @see CommandDispatcher
 * @see PipelineParser
 * @since 4.0
 */
public class DefaultCommandDispatcher implements CommandDispatcher {

    private final Terminal terminal;
    private final List<CommandGroup> groups = new ArrayList<>();
    private final PipelineParser pipelineParser = new PipelineParser();
    private final CommandSession session;
    private final DefaultJobManager jobManager;

    /**
     * Creates a new dispatcher for the given terminal.
     *
     * @param terminal the terminal
     */
    public DefaultCommandDispatcher(Terminal terminal) {
        this(terminal, null);
    }

    /**
     * Creates a new dispatcher for the given terminal with optional job management.
     * <p>
     * When a non-null {@link JobManager} is provided, the dispatcher automatically
     * registers {@link JobCommands} and supports background execution via trailing {@code &}.
     *
     * @param terminal the terminal
     * @param jobManager the job manager, or null for no job control
     */
    public DefaultCommandDispatcher(Terminal terminal, JobManager jobManager) {
        this.terminal = terminal;
        this.session = new CommandSession(terminal);
        this.jobManager = jobManager instanceof DefaultJobManager ? (DefaultJobManager) jobManager : null;
        if (this.jobManager != null) {
            groups.add(new JobCommands(jobManager));
        }
    }

    @Override
    public void addGroup(CommandGroup group) {
        groups.add(group);
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
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        // Check for background execution
        String trimmed = line.trim();
        if (jobManager != null && trimmed.endsWith("&")) {
            String cmdLine = trimmed.substring(0, trimmed.length() - 1).trim();
            if (cmdLine.isEmpty()) {
                return null;
            }
            return executeBackground(cmdLine);
        }

        Pipeline pipeline = pipelineParser.parse(line);
        return executePipeline(pipeline, line.trim());
    }

    private Object executeBackground(String cmdLine) {
        Pipeline pipeline = pipelineParser.parse(cmdLine);

        // Extract command name for thread naming
        String cmdName = cmdLine.split("\\s+", 2)[0];

        AtomicReference<DefaultJob> jobRef = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                executePipelineStages(pipeline);
            } catch (Exception e) {
                PrintStream err = new PrintStream(terminal.output());
                err.println("[" + jobRef.get().id() + "]  Error: " + e.getMessage());
            } finally {
                DefaultJob job = jobRef.get();
                if (job != null) {
                    jobManager.completeJob(job);
                }
            }
        });

        DefaultJob job = jobManager.createJob(cmdLine, thread);
        job.setStatus(Job.Status.Background);
        jobRef.set(job);
        thread.setName("job-" + job.id() + "-" + cmdName);
        thread.setDaemon(true);
        thread.start();

        PrintStream out = new PrintStream(terminal.output());
        out.println("[" + job.id() + "]  " + cmdLine + " &");

        return null;
    }

    private Object executePipeline(Pipeline pipeline, String line) throws Exception {
        return executePipelineStages(pipeline);
    }

    @Override
    public Object execute(Pipeline pipeline) throws Exception {
        return executePipelineStages(pipeline);
    }

    private Object executePipelineStages(Pipeline pipeline) throws Exception {
        List<Pipeline.Stage> stages = pipeline.stages();
        Object lastResult = null;
        String lastOutput = null;
        boolean skip = false;

        for (int i = 0; i < stages.size(); i++) {
            Pipeline.Stage stage = stages.get(i);
            String cmdLine = stage.commandLine();

            if (cmdLine == null || cmdLine.trim().isEmpty()) {
                continue;
            }

            if (skip) {
                // Check if we should resume after a conditional
                Pipeline.Stage prevStage = i > 0 ? stages.get(i - 1) : null;
                if (prevStage != null && prevStage.operator() == Operator.OR) {
                    skip = false;
                } else {
                    continue;
                }
            }

            // Parse command name and args
            String[] parts = cmdLine.trim().split("\\s+", 2);
            String cmdName = parts[0];
            String argsStr = parts.length > 1 ? parts[1] : "";

            // Handle pipe input: prepend to args
            if (lastOutput != null && i > 0) {
                Pipeline.Stage prevStage = stages.get(i - 1);
                if (prevStage.operator() == Operator.PIPE) {
                    // For pipe, we pass input via session variable
                    session.put("_pipe_input", lastOutput);
                } else if (prevStage.operator() == Operator.FLIP) {
                    // For flip, append output as argument
                    argsStr = argsStr.isEmpty() ? lastOutput.trim() : argsStr + " " + lastOutput.trim();
                }
            }

            Command cmd = findCommand(cmdName);
            if (cmd == null) {
                throw new IllegalArgumentException("Unknown command: " + cmdName);
            }

            String[] args = argsStr.isEmpty() ? new String[0] : argsStr.split("\\s+");

            // If this stage pipes into the next, capture stdout instead of printing to terminal
            Operator op = stage.operator();
            boolean captureOutput = (op == Operator.PIPE || op == Operator.FLIP);
            ByteArrayOutputStream capture = null;
            PrintStream originalOut = session.out();
            if (captureOutput) {
                capture = new ByteArrayOutputStream();
                session.setOut(new PrintStream(capture));
            }

            try {
                lastResult = cmd.execute(session, args);
                if (captureOutput) {
                    session.out().flush();
                    String captured = capture.toString().trim();
                    lastOutput = captured.isEmpty() ? (lastResult != null ? lastResult.toString() : null) : captured;
                } else {
                    lastOutput = lastResult != null ? lastResult.toString() : null;
                }
                session.setLastExitCode(0);
            } catch (Exception e) {
                session.setLastExitCode(1);
                lastResult = null;
                lastOutput = null;

                // Handle conditionals
                if (op == Operator.AND) {
                    skip = true;
                    continue;
                } else if (op != Operator.OR) {
                    throw e;
                }
                continue;
            } finally {
                if (captureOutput) {
                    session.setOut(originalOut);
                }
            }

            // Handle redirect/append
            if (op == Operator.REDIRECT || op == Operator.APPEND) {
                if (stage.redirectTarget() != null && lastOutput != null) {
                    if (op == Operator.APPEND) {
                        Files.writeString(
                                stage.redirectTarget(),
                                lastOutput + System.lineSeparator(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND);
                    } else {
                        Files.writeString(stage.redirectTarget(), lastOutput + System.lineSeparator());
                    }
                    lastResult = null;
                    lastOutput = null;
                }
            } else if (op == Operator.AND) {
                // Success, continue to next
                skip = false;
            } else if (op == Operator.OR) {
                // Success, skip OR branch
                skip = true;
            }
        }

        return lastResult;
    }

    @Override
    public Completer completer() {
        SystemCompleter completer = new SystemCompleter();
        for (CommandGroup group : groups) {
            for (Command cmd : group.commands()) {
                List<Completer> cmdCompleters = cmd.completers();
                if (!cmdCompleters.isEmpty()) {
                    List<Completer> all = new ArrayList<>(cmdCompleters);
                    all.add(NullCompleter.INSTANCE);
                    completer.add(cmd.name(), new ArgumentCompleter(all));
                    for (String alias : cmd.aliases()) {
                        completer.add(alias, new ArgumentCompleter(all));
                    }
                } else {
                    completer.add(cmd.name(), NullCompleter.INSTANCE);
                    for (String alias : cmd.aliases()) {
                        completer.add(alias, NullCompleter.INSTANCE);
                    }
                }
            }
        }
        completer.compile(this::createCandidate);
        return completer;
    }

    private Candidate createCandidate(String command) {
        String group = null;
        String desc = null;
        for (CommandGroup g : groups) {
            Command cmd = g.command(command);
            if (cmd != null) {
                group = g.name();
                desc = cmd.description();
                break;
            }
        }
        return new Candidate(command, command, group, desc, null, null, true);
    }

    @Override
    public CommandDescription describe(CommandLine commandLine) {
        if (commandLine == null || commandLine.args().isEmpty()) {
            return null;
        }
        String cmdName = commandLine.args().get(0);
        Command cmd = findCommand(cmdName);
        if (cmd != null) {
            return cmd.describe(commandLine.args());
        }
        return null;
    }

    @Override
    public Terminal terminal() {
        return terminal;
    }

    @Override
    public void close() {
        // Nothing to close by default
    }
}

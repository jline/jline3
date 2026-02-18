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
import org.jline.reader.impl.completer.StringsCompleter;
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
 * <p>
 * When an {@link AliasManager} is configured, the dispatcher expands aliases
 * before pipeline parsing and automatically registers {@code alias}/{@code unalias} commands.
 *
 * @see CommandDispatcher
 * @see PipelineParser
 * @since 4.0
 */
public class DefaultCommandDispatcher implements CommandDispatcher {

    private final Terminal terminal;
    private final List<CommandGroup> groups = new ArrayList<>();
    private final PipelineParser pipelineParser;
    private final CommandSession session;
    private final DefaultJobManager jobManager;
    private final AliasManager aliasManager;
    private final LineExpander lineExpander;
    private final ScriptRunner scriptRunner;
    private volatile Thread commandThread;

    /**
     * Creates a new dispatcher for the given terminal.
     *
     * @param terminal the terminal
     */
    public DefaultCommandDispatcher(Terminal terminal) {
        this(terminal, null, null, null);
    }

    /**
     * Creates a new dispatcher for the given terminal with optional job management.
     *
     * @param terminal the terminal
     * @param jobManager the job manager, or null for no job control
     */
    public DefaultCommandDispatcher(Terminal terminal, JobManager jobManager) {
        this(terminal, jobManager, null, null);
    }

    /**
     * Creates a new dispatcher with all configuration options.
     * <p>
     * When a non-null {@link JobManager} is provided, the dispatcher automatically
     * registers {@link JobCommands} and supports background execution via trailing {@code &}.
     * <p>
     * When a non-null {@link AliasManager} is provided, the dispatcher expands aliases
     * before pipeline parsing and automatically registers {@link AliasCommands}.
     * <p>
     * When a non-null {@link PipelineParser} is provided, it is used instead of the
     * default parser. This allows custom operator registration and subclass overrides.
     *
     * @param terminal the terminal
     * @param jobManager the job manager, or null for no job control
     * @param pipelineParser the pipeline parser, or null for the default parser
     * @param aliasManager the alias manager, or null for no alias support
     */
    public DefaultCommandDispatcher(
            Terminal terminal, JobManager jobManager, PipelineParser pipelineParser, AliasManager aliasManager) {
        this(terminal, jobManager, pipelineParser, aliasManager, null, null);
    }

    /**
     * Creates a new dispatcher with all configuration options including line expansion
     * and script execution.
     *
     * @param terminal the terminal
     * @param jobManager the job manager, or null for no job control
     * @param pipelineParser the pipeline parser, or null for the default parser
     * @param aliasManager the alias manager, or null for no alias support
     * @param lineExpander the line expander, or null for no variable expansion
     * @param scriptRunner the script runner, or null for no script support
     */
    public DefaultCommandDispatcher(
            Terminal terminal,
            JobManager jobManager,
            PipelineParser pipelineParser,
            AliasManager aliasManager,
            LineExpander lineExpander,
            ScriptRunner scriptRunner) {
        this.terminal = terminal;
        this.session = new CommandSession(terminal);
        this.pipelineParser = pipelineParser != null ? pipelineParser : new PipelineParser();
        this.jobManager = jobManager instanceof DefaultJobManager ? (DefaultJobManager) jobManager : null;
        this.aliasManager = aliasManager;
        this.lineExpander = lineExpander;
        this.scriptRunner = scriptRunner;
        if (this.jobManager != null) {
            groups.add(new JobCommands(jobManager));
        }
        if (this.aliasManager != null) {
            groups.add(new AliasCommands(aliasManager));
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

        // Expand aliases before parsing
        String expanded = line;
        if (aliasManager != null) {
            expanded = aliasManager.expand(line);
        }

        // Expand variables after alias expansion
        if (lineExpander != null) {
            expanded = lineExpander.expand(expanded, session);
        }

        // Handle bare variable assignment: NAME=VALUE
        String assignTrimmed = expanded.trim();
        if (isVariableAssignment(assignTrimmed)) {
            int eq = assignTrimmed.indexOf('=');
            String name = assignTrimmed.substring(0, eq);
            String value = assignTrimmed.substring(eq + 1);
            session.put(name, value);
            return null;
        }

        // Check for background execution
        String trimmed = expanded.trim();
        if (jobManager != null && trimmed.endsWith("&") && !trimmed.endsWith("&&")) {
            String cmdLine = trimmed.substring(0, trimmed.length() - 1).trim();
            if (cmdLine.isEmpty()) {
                return null;
            }
            return executeBackground(cmdLine);
        }

        Pipeline pipeline = pipelineParser.parse(expanded);
        return executePipeline(pipeline, expanded.trim());
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
        commandThread = Thread.currentThread();
        try {
            return doExecutePipelineStages(pipeline);
        } finally {
            commandThread = null;
        }
    }

    private Object doExecutePipelineStages(Pipeline pipeline) throws Exception {
        List<Pipeline.Stage> stages = pipeline.stages();
        Object lastResult = null;
        String lastOutput = null;

        for (int i = 0; i < stages.size(); i++) {
            Pipeline.Stage stage = stages.get(i);
            String cmdLine = stage.commandLine();

            if (cmdLine == null || cmdLine.trim().isEmpty()) {
                continue;
            }

            // Determine if this stage should be skipped based on previous operator and exit code
            if (i > 0) {
                Operator prevOp = stages.get(i - 1).operator();
                if (prevOp == Operator.AND && session.lastExitCode() != 0) {
                    // AND: skip if previous failed
                    continue;
                }
                if (prevOp == Operator.OR && session.lastExitCode() == 0) {
                    // OR: skip if previous succeeded
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

            // Subcommand routing: check if first arg matches a subcommand
            if (args.length > 0 && !cmd.subcommands().isEmpty()) {
                Command sub = cmd.subcommands().get(args[0]);
                if (sub != null) {
                    cmd = sub;
                    args = Arrays.copyOfRange(args, 1, args.length);
                }
            }

            // Handle input redirection
            InputStream originalIn = session.in();
            boolean inputRedirected = false;
            if (stage.inputSource() != null) {
                byte[] inputBytes = Files.readAllBytes(stage.inputSource());
                session.setIn(new ByteArrayInputStream(inputBytes));
                inputRedirected = true;
            }

            // If this stage pipes into the next, capture stdout instead of printing to terminal
            Operator op = stage.operator();
            boolean captureOutput = (op == Operator.PIPE || op == Operator.FLIP);

            // For stderr/combined redirect, we redirect streams before execution
            PrintStream originalOut = session.out();
            PrintStream originalErr = session.err();
            ByteArrayOutputStream capture = null;
            boolean stderrRedirected = false;

            if (captureOutput) {
                capture = new ByteArrayOutputStream();
                session.setOut(new PrintStream(capture));
            } else if (op == Operator.STDERR_REDIRECT && stage.redirectTarget() != null) {
                OutputStream errFile = Files.newOutputStream(
                        stage.redirectTarget(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                session.setErr(new PrintStream(errFile));
                stderrRedirected = true;
            } else if (op == Operator.COMBINED_REDIRECT && stage.redirectTarget() != null) {
                OutputStream combinedFile = Files.newOutputStream(
                        stage.redirectTarget(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                PrintStream combinedStream = new PrintStream(combinedFile);
                session.setOut(combinedStream);
                session.setErr(combinedStream);
                stderrRedirected = true;
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

                // For conditionals and sequence, swallow the exception and continue
                if (op == Operator.AND || op == Operator.OR || op == Operator.SEQUENCE) {
                    continue;
                }
                throw e;
            } finally {
                if (captureOutput) {
                    session.setOut(originalOut);
                }
                if (stderrRedirected) {
                    session.out().flush();
                    session.err().flush();
                    session.setOut(originalOut);
                    session.setErr(originalErr);
                }
                if (inputRedirected) {
                    session.setIn(originalIn);
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
            } else if (op == Operator.STDERR_REDIRECT || op == Operator.COMBINED_REDIRECT) {
                // Already handled above via stream redirection
                lastResult = null;
                lastOutput = null;
            } else if (op == Operator.SEQUENCE) {
                // Sequence: always continue, reset output for next independent command
                lastOutput = null;
            }
        }

        return lastResult;
    }

    @Override
    public Completer completer() {
        SystemCompleter completer = new SystemCompleter();
        for (CommandGroup group : groups) {
            for (Command cmd : group.commands()) {
                Completer cmdCompleter = buildCommandCompleter(cmd);
                completer.add(cmd.name(), cmdCompleter);
                for (String alias : cmd.aliases()) {
                    completer.add(alias, cmdCompleter);
                }
            }
        }
        completer.compile(this::createCandidate);
        return completer;
    }

    private Completer buildCommandCompleter(Command cmd) {
        Map<String, Command> subs = cmd.subcommands();
        if (!subs.isEmpty()) {
            // Build a completer that offers subcommand names, then delegates
            List<Completer> subCompleters = new ArrayList<>();
            subCompleters.add(new StringsCompleter(subs.keySet()));
            subCompleters.add(NullCompleter.INSTANCE);
            return new ArgumentCompleter(subCompleters);
        }
        List<Completer> cmdCompleters = cmd.completers();
        if (!cmdCompleters.isEmpty()) {
            List<Completer> all = new ArrayList<>(cmdCompleters);
            all.add(NullCompleter.INSTANCE);
            return new ArgumentCompleter(all);
        }
        return NullCompleter.INSTANCE;
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

    /**
     * Returns the command session used by this dispatcher.
     *
     * @return the command session
     */
    public CommandSession session() {
        return session;
    }

    /**
     * Interrupts the currently executing command, if any.
     * <p>
     * This is typically called from a signal handler (e.g., SIGINT) to
     * interrupt the foreground command without killing the shell.
     */
    public void interruptCurrentCommand() {
        Thread t = commandThread;
        if (t != null) {
            t.interrupt();
        }
    }

    @Override
    public void initialize(File script) throws Exception {
        if (script != null && script.exists() && scriptRunner != null) {
            scriptRunner.execute(script.toPath(), session, this);
        }
    }

    /**
     * Checks whether a line is a bare variable assignment ({@code NAME=VALUE}).
     * <p>
     * A line is a variable assignment if it contains {@code =} and the part before
     * it is a valid identifier (letters, digits, underscores, starting with a letter
     * or underscore). The line must not contain spaces before the {@code =}.
     *
     * @param line the trimmed input line
     * @return true if the line is a variable assignment
     */
    private static boolean isVariableAssignment(String line) {
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return false;
        }
        // Check that there are no spaces before the '='
        String name = line.substring(0, eq);
        if (name.indexOf(' ') >= 0) {
            return false;
        }
        // Check that the name is a valid identifier
        if (!Character.isLetter(name.charAt(0)) && name.charAt(0) != '_') {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        // Nothing to close by default
    }
}

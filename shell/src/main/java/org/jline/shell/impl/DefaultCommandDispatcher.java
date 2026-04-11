/*
 * Copyright (c) the original author(s).
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
import org.jline.utils.NonBlockingPumpInputStream;

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

        int i = 0;
        while (i < stages.size()) {
            // Identify pipe group: consecutive stages connected by PIPE
            int groupStart = i;
            while (i < stages.size() - 1 && stages.get(i).operator() == Operator.PIPE) {
                i++;
            }
            int groupEnd = i;
            i++;

            // Skip based on previous group's trailing operator
            if (groupStart > 0) {
                Operator prevOp = stages.get(groupStart - 1).operator();
                if (prevOp == Operator.AND && session.lastExitCode() != 0) {
                    continue;
                }
                if (prevOp == Operator.OR && session.lastExitCode() == 0) {
                    continue;
                }
            }

            // FLIP: previous output becomes arguments for first stage
            String flipArgs = null;
            if (groupStart > 0 && stages.get(groupStart - 1).operator() == Operator.FLIP && lastOutput != null) {
                flipArgs = lastOutput.trim();
            }

            int groupSize = groupEnd - groupStart + 1;
            Pipeline.Stage lastGroupStage = stages.get(groupEnd);
            Operator lastGroupOp = lastGroupStage.operator();

            if (groupSize > 1) {
                // Multi-stage concurrent pipe group
                Object[] result = executePipeGroup(stages.subList(groupStart, groupEnd + 1), flipArgs);
                lastResult = result[0];
                lastOutput = (String) result[1];
            } else {
                // Single-stage execution
                Pipeline.Stage stage = stages.get(groupStart);
                String cmdLine = stage.commandLine();

                if (cmdLine == null || cmdLine.trim().isEmpty()) {
                    continue;
                }

                // Parse command name and args
                String[] parts = cmdLine.trim().split("\\s+", 2);
                String cmdName = parts[0];
                String argsStr = parts.length > 1 ? parts[1] : "";

                // Handle FLIP args from previous group
                if (flipArgs != null) {
                    argsStr = argsStr.isEmpty() ? flipArgs : argsStr + " " + flipArgs;
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
                        lastOutput =
                                captured.isEmpty() ? (lastResult != null ? lastResult.toString() : null) : captured;
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
            }

            // Post-execution: handle redirect/append for the last stage of the group
            if (lastGroupOp == Operator.REDIRECT || lastGroupOp == Operator.APPEND) {
                if (lastGroupStage.redirectTarget() != null && lastOutput != null) {
                    if (lastGroupOp == Operator.APPEND) {
                        Files.writeString(
                                lastGroupStage.redirectTarget(),
                                lastOutput + System.lineSeparator(),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND);
                    } else {
                        Files.writeString(lastGroupStage.redirectTarget(), lastOutput + System.lineSeparator());
                    }
                    lastResult = null;
                    lastOutput = null;
                }
            } else if (lastGroupOp == Operator.STDERR_REDIRECT || lastGroupOp == Operator.COMBINED_REDIRECT) {
                lastResult = null;
                lastOutput = null;
            } else if (lastGroupOp == Operator.SEQUENCE) {
                lastOutput = null;
            }
        }

        return lastResult;
    }

    /**
     * Executes a group of stages connected by PIPE operators concurrently with streaming I/O.
     * <p>
     * Stages are connected via {@link PipePumpInputStream} pairs. Producer stages (all but
     * the last) run in separate threads. The last stage runs in the current thread. When a
     * producer finishes, closing its output stream signals EOF to the downstream consumer.
     *
     * @param groupStages the stages in the pipe group (size &gt; 1)
     * @param flipArgs arguments from a preceding FLIP operator, or null
     * @return a two-element array: {@code [result, output]} from the last stage
     * @throws Exception if the last stage fails and the error should propagate
     */
    private Object[] executePipeGroup(List<Pipeline.Stage> groupStages, String flipArgs) throws Exception {
        int n = groupStages.size();
        Pipeline.Stage lastGroupStage = groupStages.get(n - 1);
        Operator lastGroupOp = lastGroupStage.operator();

        // Create N-1 pump connections between stages
        PipePumpInputStream[] pumps = new PipePumpInputStream[n - 1];
        for (int p = 0; p < n - 1; p++) {
            pumps[p] = new PipePumpInputStream();
        }

        // Resolve commands and arguments for all stages
        Command[] commands = new Command[n];
        String[][] argsArrays = new String[n][];
        for (int s = 0; s < n; s++) {
            Pipeline.Stage stage = groupStages.get(s);
            String cmdLine = stage.commandLine();
            if (cmdLine == null || cmdLine.trim().isEmpty()) {
                closePumps(pumps);
                throw new IllegalArgumentException("Empty command in pipeline");
            }

            String[] parts = cmdLine.trim().split("\\s+", 2);
            String cmdName = parts[0];
            String argsStr = parts.length > 1 ? parts[1] : "";

            // FLIP args apply to the first stage only
            if (s == 0 && flipArgs != null) {
                argsStr = argsStr.isEmpty() ? flipArgs : argsStr + " " + flipArgs;
            }

            Command cmd = findCommand(cmdName);
            if (cmd == null) {
                closePumps(pumps);
                throw new IllegalArgumentException("Unknown command: " + cmdName);
            }

            String[] cmdArgs = argsStr.isEmpty() ? new String[0] : argsStr.split("\\s+");
            if (cmdArgs.length > 0 && !cmd.subcommands().isEmpty()) {
                Command sub = cmd.subcommands().get(cmdArgs[0]);
                if (sub != null) {
                    cmd = sub;
                    cmdArgs = Arrays.copyOfRange(cmdArgs, 1, cmdArgs.length);
                }
            }

            commands[s] = cmd;
            argsArrays[s] = cmdArgs;
        }

        // Set up first stage input
        InputStream firstIn = session.in();
        Pipeline.Stage firstStage = groupStages.get(0);
        if (firstStage.inputSource() != null) {
            firstIn = new ByteArrayInputStream(Files.readAllBytes(firstStage.inputSource()));
        }

        // Set up last stage output and error
        boolean captureLastOutput = (lastGroupOp == Operator.PIPE || lastGroupOp == Operator.FLIP);
        ByteArrayOutputStream lastCapture = captureLastOutput ? new ByteArrayOutputStream() : null;
        PrintStream lastOut = captureLastOutput ? new PrintStream(lastCapture) : session.out();
        PrintStream lastErr = session.err();
        boolean lastStderrRedirected = false;
        if (lastGroupOp == Operator.STDERR_REDIRECT && lastGroupStage.redirectTarget() != null) {
            OutputStream errFile = Files.newOutputStream(
                    lastGroupStage.redirectTarget(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            lastErr = new PrintStream(errFile);
            lastStderrRedirected = true;
        } else if (lastGroupOp == Operator.COMBINED_REDIRECT && lastGroupStage.redirectTarget() != null) {
            OutputStream combinedFile = Files.newOutputStream(
                    lastGroupStage.redirectTarget(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            PrintStream combinedStream = new PrintStream(combinedFile);
            lastOut = combinedStream;
            lastErr = combinedStream;
            lastStderrRedirected = true;
        }

        // Create per-stage sessions with wired I/O
        CommandSession[] stageSessions = new CommandSession[n];
        for (int s = 0; s < n; s++) {
            InputStream stageIn;
            PrintStream stageOut;
            PrintStream stageErr = session.err();

            if (s == 0) {
                stageIn = firstIn;
                stageOut = new PrintStream(pumps[0].getOutputStream(), true);
            } else if (s == n - 1) {
                stageIn = pumps[n - 2];
                stageOut = lastOut;
                stageErr = lastErr;
            } else {
                stageIn = pumps[s - 1];
                stageOut = new PrintStream(pumps[s].getOutputStream(), true);
            }

            // Per-stage input redirect overrides pipe input
            Pipeline.Stage stage = groupStages.get(s);
            if (s > 0 && stage.inputSource() != null) {
                stageIn = new ByteArrayInputStream(Files.readAllBytes(stage.inputSource()));
            }

            stageSessions[s] = session.fork(stageIn, stageOut, stageErr);
        }

        // Start producer threads (stages 0..n-2)
        Thread[] threads = new Thread[n - 1];
        for (int t = 0; t < n - 1; t++) {
            final int idx = t;
            threads[t] = new Thread(() -> {
                try {
                    commands[idx].execute(stageSessions[idx], argsArrays[idx]);
                } catch (Exception ignored) {
                    // Producer errors are ignored; the pipeline result
                    // is determined by the last stage (bash semantics).
                } finally {
                    stageSessions[idx].out().close();
                }
            });
            threads[t].setDaemon(true);
            threads[t].start();
        }

        // Run last stage in current thread
        Object lastResult;
        String lastOutput;
        try {
            lastResult = commands[n - 1].execute(stageSessions[n - 1], argsArrays[n - 1]);
            if (captureLastOutput) {
                stageSessions[n - 1].out().flush();
                String captured = lastCapture.toString().trim();
                lastOutput = captured.isEmpty() ? (lastResult != null ? lastResult.toString() : null) : captured;
            } else {
                lastOutput = lastResult != null ? lastResult.toString() : null;
            }
            session.setLastExitCode(0);
        } catch (Exception e) {
            session.setLastExitCode(1);
            if (lastGroupOp == Operator.AND || lastGroupOp == Operator.OR || lastGroupOp == Operator.SEQUENCE) {
                lastResult = null;
                lastOutput = null;
            } else {
                throw e;
            }
        } finally {
            // Close all pumps to unblock any stuck producers
            closePumps(pumps);
            // Wait for all producer threads to complete
            for (Thread t : threads) {
                t.join();
            }
            // Flush redirected streams
            if (lastStderrRedirected) {
                lastOut.flush();
                lastErr.flush();
            }
        }

        return new Object[] {lastResult, lastOutput};
    }

    private static void closePumps(PipePumpInputStream[] pumps) {
        for (PipePumpInputStream pump : pumps) {
            try {
                pump.close();
            } catch (IOException ignored) {
            }
        }
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
        // Check for a custom completer first (e.g., picocli-based completion)
        Completer custom = cmd.completer();
        if (custom != null) {
            return custom;
        }
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

    /**
     * A {@link NonBlockingPumpInputStream} subclass that allows draining buffered data
     * after the write side is closed.
     * <p>
     * The base class's {@code checkClosed()} throws {@link org.jline.utils.ClosedException}
     * in strict mode (default in JLine 4.x), which prevents reading data that is still
     * buffered when the producer closes the stream. This override lets {@code wait()} handle
     * the closed state correctly: it returns available data first, then EOF once the buffer
     * is empty and the stream is closed.
     */
    private static class PipePumpInputStream extends NonBlockingPumpInputStream {
        @Override
        protected void checkClosed() throws IOException {
            // Allow reads on a closed pump so buffered data can be drained.
            // The internal wait() method returns data when the read buffer has
            // remaining bytes, or EOF once the buffer is empty and closed is true.
        }
    }

    @Override
    public void close() {
        // Nothing to close by default
    }
}

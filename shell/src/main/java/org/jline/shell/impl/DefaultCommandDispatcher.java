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
import java.nio.file.Path;
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

    /**
     * Execute the pipeline's stages, honoring pipes, conditional operators, FLIP semantics,
     * redirections (redirect/append/stderr/combined), and sequencing.
     *
     * <p>Stages connected by PIPE are grouped and executed as a concurrent pipeline; single-stage
     * stages are executed inline. The method updates the session's last exit code, applies
     * FLIP by passing trimmed previous output as arguments to the first stage of the next group,
     * captures or redirects stdout/stderr as required, and applies redirect/append targets
     * after a group's execution when present.</p>
     *
     * @return the result object produced by the last executed stage, or `null` if no result was produced
     * @throws Exception if execution or I/O fails and the error is not suppressed by conditional operators
     */
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

                // Resolve command and arguments
                Object[] resolved = resolveCommand(cmdLine, flipArgs);
                Command cmd = (Command) resolved[0];
                String[] args = (String[]) resolved[1];

                // Handle input redirection
                InputStream originalIn = session.in();
                InputStream inputRedirect = null;
                if (stage.inputSource() != null) {
                    inputRedirect = Files.newInputStream(stage.inputSource());
                    session.setIn(inputRedirect);
                }

                // If this stage pipes into the next, capture stdout instead of printing to terminal
                Operator op = stage.operator();
                boolean captureOutput = (op == Operator.PIPE || op == Operator.FLIP);

                // Redirect streams before execution
                PrintStream originalOut = session.out();
                PrintStream originalErr = session.err();
                ByteArrayOutputStream capture = null;
                OutputStream redirectStream = null;

                if (captureOutput) {
                    capture = new ByteArrayOutputStream();
                    session.setOut(new PrintStream(capture));
                } else {
                    PrintStream[] outErr = {originalOut, originalErr};
                    redirectStream = setupRedirectStreams(op, stage.redirectTarget(), outErr);
                    if (redirectStream != null) {
                        session.setOut(outErr[0]);
                        session.setErr(outErr[1]);
                    }
                }

                try {
                    lastResult = cmd.execute(session, args);
                    if (captureOutput) {
                        session.out().flush();
                    }
                    lastOutput = resolveOutputString(captureOutput ? capture : null, lastResult);
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
                    session.setOut(originalOut);
                    session.setErr(originalErr);
                    if (redirectStream != null) {
                        redirectStream.close();
                    }
                    if (inputRedirect != null) {
                        session.setIn(originalIn);
                        inputRedirect.close();
                    }
                }
            }

            // Post-execution: update result tracking based on trailing operator
            if (lastGroupOp == Operator.REDIRECT
                    || lastGroupOp == Operator.APPEND
                    || lastGroupOp == Operator.STDERR_REDIRECT
                    || lastGroupOp == Operator.COMBINED_REDIRECT) {
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
        resolveAllCommands(groupStages, flipArgs, pumps, commands, argsArrays);

        // Set up input redirects (tracked for cleanup)
        List<InputStream> inputRedirects = new ArrayList<>();
        InputStream firstIn = setupInputRedirect(groupStages.get(0), session.in(), inputRedirects);

        // Set up last stage output/error redirection
        boolean captureLastOutput = (lastGroupOp == Operator.PIPE || lastGroupOp == Operator.FLIP);
        ByteArrayOutputStream lastCapture = captureLastOutput ? new ByteArrayOutputStream() : null;
        PrintStream[] outErr = {captureLastOutput ? new PrintStream(lastCapture) : session.out(), session.err()};
        OutputStream redirectStream =
                captureLastOutput ? null : setupRedirectStreams(lastGroupOp, lastGroupStage.redirectTarget(), outErr);

        // Create per-stage sessions and start producer threads
        CommandSession[] stageSessions =
                createStageSessions(groupStages, pumps, firstIn, outErr[0], outErr[1], inputRedirects);
        Thread[] threads = startProducers(commands, stageSessions, argsArrays);

        // Run last stage in current thread
        Object lastResult;
        String lastOutput;
        try {
            lastResult = commands[n - 1].execute(stageSessions[n - 1], argsArrays[n - 1]);
            if (captureLastOutput) {
                stageSessions[n - 1].out().flush();
            }
            lastOutput = resolveOutputString(captureLastOutput ? lastCapture : null, lastResult);
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
            closePumps(pumps);
            joinProducers(threads);
            if (redirectStream != null) {
                outErr[0].flush();
                outErr[1].flush();
            }
            closeStreams(redirectStream, inputRedirects);
        }

        return new Object[] {lastResult, lastOutput};
    }

    /**
     * Resolves commands and arguments for all stages in a pipe group.
     * On failure, closes pumps before throwing.
     */
    private void resolveAllCommands(
            List<Pipeline.Stage> groupStages,
            String flipArgs,
            PipePumpInputStream[] pumps,
            Command[] commands,
            String[][] argsArrays) {
        for (int s = 0; s < groupStages.size(); s++) {
            String cmdLine = groupStages.get(s).commandLine();
            if (cmdLine == null || cmdLine.trim().isEmpty()) {
                closePumps(pumps);
                throw new IllegalArgumentException("Empty command in pipeline");
            }
            try {
                Object[] resolved = resolveCommand(cmdLine, s == 0 ? flipArgs : null);
                commands[s] = (Command) resolved[0];
                argsArrays[s] = (String[]) resolved[1];
            } catch (IllegalArgumentException e) {
                closePumps(pumps);
                throw e;
            }
        }
    }

    /**
     * Sets up input redirection for a stage, tracking the opened stream for cleanup.
     * Returns the redirect stream if the stage has an input source, or the default otherwise.
     */
    private static InputStream setupInputRedirect(
            Pipeline.Stage stage, InputStream defaultIn, List<InputStream> inputRedirects) throws IOException {
        if (stage.inputSource() != null) {
            InputStream redirect = Files.newInputStream(stage.inputSource());
            inputRedirects.add(redirect);
            return redirect;
        }
        return defaultIn;
    }

    /**
     * Creates per-stage sessions with wired I/O for a pipe group.
     */
    private CommandSession[] createStageSessions(
            List<Pipeline.Stage> groupStages,
            PipePumpInputStream[] pumps,
            InputStream firstIn,
            PrintStream lastOut,
            PrintStream lastErr,
            List<InputStream> inputRedirects)
            throws IOException {
        int n = groupStages.size();
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

            // Per-stage input redirect overrides pipe input (non-first stages)
            if (s > 0) {
                stageIn = setupInputRedirect(groupStages.get(s), stageIn, inputRedirects);
            }

            stageSessions[s] = session.fork(stageIn, stageOut, stageErr);
        }
        return stageSessions;
    }

    /**
     * Creates and starts producer threads for all stages except the last.
     */
    private static Thread[] startProducers(Command[] commands, CommandSession[] stageSessions, String[][] argsArrays) {
        Thread[] threads = new Thread[commands.length - 1];
        for (int t = 0; t < threads.length; t++) {
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
        return threads;
    }

    /**
     * Configures output/error redirect streams based on the operator.
     * If a redirect applies, the corresponding entry in {@code outErr} is replaced.
     *
     * @return the underlying redirect OutputStream (caller must close), or null if no redirect
     */
    private static OutputStream setupRedirectStreams(Operator op, Path target, PrintStream[] outErr)
            throws IOException {
        if (target == null) {
            return null;
        }
        OutputStream rs = openRedirectStream(op, target);
        switch (op) {
            case REDIRECT:
            case APPEND:
                outErr[0] = new PrintStream(rs);
                return rs;
            case STDERR_REDIRECT:
                outErr[1] = new PrintStream(rs);
                return rs;
            case COMBINED_REDIRECT:
                PrintStream combined = new PrintStream(rs);
                outErr[0] = combined;
                outErr[1] = combined;
                return rs;
            default:
                rs.close();
                return null;
        }
    }

    /**
     * Extracts the output string from a captured stream or command result.
     */
    private static String resolveOutputString(ByteArrayOutputStream capture, Object result) {
        if (capture != null) {
            String captured = capture.toString().trim();
            if (!captured.isEmpty()) {
                return captured;
            }
        }
        return result != null ? result.toString() : null;
    }

    /**
     * Parses a command line, resolves the command and any subcommand, and applies
     * optional flip arguments. Returns a two-element array: {@code [Command, String[]]}.
     */
    private Object[] resolveCommand(String cmdLine, String flipArgs) {
        String[] parts = cmdLine.trim().split("\\s+", 2);
        String cmdName = parts[0];
        String argsStr = parts.length > 1 ? parts[1] : "";

        if (flipArgs != null) {
            argsStr = argsStr.isEmpty() ? flipArgs : argsStr + " " + flipArgs;
        }

        Command cmd = findCommand(cmdName);
        if (cmd == null) {
            throw new IllegalArgumentException("Unknown command: " + cmdName);
        }

        String[] args = argsStr.isEmpty() ? new String[0] : argsStr.split("\\s+");

        if (args.length > 0 && !cmd.subcommands().isEmpty()) {
            Command sub = cmd.subcommands().get(args[0]);
            if (sub != null) {
                cmd = sub;
                args = Arrays.copyOfRange(args, 1, args.length);
            }
        }

        return new Object[] {cmd, args};
    }

    /**
     * Opens an output stream for a redirect operator.
     */
    private static OutputStream openRedirectStream(Operator op, Path target) throws IOException {
        if (op == Operator.APPEND) {
            return Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        return Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Waits for all producer threads to complete, handling interruption gracefully.
     */
    private static void joinProducers(Thread[] threads) {
        boolean interrupted = false;
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Closes redirect and input streams, suppressing IOExceptions.
     */
    private static void closeStreams(OutputStream redirectStream, List<InputStream> inputRedirects) {
        if (redirectStream != null) {
            try {
                redirectStream.close();
            } catch (IOException ignored) {
            }
        }
        for (InputStream is : inputRedirects) {
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void closePumps(PipePumpInputStream[] pumps) {
        for (PipePumpInputStream pump : pumps) {
            try {
                pump.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Creates a completer that provides completions for every registered command name and its aliases.
     *
     * Builds a SystemCompleter, registers a completer for each command and each of its alias strings, and
     * finalizes it into a candidate-producing completer.
     *
     * @return a Completer that offers argument and command-name completions for all registered commands and aliases
     */
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
     * Determines whether a trimmed line is a bare variable assignment of the form NAME=VALUE.
     *
     * The name portion must contain no spaces, start with a letter or underscore, and consist only of letters,
     * digits, or underscores.
     *
     * @param line the trimmed input line
     * @return {@code true} if the line matches `NAME=VALUE` with a valid name, {@code false} otherwise
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
        /**
         * No-op override that permits reading any buffered bytes after the pump is closed.
         *
         * This prevents the stream from enforcing a strict closed state so consumers can
         * drain remaining data instead of encountering an immediate closed error.
         */
        @Override
        protected void checkClosed() throws IOException {
            // Allow reads on a closed pump so buffered data can be drained.
            // The internal wait() method returns data when the read buffer has
            // remaining bytes, or EOF once the buffer is empty and closed is true.
        }
    }

    /**
     * No-op default implementation; does nothing.
     *
     * Subclasses may override to release or close allocated resources.
     */
    @Override
    public void close() {
        // Nothing to close by default
    }
}

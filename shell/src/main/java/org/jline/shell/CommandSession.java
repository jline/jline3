/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jline.terminal.Terminal;

/**
 * Encapsulates the execution context for a command, including the terminal,
 * I/O streams, session variables, working directory, and job state.
 *
 * @see Command#execute(CommandSession, String[])
 * @since 4.0
 */
public class CommandSession {

    private final Terminal terminal;
    private final InputStream in;
    private PrintStream out;
    private final PrintStream err;
    private final Map<String, Object> variables;
    private Path workingDirectory;
    private int lastExitCode;
    private Job foregroundJob;

    /**
     * Creates a new command session with the system's standard I/O streams.
     * The terminal will be null.
     */
    public CommandSession() {
        this.in = System.in;
        this.out = System.out;
        this.err = System.err;
        this.terminal = null;
        this.variables = new LinkedHashMap<>();
    }

    /**
     * Creates a new command session with the specified terminal.
     * I/O streams are derived from the terminal.
     *
     * @param terminal the terminal
     */
    public CommandSession(Terminal terminal) {
        this(terminal, terminal.input(), new PrintStream(terminal.output()), new PrintStream(terminal.output()));
    }

    /**
     * Creates a new command session with the specified terminal and I/O streams.
     *
     * @param terminal the terminal
     * @param in the input stream
     * @param out the output stream
     * @param err the error stream
     */
    public CommandSession(Terminal terminal, InputStream in, PrintStream out, PrintStream err) {
        this.terminal = terminal;
        this.in = in;
        this.out = out;
        this.err = err;
        this.variables = new LinkedHashMap<>();
    }

    /**
     * Returns the terminal for this session.
     *
     * @return the terminal, or null if not associated
     */
    public Terminal terminal() {
        return terminal;
    }

    /**
     * Returns the input stream.
     *
     * @return the input stream
     */
    public InputStream in() {
        return in;
    }

    /**
     * Returns the output stream.
     *
     * @return the output stream
     */
    public PrintStream out() {
        return out;
    }

    /**
     * Sets the output stream.
     *
     * @param out the new output stream
     */
    public void setOut(PrintStream out) {
        this.out = out;
    }

    /**
     * Returns the error stream.
     *
     * @return the error stream
     */
    public PrintStream err() {
        return err;
    }

    /**
     * Returns the value of a session variable.
     *
     * @param name the variable name
     * @return the value, or null if not set
     */
    public Object get(String name) {
        return variables.get(name);
    }

    /**
     * Sets a session variable.
     *
     * @param name the variable name
     * @param value the value
     */
    public void put(String name, Object value) {
        variables.put(name, value);
    }

    /**
     * Returns all session variables.
     *
     * @return unmodifiable view of the variables
     */
    public Map<String, Object> variables() {
        return Collections.unmodifiableMap(variables);
    }

    /**
     * Returns the working directory.
     *
     * @return the working directory, or null
     */
    public Path workingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the working directory.
     *
     * @param workingDirectory the working directory
     */
    public void setWorkingDirectory(Path workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    /**
     * Returns the exit code of the last executed command.
     *
     * @return the last exit code
     */
    public int lastExitCode() {
        return lastExitCode;
    }

    /**
     * Sets the exit code of the last executed command.
     *
     * @param lastExitCode the exit code
     */
    public void setLastExitCode(int lastExitCode) {
        this.lastExitCode = lastExitCode;
    }

    /**
     * Returns the current foreground job.
     *
     * @return the foreground job, or null
     */
    public Job foregroundJob() {
        return foregroundJob;
    }

    /**
     * Sets the current foreground job.
     *
     * @param foregroundJob the foreground job
     */
    public void setForegroundJob(Job foregroundJob) {
        this.foregroundJob = foregroundJob;
    }
}

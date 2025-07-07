/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jline.reader.LineReader;
import org.jline.terminal.Terminal;

/**
 * Default implementation of CommandContext.
 */
public class DefaultCommandContext implements CommandContext {

    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;
    private final Path currentDir;
    private final Terminal terminal;
    private final LineReader lineReader;
    private final SystemRegistry systemRegistry;
    private final Map<String, Object> variables;
    private final Function<String, Object> variableProvider;

    protected DefaultCommandContext(Builder builder) {
        this.terminal = builder.terminal;
        this.in = builder.in != null ? builder.in : (terminal != null ? terminal.input() : System.in);
        this.out = builder.out != null ? builder.out : 
                   (terminal != null ? new PrintStream(terminal.output()) : System.out);
        this.err = builder.err != null ? builder.err : 
                   (terminal != null ? new PrintStream(terminal.output()) : System.err);
        this.currentDir = builder.currentDir != null ? builder.currentDir : Paths.get(".");
        this.lineReader = builder.lineReader;
        this.systemRegistry = builder.systemRegistry;
        this.variables = new ConcurrentHashMap<>(builder.environment);
        this.variableProvider = builder.variableProvider;
    }

    @Override
    public InputStream in() {
        return in;
    }

    @Override
    public PrintStream out() {
        return out;
    }

    @Override
    public PrintStream err() {
        return err;
    }

    @Override
    public Path currentDir() {
        return currentDir;
    }

    @Override
    public Terminal terminal() {
        return terminal;
    }

    @Override
    public Object get(String name) {
        Object value = variables.get(name);
        if (value == null && variableProvider != null) {
            value = variableProvider.apply(name);
        }
        return value;
    }

    @Override
    public void set(String name, Object value) {
        variables.put(name, value);
    }

    @Override
    public Map<String, Object> getEnvironment() {
        return new HashMap<>(variables);
    }

    @Override
    public LineReader lineReader() {
        return lineReader;
    }

    @Override
    public SystemRegistry systemRegistry() {
        return systemRegistry;
    }

    @Override
    public CommandContext withCurrentDirectory(Path newCurrentDir) {
        return new Builder(this).currentDir(newCurrentDir).build();
    }

    @Override
    public CommandContext withVariables(Map<String, Object> newVariables) {
        return new Builder(this).environment(newVariables).build();
    }

    /**
     * Builder for DefaultCommandContext.
     */
    public static class Builder implements CommandContext.Builder {
        private Terminal terminal;
        private InputStream in;
        private PrintStream out;
        private PrintStream err;
        private Path currentDir;
        private LineReader lineReader;
        private SystemRegistry systemRegistry;
        private Map<String, Object> environment = new HashMap<>();
        private Function<String, Object> variableProvider;

        public Builder() {
            // Initialize with system environment
            System.getenv().forEach(environment::put);
        }

        public Builder(DefaultCommandContext existing) {
            this.terminal = existing.terminal;
            this.in = existing.in;
            this.out = existing.out;
            this.err = existing.err;
            this.currentDir = existing.currentDir;
            this.lineReader = existing.lineReader;
            this.systemRegistry = existing.systemRegistry;
            this.environment = new HashMap<>(existing.variables);
            this.variableProvider = existing.variableProvider;
        }

        @Override
        public Builder terminal(Terminal terminal) {
            this.terminal = terminal;
            return this;
        }

        public Builder in(InputStream in) {
            this.in = in;
            return this;
        }

        public Builder out(PrintStream out) {
            this.out = out;
            return this;
        }

        public Builder err(PrintStream err) {
            this.err = err;
            return this;
        }

        @Override
        public Builder currentDir(Path currentDir) {
            this.currentDir = currentDir;
            return this;
        }

        @Override
        public Builder environment(Map<String, Object> environment) {
            this.environment.putAll(environment);
            return this;
        }

        @Override
        public Builder lineReader(LineReader lineReader) {
            this.lineReader = lineReader;
            return this;
        }

        @Override
        public Builder systemRegistry(SystemRegistry systemRegistry) {
            this.systemRegistry = systemRegistry;
            return this;
        }

        @Override
        public Builder variableProvider(Function<String, Object> variableProvider) {
            this.variableProvider = variableProvider;
            return this;
        }

        @Override
        public CommandContext build() {
            return new DefaultCommandContext(this);
        }
    }
}

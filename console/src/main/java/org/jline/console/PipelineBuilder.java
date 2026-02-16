/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jline.console.impl.DefaultPipeline;

/**
 * Fluent builder for constructing {@link Pipeline} instances programmatically.
 * <p>
 * Example:
 * <pre>
 * Pipeline pipeline = Pipeline.of("ls -la")
 *     .pipe("grep pattern")
 *     .redirect(Paths.get("output.txt"))
 *     .build();
 * </pre>
 *
 * @see Pipeline
 */
public class PipelineBuilder {

    private final List<DefaultPipeline.DefaultStage> stages = new ArrayList<>();
    private String currentCommand;
    private boolean background = false;
    private final StringBuilder source = new StringBuilder();

    /**
     * Creates a new builder with the first command.
     *
     * @param command the first command in the pipeline
     */
    PipelineBuilder(String command) {
        this.currentCommand = command;
        this.source.append(command);
    }

    /**
     * Adds a pipe ({@code |}) to the next command.
     *
     * @param command the next command to pipe to
     * @return this builder
     */
    public PipelineBuilder pipe(String command) {
        stages.add(new DefaultPipeline.DefaultStage(currentCommand, Pipeline.Operator.PIPE, null, false));
        source.append(" | ").append(command);
        currentCommand = command;
        return this;
    }

    /**
     * Adds a flip pipe ({@code |;}) to the next command.
     * The output of the previous command is appended as an argument to the next.
     *
     * @param command the next command
     * @return this builder
     */
    public PipelineBuilder flip(String command) {
        stages.add(new DefaultPipeline.DefaultStage(currentCommand, Pipeline.Operator.FLIP, null, false));
        source.append(" |; ").append(command);
        currentCommand = command;
        return this;
    }

    /**
     * Adds a conditional AND ({@code &&}) to the next command.
     * The next command executes only if the previous succeeded.
     *
     * @param command the next command
     * @return this builder
     */
    public PipelineBuilder and(String command) {
        stages.add(new DefaultPipeline.DefaultStage(currentCommand, Pipeline.Operator.AND, null, false));
        source.append(" && ").append(command);
        currentCommand = command;
        return this;
    }

    /**
     * Adds a conditional OR ({@code ||}) to the next command.
     * The next command executes only if the previous failed.
     *
     * @param command the next command
     * @return this builder
     */
    public PipelineBuilder or(String command) {
        stages.add(new DefaultPipeline.DefaultStage(currentCommand, Pipeline.Operator.OR, null, false));
        source.append(" || ").append(command);
        currentCommand = command;
        return this;
    }

    /**
     * Adds an output redirection ({@code >}) to the specified file.
     *
     * @param file the file to redirect output to
     * @return this builder
     */
    public PipelineBuilder redirect(Path file) {
        stages.add(new DefaultPipeline.DefaultStage(currentCommand, Pipeline.Operator.REDIRECT, file, false));
        source.append(" > ").append(file);
        currentCommand = null;
        return this;
    }

    /**
     * Adds an append redirection ({@code >>}) to the specified file.
     *
     * @param file the file to append output to
     * @return this builder
     */
    public PipelineBuilder append(Path file) {
        stages.add(new DefaultPipeline.DefaultStage(currentCommand, Pipeline.Operator.APPEND, file, true));
        source.append(" >> ").append(file);
        currentCommand = null;
        return this;
    }

    /**
     * Marks this pipeline for background execution.
     *
     * @return this builder
     */
    public PipelineBuilder background() {
        this.background = true;
        source.append(" &");
        return this;
    }

    /**
     * Builds the pipeline.
     *
     * @return the constructed pipeline
     */
    public Pipeline build() {
        // Add the final stage (no operator after last command)
        if (currentCommand != null) {
            stages.add(new DefaultPipeline.DefaultStage(currentCommand, null, null, false));
        }
        return new DefaultPipeline(
                Collections.unmodifiableList(new ArrayList<>(stages)), source.toString(), background);
    }
}

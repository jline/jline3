/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.nio.file.Path;
import java.util.List;

import org.jline.shell.Pipeline;

/**
 * Default implementation of {@link Pipeline}.
 * <p>
 * A pipeline is an ordered list of {@link Stage}s connected by {@link Operator}s.
 * Instances are immutable and created via {@link Pipeline#of(String)} or
 * {@link PipelineParser#parse(String)}.
 *
 * @since 4.0
 */
public class DefaultPipeline implements Pipeline {

    private final List<Stage> stages;
    private final String source;
    private final boolean background;

    /**
     * Creates a new pipeline.
     *
     * @param stages the ordered stages
     * @param source the original command line
     * @param background whether to execute in background
     */
    public DefaultPipeline(List<Stage> stages, String source, boolean background) {
        this.stages = stages;
        this.source = source;
        this.background = background;
    }

    @Override
    public List<Stage> stages() {
        return stages;
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public boolean isBackground() {
        return background;
    }

    @Override
    public String toString() {
        return source;
    }

    /**
     * Default implementation of {@link Stage}.
     */
    public static class DefaultStage implements Stage {
        private final String commandLine;
        private final Operator operator;
        private final Path redirectTarget;
        private final boolean append;
        private final Path inputSource;

        /**
         * Creates a new stage.
         *
         * @param commandLine the command line for this stage
         * @param operator the operator following this stage (null for last stage)
         * @param redirectTarget the redirect file (for REDIRECT/APPEND operators)
         * @param append whether to append (for APPEND operator)
         */
        public DefaultStage(String commandLine, Operator operator, Path redirectTarget, boolean append) {
            this(commandLine, operator, redirectTarget, append, null);
        }

        /**
         * Creates a new stage with input source.
         *
         * @param commandLine the command line for this stage
         * @param operator the operator following this stage (null for last stage)
         * @param redirectTarget the redirect file (for REDIRECT/APPEND/STDERR_REDIRECT/COMBINED_REDIRECT operators)
         * @param append whether to append (for APPEND operator)
         * @param inputSource the input source file (for INPUT_REDIRECT operator)
         */
        public DefaultStage(
                String commandLine, Operator operator, Path redirectTarget, boolean append, Path inputSource) {
            this.commandLine = commandLine;
            this.operator = operator;
            this.redirectTarget = redirectTarget;
            this.append = append;
            this.inputSource = inputSource;
        }

        @Override
        public String commandLine() {
            return commandLine;
        }

        @Override
        public Operator operator() {
            return operator;
        }

        @Override
        public Path redirectTarget() {
            return redirectTarget;
        }

        @Override
        public boolean isAppend() {
            return append;
        }

        @Override
        public Path inputSource() {
            return inputSource;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(commandLine);
            if (operator != null) {
                sb.append(" ").append(operator.symbol());
                if (redirectTarget != null) {
                    sb.append(" ").append(redirectTarget);
                }
            }
            return sb.toString();
        }
    }
}

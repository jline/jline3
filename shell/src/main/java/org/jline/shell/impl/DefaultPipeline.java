/*
 * Copyright (c) the original author(s).
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
     * <p>
     * Redirect targets and input sources are stored as raw strings to avoid calling
     * {@code Path.of()} on unexpanded tokens that may contain shell syntax characters
     * (such as {@code :} in {@code ${VAR:-default.txt}}) that are invalid in platform
     * paths on Windows. {@code Path} objects are derived on demand via the
     * {@link #redirectTarget()} and {@link #inputSource()} getters, which must only be
     * called after variable expansion has been applied to the raw strings.
     */
    public static class DefaultStage implements Stage {
        private final String commandLine;
        private final Operator operator;
        private final String redirectTargetString;
        private final boolean append;
        private final String inputSourceString;

        /**
         * Creates a new stage.
         *
         * @param commandLine the command line for this stage
         * @param operator the operator following this stage (null for last stage)
         * @param redirectTarget the redirect file (for REDIRECT/APPEND operators)
         * @param append whether to append (for APPEND operator)
         */
        public DefaultStage(String commandLine, Operator operator, Path redirectTarget, boolean append) {
            this(commandLine, operator, redirectTarget, append, (Path) null);
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
            this.redirectTargetString = redirectTarget != null ? redirectTarget.toString() : null;
            this.append = append;
            this.inputSourceString = inputSource != null ? inputSource.toString() : null;
        }

        /**
         * Creates a new stage with raw string redirect target and input source.
         * <p>
         * Use this constructor when the target strings have not yet been through variable
         * expansion and may contain shell syntax that is invalid as a platform path
         * (e.g. {@code ${VAR:-fallback.txt}} on Windows).
         *
         * @param commandLine the command line for this stage
         * @param operator the operator following this stage (null for last stage)
         * @param redirectTargetString the raw redirect target string, or null
         * @param append whether to append (for APPEND operator)
         * @param inputSourceString the raw input source string, or null
         */
        public DefaultStage(
                String commandLine,
                Operator operator,
                String redirectTargetString,
                boolean append,
                String inputSourceString) {
            this.commandLine = commandLine;
            this.operator = operator;
            this.redirectTargetString = redirectTargetString;
            this.append = append;
            this.inputSourceString = inputSourceString;
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
        public String redirectTargetString() {
            return redirectTargetString;
        }

        @Override
        public Path redirectTarget() {
            return redirectTargetString != null ? Path.of(redirectTargetString) : null;
        }

        @Override
        public boolean isAppend() {
            return append;
        }

        @Override
        public String inputSourceString() {
            return inputSourceString;
        }

        @Override
        public Path inputSource() {
            return inputSourceString != null ? Path.of(inputSourceString) : null;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(commandLine);
            if (operator != null) {
                sb.append(" ").append(operator.symbol());
                if (redirectTargetString != null) {
                    sb.append(" ").append(redirectTargetString);
                }
            }
            return sb.toString();
        }
    }
}

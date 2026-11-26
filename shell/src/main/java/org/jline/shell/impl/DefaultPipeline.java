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
     * Redirect targets and input sources can be stored either as resolved {@link Path}
     * objects (via the {@code Path}-based constructors, used by the {@link PipelineBuilder})
     * or as raw strings (via {@link #fromRawStrings}, used by {@link PipelineParser}).
     * <p>
     * The raw-string path is used by {@link PipelineParser} to avoid calling
     * {@code Path.of()} on unexpanded tokens that may contain shell syntax characters
     * (such as {@code :} in {@code ${VAR:-default.txt}}) that are invalid in platform
     * paths on Windows. The dispatcher expands variables in the raw string before
     * constructing a {@link Path}.
     */
    public static class DefaultStage implements Stage {
        private final String commandLine;
        private final Operator operator;
        /** Raw redirect-target string; null when path is known. */
        private final String redirectTargetString;
        /** Resolved redirect-target path; null when raw string is stored. */
        private final Path redirectTargetPath;

        private final boolean append;
        /** Raw input-source string; null when path is known. */
        private final String inputSourceString;
        /** Resolved input-source path; null when raw string is stored. */
        private final Path inputSourcePath;

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
            // Store the supplied Path objects directly so that non-default-filesystem paths
            // (e.g. from a ZIP FileSystem) are preserved exactly and are not silently
            // rebuilt against the default file system by a later Path.of(toString()) call.
            this.redirectTargetPath = redirectTarget;
            this.redirectTargetString = redirectTarget != null ? redirectTarget.toString() : null;
            this.append = append;
            this.inputSourcePath = inputSource;
            this.inputSourceString = inputSource != null ? inputSource.toString() : null;
        }

        private DefaultStage(
                String commandLine,
                Operator operator,
                String redirectTargetString,
                boolean append,
                String inputSourceString) {
            this.commandLine = commandLine;
            this.operator = operator;
            this.redirectTargetString = redirectTargetString;
            this.redirectTargetPath = null;
            this.append = append;
            this.inputSourceString = inputSourceString;
            this.inputSourcePath = null;
        }

        /**
         * Creates a stage that holds redirect and input-source targets as raw, unexpanded
         * strings instead of resolved {@link Path} objects.
         * <p>
         * Use this factory when the target tokens have not yet been through variable
         * expansion and may contain shell syntax that is invalid as a platform path
         * (e.g. {@code ${VAR:-fallback.txt}} on Windows, where {@code :} is illegal
         * outside a drive-letter prefix). The dispatcher must call
         * {@link #redirectTargetString()} / {@link #inputSourceString()} to expand
         * variables before constructing the final {@link Path}.
         *
         * @param commandLine the command line for this stage
         * @param operator the operator following this stage (null for last stage)
         * @param redirectTargetString the raw redirect target string, or null
         * @param append whether to append (for APPEND operator)
         * @param inputSourceString the raw input source string, or null
         * @return a new stage backed by raw strings
         */
        public static DefaultStage fromRawStrings(
                String commandLine,
                Operator operator,
                String redirectTargetString,
                boolean append,
                String inputSourceString) {
            return new DefaultStage(commandLine, operator, redirectTargetString, append, inputSourceString);
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
            if (redirectTargetPath != null) {
                return redirectTargetPath;
            }
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
            if (inputSourcePath != null) {
                return inputSourcePath;
            }
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

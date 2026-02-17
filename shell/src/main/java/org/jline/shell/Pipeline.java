/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.nio.file.Path;
import java.util.List;

/**
 * Represents a pipeline of commands connected by operators such as pipes,
 * redirections, and conditional connectors.
 * <p>
 * A pipeline is built by chaining commands with operators:
 * <pre>
 * Pipeline pipeline = Pipeline.of("ls")
 *     .pipe("grep pattern")       // |
 *     .redirect(Paths.get("out")) // &gt; out
 *     .build();
 * </pre>
 * <p>
 * Pipelines are immutable once built. Use {@link #of(String)} to start
 * building a pipeline.
 *
 * @see Pipeline.Operator
 * @since 4.0
 */
public interface Pipeline {

    /**
     * Operators that connect commands in a pipeline.
     */
    enum Operator {
        /** Standard pipe: sends output of left command to right command ({@code |}) */
        PIPE("|"),
        /** Flip pipe: appends output as argument to next command ({@code |;}) */
        FLIP("|;"),
        /** Logical AND: execute next command only if previous succeeded ({@code &&}) */
        AND("&&"),
        /** Logical OR: execute next command only if previous failed ({@code ||}) */
        OR("||"),
        /** Output redirection: write output to file ({@code >}) */
        REDIRECT(">"),
        /** Append redirection: append output to file ({@code >>}) */
        APPEND(">>"),
        /** Sequence: execute next command unconditionally ({@code ;}) */
        SEQUENCE(";");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        /**
         * Returns the string symbol for this operator.
         *
         * @return the operator symbol
         */
        public String symbol() {
            return symbol;
        }

        /**
         * Returns the operator matching the given symbol, or {@code null} if none matches.
         *
         * @param symbol the symbol to look up
         * @return the matching operator, or null
         */
        public static Operator fromSymbol(String symbol) {
            for (Operator op : values()) {
                if (op.symbol.equals(symbol)) {
                    return op;
                }
            }
            return null;
        }
    }

    /**
     * A single stage in a pipeline, consisting of a command line and the
     * operator that follows it.
     */
    interface Stage {
        /**
         * Returns the command line for this stage.
         *
         * @return the command line string
         */
        String commandLine();

        /**
         * Returns the operator that follows this stage, or {@code null} for the last stage.
         *
         * @return the operator, or null
         */
        Operator operator();

        /**
         * Returns the redirect target path, if this stage has a REDIRECT or APPEND operator.
         *
         * @return the redirect path, or null
         */
        Path redirectTarget();

        /**
         * Returns whether this is an append redirection ({@code >>}).
         *
         * @return true if append
         */
        boolean isAppend();
    }

    /**
     * Returns the ordered list of stages in this pipeline.
     *
     * @return the stages
     */
    List<Stage> stages();

    /**
     * Returns the original command line string that was parsed to create this pipeline.
     *
     * @return the source command line
     */
    String source();

    /**
     * Returns whether this pipeline should execute in the background.
     *
     * @return true if background execution was requested
     */
    boolean isBackground();

    /**
     * Starts building a new pipeline with the given command.
     *
     * @param command the first command in the pipeline
     * @return a new pipeline builder
     */
    static PipelineBuilder of(String command) {
        return new PipelineBuilder(command);
    }
}

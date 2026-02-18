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
import java.nio.file.Paths;
import java.util.*;

import org.jline.shell.Pipeline;
import org.jline.shell.Pipeline.Operator;

/**
 * Parses a command line string into a {@link Pipeline}.
 * <p>
 * By default, the parser recognizes all operators defined in {@link Operator},
 * using their default symbols. Custom symbol mappings can be provided via the
 * {@link #PipelineParser(Map)} constructor to override default symbols:
 * <pre>
 * // Replace &gt; and &gt;&gt; with |&gt; and |&gt;&gt; (e.g. for Groovy)
 * new PipelineParser(Map.of("|&gt;", Operator.REDIRECT, "|&gt;&gt;", Operator.APPEND))
 * </pre>
 * When a custom mapping targets an {@link Operator} that already has a default symbol,
 * the default symbol is replaced. This allows languages where {@code >} has a different
 * meaning to use alternative redirect syntax.
 * <p>
 * The parser respects quoting (single and double) and bracket nesting,
 * so operators inside quoted strings or brackets are not treated as pipeline operators.
 * <p>
 * Subclasses can override {@link #matchOperator(String, int)} to customize operator
 * matching beyond what the constructor-based approach supports.
 *
 * @since 4.0
 */
public class PipelineParser {

    /**
     * The operator table: symbol to Operator, sorted by symbol length descending
     * for longest-match semantics.
     */
    private final Map<String, Operator> operators;

    /**
     * Creates a new PipelineParser with default operator symbols from {@link Operator}.
     */
    public PipelineParser() {
        this(Collections.emptyMap());
    }

    /**
     * Creates a new PipelineParser with custom operator symbol mappings.
     * <p>
     * Each entry maps a symbol string to an {@link Operator}. If a custom mapping
     * targets an operator that already has a default symbol, the default symbol is
     * removed and replaced by the custom one. For example:
     * <pre>
     * // "|&gt;" replaces "&gt;" for REDIRECT, "|&gt;&gt;" replaces "&gt;&gt;" for APPEND
     * new PipelineParser(Map.of("|&gt;", Operator.REDIRECT, "|&gt;&gt;", Operator.APPEND))
     * </pre>
     * Operators not mentioned in the custom map keep their default symbols.
     *
     * @param customOperators a map from operator symbol to {@link Operator}
     */
    public PipelineParser(Map<String, Operator> customOperators) {
        // Collect which Operator values are overridden by custom mappings
        Set<Operator> overridden = EnumSet.noneOf(Operator.class);
        overridden.addAll(customOperators.values());

        // Build the operator table: defaults first (excluding overridden), then custom
        Map<String, Operator> table = new LinkedHashMap<>();
        for (Operator op : Operator.values()) {
            if (!overridden.contains(op)) {
                table.put(op.symbol(), op);
            }
        }
        table.putAll(customOperators);

        // Sort by symbol length descending for longest-match semantics
        TreeMap<String, Operator> sorted =
                new TreeMap<>(Comparator.comparingInt(String::length).reversed().thenComparing(s -> s));
        sorted.putAll(table);
        this.operators = Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }

    /**
     * Parses a command line into a {@link Pipeline}.
     *
     * @param line the command line to parse
     * @return the parsed pipeline
     */
    public Pipeline parse(String line) {
        if (line == null || line.trim().isEmpty()) {
            return new DefaultPipeline(
                    Collections.singletonList(new DefaultPipeline.DefaultStage("", null, null, false)), line, false);
        }

        String trimmed = line.trim();
        boolean background = false;

        // Check for background operator at end
        if (trimmed.endsWith("&") && !trimmed.endsWith("&&")) {
            background = true;
            trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
        }

        List<DefaultPipeline.DefaultStage> stages = new ArrayList<>();
        List<Token> tokens = tokenize(trimmed);

        if (tokens.isEmpty()) {
            return new DefaultPipeline(
                    Collections.singletonList(new DefaultPipeline.DefaultStage("", null, null, false)),
                    line,
                    background);
        }

        StringBuilder currentCmd = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.isOperator) {
                Operator op = operators.get(token.value);
                if (op == null) {
                    // Unknown operator, treat as text
                    currentCmd.append(token.value);
                    continue;
                }

                String cmd = currentCmd.toString().trim();
                currentCmd.setLength(0);

                if (op == Operator.REDIRECT
                        || op == Operator.APPEND
                        || op == Operator.STDERR_REDIRECT
                        || op == Operator.COMBINED_REDIRECT) {
                    // Next token should be the file path
                    Path target = null;
                    if (i + 1 < tokens.size()) {
                        i++;
                        target = Paths.get(tokens.get(i).value.trim());
                    }
                    stages.add(new DefaultPipeline.DefaultStage(cmd, op, target, op == Operator.APPEND));
                } else if (op == Operator.INPUT_REDIRECT) {
                    // Next token should be the input file path
                    Path inputFile = null;
                    if (i + 1 < tokens.size()) {
                        i++;
                        inputFile = Paths.get(tokens.get(i).value.trim());
                    }
                    stages.add(new DefaultPipeline.DefaultStage(cmd, null, null, false, inputFile));
                } else if (op == Operator.HEREDOC) {
                    // Next token should be the delimiter; for now treat similarly to input redirect
                    if (i + 1 < tokens.size()) {
                        i++;
                        // HEREDOC is deferred to follow-up; store delimiter as-is for now
                    }
                    stages.add(new DefaultPipeline.DefaultStage(cmd, op, null, false));
                } else {
                    stages.add(new DefaultPipeline.DefaultStage(cmd, op, null, false));
                }
            } else {
                if (currentCmd.length() > 0) {
                    currentCmd.append(" ");
                }
                currentCmd.append(token.value);
            }
        }

        // Add final stage
        String remaining = currentCmd.toString().trim();
        if (!remaining.isEmpty() || stages.isEmpty()) {
            stages.add(new DefaultPipeline.DefaultStage(remaining, null, null, false));
        }

        return new DefaultPipeline(Collections.unmodifiableList(stages), line, background);
    }

    /**
     * Tokenizes a command line into text and operator tokens.
     * Respects quoting and bracket nesting.
     */
    private List<Token> tokenize(String line) {
        List<Token> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int i = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int bracketDepth = 0; // (), [], {}

        while (i < line.length()) {
            char c = line.charAt(i);

            // Handle escape
            if (c == '\\' && i + 1 < line.length() && !inSingleQuote) {
                current.append(c);
                current.append(line.charAt(i + 1));
                i += 2;
                continue;
            }

            // Handle quotes
            if (c == '\'' && !inDoubleQuote && bracketDepth == 0) {
                inSingleQuote = !inSingleQuote;
                current.append(c);
                i++;
                continue;
            }
            if (c == '"' && !inSingleQuote && bracketDepth == 0) {
                inDoubleQuote = !inDoubleQuote;
                current.append(c);
                i++;
                continue;
            }

            // Don't process operators inside quotes or brackets
            if (inSingleQuote || inDoubleQuote) {
                current.append(c);
                i++;
                continue;
            }

            // Track bracket depth
            if (c == '(' || c == '[' || c == '{') {
                bracketDepth++;
                current.append(c);
                i++;
                continue;
            }
            if ((c == ')' || c == ']' || c == '}') && bracketDepth > 0) {
                bracketDepth--;
                current.append(c);
                i++;
                continue;
            }
            if (bracketDepth > 0) {
                current.append(c);
                i++;
                continue;
            }

            // Check for operators (order matters: check longer operators first)
            String op = matchOperator(line, i);
            if (op != null) {
                // Emit text token if we have accumulated text
                if (current.length() > 0) {
                    tokens.add(new Token(current.toString(), false));
                    current.setLength(0);
                }
                tokens.add(new Token(op, true));
                i += op.length();
            } else {
                current.append(c);
                i++;
            }
        }

        // Emit remaining text
        if (current.length() > 0) {
            tokens.add(new Token(current.toString(), false));
        }

        return tokens;
    }

    /**
     * Tries to match a pipeline operator at the given position.
     * Returns the matched operator string, or {@code null}.
     * <p>
     * The default implementation checks all operators in the operator table
     * (longest-match first). Subclasses can override this method for
     * fully custom matching logic.
     *
     * @param line the full command line string
     * @param pos the current position in the line
     * @return the matched operator string, or null if no operator matches
     */
    protected String matchOperator(String line, int pos) {
        for (String symbol : operators.keySet()) {
            if (pos + symbol.length() <= line.length()
                    && line.substring(pos, pos + symbol.length()).equals(symbol)) {
                return symbol;
            }
        }
        return null;
    }

    /**
     * A token from tokenization -- either text or an operator.
     */
    protected static class Token {
        /** The token value. */
        public final String value;
        /** Whether this token represents an operator. */
        public final boolean isOperator;

        /**
         * Creates a new token.
         *
         * @param value the token value
         * @param isOperator whether this is an operator token
         */
        public Token(String value, boolean isOperator) {
            this.value = value;
            this.isOperator = isOperator;
        }
    }
}

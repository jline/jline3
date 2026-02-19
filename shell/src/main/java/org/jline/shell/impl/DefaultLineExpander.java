/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import org.jline.shell.CommandSession;
import org.jline.shell.LineExpander;

/**
 * Default implementation of {@link LineExpander} that provides sane defaults
 * for variable expansion.
 * <p>
 * Supported expansions:
 * <ul>
 *   <li>{@code $VAR} and {@code ${VAR}} — variable expansion from session
 *       variables, then {@link System#getenv()}</li>
 *   <li>{@code ~} at word start — expands to {@code user.home} system property</li>
 *   <li>Single-quoted regions ({@code '...'}) are not expanded</li>
 *   <li>Double-quoted regions ({@code "..."}) expand variables</li>
 * </ul>
 * <p>
 * Advanced braced forms:
 * <ul>
 *   <li>{@code ${VAR:-default}} — use default if VAR is unset or empty</li>
 *   <li>{@code ${VAR:=default}} — assign default if VAR is unset or empty</li>
 *   <li>{@code ${VAR:+alt}} — use alt if VAR is set and non-empty</li>
 *   <li>{@code ${VAR:?error}} — error if VAR is unset or empty</li>
 * </ul>
 * <p>
 * Deliberately <b>not</b> included (subclass for these):
 * <ul>
 *   <li>{@code ${VAR//pattern/replacement}} — pattern substitution</li>
 *   <li>Glob expansion ({@code *}, {@code ?})</li>
 * </ul>
 * <p>
 * Subclasses can override {@link #resolve(String, CommandSession)} to customize
 * how variable names are resolved.
 *
 * @see LineExpander
 * @since 4.0
 */
public class DefaultLineExpander implements LineExpander {

    /**
     * Creates a new DefaultLineExpander.
     */
    public DefaultLineExpander() {}

    @Override
    public String expand(String line, CommandSession session) {
        if (line == null || line.isEmpty()) {
            return line;
        }

        StringBuilder result = new StringBuilder(line.length());
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int i = 0;

        while (i < line.length()) {
            char c = line.charAt(i);

            // Handle escape character
            if (c == '\\' && !inSingleQuote && i + 1 < line.length()) {
                result.append(c);
                result.append(line.charAt(i + 1));
                i += 2;
                continue;
            }

            // Handle quotes
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                result.append(c);
                i++;
                continue;
            }
            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                result.append(c);
                i++;
                continue;
            }

            // Inside single quotes, no expansion
            if (inSingleQuote) {
                result.append(c);
                i++;
                continue;
            }

            // Tilde expansion at word start (not inside quotes)
            if (c == '~' && !inDoubleQuote) {
                // Check if at word start: beginning of line or preceded by whitespace
                if (i == 0 || Character.isWhitespace(line.charAt(i - 1))) {
                    // Check if followed by / or whitespace or end of line
                    if (i + 1 >= line.length()
                            || line.charAt(i + 1) == '/'
                            || Character.isWhitespace(line.charAt(i + 1))) {
                        result.append(System.getProperty("user.home", "~"));
                        i++;
                        continue;
                    }
                }
                result.append(c);
                i++;
                continue;
            }

            // Variable expansion: $VAR or ${VAR}
            if (c == '$' && i + 1 < line.length()) {
                char next = line.charAt(i + 1);
                if (next == '{') {
                    // ${VAR} or ${VAR:-default} etc.
                    int close = findMatchingBrace(line, i + 2);
                    if (close > i + 2) {
                        String expr = line.substring(i + 2, close);
                        String expanded = expandBracedExpression(expr, session);
                        if (expanded != null) {
                            result.append(expanded);
                        } else {
                            // Unknown variable: leave as-is
                            result.append(line, i, close + 1);
                        }
                        i = close + 1;
                        continue;
                    }
                } else if (isVarStart(next)) {
                    // $VAR form
                    int start = i + 1;
                    int end = start + 1;
                    while (end < line.length() && isVarChar(line.charAt(end))) {
                        end++;
                    }
                    String name = line.substring(start, end);
                    String value = resolve(name, session);
                    if (value != null) {
                        result.append(value);
                    } else {
                        // Unknown variable: leave as-is
                        result.append(line, i, end);
                    }
                    i = end;
                    continue;
                }
            }

            result.append(c);
            i++;
        }

        return result.toString();
    }

    /**
     * Finds the closing brace for a {@code ${...}} expression, handling nested braces.
     *
     * @param line the line
     * @param start the position after {@code ${}, i.e. start of the expression
     * @return the index of the closing {@code }}, or -1 if not found
     */
    private static int findMatchingBrace(String line, int start) {
        int depth = 1;
        for (int j = start; j < line.length(); j++) {
            char ch = line.charAt(j);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return j;
                }
            }
        }
        return -1;
    }

    /**
     * Expands a braced expression such as {@code VAR}, {@code VAR:-default},
     * {@code VAR:=default}, {@code VAR:+alt}, or {@code VAR:?error}.
     *
     * @param expr the expression inside the braces (without {@code ${ }})
     * @param session the command session
     * @return the expanded value, or null if the variable is unknown and no modifier applies
     */
    protected String expandBracedExpression(String expr, CommandSession session) {
        // Check for operator forms: :-, :=, :+, :?
        int colonPos = expr.indexOf(':');
        if (colonPos > 0 && colonPos + 1 < expr.length()) {
            char op = expr.charAt(colonPos + 1);
            if (op == '-' || op == '=' || op == '+' || op == '?') {
                String name = expr.substring(0, colonPos);
                String operand = expr.substring(colonPos + 2);
                String value = resolve(name, session);
                boolean unsetOrEmpty = value == null || value.isEmpty();

                switch (op) {
                    case '-':
                        // ${VAR:-default}: use default if unset or empty
                        return unsetOrEmpty ? operand : value;
                    case '=':
                        // ${VAR:=default}: assign default if unset or empty
                        if (unsetOrEmpty) {
                            if (session != null) {
                                session.put(name, operand);
                            }
                            return operand;
                        }
                        return value;
                    case '+':
                        // ${VAR:+alt}: use alt if set and non-empty
                        return unsetOrEmpty ? "" : operand;
                    case '?':
                        // ${VAR:?error}: error if unset or empty
                        if (unsetOrEmpty) {
                            throw new IllegalArgumentException(name + ": " + operand);
                        }
                        return value;
                    default:
                        break;
                }
            }
        }

        // Simple ${VAR}
        return resolve(expr, session);
    }

    /**
     * Resolves a variable name to its value.
     * <p>
     * The default implementation checks session variables first, then
     * falls back to {@link System#getenv()}.
     * <p>
     * Subclasses can override this to provide custom resolution (e.g.,
     * Groovy evaluation, default values).
     *
     * @param name the variable name
     * @param session the current command session
     * @return the resolved value as a string, or null if not found
     */
    protected String resolve(String name, CommandSession session) {
        // Check session variables first
        if (session != null) {
            Object value = session.get(name);
            if (value != null) {
                return value.toString();
            }
        }
        // Fall back to environment variables
        String envValue = System.getenv(name);
        if (envValue != null) {
            return envValue;
        }
        return null;
    }

    private static boolean isVarStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isVarChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}

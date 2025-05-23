/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Parser interface is responsible for parsing command lines into tokens.
 * <p>
 * Parsers analyze input strings and break them into words/tokens according to
 * specific syntax rules. They handle features such as quoting, escaping special
 * characters, and comments. The parser is used by the LineReader during tab
 * completion and when accepting a line of input.
 * <p>
 * Implementations should ideally return {@link CompletingParsedLine} objects
 * to properly support completion with escaped or quoted words.
 * <p>
 * The default implementation is {@link org.jline.reader.impl.DefaultParser}.
 *
 * @see ParsedLine
 * @see CompletingParsedLine
 * @see org.jline.reader.impl.DefaultParser
 */
public interface Parser {
    String REGEX_VARIABLE = "[a-zA-Z_]+[a-zA-Z0-9_-]*";
    String REGEX_COMMAND = "[:]?[a-zA-Z]+[a-zA-Z0-9_-]*";

    ParsedLine parse(String line, int cursor, ParseContext context) throws SyntaxError;

    default ParsedLine parse(String line, int cursor) throws SyntaxError {
        return parse(line, cursor, ParseContext.UNSPECIFIED);
    }

    default boolean isEscapeChar(char ch) {
        return ch == '\\';
    }

    default boolean validCommandName(String name) {
        return name != null && name.matches(REGEX_COMMAND);
    }

    default boolean validVariableName(String name) {
        return name != null && name.matches(REGEX_VARIABLE);
    }

    default String getCommand(final String line) {
        String out;
        Pattern patternCommand = Pattern.compile("^\\s*" + REGEX_VARIABLE + "=(" + REGEX_COMMAND + ")(\\s+|$)");
        Matcher matcher = patternCommand.matcher(line);
        if (matcher.find()) {
            out = matcher.group(1);
        } else {
            out = line.trim().split("\\s+")[0];
            if (!out.matches(REGEX_COMMAND)) {
                out = "";
            }
        }
        return out;
    }

    default String getVariable(final String line) {
        String out = null;
        Pattern patternCommand = Pattern.compile("^\\s*(" + REGEX_VARIABLE + ")\\s*=[^=~].*");
        Matcher matcher = patternCommand.matcher(line);
        if (matcher.find()) {
            out = matcher.group(1);
        }
        return out;
    }

    enum ParseContext {
        UNSPECIFIED,

        /** Try a real "final" parse.
         * May throw EOFError in which case we have incomplete input.
         */
        ACCEPT_LINE,

        /** Parsed words will have all characters present in input line
         * including quotes and escape chars.
         * We should tolerate and ignore errors.
         */
        SPLIT_LINE,

        /** Parse to find completions (typically after a Tab).
         * We should tolerate and ignore errors.
         */
        COMPLETE,

        /** Called when we need to update the secondary prompts.
         * Specifically, when we need the 'missing' field from EOFError,
         * which is used by a "%M" in a prompt pattern.
         */
        SECONDARY_PROMPT
    }
}

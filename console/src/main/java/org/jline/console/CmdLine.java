/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a command line with its parsed components.
 * This class stores information about a command line, including the original line,
 * the part before and after the cursor, the parsed arguments, and the type of
 * description that should be displayed for the command.
 */
public class CmdLine {
    /**
     * Enumeration specifying the type of description that should be displayed for the command.
     */
    public enum DescriptionType {
        /**
         * Cursor is at the end of line. The args[0] is completed, the line does not have unclosed opening parenthesis
         * and does not end to the closing parenthesis.
         */
        COMMAND,
        /**
         * The part of the line from beginning till cursor has unclosed opening parenthesis.
         */
        METHOD,
        /**
         * The part of the line from beginning till cursor ends to the closing parenthesis.
         */
        SYNTAX
    }

    /** The original command line */
    private final String line;
    /** The part of the command line before the cursor, with method parameters and opening parenthesis removed */
    private final String head;
    /** The part of the command line after the cursor, with method parameters and closing parenthesis removed */
    private final String tail;
    /** The parsed command line arguments */
    private final List<String> args;
    /** The type of description that should be displayed for the command */
    private final DescriptionType descType;

    /**
     * Creates a new command line with the specified components.
     *
     * @param line     The original command line
     * @param head     The part of the command line before the cursor, with method parameters and opening parenthesis removed
     * @param tail     The part of the command line after the cursor, with method parameters and closing parenthesis removed
     * @param args     The parsed command line arguments
     * @param descType The type of description that should be displayed for the command
     */
    public CmdLine(String line, String head, String tail, List<String> args, DescriptionType descType) {
        this.line = line;
        this.head = head;
        this.tail = tail;
        this.args = new ArrayList<>(args);
        this.descType = descType;
    }

    /**
     * Returns the original command line.
     *
     * @return the original command line
     */
    public String getLine() {
        return line;
    }

    /**
     * Returns the part of the command line before the cursor, with method parameters and opening parenthesis removed.
     *
     * @return the part of the command line before the cursor
     */
    public String getHead() {
        return head;
    }

    /**
     * Returns the part of the command line after the cursor, with method parameters and closing parenthesis removed.
     *
     * @return the part of the command line after the cursor
     */
    public String getTail() {
        return tail;
    }

    /**
     * Returns the parsed command line arguments.
     *
     * @return the parsed command line arguments
     */
    public List<String> getArgs() {
        return args;
    }

    /**
     * Returns the type of description that should be displayed for the command.
     *
     * @return the type of description
     */
    public DescriptionType getDescriptionType() {
        return descType;
    }
}

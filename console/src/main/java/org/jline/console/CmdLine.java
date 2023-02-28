/*
 * Copyright (c) 2002-2020, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.util.ArrayList;
import java.util.List;

public class CmdLine {
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

    private final String line;
    private final String head;
    private final String tail;
    private final List<String> args;
    private final DescriptionType descType;

    /**
     * CmdLine class constructor.
     * @param line     Command line
     * @param head     Command line till cursor, method parameters and opening parenthesis before the cursor are removed.
     * @param tail     Command line after cursor, method parameters and closing parenthesis after the cursor are removed.
     * @param args     Parsed command line arguments.
     * @param descType Request COMMAND, METHOD or SYNTAX description
     */
    public CmdLine(String line, String head, String tail, List<String> args, DescriptionType descType) {
        this.line = line;
        this.head = head;
        this.tail = tail;
        this.args = new ArrayList<>(args);
        this.descType = descType;
    }

    public String getLine() {
        return line;
    }

    public String getHead() {
        return head;
    }

    public String getTail() {
        return tail;
    }

    public List<String> getArgs() {
        return args;
    }

    public DescriptionType getDescriptionType() {
        return descType;
    }
}

/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a parsed command line with its components.
 *
 * @see CommandDescription
 * @see CommandDispatcher#describe(CommandLine)
 * @since 4.0
 */
public class CommandLine {

    /**
     * The type of description that should be displayed.
     */
    public enum Type {
        /** Full command description */
        COMMAND,
        /** Method/function parameter description */
        METHOD,
        /** Syntax description */
        SYNTAX
    }

    private final String line;
    private final String head;
    private final String tail;
    private final List<String> args;
    private final Type type;

    /**
     * Creates a new command line.
     *
     * @param line the original command line
     * @param head the part before the cursor
     * @param tail the part after the cursor
     * @param args the parsed arguments
     * @param type the description type
     */
    public CommandLine(String line, String head, String tail, List<String> args, Type type) {
        this.line = line;
        this.head = head;
        this.tail = tail;
        this.args = Collections.unmodifiableList(new ArrayList<>(args));
        this.type = type;
    }

    /**
     * Returns the original command line.
     *
     * @return the line
     */
    public String line() {
        return line;
    }

    /**
     * Returns the part before the cursor.
     *
     * @return the head
     */
    public String head() {
        return head;
    }

    /**
     * Returns the part after the cursor.
     *
     * @return the tail
     */
    public String tail() {
        return tail;
    }

    /**
     * Returns the parsed arguments.
     *
     * @return the arguments
     */
    public List<String> args() {
        return args;
    }

    /**
     * Returns the description type.
     *
     * @return the type
     */
    public Type type() {
        return type;
    }
}

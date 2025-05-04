/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console;

import java.util.*;
import java.util.regex.Pattern;

import org.jline.utils.AttributedString;

/**
 * Represents a command description used for generating command help and documentation.
 * This class stores information about a command, including its main description,
 * argument descriptions, and option descriptions. It is used by the JLine Widgets
 * framework to display command help in the terminal.
 */
public class CmdDesc {
    /** The main description of the command */
    private List<AttributedString> mainDesc;
    /** The descriptions of the command's arguments */
    private List<ArgDesc> argsDesc;
    /** The descriptions of the command's options */
    private TreeMap<String, List<AttributedString>> optsDesc;
    /** Pattern used to identify errors in the command */
    private Pattern errorPattern;
    /** Index of the error in the command, or -1 if no error */
    private int errorIndex = -1;
    /** Whether the command is valid */
    private boolean valid = true;
    /** Whether this is a command (as opposed to a method or syntax) */
    private boolean command = false;
    /** Whether this is a subcommand */
    private boolean subcommand = false;
    /** Whether the command should be highlighted */
    private boolean highlighted = true;

    /**
     * Creates a new command description that is not a command.
     */
    public CmdDesc() {
        command = false;
    }

    /**
     * Creates a new command description with the specified validity.
     *
     * @param valid whether the command is valid
     */
    public CmdDesc(boolean valid) {
        this.valid = valid;
    }

    /**
     * Creates a new command description with the specified argument descriptions.
     *
     * @param argsDesc the descriptions of the command's arguments
     */
    public CmdDesc(List<ArgDesc> argsDesc) {
        this(new ArrayList<>(), argsDesc, new HashMap<>());
    }

    /**
     * Creates a new command description with the specified argument and option descriptions.
     *
     * @param argsDesc the descriptions of the command's arguments
     * @param optsDesc the descriptions of the command's options
     */
    public CmdDesc(List<ArgDesc> argsDesc, Map<String, List<AttributedString>> optsDesc) {
        this(new ArrayList<>(), argsDesc, optsDesc);
    }

    /**
     * Creates a new command description with the specified main description, argument descriptions,
     * and option descriptions.
     *
     * @param mainDesc the main description of the command
     * @param argsDesc the descriptions of the command's arguments
     * @param optsDesc the descriptions of the command's options
     */
    public CmdDesc(
            List<AttributedString> mainDesc, List<ArgDesc> argsDesc, Map<String, List<AttributedString>> optsDesc) {
        this.argsDesc = new ArrayList<>(argsDesc);
        this.optsDesc = new TreeMap<>(optsDesc);
        if (mainDesc.isEmpty() && optsDesc.containsKey("main")) {
            this.mainDesc = new ArrayList<>(optsDesc.get("main"));
            this.optsDesc.remove("main");
        } else {
            this.mainDesc = new ArrayList<>(mainDesc);
        }
        this.command = true;
    }

    /**
     * Returns whether the command is valid.
     *
     * @return true if the command is valid, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns whether this is a command (as opposed to a method or syntax).
     *
     * @return true if this is a command, false otherwise
     */
    public boolean isCommand() {
        return command;
    }

    /**
     * Sets whether this is a subcommand.
     *
     * @param subcommand true if this is a subcommand, false otherwise
     */
    public void setSubcommand(boolean subcommand) {
        this.subcommand = subcommand;
    }

    /**
     * Returns whether this is a subcommand.
     *
     * @return true if this is a subcommand, false otherwise
     */
    public boolean isSubcommand() {
        return subcommand;
    }

    /**
     * Sets whether the command should be highlighted.
     *
     * @param highlighted true if the command should be highlighted, false otherwise
     */
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    /**
     * Returns whether the command should be highlighted.
     *
     * @return true if the command should be highlighted, false otherwise
     */
    public boolean isHighlighted() {
        return highlighted;
    }

    /**
     * Sets the main description of the command and returns this command description.
     *
     * @param mainDesc the main description of the command
     * @return this command description
     */
    public CmdDesc mainDesc(List<AttributedString> mainDesc) {
        this.mainDesc = new ArrayList<>(mainDesc);
        return this;
    }

    /**
     * Sets the main description of the command.
     *
     * @param mainDesc the main description of the command
     */
    public void setMainDesc(List<AttributedString> mainDesc) {
        this.mainDesc = new ArrayList<>(mainDesc);
    }

    /**
     * Returns the main description of the command.
     *
     * @return the main description of the command
     */
    public List<AttributedString> getMainDesc() {
        return mainDesc;
    }

    /**
     * Returns the descriptions of the command's options.
     *
     * @return the descriptions of the command's options
     */
    public TreeMap<String, List<AttributedString>> getOptsDesc() {
        return optsDesc;
    }

    /**
     * Sets the pattern used to identify errors in the command.
     *
     * @param errorPattern the pattern used to identify errors
     */
    public void setErrorPattern(Pattern errorPattern) {
        this.errorPattern = errorPattern;
    }

    /**
     * Returns the pattern used to identify errors in the command.
     *
     * @return the pattern used to identify errors
     */
    public Pattern getErrorPattern() {
        return errorPattern;
    }

    /**
     * Sets the index of the error in the command.
     *
     * @param errorIndex the index of the error, or -1 if no error
     */
    public void setErrorIndex(int errorIndex) {
        this.errorIndex = errorIndex;
    }

    /**
     * Returns the index of the error in the command.
     *
     * @return the index of the error, or -1 if no error
     */
    public int getErrorIndex() {
        return errorIndex;
    }

    /**
     * Returns the descriptions of the command's arguments.
     *
     * @return the descriptions of the command's arguments
     */
    public List<ArgDesc> getArgsDesc() {
        return argsDesc;
    }

    /**
     * Returns whether the specified option takes a value.
     *
     * @param option the option to check
     * @return true if the option takes a value, false otherwise
     */
    public boolean optionWithValue(String option) {
        for (String key : optsDesc.keySet()) {
            if (key.matches("(^|.*\\s)" + option + "($|=.*|\\s.*)")) {
                return key.contains("=");
            }
        }
        return false;
    }

    /**
     * Returns the description of the specified option.
     *
     * @param key the option key
     * @return the description of the option, or an empty string if the option has no description
     */
    public AttributedString optionDescription(String key) {
        return !optsDesc.get(key).isEmpty() ? optsDesc.get(key).get(0) : new AttributedString("");
    }
}

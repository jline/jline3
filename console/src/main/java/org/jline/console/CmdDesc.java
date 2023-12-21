/*
 * Copyright (c) 2002-2020, the original author(s).
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

public class CmdDesc {
    private List<AttributedString> mainDesc;
    private List<ArgDesc> argsDesc;
    private TreeMap<String, List<AttributedString>> optsDesc;
    private Pattern errorPattern;
    private int errorIndex = -1;
    private boolean valid = true;
    private boolean command = false;
    private boolean subcommand = false;
    private boolean highlighted = true;

    public CmdDesc() {
        command = false;
    }

    public CmdDesc(boolean valid) {
        this.valid = valid;
    }

    public CmdDesc(List<ArgDesc> argsDesc) {
        this(new ArrayList<>(), argsDesc, new HashMap<>());
    }

    public CmdDesc(List<ArgDesc> argsDesc, Map<String, List<AttributedString>> optsDesc) {
        this(new ArrayList<>(), argsDesc, optsDesc);
    }

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

    public boolean isValid() {
        return valid;
    }

    public boolean isCommand() {
        return command;
    }

    public void setSubcommand(boolean subcommand) {
        this.subcommand = subcommand;
    }

    public boolean isSubcommand() {
        return subcommand;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public CmdDesc mainDesc(List<AttributedString> mainDesc) {
        this.mainDesc = new ArrayList<>(mainDesc);
        return this;
    }

    public void setMainDesc(List<AttributedString> mainDesc) {
        this.mainDesc = new ArrayList<>(mainDesc);
    }

    public List<AttributedString> getMainDesc() {
        return mainDesc;
    }

    public TreeMap<String, List<AttributedString>> getOptsDesc() {
        return optsDesc;
    }

    public void setErrorPattern(Pattern errorPattern) {
        this.errorPattern = errorPattern;
    }

    public Pattern getErrorPattern() {
        return errorPattern;
    }

    public void setErrorIndex(int errorIndex) {
        this.errorIndex = errorIndex;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    public List<ArgDesc> getArgsDesc() {
        return argsDesc;
    }

    public boolean optionWithValue(String option) {
        for (String key : optsDesc.keySet()) {
            if (key.matches("(^|.*\\s)" + option + "($|=.*|\\s.*)")) {
                return key.contains("=");
            }
        }
        return false;
    }

    public AttributedString optionDescription(String key) {
        return optsDesc.get(key).size() > 0 ? optsDesc.get(key).get(0) : new AttributedString("");
    }
}

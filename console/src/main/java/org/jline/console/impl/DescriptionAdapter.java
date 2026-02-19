/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.impl;

import java.util.ArrayList;
import java.util.List;

import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.console.CmdLine;
import org.jline.shell.ArgumentDescription;
import org.jline.shell.CommandDescription;
import org.jline.shell.CommandLine;

/**
 * Converts between old console description types and new shell description types.
 * <p>
 * Provides static methods for bidirectional conversion:
 * <ul>
 *   <li>{@link CommandDescription} ↔ {@link CmdDesc}</li>
 *   <li>{@link ArgumentDescription} ↔ {@link ArgDesc}</li>
 *   <li>{@link CommandLine} ↔ {@link CmdLine}</li>
 * </ul>
 *
 * @since 4.0
 */
public final class DescriptionAdapter {

    private DescriptionAdapter() {}

    /**
     * Converts a new {@link CommandDescription} to an old {@link CmdDesc}.
     *
     * @param desc the command description, may be null
     * @return the CmdDesc, or null if input is null
     */
    public static CmdDesc toCmdDesc(CommandDescription desc) {
        if (desc == null) {
            return null;
        }
        if (!desc.isValid()) {
            return new CmdDesc(false);
        }
        List<ArgDesc> argsDesc = new ArrayList<>();
        for (ArgumentDescription arg : desc.arguments()) {
            argsDesc.add(new ArgDesc(arg.name(), new ArrayList<>(arg.description())));
        }
        CmdDesc cmdDesc =
                new CmdDesc(new ArrayList<>(desc.mainDescription()), argsDesc, new java.util.TreeMap<>(desc.options()));
        cmdDesc.setHighlighted(desc.isHighlighted());
        if (desc.errorPattern() != null) {
            cmdDesc.setErrorPattern(desc.errorPattern());
        }
        if (desc.errorIndex() >= 0) {
            cmdDesc.setErrorIndex(desc.errorIndex());
        }
        return cmdDesc;
    }

    /**
     * Converts an old {@link CmdDesc} to a new {@link CommandDescription}.
     *
     * @param cmdDesc the CmdDesc, may be null
     * @return the CommandDescription, or null if input is null
     */
    public static CommandDescription toCommandDescription(CmdDesc cmdDesc) {
        if (cmdDesc == null) {
            return null;
        }
        if (!cmdDesc.isValid()) {
            return CommandDescription.builder().valid(false).build();
        }
        CommandDescription.Builder builder = CommandDescription.builder();
        if (cmdDesc.getMainDesc() != null) {
            builder.mainDescription(cmdDesc.getMainDesc());
        }
        if (cmdDesc.getArgsDesc() != null) {
            List<ArgumentDescription> args = new ArrayList<>();
            for (ArgDesc arg : cmdDesc.getArgsDesc()) {
                args.add(new ArgumentDescription(arg.getName(), arg.getDescription()));
            }
            builder.arguments(args);
        }
        if (cmdDesc.getOptsDesc() != null) {
            builder.options(cmdDesc.getOptsDesc());
        }
        builder.highlighted(cmdDesc.isHighlighted());
        if (cmdDesc.getErrorPattern() != null) {
            builder.errorPattern(cmdDesc.getErrorPattern());
        }
        if (cmdDesc.getErrorIndex() >= 0) {
            builder.errorIndex(cmdDesc.getErrorIndex());
        }
        return builder.build();
    }

    /**
     * Converts a new {@link CommandLine} to an old {@link CmdLine}.
     *
     * @param commandLine the command line, may be null
     * @return the CmdLine, or null if input is null
     */
    public static CmdLine toCmdLine(CommandLine commandLine) {
        if (commandLine == null) {
            return null;
        }
        CmdLine.DescriptionType descType;
        switch (commandLine.type()) {
            case METHOD:
                descType = CmdLine.DescriptionType.METHOD;
                break;
            case SYNTAX:
                descType = CmdLine.DescriptionType.SYNTAX;
                break;
            default:
                descType = CmdLine.DescriptionType.COMMAND;
                break;
        }
        return new CmdLine(commandLine.line(), commandLine.head(), commandLine.tail(), commandLine.args(), descType);
    }

    /**
     * Converts an old {@link CmdLine} to a new {@link CommandLine}.
     *
     * @param cmdLine the CmdLine, may be null
     * @return the CommandLine, or null if input is null
     */
    public static CommandLine toCommandLine(CmdLine cmdLine) {
        if (cmdLine == null) {
            return null;
        }
        CommandLine.Type type;
        switch (cmdLine.getDescriptionType()) {
            case METHOD:
                type = CommandLine.Type.METHOD;
                break;
            case SYNTAX:
                type = CommandLine.Type.SYNTAX;
                break;
            default:
                type = CommandLine.Type.COMMAND;
                break;
        }
        return new CommandLine(cmdLine.getLine(), cmdLine.getHead(), cmdLine.getTail(), cmdLine.getArgs(), type);
    }

    /**
     * Converts a new {@link ArgumentDescription} to an old {@link ArgDesc}.
     *
     * @param arg the argument description, may be null
     * @return the ArgDesc, or null if input is null
     */
    public static ArgDesc toArgDesc(ArgumentDescription arg) {
        if (arg == null) {
            return null;
        }
        return new ArgDesc(arg.name(), new ArrayList<>(arg.description()));
    }

    /**
     * Converts an old {@link ArgDesc} to a new {@link ArgumentDescription}.
     *
     * @param argDesc the ArgDesc, may be null
     * @return the ArgumentDescription, or null if input is null
     */
    public static ArgumentDescription toArgumentDescription(ArgDesc argDesc) {
        if (argDesc == null) {
            return null;
        }
        return new ArgumentDescription(argDesc.getName(), argDesc.getDescription());
    }
}

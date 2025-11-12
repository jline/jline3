/*
 * Copyright (c) 2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.console.picocli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jline.builtins.Completers.OptDesc;
import org.jline.console.ArgDesc;
import org.jline.console.CmdDesc;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import picocli.CommandLine.Model.ArgSpec;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;

/**
 * Generates JLine CmdDesc from Picocli CommandSpec.
 * <p>
 * This class converts Picocli command specifications into JLine's CmdDesc format,
 * enabling rich help display and command description features in JLine-based applications.
 */
public class PicocliCmdDescGenerator {

    /**
     * Generates a CmdDesc from a Picocli CommandSpec.
     * @param spec the Picocli command specification
     * @return a CmdDesc for use with JLine
     */
    public static CmdDesc generate(CommandSpec spec) {
        List<AttributedString> mainDesc = generateMainDescription(spec);
        List<ArgDesc> argsDesc = generateArgsDescription(spec);
        Map<String, List<AttributedString>> optsDesc = generateOptsDescription(spec);
        
        return new CmdDesc(mainDesc, argsDesc, optsDesc);
    }

    /**
     * Generates the main description section.
     */
    private static List<AttributedString> generateMainDescription(CommandSpec spec) {
        List<AttributedString> mainDesc = new ArrayList<>();
        
        // Command name and summary
        AttributedStringBuilder asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.BOLD);
        asb.append(spec.name());
        
        if (spec.usageMessage().header().length > 0) {
            asb.style(AttributedStyle.DEFAULT);
            asb.append(" - ");
            asb.append(spec.usageMessage().header()[0]);
        }
        mainDesc.add(asb.toAttributedString());
        
        // Description
        if (spec.usageMessage().description().length > 0) {
            mainDesc.add(AttributedString.EMPTY);
            for (String desc : spec.usageMessage().description()) {
                mainDesc.add(new AttributedString(desc));
            }
        }
        
        // Usage
        mainDesc.add(AttributedString.EMPTY);
        asb = new AttributedStringBuilder();
        asb.style(AttributedStyle.BOLD);
        asb.append("Usage:");
        mainDesc.add(asb.toAttributedString());
        
        // Generate usage line
        asb = new AttributedStringBuilder();
        asb.append("  ");
        asb.style(AttributedStyle.BOLD);
        asb.append(spec.name());
        asb.style(AttributedStyle.DEFAULT);
        
        // Add options placeholder
        if (!spec.options().isEmpty()) {
            asb.append(" [OPTIONS]");
        }
        
        // Add positional parameters
        for (PositionalParamSpec param : spec.positionalParameters()) {
            asb.append(" ");
            if (!param.required()) {
                asb.append("[");
            }
            asb.append(param.paramLabel());
            if (param.arity().max > 1) {
                asb.append("...");
            }
            if (!param.required()) {
                asb.append("]");
            }
        }
        
        mainDesc.add(asb.toAttributedString());
        
        // Examples
        if (spec.usageMessage().customSynopsis().length > 0) {
            mainDesc.add(AttributedString.EMPTY);
            asb = new AttributedStringBuilder();
            asb.style(AttributedStyle.BOLD);
            asb.append("Examples:");
            mainDesc.add(asb.toAttributedString());
            
            for (String example : spec.usageMessage().customSynopsis()) {
                asb = new AttributedStringBuilder();
                asb.append("  ");
                asb.style(AttributedStyle.ITALIC);
                asb.append(example);
                mainDesc.add(asb.toAttributedString());
            }
        }
        
        return mainDesc;
    }

    /**
     * Generates argument descriptions.
     */
    private static List<ArgDesc> generateArgsDescription(CommandSpec spec) {
        List<ArgDesc> argsDesc = new ArrayList<>();
        
        for (PositionalParamSpec param : spec.positionalParameters()) {
            String name = param.paramLabel();
            String description = param.description().length > 0 ? 
                    String.join(" ", param.description()) : "";
            
            argsDesc.add(new ArgDesc(name, description));
        }
        
        return argsDesc;
    }

    /**
     * Generates option descriptions.
     */
    private static Map<String, List<AttributedString>> generateOptsDescription(CommandSpec spec) {
        return PicocliOptDescGenerator.generateOptsDescription(spec);
    }
}

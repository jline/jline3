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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.builtins.Completers.OptDesc;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;

/**
 * Generates JLine OptDesc from Picocli CommandSpec options.
 * <p>
 * This class converts Picocli option specifications into JLine's OptDesc format,
 * enabling rich option completion and help display in JLine-based applications.
 */
public class PicocliOptDescGenerator {

    /**
     * Generates OptDesc list from Picocli CommandSpec.
     * @param spec the Picocli command specification
     * @return list of OptDesc for use with JLine completers
     */
    public static List<OptDesc> generate(CommandSpec spec) {
        List<OptDesc> optDescs = new ArrayList<>();
        
        for (OptionSpec option : spec.options()) {
            String shortOpt = null;
            String longOpt = null;
            
            // Extract short and long option names
            for (String name : option.names()) {
                if (name.startsWith("--")) {
                    longOpt = name.substring(2);
                } else if (name.startsWith("-") && name.length() == 2) {
                    shortOpt = name.substring(1);
                }
            }
            
            String description = option.description().length > 0 ? 
                    String.join(" ", option.description()) : "";
            
            String argumentName = null;
            if (option.arity().max > 0) {
                argumentName = option.paramLabel();
            }
            
            OptDesc optDesc = new OptDesc(shortOpt, longOpt, argumentName, description);
            optDescs.add(optDesc);
        }
        
        return optDescs;
    }

    /**
     * Generates detailed option descriptions for help display.
     * @param spec the Picocli command specification
     * @return map of option names to their detailed descriptions
     */
    public static Map<String, List<AttributedString>> generateOptsDescription(CommandSpec spec) {
        Map<String, List<AttributedString>> optsDesc = new HashMap<>();
        
        for (OptionSpec option : spec.options()) {
            List<AttributedString> optionDesc = new ArrayList<>();
            
            // Option names
            AttributedStringBuilder asb = new AttributedStringBuilder();
            asb.style(AttributedStyle.BOLD);
            
            String[] names = option.names();
            for (int i = 0; i < names.length; i++) {
                if (i > 0) {
                    asb.append(", ");
                }
                asb.append(names[i]);
            }
            
            // Parameter label
            if (option.arity().max > 0) {
                asb.style(AttributedStyle.DEFAULT);
                asb.append(" ");
                asb.style(AttributedStyle.ITALIC);
                asb.append(option.paramLabel());
            }
            
            optionDesc.add(asb.toAttributedString());
            
            // Description
            if (option.description().length > 0) {
                for (String desc : option.description()) {
                    optionDesc.add(new AttributedString("    " + desc));
                }
            }
            
            // Default value
            if (option.defaultValue() != null && !option.defaultValue().toString().isEmpty()) {
                asb = new AttributedStringBuilder();
                asb.append("    ");
                asb.style(AttributedStyle.FAINT);
                asb.append("Default: ");
                asb.append(option.defaultValue().toString());
                optionDesc.add(asb.toAttributedString());
            }
            
            // Required
            if (option.required()) {
                asb = new AttributedStringBuilder();
                asb.append("    ");
                asb.style(AttributedStyle.BOLD.foreground(AttributedStyle.RED));
                asb.append("Required");
                optionDesc.add(asb.toAttributedString());
            }
            
            // Add to map using the primary option name
            String primaryName = names[0];
            optsDesc.put(primaryName, optionDesc);
            
            // Also add aliases
            for (String name : names) {
                if (!name.equals(primaryName)) {
                    optsDesc.put(name, optionDesc);
                }
            }
        }
        
        return optsDesc;
    }
}

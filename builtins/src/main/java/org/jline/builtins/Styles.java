/*
 * Copyright (c) 2002-2020, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.builtins;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jline.utils.StyleResolver;

public class Styles {
    private static final String DEFAULT_LS_COLORS = "di=1;91:ex=1;92:ln=1;96:fi=";
    private static final String DEFAULT_HELP_COLORS = "ti=1;34:co=1:ar=3:op=33";
    private static final String DEFAULT_PRNT_COLORS = "th=1;34:rn=1;34:mk=1;34:em=31:vs=32";
    private static final String LS_COLORS = "LS_COLORS";
    private static final String HELP_COLORS = "HELP_COLORS";
    private static final String PRNT_COLORS = "PRNT_COLORS";
    
    private static final String KEY = "([a-z]{2}|\\*\\.[a-zA-Z0-9]+)";
    private static final String VALUE = "[0-9]*(;[0-9]+){0,2}";    
    private static final String ANSI_STYLE_PATTERN = KEY + "=" + VALUE + "(:" + KEY + "=" + VALUE + ")*(:|)";

    public static StyleResolver lsStyle() {
        return style(LS_COLORS, DEFAULT_LS_COLORS);
    }

    public static StyleResolver helpStyle() {
        return style(HELP_COLORS, DEFAULT_HELP_COLORS);
    }

    public static StyleResolver prntStyle() {
        return style(PRNT_COLORS, DEFAULT_PRNT_COLORS);
    }
    
    private static StyleResolver style(String name, String defStyle) {
        String style = consoleOption(name);
        if (style == null) {
            style = defStyle;
        }
        return style(style);
    }

    private static String consoleOption(String name) {
        String out = null;
        SystemRegistry sr = SystemRegistry.get();
        if (sr != null) {
            out = (String)sr.consoleOption(name);
            if (out != null && !out.matches(ANSI_STYLE_PATTERN)) {
                out = null;
            }
        }
        if (out == null) {
            out = System.getenv(name);
            if (out != null && !out.matches(ANSI_STYLE_PATTERN)) {
                out = null;
            }
        }
        return out;
    }
    
    private static StyleResolver style(String style) {
        Map<String, String> colors = Arrays.stream(style.split(":"))
                .collect(Collectors.toMap(s -> s.substring(0, s.indexOf('=')),
                        s -> s.substring(s.indexOf('=') + 1)));
        return new StyleResolver(colors::get);
    }    
}

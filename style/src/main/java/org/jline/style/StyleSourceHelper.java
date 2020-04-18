/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.style;

import java.util.stream.Stream;

public class StyleSourceHelper
{
    public static final String DEFAULT_LS_COLORS = "dr=1;91:ex=1;92:sl=1;96:ot=34;43:fi=";
    public static final String LS_COLORS = "LS_COLORS";

    public static final String DEFAULT_GREP_COLORS = "ms=01;31:mc=01;31:sl=:cx=:fn=35:ln=32:bn=32:se=36";
    public static final String GREP_COLORS = "GREP_COLORS";

    private static final String ansiStyleDefinitionPattern(String separator)
    {
        return "[a-z]{2}=[0-9]*(;[0-9]+)*(" + separator + "[a-z]{2}=[0-9]*(;[0-9]+)*)*";
    }

    private static final String ANSI_STYLE_PATTERN_SPACE = ansiStyleDefinitionPattern(
        " ");
    private static final String ANSI_STYLE_PATTERN_COLON = ansiStyleDefinitionPattern(
        ":");

    public static StyleSource defaultAnsiStyleDefinition()
    {
        StyleSource ms = new MemoryStyleSource();
        ms = appendAnsiStyleDefinition(ms, GREP_COLORS, DEFAULT_GREP_COLORS);
        ms = appendAnsiStyleDefinition(ms, LS_COLORS, DEFAULT_LS_COLORS);
        return ms;
    }

    public static StyleSource fromAnsiStyleDefinition(String group,
        String namedAnsiStyleDefinition)
    {
        return appendAnsiStyleDefinition(new MemoryStyleSource(), group,
            namedAnsiStyleDefinition);
    }

    public static StyleSource appendAnsiStyleDefinition(StyleSource source,
        String group,
        String namedAnsiStyleDefinition)
    {
        String separator;
        if (namedAnsiStyleDefinition.matches(ANSI_STYLE_PATTERN_COLON))
        {
            separator = ":";
        }
        else if (namedAnsiStyleDefinition.matches(ANSI_STYLE_PATTERN_SPACE))
        {
            separator = " ";
        }
        else
        {
            throw new IllegalArgumentException(
                String.format("styleDefinition does not match the pattern %s or %s",
                    ANSI_STYLE_PATTERN_COLON, ANSI_STYLE_PATTERN_SPACE));
        }
        Stream.of(namedAnsiStyleDefinition.split(separator)).forEach(t -> {

            String name = t.substring(0, t.indexOf('='));
            String style = t.substring(t.indexOf('=') + 1);
            source.set(group, name, style);
        });
        return source;
    }

}

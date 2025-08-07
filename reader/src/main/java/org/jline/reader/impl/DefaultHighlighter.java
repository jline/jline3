/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.util.regex.Pattern;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.reader.LineReader.RegionType;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.WCWidth;

/**
 * Default implementation of the {@link Highlighter} interface.
 * <p>
 * This highlighter provides basic syntax highlighting capabilities for the LineReader,
 * including:
 * <ul>
 *   <li>Highlighting of search matches</li>
 *   <li>Highlighting of errors based on patterns or indices</li>
 *   <li>Highlighting of selected regions</li>
 * </ul>
 * <p>
 * The highlighting is applied using {@link AttributedStyle} to change the appearance
 * of text in the terminal, such as colors, bold, underline, etc.
 * <p>
 * Applications can customize the highlighting behavior by extending this class
 * and overriding the {@link #highlight(LineReader, String)} method.
 *
 * @see Highlighter
 * @see AttributedStyle
 * @see org.jline.reader.LineReader
 */
public class DefaultHighlighter implements Highlighter {
    protected Pattern errorPattern;
    protected int errorIndex = -1;

    /**
     * Creates a new DefaultHighlighter.
     */
    public DefaultHighlighter() {
        // Default constructor
    }

    @Override
    public void setErrorPattern(Pattern errorPattern) {
        this.errorPattern = errorPattern;
    }

    @Override
    public void setErrorIndex(int errorIndex) {
        this.errorIndex = errorIndex;
    }

    @Override
    public AttributedString highlight(LineReader reader, String buffer) {
        int underlineStart = -1;
        int underlineEnd = -1;
        int negativeStart = -1;
        int negativeEnd = -1;
        boolean first = true;
        String search = reader.getSearchTerm();
        if (search != null && search.length() > 0) {
            underlineStart = buffer.indexOf(search);
            if (underlineStart >= 0) {
                underlineEnd = underlineStart + search.length() - 1;
            }
        }
        if (reader.getRegionActive() != RegionType.NONE) {
            negativeStart = reader.getRegionMark();
            negativeEnd = reader.getBuffer().cursor();
            if (negativeStart > negativeEnd) {
                int x = negativeEnd;
                negativeEnd = negativeStart;
                negativeStart = x;
            }
            if (reader.getRegionActive() == RegionType.LINE) {
                while (negativeStart > 0 && reader.getBuffer().atChar(negativeStart - 1) != '\n') {
                    negativeStart--;
                }
                while (negativeEnd < reader.getBuffer().length() - 1
                        && reader.getBuffer().atChar(negativeEnd + 1) != '\n') {
                    negativeEnd++;
                }
            }
        }

        AttributedStringBuilder sb = new AttributedStringBuilder();
        commandStyle(reader, sb, true);
        for (int i = 0; i < buffer.length(); i++) {
            if (i == underlineStart) {
                sb.style(AttributedStyle::underline);
            }
            if (i == negativeStart) {
                sb.style(AttributedStyle::inverse);
            }
            if (i == errorIndex) {
                sb.style(AttributedStyle::inverse);
            }

            char c = buffer.charAt(i);
            if (first && Character.isSpaceChar(c)) {
                first = false;
                commandStyle(reader, sb, false);
            }
            if (c == '\t' || c == '\n') {
                sb.append(c);
            } else if (c < 32) {
                sb.style(AttributedStyle::inverseNeg)
                        .append('^')
                        .append((char) (c + '@'))
                        .style(AttributedStyle::inverseNeg);
            } else {
                int w = WCWidth.wcwidth(c);
                if (w > 0) {
                    sb.append(c);
                }
            }
            if (i == underlineEnd) {
                sb.style(AttributedStyle::underlineOff);
            }
            if (i == negativeEnd) {
                sb.style(AttributedStyle::inverseOff);
            }
            if (i == errorIndex) {
                sb.style(AttributedStyle::inverseOff);
            }
        }
        if (errorPattern != null) {
            sb.styleMatches(errorPattern, AttributedStyle.INVERSE);
        }
        return sb.toAttributedString();
    }

    protected void commandStyle(LineReader reader, AttributedStringBuilder sb, boolean enable) {}
}

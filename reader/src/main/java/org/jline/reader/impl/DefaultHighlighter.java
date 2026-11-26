/*
 * Copyright (c) the original author(s).
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
            // Convert code point indices to char indices
            negativeStart =
                    buffer.offsetByCodePoints(0, Math.min(negativeStart, buffer.codePointCount(0, buffer.length())));
            negativeEnd =
                    buffer.offsetByCodePoints(0, Math.min(negativeEnd, buffer.codePointCount(0, buffer.length())));
        }

        // Convert errorIndex from code point index to char index
        int charErrorIndex = -1;
        if (errorIndex >= 0 && errorIndex < buffer.codePointCount(0, buffer.length())) {
            charErrorIndex = buffer.offsetByCodePoints(0, errorIndex);
        }

        AttributedStringBuilder sb = new AttributedStringBuilder();
        commandStyle(reader, sb, true);
        for (int i = 0; i < buffer.length(); ) {
            if (i == underlineStart) {
                sb.style(AttributedStyle::underline);
            }
            if (i == negativeStart) {
                sb.style(AttributedStyle::inverse);
            }
            if (i == charErrorIndex) {
                sb.style(AttributedStyle::inverse);
            }

            int cp = buffer.codePointAt(i);
            int charCount = Character.charCount(cp);
            if (first && Character.isSpaceChar(cp)) {
                first = false;
                commandStyle(reader, sb, false);
            }
            if (cp == '\t' || cp == '\n') {
                sb.append((char) cp);
            } else if (cp < 32) {
                sb.style(AttributedStyle::inverseNeg)
                        .append('^')
                        .append((char) (cp + '@'))
                        .style(AttributedStyle::inverseNeg);
            } else {
                int w = WCWidth.wcwidth(cp);
                if (w >= 0) {
                    sb.append(buffer, i, i + charCount);
                }
            }
            if (i == underlineEnd) {
                sb.style(AttributedStyle::underlineOff);
            }
            if (i == negativeEnd) {
                sb.style(AttributedStyle::inverseOff);
            }
            if (i == charErrorIndex) {
                sb.style(AttributedStyle::inverseOff);
            }
            i += charCount;
        }
        if (errorPattern != null) {
            sb.styleMatches(errorPattern, AttributedStyle.INVERSE);
        }
        return sb.toAttributedString();
    }

    /**
     * Renders control characters in an already highlighted string the way {@link #highlight} does
     * for a plain buffer: C0 controls other than TAB and LF are shown in inverse caret notation and
     * DEL and C1 controls are dropped, so they never reach the terminal as part of an escape sequence.
     * Styles of the surrounding text are kept. The input is returned as is when it holds no control
     * characters.
     * <p>
     * Highlighters that build their result from raw slices of the buffer should pass it through this
     * method, since the buffer can hold text that was never typed (a completed file name, a bracketed
     * paste, a recalled history entry).
     *
     * @param str the highlighted string
     * @return the string with control characters escaped
     */
    public static AttributedString escapeControlChars(AttributedString str) {
        AttributedStringBuilder sb = null;
        int start = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean control = c < 32 ? c != '\t' && c != '\n' : c >= 0x7f && c < 0xa0;
            if (control) {
                if (sb == null) {
                    sb = new AttributedStringBuilder(str.length() + 8);
                }
                sb.append(str, start, i);
                if (c < 32) {
                    sb.append("^" + (char) (c + '@'), str.styleAt(i).inverseNeg());
                }
                start = i + 1;
            }
        }
        if (sb == null) {
            return str;
        }
        sb.append(str, start, str.length());
        return sb.toAttributedString();
    }

    protected void commandStyle(LineReader reader, AttributedStringBuilder sb, boolean enable) {}
}

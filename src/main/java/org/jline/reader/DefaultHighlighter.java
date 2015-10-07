/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import org.jline.ConsoleReader;
import org.jline.Highlighter;
import org.jline.reader.ConsoleReaderImpl.RegionType;
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Attribute;
import org.jline.utils.WCWidth;

public class DefaultHighlighter implements Highlighter {

    @Override
    public String highlight(ConsoleReader reader, String buffer) {
        int underlineStart = -1;
        int underlineEnd = -1;
        int negativeStart = -1;
        int negativeEnd = -1;
        if (reader instanceof ConsoleReaderImpl) {
            ConsoleReaderImpl r = (ConsoleReaderImpl) reader;
            StringBuffer search = r.searchTerm;
            if (search != null && search.length() > 0) {
                underlineStart = buffer.indexOf(search.toString());
                if (underlineStart >= 0) {
                    underlineEnd = underlineStart + search.length() - 1;
                }
            }
            if (r.regionActive != RegionType.NONE) {
                negativeStart = r.regionMark;
                negativeEnd = r.buf.cursor();
                if (negativeStart > negativeEnd) {
                    int x = negativeEnd;
                    negativeEnd = negativeStart;
                    negativeStart = x;
                }
                if (r.regionActive == RegionType.LINE) {
                    while (negativeStart > 0 && r.buf.atChar(negativeStart - 1) != '\n') {
                        negativeStart--;
                    }
                    while (negativeEnd < r.buf.length() - 1 && r.buf.atChar(negativeEnd + 1) != '\n') {
                        negativeEnd++;
                    }
                }
            }
        }
        Ansi ansi = new Ansi();
        for (int i = 0; i < buffer.length(); i++) {
            if (i == underlineStart) {
                ansi.a(Attribute.UNDERLINE);
            }
            if (i == negativeStart) {
                ansi.a(Attribute.NEGATIVE_ON);
            }
            char c = buffer.charAt(i);
            if (c == '\t' || c == '\n') {
                ansi.a(c);
            } else if (c < 32) {
                ansi.a(Attribute.NEGATIVE_ON)
                        .a('^').a((char) (c + '@')).a(Attribute.NEGATIVE_OFF);
            } else {
                int w = WCWidth.wcwidth(c);
                if (w > 0) {
                    ansi.a(c);
                }
            }
            if (i == underlineEnd) {
                ansi.a(Attribute.UNDERLINE_OFF);
            }
            if (i == negativeEnd) {
                ansi.a(Attribute.NEGATIVE_OFF);
            }
        }
        return ansi.toString();
    }

}

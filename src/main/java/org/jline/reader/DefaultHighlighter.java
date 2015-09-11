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
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Attribute;
import org.jline.utils.Ansi.Color;
import org.jline.utils.WCWidth;

public class DefaultHighlighter implements Highlighter {

    @Override
    public String highlight(ConsoleReader reader, String buffer) {
        int underlineStart = -1;
        int underlineEnd = -1;
        if (reader instanceof ConsoleReaderImpl) {
            StringBuffer search = ((ConsoleReaderImpl) reader).searchTerm;
            if (search != null && search.length() > 0) {
                underlineStart = buffer.indexOf(search.toString());
                if (underlineStart >= 0) {
                    underlineEnd = underlineStart + search.length() - 1;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.length(); i++) {
            if (i == underlineStart) {
                sb.append(Ansi.ansi().a(Attribute.UNDERLINE).toString());
            }
            char c = buffer.charAt(i);
            if (c == '\t' || c == '\n') {
                sb.append(c);
            } else if (c < 32) {
                String s = Ansi.ansi().a(Attribute.NEGATIVE_ON)
                        .a('^').a((char) (c + '@')).a(Attribute.NEGATIVE_OFF).toString();
                sb.append(s);
            } else {
                int w = WCWidth.wcwidth(c);
                if (w > 0) {
                    sb.append(c);
                }
            }
            if (i == underlineEnd) {
                sb.append(Ansi.ansi().a(Attribute.UNDERLINE_OFF).toString());
            }
        }
        return sb.toString();
    }

}

/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader;

import org.jline.Highlighter;
import org.jline.utils.Ansi;
import org.jline.utils.Ansi.Color;
import org.jline.utils.WCWidth;

public class DefaultHighlighter implements Highlighter {

    @Override
    public String highlight(String buffer) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.length(); i++) {
            char c = buffer.charAt(i);
            if (c < 32) {
                String s = Ansi.ansi().bg(Color.BLACK).fg(Color.WHITE)
                        .a('^').a((char) (c + '@')).reset().toString();
                sb.append(s);
            } else {
                int w = WCWidth.wcwidth(c);
                if (w > 0) {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

}

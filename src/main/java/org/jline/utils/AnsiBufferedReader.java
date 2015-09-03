/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;

public class AnsiBufferedReader implements Closeable {

    private final Reader in;
    private final AnsiSplitterWriter splitter;

    public AnsiBufferedReader(Reader in, int begin, int end, int maxLength) {
        this.in = in;
        this.splitter = new AnsiSplitterWriter(begin, end, maxLength);
    }

    public String readLine() throws IOException {
        while (splitter.getLines().isEmpty()) {
            int c = in.read();
            if (c < 0) {
                splitter.flushLine(false);
                break;
            } else {
                splitter.write(c);
            }
        }
        if (splitter.getLines().isEmpty()) {
            return null;
        } else {
            return splitter.getLines().remove(0);
        }
    }

    @Override
    public void close() throws IOException {
    }

    public void setTabs(int tabs) {
        this.splitter.setTabs(tabs);
    }
}

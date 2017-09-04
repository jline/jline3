/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.reader.impl;

import org.jline.reader.LineReaderCallback;

/**
 * Simple {@Link LineReaderCallback} that will replace all the characters in the line with the given mask.
 * If the given mask is equal to {@link LineReaderImpl#NULL_MASK} then the line will be replaced with an empty String.
 */
public final class LineReaderMaskCallback implements LineReaderCallback{
    private final Character mask;

    public LineReaderMaskCallback(Character mask) {
        this.mask = mask;
    }

    @Override
    public String onDisplayLine(String line) {
        return getMaskedBuffer(line);
    }

    @Override
    public String onAddLineToHistory(String line) {
        return null;
    }

    @Override
    public String onHighlightLine(String line) {
        return getMaskedBuffer(line);
    }

    private String getMaskedBuffer(String buffer) {
        if (mask.equals(LineReaderImpl.NULL_MASK)) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = buffer.length(); i-- > 0;) {
                sb.append((char) mask);
            }
            return sb.toString();
        }
    }
}

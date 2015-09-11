/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class AnsiSplitterWriter extends AnsiStatefulWriter {

    private int begin;
    private int length;
    private int maxLength;
    private int escapeLength;
    private int windowState;
    private int tabs;
    private boolean forced;
    private List<String> lines = new ArrayList<>();


    public AnsiSplitterWriter(int maxLength) {
        this(0, Integer.MAX_VALUE, maxLength);
    }

    public AnsiSplitterWriter(int begin, int end, int maxLength) {
        super(new StringWriter());
        this.begin = begin;
        this.length = end - begin;
        this.maxLength = maxLength - begin;
        this.windowState = begin > 0 ? 0 : 1;
        reset();
    }

    public int getTabs() {
        return tabs;
    }

    public void setTabs(int tabs) {
        this.tabs = tabs;
    }

    public List<String> getLines() {
        return lines;
    }

    public int getRealLength() {
        return ((StringWriter) out).getBuffer().length() - escapeLength;
    }

    @Override
    public void write(int data) throws IOException {
        if (data == '\n') {
            flushLine(true);
        } else if (data == '\t') {
            StringWriter baos = (StringWriter) out;
            do {
                write(' ');
            } while ((baos.getBuffer().length() - escapeLength) % tabs > 0);
        } else {
            if (windowState != 2) {
                super.write(data);
            }
            StringWriter baos = (StringWriter) out;
            if (windowState == 0 && baos.getBuffer().length() - escapeLength > begin) {
                windowState = 1;
                int nbMissing = baos.getBuffer().length() - escapeLength - begin;
                char[] old = baos.toString().toCharArray();
                beginAttributes();
                baos.write(old, old.length - nbMissing, nbMissing);
            } else if (windowState == 1 && baos.getBuffer().length() - escapeLength >= length) {
                windowState = 2;
                endAttributes();
                reset();
            }
            if (baos.getBuffer().length() - escapeLength >= maxLength) {
                flushLine(false);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (windowState == 0) {
            beginAttributes();
        }
        flushLine(forced);
        super.close();
    }

    protected void flushLine(boolean force) throws IOException {
        this.forced = force;
        StringWriter baos = (StringWriter) out;
        if (windowState == 0) {
            beginAttributes();
        }
        if (force || baos.getBuffer().length() > escapeLength) {
            endAttributes();
            lines.add(baos.toString());
            beginAttributes();
        }
        windowState = 0;
    }

    protected void endAttributes() throws IOException {
        if (intensity != Ansi.Attribute.INTENSITY_BOLD_OFF) {
            setAttribute(Ansi.Attribute.INTENSITY_BOLD_OFF);
        }
        if (underline != Ansi.Attribute.UNDERLINE_OFF) {
            setAttribute(Ansi.Attribute.UNDERLINE_OFF);
        }
        if (blink != Ansi.Attribute.BLINK_OFF) {
            setAttribute(Ansi.Attribute.BLINK_OFF);
        }
        if (negative != Ansi.Attribute.NEGATIVE_OFF) {
            setAttribute(Ansi.Attribute.NEGATIVE_OFF);
        }
        if (fg != Ansi.Color.DEFAULT) {
            setAttributeFg(Ansi.Color.DEFAULT);
        }
        if (bg != Ansi.Color.DEFAULT) {
            setAttributeBg(Ansi.Color.DEFAULT);
        }
    }

    protected void beginAttributes() throws IOException {
        ((StringWriter) out).getBuffer().setLength(0);
        escapeLength = 0;
        if (intensity != Ansi.Attribute.INTENSITY_BOLD_OFF) {
            setAttribute(intensity);
        }
        if (underline != Ansi.Attribute.UNDERLINE_OFF) {
            setAttribute(underline);
        }
        if (blink != Ansi.Attribute.BLINK_OFF) {
            setAttribute(blink);
        }
        if (negative != Ansi.Attribute.NEGATIVE_OFF) {
            setAttribute(negative);
        }
        if (fg != Ansi.Color.DEFAULT) {
            setAttributeFg(fg);
        }
        if (bg != Ansi.Color.DEFAULT) {
            setAttributeBg(bg);
        }
    }

    protected void setAttributeFg(Ansi.Color color) throws IOException {
        String sequence = Ansi.ansi().fg(color).toString();
        escapeLength += sequence.length();
        out.write(sequence);
    }

    protected void setAttributeBg(Ansi.Color color) throws IOException {
        String sequence = Ansi.ansi().bg(color).toString();
        escapeLength += sequence.length();
        out.write(sequence);
    }

    protected void setAttribute(Ansi.Attribute attribute) throws IOException {
        String sequence = Ansi.ansi().a(attribute).toString();
        escapeLength += sequence.length();
        out.write(sequence);
    }

}

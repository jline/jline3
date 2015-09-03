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

public class AnsiSplitterWriter extends AnsiWriter {

    protected static final int ATTRIBUTE_NEGATIVE_OFF = 27;

    Ansi.Attribute intensity;
    Ansi.Attribute underline;
    Ansi.Attribute blink;
    Ansi.Attribute negative;
    Ansi.Color fg;
    Ansi.Color bg;

    private int begin;
    private int length;
    private int maxLength;
    private int escapeLength;
    private int windowState;
    private int tabs;
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

    protected void reset() {
        intensity = Ansi.Attribute.INTENSITY_BOLD_OFF;
        underline = Ansi.Attribute.UNDERLINE_OFF;
        blink = Ansi.Attribute.BLINK_OFF;
        negative = Ansi.Attribute.NEGATIVE_OFF;
        fg = Ansi.Color.DEFAULT;
        bg = Ansi.Color.DEFAULT;
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
                flushLine(true);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (windowState == 0) {
            beginAttributes();
        }
        flushLine(lines.isEmpty());
        super.close();
    }

    void flushLine(boolean force) throws IOException {
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

    private void endAttributes() throws IOException {
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

    private void beginAttributes() throws IOException {
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

    @Override
    protected void processAttributeRest() throws IOException {
        setAttribute(Ansi.Attribute.RESET);
        reset();
    }

    @Override
    protected void processSetAttribute(int attribute) throws IOException {
        switch (attribute) {
            case ATTRIBUTE_INTENSITY_BOLD:
                setIntensity(Ansi.Attribute.INTENSITY_BOLD);
                break;
            case ATTRIBUTE_INTENSITY_FAINT:
                setIntensity(Ansi.Attribute.INTENSITY_FAINT);
                break;
            case ATTRIBUTE_INTENSITY_NORMAL:
                setIntensity(Ansi.Attribute.INTENSITY_BOLD_OFF);
                break;
            case ATTRIBUTE_UNDERLINE:
                setUnderline(Ansi.Attribute.UNDERLINE);
                break;
            case ATTRIBUTE_UNDERLINE_DOUBLE:
                setUnderline(Ansi.Attribute.UNDERLINE_DOUBLE);
                break;
            case ATTRIBUTE_UNDERLINE_OFF:
                setUnderline(Ansi.Attribute.UNDERLINE_OFF);
                break;
            case ATTRIBUTE_BLINK_OFF:
                setBlink(Ansi.Attribute.BLINK_OFF);
                break;
            case ATTRIBUTE_BLINK_SLOW:
                setBlink(Ansi.Attribute.BLINK_SLOW);
                break;
            case ATTRIBUTE_BLINK_FAST:
                setBlink(Ansi.Attribute.BLINK_FAST);
                break;
            case ATTRIBUTE_NEGATIVE_ON:
                setNegative(Ansi.Attribute.NEGATIVE_ON);
                break;
            case ATTRIBUTE_NEGATIVE_OFF:
                setNegative(Ansi.Attribute.NEGATIVE_OFF);
                break;
            default:
                break;
        }
    }

    @Override
    protected void processSetForegroundColor(int color) throws IOException {
        Ansi.Color c;
        switch (color) {
            case 0:
                c = Ansi.Color.BLACK;
                break;
            case 1:
                c = Ansi.Color.RED;
                break;
            case 2:
                c = Ansi.Color.GREEN;
                break;
            case 3:
                c = Ansi.Color.YELLOW;
                break;
            case 4:
                c = Ansi.Color.BLUE;
                break;
            case 5:
                c = Ansi.Color.MAGENTA;
                break;
            case 6:
                c = Ansi.Color.CYAN;
                break;
            case 7:
                c = Ansi.Color.WHITE;
                break;
            case 9:
                c = Ansi.Color.DEFAULT;
                break;
            default:
                return;
        }
        if (this.fg != c) {
            this.fg = c;
            setAttributeFg(c);
        }
    }

    @Override
    protected void processSetBackgroundColor(int color) throws IOException {
        Ansi.Color c;
        switch (color) {
            case 0:
                c = Ansi.Color.BLACK;
                break;
            case 1:
                c = Ansi.Color.RED;
                break;
            case 2:
                c = Ansi.Color.GREEN;
                break;
            case 3:
                c = Ansi.Color.YELLOW;
                break;
            case 4:
                c = Ansi.Color.BLUE;
                break;
            case 5:
                c = Ansi.Color.MAGENTA;
                break;
            case 6:
                c = Ansi.Color.CYAN;
                break;
            case 7:
                c = Ansi.Color.WHITE;
                break;
            case 9:
                c = Ansi.Color.DEFAULT;
                break;
            default:
                return;
        }
        if (this.bg != c) {
            this.bg = c;
            setAttributeBg(c);
        }
    }

    @Override
    protected void processDefaultTextColor() throws IOException {
        processSetForegroundColor(9);
    }

    @Override
    protected void processDefaultBackgroundColor() throws IOException {
        processSetBackgroundColor(9);
    }

    protected void setIntensity(Ansi.Attribute intensity) throws IOException {
        if (this.intensity != intensity) {
            this.intensity = intensity;
            setAttribute(intensity);
        }
    }

    protected void setUnderline(Ansi.Attribute underline) throws IOException {
        if (this.underline != underline) {
            this.underline = underline;
            setAttribute(underline);
        }
    }

    protected void setBlink(Ansi.Attribute blink) throws IOException {
        if (this.blink != blink) {
            this.blink = blink;
            setAttribute(blink);
        }
    }

    protected void setNegative(Ansi.Attribute negative) throws IOException {
        if (this.negative != negative) {
            this.negative = negative;
            setAttribute(negative);
        }
    }

    private void setAttributeFg(Ansi.Color color) throws IOException {
        String sequence = Ansi.ansi().fg(color).toString();
        escapeLength += sequence.length();
        out.write(sequence);
    }

    private void setAttributeBg(Ansi.Color color) throws IOException {
        String sequence = Ansi.ansi().bg(color).toString();
        escapeLength += sequence.length();
        out.write(sequence);
    }

    private void setAttribute(Ansi.Attribute attribute) throws IOException {
        String sequence = Ansi.ansi().a(attribute).toString();
        escapeLength += sequence.length();
        out.write(sequence);
    }

}

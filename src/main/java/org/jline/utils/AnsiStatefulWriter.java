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
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class AnsiStatefulWriter extends AnsiWriter {

    protected static final int ATTRIBUTE_NEGATIVE_OFF = 27;

    protected Ansi.Attribute intensity;
    protected Ansi.Attribute underline;
    protected Ansi.Attribute blink;
    protected Ansi.Attribute negative;
    protected Ansi.Color fg;
    protected Ansi.Color bg;

    public AnsiStatefulWriter(Writer writer) {
        super(writer);
        reset();
    }

    protected void reset() {
        intensity = Ansi.Attribute.INTENSITY_BOLD_OFF;
        underline = Ansi.Attribute.UNDERLINE_OFF;
        blink = Ansi.Attribute.BLINK_OFF;
        negative = Ansi.Attribute.NEGATIVE_OFF;
        fg = Ansi.Color.DEFAULT;
        bg = Ansi.Color.DEFAULT;
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

    protected void setAttributeFg(Ansi.Color color) throws IOException {
    }

    protected void setAttributeBg(Ansi.Color color) throws IOException {
    }

    protected void setAttribute(Ansi.Attribute attribute) throws IOException {
    }

}

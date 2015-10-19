/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attributed string.
 * Instances of this class are immutables.
 * Substrings are created without any memory copy.
 *
 * @author <a href="mailto:gnodet@gmail.com">Guillaume Nodet</a>
 */
public class AttributedString extends AttributedCharSequence {

    final char[] buffer;
    final int[] style;
    final int start;
    final int end;

    public AttributedString(CharSequence str) {
        this(str, 0, str.length(), null);
    }

    public AttributedString(CharSequence str, int start, int end) {
        this(str, start, end, null);
    }

    public AttributedString(CharSequence str, AttributedStyle s) {
        this(str, 0, str.length(), s);
    }

    public AttributedString(CharSequence str, int start, int end, AttributedStyle s) {
        if (end < start) {
            throw new InvalidParameterException();
        }
        if (str instanceof AttributedString) {
            AttributedString as = (AttributedString) str;
            this.buffer = as.buffer;
            if (s != null) {
                this.style = as.style.clone();
                for (int i = 0; i < style.length; i++) {
                    this.style[i] = (this.style[i] & ~s.getMask()) | s.getStyle();
                }
            } else {
                this.style = as.style;
            }
            this.start = as.start + start;
            this.end = as.start + end;
        } else {
            int l = end - start;
            buffer = new char[l];
            for (int i = 0; i < l; i++) {
                buffer[i] = str.charAt(start + i);
            }
            style = new int[l];
            if (s != null) {
                Arrays.fill(style, s.getStyle());
            }
            this.start = 0;
            this.end = l;
        }
    }

    AttributedString(char[] buffer, int[] style, int start, int end) {
        this.buffer = buffer;
        this.style = style;
        this.start = start;
        this.end = end;
    }

    public static AttributedString fromAnsi(String ansi) {
        if (ansi == null) {
            return null;
        }
        AttributedStringBuilder sb = new AttributedStringBuilder(ansi.length());
        sb.appendAnsi(ansi);
        return sb.toAttributedString();
    }

    public static String stripAnsi(String ansi) {
        if (ansi == null) {
            return null;
        }
        AttributedStringBuilder sb = new AttributedStringBuilder(ansi.length());
        sb.appendAnsi(ansi);
        return sb.toString();
    }

    @Override
    protected char[] buffer() {
        return buffer;
    }

    @Override
    protected int offset() {
        return start;
    }

    @Override
    public int length() {
        return end - start;
    }

    public AttributedStyle styleAt(int index) {
        return new AttributedStyle(style[start + index], style[start + index]);
    }

    @Override
    public AttributedString subSequence(int start, int end) {
        return new AttributedString(this, start, end);
    }

    public AttributedString styleMatches(Pattern pattern, AttributedStyle style) {
        Matcher matcher = pattern.matcher(this);
        boolean result = matcher.find();
        if (result) {
            int[] newstyle = this.style.clone();
            do {
                for (int i = matcher.start(); i < matcher.end(); i++) {
                    newstyle[this.start + i] = (newstyle[this.start + i] & ~style.getMask()) | style.getStyle();
                }
                result = matcher.find();
            } while (result);
            return new AttributedString(buffer, newstyle, start , end);
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributedString that = (AttributedString) o;
        if (start != that.start) return false;
        if (end != that.end) return false;
        if (!Arrays.equals(buffer, that.buffer)) return false;
        return Arrays.equals(style, that.style);

    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(buffer);
        result = 31 * result + Arrays.hashCode(style);
        result = 31 * result + start;
        result = 31 * result + end;
        return result;
    }

    public static AttributedString join(AttributedString delimiter, AttributedString... elements) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(elements);
        return join(delimiter, Arrays.asList(elements));
    }

    public static AttributedString join(AttributedString delimiter, Iterable<AttributedString> elements) {
        Objects.requireNonNull(delimiter);
        Objects.requireNonNull(elements);
        AttributedStringBuilder sb = new AttributedStringBuilder();
        int i = 0;
        for (AttributedString str : elements) {
            if (i++ > 0) {
                sb.append(delimiter);
            }
            sb.append(str);
        }
        return sb.toAttributedString();
    }

}

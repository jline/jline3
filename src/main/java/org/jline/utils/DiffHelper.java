/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.utils;

import java.io.IOError;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jline.utils.Ansi.Attribute;
import org.jline.utils.Ansi.Color;

/**
 * Class containing the diff method.
 * This diff is ANSI aware and will correctly handle text attributes
 * so that any text in a Diff object is a valid ansi string.
 */
public class DiffHelper {

    /**
     * The data structure representing a diff is a Linked list of Diff objects:
     * {Diff(Operation.DELETE, "Hello"), Diff(Operation.INSERT, "Goodbye"),
     *  Diff(Operation.EQUAL, " world.")}
     * which means: delete "Hello", add "Goodbye" and keep " world."
     */
    public enum Operation {
        DELETE, INSERT, EQUAL
    }

    /**
     * Class representing one diff operation.
     */
    public static class Diff {
        /**
         * One of: INSERT, DELETE or EQUAL.
         */
        public final Operation operation;
        /**
         * The text associated with this diff operation.
         */
        public final String text;

        /**
         * Constructor.  Initializes the diff with the provided values.
         * @param operation One of INSERT, DELETE or EQUAL.
         * @param text The text being applied.
         */
        public Diff(Operation operation, String text) {
            // Construct a diff with the specified operation and text.
            this.operation = operation;
            this.text = text;
        }

        /**
         * Display a human-readable version of this Diff.
         * @return text version.
         */
        public String toString() {
            return "Diff(" + this.operation + ",\"" + this.text + "\")";
        }
    }

    /**
     * Compute a list of difference between two lines.
     * The result will contain at most 4 Diff objects, as the method
     * aims to return the common prefix, inserted text, deleted text and
     * common suffix.
     * The computation is done on characters and their attributes expressed
     * as ansi sequences.
     *
     * @param text1 the first line
     * @param text2 the second line
     * @return a list of Diff
     */
    public static List<Diff> diff(String text1, String text2) {
        List<TerminalChar> tc1 = getChars(text1);
        List<TerminalChar> tc2 = getChars(text2);

        int l1 = tc1.size();
        int l2 = tc2.size();
        int n = Math.min(l1, l2);
        int commonStart = 0;
        while (commonStart < n
                && tc1.get(commonStart).equals(tc2.get(commonStart))) {
            commonStart++;
        }
        int commonEnd = 0;
        while (commonEnd < n - commonStart
                && tc1.get(l1 - commonEnd - 1).equals(tc2.get(l2 - commonEnd - 1))) {
            commonEnd++;
        }

        LinkedList<Diff> diffs = new LinkedList<>();
        if (commonStart > 0) {
            diffs.add(new Diff(DiffHelper.Operation.EQUAL,
                    toString(tc1.subList(0, commonStart))));
        }
        if (l2 > commonStart + commonEnd) {
            diffs.add(new Diff(DiffHelper.Operation.INSERT,
                    toString(tc2.subList(commonStart, l2 - commonEnd))));
        }
        if (l1 > commonStart + commonEnd) {
            diffs.add(new Diff(DiffHelper.Operation.DELETE,
                    toString(tc1.subList(commonStart, l1 - commonEnd))));
        }
        if (commonEnd > 0) {
            diffs.add(new Diff(DiffHelper.Operation.EQUAL,
                    toString(tc1.subList(l1 - commonEnd, l1))));
        }
        return diffs;
    }

    private static List<TerminalChar> getChars(String ansiString) {
        try {
            AnsiCharProducer producer = new AnsiCharProducer();
            producer.write(ansiString);
            return producer.getLine();
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static String toString(List<TerminalChar> chars) {
        TerminalChar last = new TerminalChar(0, Color.DEFAULT, Color.DEFAULT, Attribute.INTENSITY_BOLD_OFF, Attribute.UNDERLINE_OFF, Attribute.BLINK_OFF, Attribute.NEGATIVE_OFF);
        Ansi ansi = Ansi.ansi();
        boolean modified = false;
        for (TerminalChar tc : chars) {
            if (last.fg != tc.fg) {
                ansi.fg(tc.fg);
                modified = true;
            }
            if (last.bg != tc.bg) {
                ansi.fg(tc.fg);
                modified = true;
            }
            if (last.intensity != tc.intensity) {
                ansi.a(tc.intensity);
                modified = true;
            }
            if (last.underline != tc.underline) {
                ansi.a(tc.underline);
                modified = true;
            }
            if (last.blink != tc.blink) {
                ansi.a(tc.blink);
                modified = true;
            }
            if (last.negative != tc.negative) {
                ansi.a(tc.negative);
                modified = true;
            }
            ansi.a(new String(Character.toChars(tc.chr)));
            last = tc;
        }
        if (modified) {
            ansi.reset();
        }
        return ansi.toString();
    }


    private static class TerminalChar {
        final int chr;
        final Ansi.Color fg;
        final Ansi.Color bg;
        final Ansi.Attribute intensity;
        final Ansi.Attribute underline;
        final Ansi.Attribute blink;
        final Ansi.Attribute negative;

        public TerminalChar(int chr, Color fg, Color bg, Attribute intensity, Attribute underline, Attribute blink, Attribute negative) {
            this.chr = chr;
            this.fg = fg;
            this.bg = bg;
            this.intensity = intensity;
            this.underline = underline;
            this.blink = blink;
            this.negative = negative;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TerminalChar that = (TerminalChar) o;

            if (chr != that.chr) return false;
            if (fg != that.fg) return false;
            if (bg != that.bg) return false;
            if (intensity != that.intensity) return false;
            if (underline != that.underline) return false;
            if (blink != that.blink) return false;
            return negative == that.negative;

        }

        @Override
        public int hashCode() {
            int result = chr;
            result = 31 * result + fg.hashCode();
            result = 31 * result + bg.hashCode();
            result = 31 * result + intensity.hashCode();
            result = 31 * result + underline.hashCode();
            result = 31 * result + blink.hashCode();
            result = 31 * result + negative.hashCode();
            return result;
        }
    }

    private static class AnsiCharProducer extends AnsiStatefulWriter {

        private List<TerminalChar> line = new ArrayList<>();
        private int surrogate;

        public AnsiCharProducer() {
            super(new StringWriter());
            reset();
        }

        protected void doWrite(int c) throws IOException {
            if (Character.isHighSurrogate((char) c)) {
                surrogate = c;
                return;
            }
            if (surrogate > 0) {
                c = Character.toCodePoint((char) surrogate, (char) c);
                surrogate = 0;
            }
            TerminalChar tc = new TerminalChar(c, fg, bg, intensity, underline, blink, negative);
            line.add(tc);
        }

        public List<TerminalChar> getLine() {
            return line;
        }

    }

}
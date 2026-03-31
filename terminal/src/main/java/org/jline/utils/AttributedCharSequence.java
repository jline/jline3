/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.util.ArrayList;
import java.util.List;

import org.jline.terminal.Terminal;
import org.jline.utils.InfoCmp.Capability;

import static org.jline.terminal.TerminalBuilder.PROP_DISABLE_ALTERNATE_CHARSET;
import static org.jline.utils.AttributedStyle.BG_COLOR;
import static org.jline.utils.AttributedStyle.BG_COLOR_EXP;
import static org.jline.utils.AttributedStyle.FG_COLOR;
import static org.jline.utils.AttributedStyle.FG_COLOR_EXP;
import static org.jline.utils.AttributedStyle.F_BACKGROUND;
import static org.jline.utils.AttributedStyle.F_BACKGROUND_IND;
import static org.jline.utils.AttributedStyle.F_BACKGROUND_RGB;
import static org.jline.utils.AttributedStyle.F_BLINK;
import static org.jline.utils.AttributedStyle.F_BOLD;
import static org.jline.utils.AttributedStyle.F_CONCEAL;
import static org.jline.utils.AttributedStyle.F_CROSSED_OUT;
import static org.jline.utils.AttributedStyle.F_FAINT;
import static org.jline.utils.AttributedStyle.F_FOREGROUND;
import static org.jline.utils.AttributedStyle.F_FOREGROUND_IND;
import static org.jline.utils.AttributedStyle.F_FOREGROUND_RGB;
import static org.jline.utils.AttributedStyle.F_HIDDEN;
import static org.jline.utils.AttributedStyle.F_INVERSE;
import static org.jline.utils.AttributedStyle.F_ITALIC;
import static org.jline.utils.AttributedStyle.F_UNDERLINE;
import static org.jline.utils.AttributedStyle.MASK;

/**
 * A character sequence with ANSI style attributes.
 *
 * <p>
 * The AttributedCharSequence class is an abstract base class for character sequences
 * that have ANSI style attributes (colors, bold, underline, etc.) associated with
 * each character. It provides methods for rendering the character sequence with its
 * attributes to various outputs, such as ANSI terminals, non-ANSI terminals, and
 * plain text.
 * </p>
 *
 * <p>
 * This class serves as the foundation for styled text in JLine, allowing for rich
 * text formatting in terminal applications. It is extended by concrete classes like
 * {@link AttributedString} and {@link AttributedStringBuilder} that provide specific
 * implementations for different use cases.
 * </p>
 *
 * <p>
 * The class provides methods to:
 * </p>
 * <ul>
 *   <li>Convert the sequence to a plain string without attributes</li>
 *   <li>Render the sequence with ANSI escape codes for compatible terminals</li>
 *   <li>Render the sequence for terminals with limited attribute support</li>
 *   <li>Calculate the visible length of the sequence (excluding escape codes)</li>
 *   <li>Extract substrings while preserving attributes</li>
 * </ul>
 */
public abstract class AttributedCharSequence implements CharSequence {

    /**
     * Default constructor.
     */
    public AttributedCharSequence() {
        // Default constructor
    }

    public static final int TRUE_COLORS = 0x1000000;
    private static final int HIGH_COLORS = 0x7FFF;

    /**
     * Enum defining color mode forcing options for ANSI rendering.
     *
     * <p>
     * This enum specifies how color rendering should be forced when generating
     * ANSI escape sequences, regardless of the terminal's reported capabilities.
     * </p>
     */
    public enum ForceMode {
        /**
         * No forcing; use the terminal's reported color capabilities.
         */
        None,

        /**
         * Force the use of 256-color mode (8-bit colors).
         */
        Force256Colors,

        /**
         * Force the use of true color mode (24-bit RGB colors).
         */
        ForceTrueColors
    }

    // cache the value here as we can't afford to get it each time
    static final boolean DISABLE_ALTERNATE_CHARSET = Boolean.getBoolean(PROP_DISABLE_ALTERNATE_CHARSET);

    /**
     * Prints this attributed string to the specified terminal.
     *
     * <p>
     * This method renders the attributed string with appropriate ANSI escape
     * sequences for the specified terminal and prints it to the terminal's writer.
     * </p>
     *
     * @param terminal the terminal to print to
     */
    public void print(Terminal terminal) {
        terminal.writer().print(toAnsi(terminal));
    }

    /**
     * Prints this attributed string to the specified terminal, followed by a line break.
     *
     * <p>
     * This method renders the attributed string with appropriate ANSI escape
     * sequences for the specified terminal and prints it to the terminal's writer,
     * followed by a line break.
     * </p>
     *
     * @param terminal the terminal to print to
     */
    public void println(Terminal terminal) {
        terminal.writer().println(toAnsi(terminal));
    }

    /**
     * Converts this attributed string to an ANSI escape sequence string.
     *
     * <p>
     * This method renders the attributed string with ANSI escape sequences
     * to represent the text attributes (colors, bold, underline, etc.).
     * It uses default color capabilities (256 colors) and no forced color mode.
     * </p>
     *
     * @return a string with ANSI escape sequences representing this attributed string
     * @see #toAnsi(Terminal)
     */
    public String toAnsi() {
        return toAnsi(null);
    }

    /**
     * Converts this attributed string to an ANSI escape sequence string
     * appropriate for the specified terminal.
     *
     * <p>
     * This method renders the attributed string with ANSI escape sequences
     * to represent the text attributes (colors, bold, underline, etc.),
     * taking into account the capabilities of the specified terminal.
     * </p>
     *
     * <p>
     * If the terminal is a dumb terminal (Terminal.TYPE_DUMB), this method
     * returns the plain text without any escape sequences.
     * </p>
     *
     * @param terminal the terminal to generate ANSI sequences for, or null to use default capabilities
     * @return a string with ANSI escape sequences representing this attributed string
     */
    public String toAnsi(Terminal terminal) {
        if (terminal != null && Terminal.TYPE_DUMB.equals(terminal.getType())) {
            return toString();
        }
        int colors = 256;
        ForceMode forceMode = ForceMode.None;
        ColorPalette palette = null;
        String alternateIn = null, alternateOut = null;
        if (terminal != null) {
            Integer max_colors = terminal.getNumericCapability(Capability.max_colors);
            if (max_colors != null) {
                colors = max_colors;
            }
            palette = terminal.getPalette();
            if (!DISABLE_ALTERNATE_CHARSET) {
                alternateIn = Curses.tputs(terminal.getStringCapability(Capability.enter_alt_charset_mode));
                alternateOut = Curses.tputs(terminal.getStringCapability(Capability.exit_alt_charset_mode));
            }
        }
        return toAnsi(colors, forceMode, palette, alternateIn, alternateOut);
    }

    /**
     * Converts this attributed string to an ANSI escape sequence string
     * with the specified color capabilities and force mode.
     *
     * <p>
     * This method renders the attributed string with ANSI escape sequences
     * using the specified number of colors and force mode.
     * </p>
     *
     * @param colors the number of colors to use (8, 256, or 16777216 for true colors)
     * @param force the force mode to use for color rendering
     * @return a string with ANSI escape sequences representing this attributed string
     */
    public String toAnsi(int colors, ForceMode force) {
        return toAnsi(colors, force, null, null, null);
    }

    /**
     * Converts this attributed string to an ANSI escape sequence string
     * with the specified color capabilities, force mode, and color palette.
     *
     * <p>
     * This method renders the attributed string with ANSI escape sequences
     * using the specified number of colors, force mode, and color palette.
     * </p>
     *
     * @param colors the number of colors to use (8, 256, or 16777216 for true colors)
     * @param force the force mode to use for color rendering
     * @param palette the color palette to use for color conversion
     * @return a string with ANSI escape sequences representing this attributed string
     */
    public String toAnsi(int colors, ForceMode force, ColorPalette palette) {
        return toAnsi(colors, force, palette, null, null);
    }

    /**
     * Converts this attributed string to an ANSI escape sequence string
     * with the specified color capabilities, force mode, color palette,
     * and alternate character set sequences.
     *
     * <p>
     * This method renders the attributed string with ANSI escape sequences
     * using the specified number of colors, force mode, color palette, and
     * alternate character set sequences for box drawing characters.
     * </p>
     *
     * @param colors the number of colors to use (8, 256, or 16777216 for true colors)
     * @param force the force mode to use for color rendering
     * @param palette the color palette to use for color conversion, or null for the default palette
     * @param altIn the sequence to enable the alternate character set, or null to disable
     * @param altOut the sequence to disable the alternate character set, or null to disable
     * @return a string with ANSI escape sequences representing this attributed string
     */
    public String toAnsi(int colors, ForceMode force, ColorPalette palette, String altIn, String altOut) {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        toAnsiBytes(buf, colors, force, palette, altIn, altOut);
        return buf.toStringUtf8();
    }

    /**
     * Writes the ANSI rendering of this attributed string directly as UTF-8 bytes
     * to the given {@link ByteArrayBuilder}, avoiding intermediate String allocations.
     *
     * <p>This method produces output equivalent to
     * {@link #toAnsi(int, ForceMode, ColorPalette, String, String)} but writes bytes
     * directly, eliminating StringBuilder, Integer.toString(), and charset encoding overhead.</p>
     *
     * @param buf     the byte buffer to write to
     * @param colors  the number of colors to use
     * @param force   the force mode for color rendering
     * @param palette the color palette, or null for default
     * @param altIn   the alternate charset enter sequence, or null
     * @param altOut  the alternate charset exit sequence, or null
     */
    void toAnsiBytes(
            ByteArrayBuilder buf, int colors, ForceMode force, ColorPalette palette, String altIn, String altOut) {
        long style = 0;
        long[] colorState = {0, 0}; // [foreground, background]
        boolean alt = false;
        if (palette == null) {
            palette = ColorPalette.DEFAULT;
        }
        int i = 0;
        int len = length();
        while (i < len) {
            char c = substituteChar(charAt(i), altIn, altOut);
            alt = emitAltCharset(buf, charAt(i), alt, altIn, altOut);
            long s = styleCodeAt(i) & ~F_HIDDEN;
            if (style != s) {
                emitStyleChange(buf, style, s, colorState, colors, force, palette);
                style = s;
            }
            i += emitUtf8Char(buf, c, i, len);
        }
        if (alt) {
            buf.appendAscii(altOut);
        }
        if (style != 0) {
            buf.csi().appendAscii("0m");
        }
    }

    private static boolean emitAltCharset(
            ByteArrayBuilder buf, char originalChar, boolean alt, String altIn, String altOut) {
        if (altIn != null && altOut != null) {
            boolean newAlt = isBoxDrawing(originalChar);
            if (alt != newAlt) {
                buf.appendAscii(newAlt ? altIn : altOut);
            }
            return newAlt;
        }
        return alt;
    }

    private int emitUtf8Char(ByteArrayBuilder buf, char c, int i, int len) {
        if (Character.isHighSurrogate(c) && i + 1 < len) {
            char next = charAt(i + 1);
            if (Character.isLowSurrogate(next)) {
                buf.appendUtf8(Character.toCodePoint(c, next));
                return 2;
            }
        }
        buf.appendUtf8(c);
        return 1;
    }

    private static void emitStyleChange(
            ByteArrayBuilder buf,
            long prevStyle,
            long newStyle,
            long[] colorState,
            int colors,
            ForceMode force,
            ColorPalette palette) {
        long fg = (newStyle & F_FOREGROUND) != 0 ? newStyle & (FG_COLOR | F_FOREGROUND) : 0;
        long bg = (newStyle & F_BACKGROUND) != 0 ? newStyle & (BG_COLOR | F_BACKGROUND) : 0;
        if (newStyle == 0) {
            buf.csi().appendAscii("0m");
            colorState[0] = 0;
            colorState[1] = 0;
            return;
        }
        long d = (prevStyle ^ newStyle) & MASK;
        buf.csi();
        boolean first = true;
        first = appendDecorationAttrsB(buf, d, newStyle, first);
        if (colorState[0] != fg) {
            first = appendColorB(
                    buf,
                    fg,
                    F_FOREGROUND_RGB,
                    F_FOREGROUND_IND,
                    FG_COLOR_EXP,
                    38,
                    30,
                    90,
                    "39",
                    colors,
                    force,
                    palette,
                    first);
            if (fg > 0 && usedBasicFgColor(fg, colors, force, palette)) {
                d |= (newStyle & F_BOLD);
            }
            colorState[0] = fg;
        }
        if (colorState[1] != bg) {
            first = appendColorB(
                    buf,
                    bg,
                    F_BACKGROUND_RGB,
                    F_BACKGROUND_IND,
                    BG_COLOR_EXP,
                    48,
                    40,
                    100,
                    "49",
                    colors,
                    force,
                    palette,
                    first);
            colorState[1] = bg;
        }
        first = appendBoldFaintB(buf, d, newStyle, first);
        buf.appendAscii('m');
    }

    private static boolean appendDecorationAttrsB(ByteArrayBuilder buf, long d, long s, boolean first) {
        if ((d & F_ITALIC) != 0) {
            first = attrB(buf, (s & F_ITALIC) != 0 ? "3" : "23", first);
        }
        if ((d & F_UNDERLINE) != 0) {
            first = attrB(buf, (s & F_UNDERLINE) != 0 ? "4" : "24", first);
        }
        if ((d & F_BLINK) != 0) {
            first = attrB(buf, (s & F_BLINK) != 0 ? "5" : "25", first);
        }
        if ((d & F_INVERSE) != 0) {
            first = attrB(buf, (s & F_INVERSE) != 0 ? "7" : "27", first);
        }
        if ((d & F_CONCEAL) != 0) {
            first = attrB(buf, (s & F_CONCEAL) != 0 ? "8" : "28", first);
        }
        if ((d & F_CROSSED_OUT) != 0) {
            first = attrB(buf, (s & F_CROSSED_OUT) != 0 ? "9" : "29", first);
        }
        return first;
    }

    private static boolean appendColorB(
            ByteArrayBuilder buf,
            long colorValue,
            long rgbFlag,
            long indFlag,
            int colorExp,
            int csiPrefix,
            int lowBase,
            int highBase,
            String defaultCode,
            int colors,
            ForceMode force,
            ColorPalette palette,
            boolean first) {
        if (colorValue <= 0) {
            return attrB(buf, defaultCode, first);
        }
        if ((colorValue & rgbFlag) != 0) {
            int r = (int) (colorValue >> (colorExp + 16)) & 0xFF;
            int g = (int) (colorValue >> (colorExp + 8)) & 0xFF;
            int b = (int) (colorValue >> colorExp) & 0xFF;
            if (colors >= HIGH_COLORS) {
                return attrRgbB(buf, csiPrefix, r, g, b, first);
            }
            return appendRoundedColorB(
                    buf, palette.round(r, g, b), csiPrefix, lowBase, highBase, colors, force, palette, first);
        }
        if ((colorValue & indFlag) != 0) {
            int rounded = palette.round((int) (colorValue >> colorExp) & 0xFF);
            return appendRoundedColorB(buf, rounded, csiPrefix, lowBase, highBase, colors, force, palette, first);
        }
        return first;
    }

    private static boolean appendRoundedColorB(
            ByteArrayBuilder buf,
            int rounded,
            int csiPrefix,
            int lowBase,
            int highBase,
            int colors,
            ForceMode force,
            ColorPalette palette,
            boolean first) {
        if (rounded < 0) {
            return first;
        }
        if (colors >= HIGH_COLORS && force == ForceMode.ForceTrueColors) {
            int col = palette.getColor(rounded);
            return attrRgbB(buf, csiPrefix, (col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, first);
        }
        if (force == ForceMode.Force256Colors || rounded >= 16) {
            return attrIdxB(buf, csiPrefix, rounded, first);
        }
        if (rounded >= 8) {
            return attrIntB(buf, highBase + rounded - 8, first);
        }
        return attrIntB(buf, lowBase + rounded, first);
    }

    private static boolean usedBasicFgColor(long fg, int colors, ForceMode force, ColorPalette palette) {
        int rounded;
        if ((fg & F_FOREGROUND_RGB) != 0) {
            if (colors >= HIGH_COLORS) {
                return false;
            }
            int r = (int) (fg >> (FG_COLOR_EXP + 16)) & 0xFF;
            int g = (int) (fg >> (FG_COLOR_EXP + 8)) & 0xFF;
            int b = (int) (fg >> FG_COLOR_EXP) & 0xFF;
            rounded = palette.round(r, g, b);
        } else if ((fg & F_FOREGROUND_IND) != 0) {
            rounded = palette.round((int) (fg >> FG_COLOR_EXP) & 0xFF);
        } else {
            return false;
        }
        return rounded >= 0
                && rounded < 16
                && !(colors >= HIGH_COLORS && force == ForceMode.ForceTrueColors)
                && force != ForceMode.Force256Colors;
    }

    private static boolean appendBoldFaintB(ByteArrayBuilder buf, long d, long s, boolean first) {
        if ((d & (F_BOLD | F_FAINT)) != 0) {
            if ((d & F_BOLD) != 0 && (s & F_BOLD) == 0 || (d & F_FAINT) != 0 && (s & F_FAINT) == 0) {
                first = attrB(buf, "22", first);
            }
            if ((d & F_BOLD) != 0 && (s & F_BOLD) != 0) {
                first = attrB(buf, "1", first);
            }
            if ((d & F_FAINT) != 0 && (s & F_FAINT) != 0) {
                first = attrB(buf, "2", first);
            }
        }
        return first;
    }

    // @spotless:off
    /**
     * Substitutes box-drawing characters with alternate charset or ASCII equivalents.
     */
    private static char substituteChar(char c, String altIn, String altOut) {
        if (altIn != null && altOut != null) {
            switch (c) {
                case '┘': return 'j';
                case '┐': return 'k';
                case '┌': return 'l';
                case '└': return 'm';
                case '┼': return 'n';
                case '─': return 'q';
                case '├': return 't';
                case '┤': return 'u';
                case '┴': return 'v';
                case '┬': return 'w';
                case '│': return 'x';
                default:  return c;
            }
        } else {
            switch (c) {
                case '┘': case '┐': case '┌': case '└': return '+';
                case '┼': return '+';
                case '─': return '-';
                case '├': case '┤': case '┴': case '┬': return '+';
                case '│': return '|';
                default:  return c;
            }
        }
    }

    private static boolean isBoxDrawing(char c) {
        switch (c) {
            case '┘': case '┐': case '┌': case '└':
            case '┼': case '─': case '├': case '┤':
            case '┴': case '┬': case '│':
                return true;
            default:
                return false;
        }
    }
    // @spotless:on

    // ByteArrayBuilder helpers — zero-allocation integer formatting
    private static boolean attrB(ByteArrayBuilder buf, String s, boolean first) {
        if (!first) buf.appendAscii(';');
        buf.appendAscii(s);
        return false;
    }

    private static boolean attrIntB(ByteArrayBuilder buf, int value, boolean first) {
        if (!first) buf.appendAscii(';');
        buf.appendInt(value);
        return false;
    }

    private static boolean attrRgbB(ByteArrayBuilder buf, int prefix, int r, int g, int b, boolean first) {
        if (!first) buf.appendAscii(';');
        buf.appendInt(prefix)
                .appendAscii(";2;")
                .appendInt(r)
                .appendAscii(';')
                .appendInt(g)
                .appendAscii(';')
                .appendInt(b);
        return false;
    }

    private static boolean attrIdxB(ByteArrayBuilder buf, int prefix, int idx, boolean first) {
        if (!first) buf.appendAscii(';');
        buf.appendInt(prefix).appendAscii(";5;").appendInt(idx);
        return false;
    }

    /**
     * Returns the style at the specified index in this attributed string.
     *
     * <p>
     * This method returns the AttributedStyle object associated with the
     * character at the specified index in this attributed string.
     * </p>
     *
     * @param index the index of the character whose style to return
     * @return the style at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public abstract AttributedStyle styleAt(int index);

    /**
     * Returns the style code at the specified index in this attributed string.
     *
     * <p>
     * This method returns the raw style code (as a long value) associated with the
     * character at the specified index in this attributed string.
     * </p>
     *
     * @param index the index of the character whose style code to return
     * @return the style code at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    long styleCodeAt(int index) {
        return styleAt(index).getStyle();
    }

    /**
     * Returns whether the character at the specified index is hidden.
     *
     * <p>
     * This method checks if the character at the specified index has the
     * hidden attribute set, which means it should not be displayed.
     * </p>
     *
     * @param index the index of the character to check
     * @return true if the character is hidden, false otherwise
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public boolean isHidden(int index) {
        return (styleCodeAt(index) & F_HIDDEN) != 0;
    }

    /**
     * Returns the start index of the run of characters with the same style
     * that includes the character at the specified index.
     *
     * <p>
     * A run is a sequence of consecutive characters that have the same style.
     * This method finds the first character in the run that includes the
     * character at the specified index.
     * </p>
     *
     * @param index the index of a character in the run
     * @return the start index of the run (inclusive)
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public int runStart(int index) {
        AttributedStyle style = styleAt(index);
        while (index > 0 && styleAt(index - 1).equals(style)) {
            index--;
        }
        return index;
    }

    /**
     * Returns the limit index of the run of characters with the same style
     * that includes the character at the specified index.
     *
     * <p>
     * A run is a sequence of consecutive characters that have the same style.
     * This method finds the index after the last character in the run that
     * includes the character at the specified index.
     * </p>
     *
     * @param index the index of a character in the run
     * @return the limit index of the run (exclusive)
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public int runLimit(int index) {
        AttributedStyle style = styleAt(index);
        while (index < length() - 1 && styleAt(index + 1).equals(style)) {
            index++;
        }
        return index + 1;
    }

    @Override
    public abstract AttributedString subSequence(int start, int end);

    /**
     * Returns a new AttributedString that is a substring of this attributed string.
     *
     * <p>
     * This method returns a new AttributedString that contains the characters and
     * styles from this attributed string starting at the specified start index
     * (inclusive) and ending at the specified end index (exclusive).
     * </p>
     *
     * <p>
     * This method is equivalent to {@link #subSequence(int, int)} but returns
     * an AttributedString instead of an AttributedCharSequence.
     * </p>
     *
     * @param start the start index, inclusive
     * @param end the end index, exclusive
     * @return the specified substring with its attributes
     * @throws IndexOutOfBoundsException if start or end are negative,
     *         if end is greater than length(), or if start is greater than end
     */
    public AttributedString substring(int start, int end) {
        return subSequence(start, end);
    }

    protected abstract char[] buffer();

    protected abstract int offset();

    @Override
    public char charAt(int index) {
        return buffer()[offset() + index];
    }

    /**
     * Returns the code point at the specified index in this attributed string.
     *
     * <p>
     * This method returns the Unicode code point at the specified index.
     * If the character at the specified index is a high-surrogate code unit
     * and the following character is a low-surrogate code unit, then the
     * supplementary code point is returned; otherwise, the code unit at the
     * specified index is returned.
     * </p>
     *
     * @param index the index to the code point
     * @return the code point at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public int codePointAt(int index) {
        return Character.codePointAt(buffer(), index + offset());
    }

    /**
     * Returns whether this attributed string contains the specified character.
     *
     * <p>
     * This method checks if the specified character appears in this attributed string.
     * </p>
     *
     * @param c the character to search for
     * @return true if this attributed string contains the specified character, false otherwise
     */
    public boolean contains(char c) {
        for (int i = 0; i < length(); i++) {
            if (charAt(i) == c) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the code point before the specified index in this attributed string.
     *
     * <p>
     * This method returns the Unicode code point before the specified index.
     * If the character before the specified index is a low-surrogate code unit
     * and the character before that is a high-surrogate code unit, then the
     * supplementary code point is returned; otherwise, the code unit before the
     * specified index is returned.
     * </p>
     *
     * @param index the index following the code point that should be returned
     * @return the Unicode code point value before the specified index
     * @throws IndexOutOfBoundsException if the index is less than 1 or greater than length()
     */
    public int codePointBefore(int index) {
        return Character.codePointBefore(buffer(), index + offset());
    }

    /**
     * Returns the number of Unicode code points in the specified range of this attributed string.
     *
     * <p>
     * This method counts the number of Unicode code points in the range of this
     * attributed string starting at the specified index (inclusive) and extending
     * for the specified length. A surrogate pair is counted as one code point.
     * </p>
     *
     * @param index the index to the first character of the range
     * @param length the length of the range in characters
     * @return the number of Unicode code points in the specified range
     * @throws IndexOutOfBoundsException if index is negative, or length is negative,
     *         or index + length is greater than length()
     */
    public int codePointCount(int index, int length) {
        return Character.codePointCount(buffer(), index + offset(), length);
    }

    /**
     * Returns the display width of this attributed string in columns.
     *
     * <p>
     * This method calculates the display width of this attributed string in columns,
     * taking into account wide characters (such as East Asian characters), zero-width
     * characters (such as combining marks), and hidden characters. This is useful for
     * determining how much space the string will occupy when displayed in a terminal.
     * </p>
     *
     * <p>
     * Hidden characters (those with the hidden attribute set) are not counted in the
     * column length.
     * </p>
     *
     * @return the display width of this attributed string in columns
     */
    public int columnLength() {
        return columnLength(null);
    }

    /**
     * Returns the display width of this attributed string in columns.
     *
     * <p>When the terminal has grapheme cluster mode enabled, multi-codepoint
     * sequences (ZWJ emoji, flags, etc.) are measured as single display units
     * matching the terminal's cursor positioning.</p>
     *
     * @param terminal the terminal to query for grapheme cluster mode, or {@code null}
     * @return the display width in columns
     */
    public int columnLength(Terminal terminal) {
        int cols = 0;
        int len = length();
        for (int cur = 0; cur < len; ) {
            int charCount = WCWidth.charCountForDisplay(this, cur, terminal);
            int w = isHidden(cur) ? 0 : WCWidth.wcwidthForDisplay(this, cur, terminal, charCount);
            cur += charCount;
            cols += w;
        }
        return cols;
    }

    /**
     * Returns a subsequence of this attributed string based on column positions.
     *
     * <p>
     * This method returns a subsequence of this attributed string that spans from
     * the specified start column position (inclusive) to the specified stop column
     * position (exclusive). Column positions are determined by the display width of
     * characters, taking into account wide characters, zero-width characters, and
     * hidden characters.
     * </p>
     *
     * <p>
     * This method is useful for extracting portions of text based on their visual
     * position in a terminal, rather than their character indices.
     * </p>
     *
     * @param start the starting column position (inclusive)
     * @param stop the ending column position (exclusive)
     * @return the subsequence spanning the specified column range
     */
    public AttributedString columnSubSequence(int start, int stop) {
        return columnSubSequence(start, stop, null);
    }

    /**
     * Returns a subsequence of this attributed string based on column positions.
     *
     * @param start    the starting column position (inclusive)
     * @param stop     the ending column position (exclusive)
     * @param terminal the terminal to query for grapheme cluster mode, or {@code null}
     * @return the subsequence spanning the specified column range
     */
    public AttributedString columnSubSequence(int start, int stop, Terminal terminal) {
        int begin = 0;
        int col = 0;
        while (begin < this.length()) {
            int charCount = WCWidth.charCountForDisplay(this, begin, terminal);
            int w = isHidden(begin) ? 0 : WCWidth.wcwidthForDisplay(this, begin, terminal, charCount);
            if (col + w > start) {
                break;
            }
            begin += charCount;
            col += w;
        }
        int end = begin;
        while (end < this.length()) {
            int cp = codePointAt(end);
            if (cp == '\n') break;
            int charCount = WCWidth.charCountForDisplay(this, end, terminal);
            int w = isHidden(end) ? 0 : WCWidth.wcwidthForDisplay(this, end, terminal, charCount);
            if (col + w > stop) {
                break;
            }
            end += charCount;
            col += w;
        }
        return subSequence(begin, end);
    }

    /**
     * Splits this attributed string into multiple lines based on column width.
     *
     * <p>
     * This method splits this attributed string into multiple lines, each with a
     * maximum width of the specified number of columns. The splitting is done based
     * on the display width of characters, taking into account wide characters,
     * zero-width characters, and hidden characters.
     * </p>
     *
     * <p>
     * This is equivalent to calling {@link #columnSplitLength(int, boolean, boolean)}
     * with {@code includeNewlines=false} and {@code delayLineWrap=true}.
     * </p>
     *
     * @param columns the maximum width of each line in columns
     * @return a list of attributed strings, each representing a line
     */
    public List<AttributedString> columnSplitLength(int columns) {
        return columnSplitLength(columns, false, true);
    }

    /**
     * Splits this attributed string into multiple lines based on column width,
     * with options for handling newlines and line wrapping.
     *
     * <p>
     * This method splits this attributed string into multiple lines, each with a
     * maximum width of the specified number of columns. The splitting is done based
     * on the display width of characters, taking into account wide characters,
     * zero-width characters, and hidden characters.
     * </p>
     *
     * @param columns the maximum width of each line in columns
     * @param includeNewlines whether to include newline characters in the resulting lines
     * @param delayLineWrap whether to delay line wrapping until the last possible moment
     * @return a list of attributed strings, each representing a line
     */
    public List<AttributedString> columnSplitLength(int columns, boolean includeNewlines, boolean delayLineWrap) {
        return columnSplitLength(columns, includeNewlines, delayLineWrap, (Terminal) null);
    }

    /**
     * Splits this attributed string into multiple lines based on column width.
     *
     * @param columns         the maximum width of each line in columns
     * @param includeNewlines whether to include newline characters in the resulting lines
     * @param delayLineWrap   whether to delay line wrapping until the last possible moment
     * @param terminal        the terminal to query for grapheme cluster mode, or {@code null}
     * @return a list of attributed strings, each representing a line
     */
    public List<AttributedString> columnSplitLength(
            int columns, boolean includeNewlines, boolean delayLineWrap, Terminal terminal) {
        List<AttributedString> strings = new ArrayList<>();
        int cur = 0;
        int beg = cur;
        int col = 0;
        while (cur < length()) {
            int cp = codePointAt(cur);
            int charCount = WCWidth.charCountForDisplay(this, cur, terminal);
            int w = isHidden(cur) ? 0 : WCWidth.wcwidthForDisplay(this, cur, terminal, charCount);
            if (cp == '\n') {
                strings.add(subSequence(beg, includeNewlines ? cur + 1 : cur));
                beg = cur + 1;
                col = 0;
            } else if ((col += w) > columns) {
                strings.add(subSequence(beg, cur));
                beg = cur;
                col = w;
            }
            cur += charCount;
        }
        strings.add(subSequence(beg, cur));
        return strings;
    }

    @Override
    public String toString() {
        return new String(buffer(), offset(), length());
    }

    /**
     * Converts this attributed character sequence to an AttributedString.
     *
     * <p>
     * This method creates a new AttributedString that contains all the characters
     * and styles from this attributed character sequence. This is useful for
     * converting an AttributedCharSequence to an immutable AttributedString.
     * </p>
     *
     * @return a new AttributedString containing all characters and styles from this sequence
     */
    public AttributedString toAttributedString() {
        return substring(0, length());
    }
}

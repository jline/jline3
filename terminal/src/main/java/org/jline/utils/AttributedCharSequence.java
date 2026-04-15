/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.utils;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
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
     * Controls how indexed (palette) colors are rendered in ANSI output.
     *
     * <p>These modes only affect colors that have been resolved to a palette index.
     * Direct RGB colors (set via {@link AttributedStyle#foreground(int, int, int)})
     * are always emitted as {@code 38;2;r;g;b} when the terminal supports
     * {@link #HIGH_COLORS}, regardless of this setting.</p>
     */
    public enum ForceMode {
        /**
         * No forcing; indexed colors are rendered using the best encoding for
         * the terminal's reported color count (basic SGR 30-37/90-97,
         * 256-color {@code 38;5;n}, or true-color {@code 38;2;r;g;b}).
         */
        None,

        /**
         * Force indexed colors to use 256-color encoding ({@code 38;5;n})
         * even when a basic SGR code would suffice.
         */
        Force256Colors,

        /**
         * Force indexed colors to be expanded to true-color RGB
         * ({@code 38;2;r;g;b}) via the palette, but only when the terminal
         * supports {@link #HIGH_COLORS}. This does not override the color
         * count check for direct RGB values.
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
     * Render this attributed string as an ANSI escape sequence string using the provided
     * color capabilities and alternate character set sequences.
     *
     * @param colors the number of colors to use (commonly 8, 256, or 16777216 for true color)
     * @param force the force mode controlling whether 256-color or true-color forms are preferred
     * @param palette the color palette used to map colors, or {@code null} to use the default palette
     * @param altIn the sequence to enable the alternate character set for box-drawing, or {@code null} to disable
     * @param altOut the sequence to disable the alternate character set, or {@code null} to disable
     * @return the ANSI-encoded representation of this attributed string
     */
    public String toAnsi(int colors, ForceMode force, ColorPalette palette, String altIn, String altOut) {
        ByteArrayBuilder buf = new ByteArrayBuilder();
        long[] state = {0, 0};
        toAnsiBytes(buf, 0, length(), colors, force, palette, altIn, altOut, state);
        if (state[1] != 0) {
            buf.appendAscii(altOut);
        }
        if (state[0] != 0) {
            buf.csi().appendAscii("0m");
        }
        return buf.toStringUtf8();
    }

    /**
     * Write ANSI-encoded UTF-8 bytes for a range, carrying style state across calls.
     *
     * <p>This method does not reset style state on entry and does not emit a final
     * {@code \e[0m} reset on exit. The caller manages the terminal style state via
     * the {@code state} array:</p>
     * <ul>
     *   <li>{@code state[0]} — current style code (long)</li>
     *   <li>{@code state[1]} — alt charset active (0 or 1)</li>
     * </ul>
     *
     * <p>For self-contained rendering, initialize {@code state} to {@code {0, 0}}
     * and emit a final reset after the call if {@code state[0] != 0}.</p>
     *
     * @param buf        the byte buffer to write UTF-8 bytes and ANSI control sequences to
     * @param rangeStart start index in this sequence (inclusive)
     * @param rangeEnd   end index in this sequence (exclusive)
     * @param colors     the number of displayable colors to target
     * @param force      the force mode for color rendering
     * @param palette    the color palette, or null to use the default
     * @param altIn      the sequence to enter alternate character set, or null
     * @param altOut     the sequence to exit alternate character set, or null
     * @param state      a reusable two-element array tracking style [0] and alt charset [1] state
     */
    @SuppressWarnings("java:S107") // parameter count justified: avoids allocation of a parameter object in hot path
    void toAnsiBytes(
            ByteArrayBuilder buf,
            int rangeStart,
            int rangeEnd,
            int colors,
            ForceMode force,
            ColorPalette palette,
            String altIn,
            String altOut,
            long[] state) {
        long style = state[0];
        boolean alt = state[1] != 0;
        if (palette == null) {
            palette = ColorPalette.DEFAULT;
        }
        int i = rangeStart;
        while (i < rangeEnd) {
            char c = substituteChar(charAt(i), altIn, altOut);
            alt = emitAltCharset(buf, charAt(i), alt, altIn, altOut);
            long s = styleCodeAt(i) & ~F_HIDDEN;
            if (style != s) {
                emitStyleChange(buf, style, s, colors, force, palette);
                style = s;
            }
            i += emitUtf8Char(buf, c, i, rangeEnd);
        }
        state[0] = style;
        state[1] = alt ? 1 : 0;
    }

    /**
     * Toggle and emit the terminal alternate character set sequence when encountering
     * box-drawing characters.
     *
     * @param buf          buffer to which enter/exit alternate-char sequences are appended
     * @param originalChar the character being examined for box-drawing status
     * @param alt          whether the alternate character set is currently active
     * @param altIn        sequence to enable alternate character set (may be null)
     * @param altOut       sequence to disable alternate character set (may be null)
     * @return              `true` if the alternate character set should be active after processing `originalChar`, `false` otherwise
     */
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

    /**
     * Appends the UTF-8 encoding of the character (or surrogate pair) at position `i` to the buffer.
     *
     * If `c` is a UTF-16 high-surrogate and the following code unit (within `len`) is a low-surrogate,
     * the combined code point is appended.
     *
     * @param buf the byte buffer to append UTF-8 bytes into
     * @param c the character at index `i`
     * @param i the index within the sequence corresponding to `c`
     * @param len the sequence length (used to ensure the low-surrogate is within bounds)
     * @return 2 if a surrogate pair was consumed and appended, otherwise 1
     */
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

    private static final long[] DECORATION_FLAGS = {F_ITALIC, F_UNDERLINE, F_BLINK, F_INVERSE, F_CONCEAL, F_CROSSED_OUT
    };
    private static final String[] DECORATION_ON = {"3", "4", "5", "7", "8", "9"};
    private static final String[] DECORATION_OFF = {"23", "24", "25", "27", "28", "29"};

    private static final long COLOR_BITS = F_FOREGROUND | F_BACKGROUND | FG_COLOR | BG_COLOR;
    private static final long BOLD_FAINT_BITS = F_BOLD | F_FAINT;
    private static final long DECORATION_BITS = Arrays.stream(DECORATION_FLAGS).reduce(0L, (a, b) -> a | b);

    /**
     * Emit a single CSI SGR sequence that transitions terminal attributes from prevStyle to newStyle.
     * <p>
     * Writes ANSI SGR parameters into the provided ByteArrayBuilder and updates colorState to reflect the
     * currently applied foreground (index 0) and background (index 1) encodings. If newStyle is zero, a
     * reset sequence is emitted and colorState entries are cleared.
     *
     * @param buf        the byte-oriented builder to which CSI parameters and the final 'm' are appended
     * @param prevStyle  previously applied style code
     * @param newStyle   target style code to apply
     * @param colors     terminal maximum color capability (affects truecolor/256-color selection)
     * @param force      force mode controlling preference for 256/true color output
     * @param palette    color palette used to round/lookup colors when not emitting direct RGB
     */
    private static void emitStyleChange(
            ByteArrayBuilder buf, long prevStyle, long newStyle, int colors, ForceMode force, ColorPalette palette) {
        buf.csi();
        if (newStyle == 0) {
            buf.appendAscii("0m");
            return;
        }
        boolean first = true;
        long diff = prevStyle ^ newStyle;
        if ((diff & DECORATION_BITS) != 0) {
            first = appendDecorationAttrsB(buf, diff, newStyle, first);
        }
        if ((diff & COLOR_BITS) != 0) {
            long fg = (newStyle & F_FOREGROUND) != 0 ? newStyle & (FG_COLOR | F_FOREGROUND) : 0;
            long bg = (newStyle & F_BACKGROUND) != 0 ? newStyle & (BG_COLOR | F_BACKGROUND) : 0;
            long prevFg = (prevStyle & F_FOREGROUND) != 0 ? prevStyle & (FG_COLOR | F_FOREGROUND) : 0;
            long prevBg = (prevStyle & F_BACKGROUND) != 0 ? prevStyle & (BG_COLOR | F_BACKGROUND) : 0;

            first = appendDecorationAttrsB(buf, diff, newStyle, first);
            if (prevFg != fg) {
                first = appendColorB(buf, fg, true, colors, force, palette, first);
                if (fg > 0 && usedBasicFgColor(fg, colors, force, palette)) {
                    diff |= (newStyle & F_BOLD);
                }
            }
            if (prevBg != bg) {
                first = appendColorB(buf, bg, false, colors, force, palette, first);
            }
        }
        if ((diff & BOLD_FAINT_BITS) != 0) {
            appendBoldFaintB(buf, diff, newStyle, first);
        }
        buf.appendAscii('m');
    }

    /**
     * Appends CSI decoration parameters for each decoration flag present in `d` to `buf`, using the
     * enabled/disabled codes from `s`.
     *
     * @param buf the byte buffer receiving CSI parameters
     * @param d bitmask of decoration flags that have changed and should be emitted
     * @param s current style bitmask used to choose the ON or OFF code for each flag
     * @param first true if no CSI parameters have yet been written (affects separator emission)
     * @return `true` if no parameters were appended (so subsequent parameter should not be prefixed),
     *         `false` if at least one parameter was appended (so subsequent parameter should be prefixed)
     */
    private static boolean appendDecorationAttrsB(ByteArrayBuilder buf, long d, long s, boolean first) {
        for (int i = 0; i < DECORATION_FLAGS.length; i++) {
            long flag = DECORATION_FLAGS[i];
            if ((d & flag) != 0) {
                first = attrB(buf, (s & flag) != 0 ? DECORATION_ON[i] : DECORATION_OFF[i], first);
            }
        }
        return first;
    }

    /**
     * Appends CSI color parameters for a foreground or background color value to the byte buffer.
     *
     * <p>Handles three color encodings encoded in {@code colorValue}:
     * - default (<= 0) emits the reset parameter (39 for foreground, 49 for background),
     * - RGB (flag present) emits a truecolor parameter when supported or delegates to palette rounding,
     * - indexed (flag present) rounds the index via {@code palette} and emits an appropriate form.
     *
     * @param buf the byte-oriented builder to receive CSI parameters
     * @param colorValue encoded color value containing flags and components (RGB or indexed)
     * @param isForeground true when emitting a foreground color, false for background
     * @param colors current terminal color capability (used to choose truecolor vs palette/indexed forms)
     * @param force color forcing mode that may override form selection
     * @param palette palette used to round RGB or indexed values into a terminal index
     * @param first true if this is the first CSI parameter (no leading separator); updated based on what is emitted
     * @return true if no parameter was appended (the "first" state remains), false if a parameter was appended */
    private static boolean appendColorB(
            ByteArrayBuilder buf,
            long colorValue,
            boolean isForeground,
            int colors,
            ForceMode force,
            ColorPalette palette,
            boolean first) {
        long rgbFlag = isForeground ? F_FOREGROUND_RGB : F_BACKGROUND_RGB;
        long indFlag = isForeground ? F_FOREGROUND_IND : F_BACKGROUND_IND;
        int colorExp = isForeground ? FG_COLOR_EXP : BG_COLOR_EXP;
        int csiPrefix = isForeground ? 38 : 48;
        if (colorValue <= 0) {
            return attrB(buf, isForeground ? "39" : "49", first);
        }
        if ((colorValue & rgbFlag) != 0) {
            int r = (int) (colorValue >> (colorExp + 16)) & 0xFF;
            int g = (int) (colorValue >> (colorExp + 8)) & 0xFF;
            int b = (int) (colorValue >> colorExp) & 0xFF;
            // Direct RGB: emit 38;2/48;2 only if terminal supports high colors;
            // ForceMode does not override this — it only affects indexed colors
            if (colors >= HIGH_COLORS) {
                return attrRgbB(buf, csiPrefix, r, g, b, first);
            }
            return appendRoundedColorB(buf, palette.round(r, g, b), isForeground, colors, force, palette, first);
        }
        if ((colorValue & indFlag) != 0) {
            int rounded = palette.round((int) (colorValue >> colorExp) & 0xFF);
            return appendRoundedColorB(buf, rounded, isForeground, colors, force, palette, first);
        }
        return first;
    }

    /**
     * Append the appropriate CSI color parameter (truecolor RGB, 256-color index, or basic ANSI color)
     * for a rounded palette entry to the given buffer.
     *
     * @param buf         the byte buffer receiving CSI parameters
     * @param rounded     the palette index for the desired color, or a negative value to indicate no color
     * @param isForeground true to emit a foreground color parameter, false for background
     * @param colors      the terminal's reported color capacity (numeric)
     * @param force       a ForceMode hint that can force 256- or true-color emission
     * @param palette     the ColorPalette used to resolve truecolor values when required
     * @param first       whether this is the first CSI parameter (affects whether a leading separator is emitted)
     * @return `true` if no separator was emitted (i.e., still first), `false` otherwise.
     */
    private static boolean appendRoundedColorB(
            ByteArrayBuilder buf,
            int rounded,
            boolean isForeground,
            int colors,
            ForceMode force,
            ColorPalette palette,
            boolean first) {
        int csiPrefix = isForeground ? 38 : 48;
        if (rounded < 0) {
            return first;
        }
        // ForceTrueColors: expand indexed color to RGB via palette (requires high-color terminal)
        if (colors >= HIGH_COLORS && force == ForceMode.ForceTrueColors) {
            int col = palette.getColor(rounded);
            return attrRgbB(buf, csiPrefix, (col >> 16) & 0xFF, (col >> 8) & 0xFF, col & 0xFF, first);
        }
        // Force256Colors: always use 38;5;n encoding; also used for extended palette (index >= 16)
        if (force == ForceMode.Force256Colors || rounded >= 16) {
            return attrIdxB(buf, csiPrefix, rounded, first);
        }
        int lowBase = isForeground ? 30 : 40;
        if (rounded >= 8) {
            return attrIntB(buf, (isForeground ? 90 : 100) + rounded - 8, first);
        }
        return attrIntB(buf, lowBase + rounded, first);
    }

    /**
     * Determines whether the encoded foreground color should be rendered using a basic (0–15) terminal color.
     *
     * <p>For an RGB-encoded or indexed foreground value in `fg`, returns `true` when the palette rounds it to an
     * index in the 0–15 range and the current `colors`/`force` configuration does not require forcing 256-color
     * or truecolor output; otherwise returns `false`.</p>
     *
     * @param fg      encoded foreground style bits (may contain RGB or indexed color encoding)
     * @param colors  terminal reported color capacity (used to decide truecolor behavior)
     * @param force   color forcing mode that may require 256-color or truecolor output
     * @param palette palette used to round RGB or indexed values to a terminal color index
     * @return `true` if the foreground maps to a basic terminal color (0–15) and basic-color emission is allowed;
     *         `false` otherwise.
     */
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

    /**
     * Appends appropriate SGR parameters for bold and faint transitions to the buffer when those
     * attributes changed, and updates the CSI parameter separation state.
     *
     * @param buf   the byte buffer used to build the CSI sequence
     * @param d     bitmask of attributes that changed (diff between previous and new style)
     * @param s     the current style bitmask (after change)
     * @param first true if no CSI parameter has yet been emitted for this sequence; used to decide
     *              whether to prepend a separator
     * @return      the updated `first` flag indicating whether subsequent parameters need a separator
     *              (`true` if still first, `false` if a parameter was emitted)
     */
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
     * Map box-drawing characters to alternate-charset codes when alternate sequences are available, otherwise to simple ASCII equivalents.
     *
     * @param c the input character to substitute
     * @param altIn the terminal's enter-alternate-charset sequence, or null if not available
     * @param altOut the terminal's exit-alternate-charset sequence, or null if not available
     * @return the substituted character (an alternate-charset code or an ASCII fallback), or the original character if no substitution applies
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

    /**
     * Determines whether the given character is one of the supported Unicode box-drawing characters.
     *
     * @param c the character to test
     * @return true if the character is a box-drawing glyph handled by this class, false otherwise
     */
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

    /**
     * Append a CSI parameter string to the byte buffer, prefixing with ';' when not the first parameter.
     *
     * @param buf   destination ByteArrayBuilder to append ASCII bytes to
     * @param s     parameter string to append (ASCII)
     * @param first whether this is the first parameter in the CSI sequence
     * @return      `false` to indicate subsequent parameters must be separated by ';'
     */
    private static boolean attrB(ByteArrayBuilder buf, String s, boolean first) {
        if (!first) buf.appendAscii(';');
        buf.appendAscii(s);
        return false;
    }

    /**
     * Append an integer parameter to the byte buffer, prefixing it with ';' when it is not the first parameter.
     *
     * @param buf   the byte buffer to append into
     * @param value the integer value to append as a parameter
     * @param first true if this is the first CSI parameter (no leading ';'), false otherwise
     * @return      `false` to indicate subsequent parameters are not the first
     */
    private static boolean attrIntB(ByteArrayBuilder buf, int value, boolean first) {
        if (!first) buf.appendAscii(';');
        buf.appendInt(value);
        return false;
    }

    /**
     * Append an RGB color parameter sequence to the provided ByteArrayBuilder for a CSI sequence.
     *
     * Appends the form "{prefix};2;{r};{g};{b}" and writes a leading ';' if `first` is false.
     *
     * @param buf    the byte-oriented builder to append CSI parameters to
     * @param prefix CSI color prefix (typically 38 for foreground or 48 for background)
     * @param r      red component (0–255)
     * @param g      green component (0–255)
     * @param b      blue component (0–255)
     * @param first  true when this is the first CSI parameter (omit leading ';'); false otherwise
     * @return       `false` indicating subsequent parameters are not the first
     */
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

    /**
     * Appends a CSI indexed-color parameter of the form `prefix;5;idx` to the byte buffer,
     * inserting a leading `;` only if this is not the first parameter.
     *
     * @param buf the byte buffer to append into
     * @param prefix the CSI color prefix (commonly `38` for foreground or `48` for background)
     * @param idx the palette index to emit
     * @param first true if this is the first CSI parameter (omits a leading `;`), false otherwise
     * @return false (marks that subsequent parameters are no longer the first)
     */
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

    abstract long[] styleBuffer();

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
        return columnLength(terminal, 0, length());
    }

    /**
     * Returns the display width in columns for a range of this attributed string.
     *
     * <p>This is an allocation-free alternative to creating a subSequence and calling
     * {@link #columnLength(Terminal)} on it.</p>
     *
     * @param terminal   the terminal to query for grapheme cluster mode, or {@code null}
     * @param rangeStart start index in this sequence (inclusive)
     * @param rangeEnd   end index in this sequence (exclusive)
     * @return the display width in columns for the specified range
     * @since 4.1.0
     */
    public int columnLength(Terminal terminal, int rangeStart, int rangeEnd) {
        BreakIterator bi = WCWidth.HAS_JDK_GRAPHEME_SUPPORT ? BreakIterator.getCharacterInstance() : null;
        return columnLength(terminal, bi, new WCWidth.CharSequenceCharacterIterator(), rangeStart, rangeEnd);
    }

    /**
     * Returns the display width in columns for a range of this attributed string,
     * using a pre-allocated {@link BreakIterator} and {@link WCWidth.CharSequenceCharacterIterator}
     * to avoid per-call allocation.
     *
     * @param terminal   the terminal to query for grapheme cluster mode, or {@code null}
     * @param rangeStart start index in this sequence (inclusive)
     * @param rangeEnd   end index in this sequence (exclusive)
     * @param bi         a pre-allocated BreakIterator, or {@code null}
     * @param iter       a reusable CharSequenceCharacterIterator
     * @return the display width in columns for the specified range
     */
    int columnLength(
            Terminal terminal,
            BreakIterator bi,
            WCWidth.CharSequenceCharacterIterator iter,
            int rangeStart,
            int rangeEnd) {
        int cols = 0;
        if (bi != null
                && ((terminal != null && terminal.getGraphemeClusterMode())
                        || (terminal == null && WCWidth.HAS_JDK_GRAPHEME_SUPPORT))) {
            WCWidth.resetGraphemeBreakIterator(bi, this, iter);
        } else {
            bi = null;
        }
        int cur = rangeStart;
        while (cur < rangeEnd) {
            int charCount = WCWidth.charCountForDisplay(this, cur, terminal, bi);
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
        BreakIterator bi = WCWidth.createGraphemeBreakIterator(this);
        int begin = 0;
        int col = 0;
        while (begin < this.length()) {
            int charCount = WCWidth.charCountForDisplay(this, begin, terminal, bi);
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
            int charCount = WCWidth.charCountForDisplay(this, end, terminal, bi);
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
        BreakIterator bi = WCWidth.createGraphemeBreakIterator(this);
        while (cur < length()) {
            int cp = codePointAt(cur);
            int charCount = WCWidth.charCountForDisplay(this, cur, terminal, bi);
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

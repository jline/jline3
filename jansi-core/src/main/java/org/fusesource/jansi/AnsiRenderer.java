/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.fusesource.jansi;

import java.io.IOException;
import java.util.Locale;

/**
 * Renders ANSI color escape-codes in strings by parsing out some special syntax to pick up the correct fluff to use.
 *
 * The syntax for embedded ANSI codes is:
 *
 * <pre>
 *   &#64;|<em>code</em>(,<em>code</em>)* <em>text</em>|&#64;
 * </pre>
 *
 * Examples:
 *
 * <pre>
 *   &#64;|bold Hello|&#64;
 * </pre>
 *
 * <pre>
 *   &#64;|bold,red Warning!|&#64;
 * </pre>
 *
 * @since 2.2
 * @deprecated Use {@link org.jline.jansi.AnsiRenderer} instead.
 */
@Deprecated
public class AnsiRenderer {

    public static final String BEGIN_TOKEN = "@|";

    public static final String END_TOKEN = "|@";

    public static final String CODE_TEXT_SEPARATOR = " ";

    public static final String CODE_LIST_SEPARATOR = ",";

    private static final int BEGIN_TOKEN_LEN = 2;

    private static final int END_TOKEN_LEN = 2;

    public static String render(final String input) throws IllegalArgumentException {
        try {
            return render(input, new StringBuilder()).toString();
        } catch (IOException e) {
            // Cannot happen because StringBuilder does not throw IOException
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Renders the given input to the target Appendable.
     *
     * @param input
     *            source to render
     * @param target
     *            render onto this target Appendable.
     * @return the given Appendable
     * @throws IOException
     *             If an I/O error occurs
     */
    public static Appendable render(final String input, Appendable target) throws IOException {

        int i = 0;
        int j, k;

        while (true) {
            j = input.indexOf(BEGIN_TOKEN, i);
            if (j == -1) {
                if (i == 0) {
                    target.append(input);
                    return target;
                }
                target.append(input.substring(i));
                return target;
            }
            target.append(input.substring(i, j));
            k = input.indexOf(END_TOKEN, j);

            if (k == -1) {
                target.append(input);
                return target;
            }
            j += BEGIN_TOKEN_LEN;

            // Check for invalid string with END_TOKEN before BEGIN_TOKEN
            if (k < j) {
                throw new IllegalArgumentException("Invalid input string found.");
            }
            String spec = input.substring(j, k);

            String[] items = spec.split(CODE_TEXT_SEPARATOR, 2);
            if (items.length == 1) {
                target.append(input);
                return target;
            }
            String replacement = render(items[1], items[0].split(CODE_LIST_SEPARATOR));

            target.append(replacement);

            i = k + END_TOKEN_LEN;
        }
    }

    public static String render(final String text, final String... codes) {
        return render(Ansi.ansi(), codes).a(text).reset().toString();
    }

    /**
     * Renders {@link Code} names as an ANSI escape string.
     * @param codes The code names to render
     * @return an ANSI escape string.
     */
    public static String renderCodes(final String... codes) {
        return render(Ansi.ansi(), codes).toString();
    }

    /**
     * Renders {@link Code} names as an ANSI escape string.
     * @param codes A space separated list of code names to render
     * @return an ANSI escape string.
     */
    public static String renderCodes(final String codes) {
        return renderCodes(codes.split("\\s"));
    }

    private static Ansi render(Ansi ansi, String... names) {
        for (String name : names) {
            Code code = Code.valueOf(name.toUpperCase(Locale.ENGLISH));
            if (code.isColor()) {
                if (code.isBackground()) {
                    ansi.bg(code.getColor());
                } else {
                    ansi.fg(code.getColor());
                }
            } else if (code.isAttribute()) {
                ansi.a(code.getAttribute());
            }
        }
        return ansi;
    }

    public static boolean test(final String text) {
        return text != null && text.contains(BEGIN_TOKEN);
    }

    @SuppressWarnings("unused")
    public enum Code {
        //
        // TODO: Find a better way to keep Code in sync with Color/Attribute/Erase
        //

        // Colors
        BLACK(Ansi.Color.BLACK),
        RED(Ansi.Color.RED),
        GREEN(Ansi.Color.GREEN),
        YELLOW(Ansi.Color.YELLOW),
        BLUE(Ansi.Color.BLUE),
        MAGENTA(Ansi.Color.MAGENTA),
        CYAN(Ansi.Color.CYAN),
        WHITE(Ansi.Color.WHITE),
        DEFAULT(Ansi.Color.DEFAULT),

        // Foreground Colors
        FG_BLACK(Ansi.Color.BLACK, false),
        FG_RED(Ansi.Color.RED, false),
        FG_GREEN(Ansi.Color.GREEN, false),
        FG_YELLOW(Ansi.Color.YELLOW, false),
        FG_BLUE(Ansi.Color.BLUE, false),
        FG_MAGENTA(Ansi.Color.MAGENTA, false),
        FG_CYAN(Ansi.Color.CYAN, false),
        FG_WHITE(Ansi.Color.WHITE, false),
        FG_DEFAULT(Ansi.Color.DEFAULT, false),

        // Background Colors
        BG_BLACK(Ansi.Color.BLACK, true),
        BG_RED(Ansi.Color.RED, true),
        BG_GREEN(Ansi.Color.GREEN, true),
        BG_YELLOW(Ansi.Color.YELLOW, true),
        BG_BLUE(Ansi.Color.BLUE, true),
        BG_MAGENTA(Ansi.Color.MAGENTA, true),
        BG_CYAN(Ansi.Color.CYAN, true),
        BG_WHITE(Ansi.Color.WHITE, true),
        BG_DEFAULT(Ansi.Color.DEFAULT, true),

        // Attributes
        RESET(Ansi.Attribute.RESET),
        INTENSITY_BOLD(Ansi.Attribute.INTENSITY_BOLD),
        INTENSITY_FAINT(Ansi.Attribute.INTENSITY_FAINT),
        ITALIC(Ansi.Attribute.ITALIC),
        UNDERLINE(Ansi.Attribute.UNDERLINE),
        BLINK_SLOW(Ansi.Attribute.BLINK_SLOW),
        BLINK_FAST(Ansi.Attribute.BLINK_FAST),
        BLINK_OFF(Ansi.Attribute.BLINK_OFF),
        NEGATIVE_ON(Ansi.Attribute.NEGATIVE_ON),
        NEGATIVE_OFF(Ansi.Attribute.NEGATIVE_OFF),
        CONCEAL_ON(Ansi.Attribute.CONCEAL_ON),
        CONCEAL_OFF(Ansi.Attribute.CONCEAL_OFF),
        UNDERLINE_DOUBLE(Ansi.Attribute.UNDERLINE_DOUBLE),
        UNDERLINE_OFF(Ansi.Attribute.UNDERLINE_OFF),

        // Aliases
        BOLD(Ansi.Attribute.INTENSITY_BOLD),
        FAINT(Ansi.Attribute.INTENSITY_FAINT);

        private final Enum<?> n;

        private final boolean background;

        Code(final Enum<?> n, boolean background) {
            this.n = n;
            this.background = background;
        }

        Code(final Enum<?> n) {
            this(n, false);
        }

        public boolean isColor() {
            return n instanceof Ansi.Color;
        }

        public Ansi.Color getColor() {
            return (Ansi.Color) n;
        }

        public boolean isAttribute() {
            return n instanceof Ansi.Attribute;
        }

        public Ansi.Attribute getAttribute() {
            return (Ansi.Attribute) n;
        }

        public boolean isBackground() {
            return background;
        }
    }

    private AnsiRenderer() {}
}

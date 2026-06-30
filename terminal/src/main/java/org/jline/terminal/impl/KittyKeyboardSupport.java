/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

/**
 * Utility class for Kitty Keyboard Protocol support in terminals.
 *
 * <p>
 * The Kitty Keyboard Protocol is a progressive enhancement to terminal keyboard
 * handling that replaces legacy, ambiguous escape code encodings with unambiguous
 * {@code CSI … u} sequences. It allows applications to reliably distinguish key
 * combinations that were previously indistinguishable, such as Shift+Enter versus
 * Enter, or Ctrl+I versus Tab.
 * </p>
 *
 * <p>
 * The protocol uses a flags-based push/pop stack model. Applications push a set
 * of enhancement flags when entering an interactive mode and pop them when leaving.
 * Main and alternate screens maintain independent stacks.
 * </p>
 *
 * <h2>Enhancement Flags</h2>
 * <table>
 *   <tr><th>Flag</th><th>Value</th><th>Effect</th></tr>
 *   <tr><td>Disambiguate</td><td>1</td><td>All keys get unambiguous CSI … u encoding</td></tr>
 *   <tr><td>Report event types</td><td>2</td><td>Adds press/repeat/release distinction</td></tr>
 *   <tr><td>Report alternate keys</td><td>4</td><td>Sends shifted + base-layout key codes</td></tr>
 *   <tr><td>Report all keys</td><td>8</td><td>Even plain letters become CSI sequences</td></tr>
 *   <tr><td>Report associated text</td><td>16</td><td>Adds generated text as codepoints</td></tr>
 * </table>
 *
 * @see <a href="https://sw.kovidgoyal.net/kitty/keyboard-protocol/">Kitty Keyboard Protocol</a>
 */
public final class KittyKeyboardSupport {

    private KittyKeyboardSupport() {}

    // ---- Enhancement Flags ----

    /** Disambiguate escape codes: all keys get unambiguous CSI encoding. */
    public static final int FLAG_DISAMBIGUATE = 1;
    /** Report event types: press, repeat, release. */
    public static final int FLAG_REPORT_EVENTS = 2;
    /** Report alternate keys: shifted and base-layout key codes. */
    public static final int FLAG_REPORT_ALTERNATES = 4;
    /** Report all keys as escape codes, including plain text keys. */
    public static final int FLAG_REPORT_ALL_KEYS = 8;
    /** Report associated text as Unicode codepoints. */
    public static final int FLAG_REPORT_TEXT = 16;

    // ---- Escape Sequences ----

    /** Query the current enhancement flags: {@code CSI ? u} */
    public static final String QUERY_FLAGS = "\033[?u";

    /** DA1 (Primary Device Attributes) query used as sentinel: {@code CSI c} */
    public static final String DA1_QUERY = "\033[c";

    // ---- Modifier Bits (in the protocol, transmitted value = 1 + bitmask) ----

    public static final int MOD_SHIFT = 1;
    public static final int MOD_ALT = 2;
    public static final int MOD_CTRL = 4;
    public static final int MOD_SUPER = 8;
    public static final int MOD_HYPER = 16;
    public static final int MOD_META = 32;
    public static final int MOD_CAPS_LOCK = 64;
    public static final int MOD_NUM_LOCK = 128;

    // ---- Event Types ----

    public static final int EVENT_PRESS = 1;
    public static final int EVENT_REPEAT = 2;
    public static final int EVENT_RELEASE = 3;

    // ---- Special Key Codes (Unicode Private Use Area) ----

    public static final int KEY_ESCAPE = 27;
    public static final int KEY_ENTER = 13;
    public static final int KEY_TAB = 9;
    public static final int KEY_BACKSPACE = 127;

    public static final int KEY_INSERT = 2;
    public static final int KEY_DELETE = 3;
    public static final int KEY_PAGE_UP = 5;
    public static final int KEY_PAGE_DOWN = 6;

    // Lock keys
    public static final int KEY_CAPS_LOCK = 57358;
    public static final int KEY_SCROLL_LOCK = 57359;
    public static final int KEY_NUM_LOCK = 57360;

    // System keys
    public static final int KEY_PRINT_SCREEN = 57361;
    public static final int KEY_PAUSE = 57362;
    public static final int KEY_MENU = 57363;

    // Extended function keys F13-F35
    public static final int KEY_F13 = 57376;

    // Keypad keys
    public static final int KEY_KP_0 = 57399;
    public static final int KEY_KP_1 = 57400;
    public static final int KEY_KP_2 = 57401;
    public static final int KEY_KP_3 = 57402;
    public static final int KEY_KP_4 = 57403;
    public static final int KEY_KP_5 = 57404;
    public static final int KEY_KP_6 = 57405;
    public static final int KEY_KP_7 = 57406;
    public static final int KEY_KP_8 = 57407;
    public static final int KEY_KP_9 = 57408;
    public static final int KEY_KP_DECIMAL = 57409;
    public static final int KEY_KP_DIVIDE = 57410;
    public static final int KEY_KP_MULTIPLY = 57411;
    public static final int KEY_KP_SUBTRACT = 57412;
    public static final int KEY_KP_ADD = 57413;
    public static final int KEY_KP_ENTER = 57414;
    public static final int KEY_KP_EQUAL = 57415;
    public static final int KEY_KP_SEPARATOR = 57416;
    public static final int KEY_KP_LEFT = 57417;
    public static final int KEY_KP_RIGHT = 57418;
    public static final int KEY_KP_UP = 57419;
    public static final int KEY_KP_DOWN = 57420;
    public static final int KEY_KP_PAGE_UP = 57421;
    public static final int KEY_KP_PAGE_DOWN = 57422;
    public static final int KEY_KP_HOME = 57423;
    public static final int KEY_KP_END = 57424;
    public static final int KEY_KP_INSERT = 57425;
    public static final int KEY_KP_DELETE = 57426;

    // Media keys
    public static final int KEY_MEDIA_PLAY = 57428;
    public static final int KEY_MEDIA_PAUSE = 57429;
    public static final int KEY_MEDIA_PLAY_PAUSE = 57430;
    public static final int KEY_MEDIA_STOP = 57432;

    // Modifier keys (as key events)
    public static final int KEY_LEFT_SHIFT = 57441;
    public static final int KEY_LEFT_CONTROL = 57442;
    public static final int KEY_LEFT_ALT = 57443;
    public static final int KEY_LEFT_SUPER = 57444;
    public static final int KEY_LEFT_HYPER = 57445;
    public static final int KEY_LEFT_META = 57446;
    public static final int KEY_RIGHT_SHIFT = 57447;
    public static final int KEY_RIGHT_CONTROL = 57448;
    public static final int KEY_RIGHT_ALT = 57449;
    public static final int KEY_RIGHT_SUPER = 57450;
    public static final int KEY_RIGHT_HYPER = 57451;
    public static final int KEY_RIGHT_META = 57452;

    // ---- Sequence Builders ----

    /**
     * Returns the escape sequence to push enhancement flags onto the stack.
     *
     * @param flags the enhancement flags (bitmask of FLAG_* constants)
     * @return the push escape sequence {@code CSI > flags u}
     */
    public static String pushFlags(int flags) {
        return "\033[>" + flags + "u";
    }

    /**
     * Returns the escape sequence to pop enhancement flags from the stack.
     *
     * @param count the number of entries to pop (default 1)
     * @return the pop escape sequence {@code CSI < count u}
     */
    public static String popFlags(int count) {
        return "\033[<" + count + "u";
    }

    /**
     * Returns the escape sequence to pop a single entry from the stack.
     *
     * @return the pop escape sequence {@code CSI < 1 u}
     */
    public static String popFlags() {
        return popFlags(1);
    }

    /**
     * Builds a kitty keyboard escape sequence string for a key with modifiers.
     * Used for registering key bindings in KeyMap.
     *
     * @param keyCode the Unicode key code
     * @param modifiers the modifier value (1 + bitmask), or 0 for no modifiers
     * @return the escape sequence string
     */
    public static String keySequence(int keyCode, int modifiers) {
        if (modifiers <= 1) {
            return "\033[" + keyCode + "u";
        }
        return "\033[" + keyCode + ";" + modifiers + "u";
    }

    /**
     * Builds a kitty keyboard escape sequence for Ctrl+letter.
     * With the kitty protocol, Ctrl+A sends {@code CSI 97;5u} instead of 0x01.
     *
     * @param letter the lowercase letter ('a' through 'z')
     * @return the kitty escape sequence for Ctrl+letter
     */
    public static String ctrlKey(char letter) {
        // modifiers value = 1 + ctrl(4) = 5
        return keySequence(letter, 1 + MOD_CTRL);
    }

    /**
     * Builds a kitty keyboard escape sequence for Alt+letter.
     * With the kitty protocol, Alt+A sends {@code CSI 97;3u} instead of ESC followed by 'a'.
     *
     * @param letter the lowercase letter ('a' through 'z')
     * @return the kitty escape sequence for Alt+letter
     */
    public static String altKey(char letter) {
        // modifiers value = 1 + alt(2) = 3
        return keySequence(letter, 1 + MOD_ALT);
    }

    /**
     * Builds a kitty keyboard escape sequence for Ctrl+Alt+letter.
     *
     * @param letter the lowercase letter ('a' through 'z')
     * @return the kitty escape sequence for Ctrl+Alt+letter
     */
    public static String ctrlAltKey(char letter) {
        // modifiers value = 1 + ctrl(4) + alt(2) = 7
        return keySequence(letter, 1 + MOD_CTRL + MOD_ALT);
    }

    /**
     * Builds a kitty keyboard escape sequence for a special key with modifiers.
     *
     * @param keyCode the special key code (e.g., KEY_ENTER, KEY_TAB)
     * @param shift   whether shift is held
     * @param alt     whether alt is held
     * @param ctrl    whether ctrl is held
     * @return the kitty escape sequence
     */
    public static String modifiedKey(int keyCode, boolean shift, boolean alt, boolean ctrl) {
        int mod = 1;
        if (shift) mod += MOD_SHIFT;
        if (alt) mod += MOD_ALT;
        if (ctrl) mod += MOD_CTRL;
        return keySequence(keyCode, mod);
    }

    /**
     * Checks whether a raw sequence looks like a kitty keyboard flags query response.
     * The response format is {@code CSI ? flags u}.
     *
     * @param sequence the sequence to check
     * @return true if it matches the response pattern
     */
    public static boolean isFlagsResponse(String sequence) {
        return sequence.startsWith("\033[?") && sequence.endsWith("u") && sequence.length() > 4;
    }

    /**
     * Parses the flags value from a kitty keyboard query response.
     *
     * @param sequence the response sequence {@code CSI ? flags u}
     * @return the flags value, or -1 if the sequence is not a valid response
     */
    public static int parseFlagsResponse(String sequence) {
        if (!isFlagsResponse(sequence)) {
            return -1;
        }
        try {
            return Integer.parseInt(sequence.substring(3, sequence.length() - 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}

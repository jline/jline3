/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

import java.util.EnumSet;

/**
 * Utility class for parsing raw terminal input sequences into KeyEvent objects.
 */
public class KeyParser {

    private KeyParser() {}

    /**
     * Parses a raw input sequence into a KeyEvent.
     *
     * @param rawSequence the raw input sequence from the terminal
     * @return a KeyEvent representing the parsed input
     */
    public static KeyEvent parse(String rawSequence) {
        if (rawSequence == null || rawSequence.isEmpty()) {
            return new KeyEvent(rawSequence);
        }

        // Handle escape sequences
        if (rawSequence.startsWith("\u001b")) {
            return parseEscapeSequence(rawSequence);
        }

        // Handle control characters
        if (rawSequence.length() == 1) {
            char ch = rawSequence.charAt(0);

            // Control characters (0x00-0x1F)
            if (ch >= 0 && ch <= 31) {
                return parseControlCharacter(ch, rawSequence);
            }

            // Regular printable character
            if (ch >= 32 && ch <= 126) {
                return new KeyEvent(ch, EnumSet.noneOf(KeyEvent.Modifier.class), rawSequence);
            }

            // Extended ASCII or Unicode
            if (ch > 126) {
                return new KeyEvent(ch, EnumSet.noneOf(KeyEvent.Modifier.class), rawSequence);
            }
        }

        // Multi-character sequence that's not an escape sequence
        return new KeyEvent(rawSequence);
    }

    private static KeyEvent parseEscapeSequence(String sequence) {
        if (sequence.length() < 2) {
            return new KeyEvent(KeyEvent.Special.Escape, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
        }

        // Alt+character sequences (ESC followed by a character)
        if (sequence.length() == 2) {
            char ch = sequence.charAt(1);
            EnumSet<KeyEvent.Modifier> modifiers = EnumSet.of(KeyEvent.Modifier.Alt);

            if (ch >= 32 && ch <= 126) {
                return new KeyEvent(ch, modifiers, sequence);
            }
        }

        // ANSI escape sequences
        if (sequence.startsWith("\u001b[")) {
            return parseAnsiSequence(sequence);
        }

        // SS3 escape sequences (ESC O)
        if (sequence.startsWith("\u001bO")) {
            return parseSS3Sequence(sequence);
        }

        // Other escape sequences
        return new KeyEvent(sequence);
    }

    private static KeyEvent parseAnsiSequence(String sequence) {
        // Kitty keyboard protocol: CSI … u sequences
        if (sequence.endsWith("u")) {
            return parseKittySequence(sequence);
        }

        // Common ANSI sequences
        switch (sequence) {
            // Arrow keys
            case "\u001b[A":
                return new KeyEvent(KeyEvent.Arrow.Up, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[B":
                return new KeyEvent(KeyEvent.Arrow.Down, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[C":
                return new KeyEvent(KeyEvent.Arrow.Right, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[D":
                return new KeyEvent(KeyEvent.Arrow.Left, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);

            // Function keys
            case "\u001b[11~":
            case "\u001bOP":
                return new KeyEvent(1, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[12~":
            case "\u001bOQ":
                return new KeyEvent(2, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[13~":
            case "\u001bOR":
                return new KeyEvent(3, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[14~":
            case "\u001bOS":
                return new KeyEvent(4, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[15~":
                return new KeyEvent(5, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[17~":
                return new KeyEvent(6, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[18~":
                return new KeyEvent(7, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[19~":
                return new KeyEvent(8, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[20~":
                return new KeyEvent(9, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[21~":
                return new KeyEvent(10, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[23~":
                return new KeyEvent(11, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[24~":
                return new KeyEvent(12, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);

            // Special keys
            case "\u001b[H":
                return new KeyEvent(KeyEvent.Special.Home, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[F":
                return new KeyEvent(KeyEvent.Special.End, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[2~":
                return new KeyEvent(KeyEvent.Special.Insert, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[3~":
                return new KeyEvent(KeyEvent.Special.Delete, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[5~":
                return new KeyEvent(KeyEvent.Special.PageUp, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001b[6~":
                return new KeyEvent(KeyEvent.Special.PageDown, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);

            // Backtab (Shift+Tab)
            case "\u001b[Z":
                return new KeyEvent(KeyEvent.Special.Tab, EnumSet.of(KeyEvent.Modifier.Shift), sequence);

            default:
                // Try to parse modified keys (with Shift, Alt, Ctrl)
                return parseModifiedAnsiSequence(sequence);
        }
    }

    private static KeyEvent parseSS3Sequence(String sequence) {
        // SS3 sequences (ESC O)
        switch (sequence) {
            // Function keys
            case "\u001bOP":
                return new KeyEvent(1, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001bOQ":
                return new KeyEvent(2, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001bOR":
                return new KeyEvent(3, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case "\u001bOS":
                return new KeyEvent(4, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            default:
                return new KeyEvent(sequence);
        }
    }

    private static KeyEvent parseModifiedAnsiSequence(String sequence) {
        // Pattern: \E[1;modifiers{A,B,C,D} for modified arrow keys
        // Pattern: \E[{number};modifiers~ for modified special keys

        // Modified arrow keys: \E[1;{mod}{A,B,C,D}
        if (sequence.matches("\\u001b\\[1;[2-8][ABCD]")) {
            int modCode = Character.getNumericValue(sequence.charAt(4));
            char arrowChar = sequence.charAt(5);

            EnumSet<KeyEvent.Modifier> modifiers = parseModifierCode(modCode);
            KeyEvent.Arrow arrow = parseArrowChar(arrowChar);

            if (arrow != null) {
                return new KeyEvent(arrow, modifiers, sequence);
            }
        }

        // Modified function keys: \E[{fn};{mod}~
        if (sequence.matches("\\u001b\\[[0-9]+;[2-8]~")) {
            String[] parts = sequence.substring(2, sequence.length() - 1).split(";");
            if (parts.length == 2) {
                try {
                    int fnNum = Integer.parseInt(parts[0]);
                    int modCode = Integer.parseInt(parts[1]);

                    EnumSet<KeyEvent.Modifier> modifiers = parseModifierCode(modCode);
                    int functionKey = mapFunctionKeyNumber(fnNum);

                    if (functionKey > 0) {
                        return new KeyEvent(functionKey, modifiers, sequence);
                    }
                } catch (NumberFormatException e) {
                    // Fall through to unknown
                }
            }
        }

        // Modified special keys: \E[{special};{mod}~
        if (sequence.matches("\\u001b\\[[2-6];[2-8]~")) {
            String[] parts = sequence.substring(2, sequence.length() - 1).split(";");
            if (parts.length == 2) {
                try {
                    int specialCode = Integer.parseInt(parts[0]);
                    int modCode = Integer.parseInt(parts[1]);

                    EnumSet<KeyEvent.Modifier> modifiers = parseModifierCode(modCode);
                    KeyEvent.Special special = mapSpecialKeyCode(specialCode);

                    if (special != null) {
                        return new KeyEvent(special, modifiers, sequence);
                    }
                } catch (NumberFormatException e) {
                    // Fall through to unknown
                }
            }
        }

        return new KeyEvent(sequence);
    }

    private static EnumSet<KeyEvent.Modifier> parseModifierCode(int modCode) {
        EnumSet<KeyEvent.Modifier> modifiers = EnumSet.noneOf(KeyEvent.Modifier.class);

        // Modifier codes: 2=Shift, 3=Alt, 4=Shift+Alt, 5=Ctrl, 6=Shift+Ctrl, 7=Alt+Ctrl, 8=Shift+Alt+Ctrl
        // The encoding is: 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0)
        int mod = modCode - 1; // Remove base offset

        if ((mod & 1) != 0) { // Shift bit
            modifiers.add(KeyEvent.Modifier.Shift);
        }
        if ((mod & 2) != 0) { // Alt bit
            modifiers.add(KeyEvent.Modifier.Alt);
        }
        if ((mod & 4) != 0) { // Ctrl bit
            modifiers.add(KeyEvent.Modifier.Control);
        }

        return modifiers;
    }

    private static KeyEvent.Arrow parseArrowChar(char arrowChar) {
        switch (arrowChar) {
            case 'A':
                return KeyEvent.Arrow.Up;
            case 'B':
                return KeyEvent.Arrow.Down;
            case 'C':
                return KeyEvent.Arrow.Right;
            case 'D':
                return KeyEvent.Arrow.Left;
            default:
                return null;
        }
    }

    private static int mapFunctionKeyNumber(int fnNum) {
        // Map ANSI function key numbers to F1-F12
        switch (fnNum) {
            case 11:
                return 1; // F1
            case 12:
                return 2; // F2
            case 13:
                return 3; // F3
            case 14:
                return 4; // F4
            case 15:
                return 5; // F5
            case 17:
                return 6; // F6
            case 18:
                return 7; // F7
            case 19:
                return 8; // F8
            case 20:
                return 9; // F9
            case 21:
                return 10; // F10
            case 23:
                return 11; // F11
            case 24:
                return 12; // F12
            default:
                return 0;
        }
    }

    private static KeyEvent.Special mapSpecialKeyCode(int specialCode) {
        switch (specialCode) {
            case 2:
                return KeyEvent.Special.Insert;
            case 3:
                return KeyEvent.Special.Delete;
            case 5:
                return KeyEvent.Special.PageUp;
            case 6:
                return KeyEvent.Special.PageDown;
            default:
                return null;
        }
    }

    private static KeyEvent parseControlCharacter(char ch, String sequence) {
        switch (ch) {
            case '\t':
                return new KeyEvent(KeyEvent.Special.Tab, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case '\r':
            case '\n':
                return new KeyEvent(KeyEvent.Special.Enter, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case '\u001b':
                return new KeyEvent(KeyEvent.Special.Escape, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            case '\b':
            case '\u007f':
                return new KeyEvent(KeyEvent.Special.Backspace, EnumSet.noneOf(KeyEvent.Modifier.class), sequence);
            default:
                // Other control characters - could be Ctrl+letter combinations
                if (ch >= 1 && ch <= 26) {
                    // Ctrl+A through Ctrl+Z
                    char letter = (char) ('a' + ch - 1);
                    return new KeyEvent(letter, EnumSet.of(KeyEvent.Modifier.Control), sequence);
                }
                return new KeyEvent(sequence);
        }
    }

    // ---- Kitty Keyboard Protocol parsing ----

    /**
     * Parses a Kitty Keyboard Protocol {@code CSI … u} sequence.
     *
     * <p>Format: {@code CSI keycode:shifted:base ; modifiers:eventtype ; text u}</p>
     *
     * <p>All fields except keycode are optional. Sub-fields use colon separators;
     * main fields use semicolon separators.</p>
     */
    private static KeyEvent parseKittySequence(String sequence) {
        // Strip CSI prefix and 'u' suffix
        String body = sequence.substring(2, sequence.length() - 1);

        // Split into main fields by semicolon
        String[] fields = body.split(";", -1);

        // Field 1: keycode[:shifted[:base]]
        int[] keyCodes = parseKittyKeyCodes(fields.length >= 1 ? fields[0] : "");

        // Field 2: modifiers[:eventtype]
        int[] modAndEvent = parseKittyModAndEvent(fields.length >= 2 ? fields[1] : "");

        // Field 3: text-as-codepoints (colon-separated)
        String associatedText = (fields.length >= 3 && !fields[2].isEmpty()) ? parseTextCodepoints(fields[2]) : null;

        EnumSet<KeyEvent.Modifier> modifiers = parseKittyModifiers(modAndEvent[0]);
        KeyEvent.EventType eventType = parseKittyEventType(modAndEvent[1]);

        return buildKittyKeyEvent(
                keyCodes[0], modifiers, eventType, keyCodes[1], keyCodes[2], associatedText, sequence);
    }

    /**
     * Parses the key code field {@code keycode[:shifted[:base]]} into an array
     * of three ints: [keyCode, shiftedKeyCode, baseLayoutKeyCode].
     */
    private static int[] parseKittyKeyCodes(String field) {
        int keyCode = 0;
        int shiftedKeyCode = 0;
        int baseLayoutKeyCode = 0;
        if (!field.isEmpty()) {
            String[] parts = field.split(":", -1);
            keyCode = parseIntSafe(parts[0]);
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                shiftedKeyCode = parseIntSafe(parts[1]);
            }
            if (parts.length >= 3 && !parts[2].isEmpty()) {
                baseLayoutKeyCode = parseIntSafe(parts[2]);
            }
        }
        return new int[] {keyCode, shiftedKeyCode, baseLayoutKeyCode};
    }

    /**
     * Parses the modifier field {@code modifiers[:eventtype]} into an array
     * of two ints: [modValue, eventTypeValue].
     */
    private static int[] parseKittyModAndEvent(String field) {
        int modValue = 1; // default: no modifiers
        int eventTypeValue = 1; // default: press
        if (!field.isEmpty()) {
            String[] parts = field.split(":", -1);
            modValue = parseIntSafe(parts[0]);
            if (modValue == 0) modValue = 1;
            if (parts.length >= 2 && !parts[1].isEmpty()) {
                eventTypeValue = parseIntSafe(parts[1]);
            }
        }
        return new int[] {modValue, eventTypeValue};
    }

    /**
     * Parses the kitty modifier value (1 + bitmask) into a set of modifiers.
     */
    static EnumSet<KeyEvent.Modifier> parseKittyModifiers(int modValue) {
        EnumSet<KeyEvent.Modifier> modifiers = EnumSet.noneOf(KeyEvent.Modifier.class);
        int bits = modValue - 1;
        if ((bits & 1) != 0) modifiers.add(KeyEvent.Modifier.Shift);
        if ((bits & 2) != 0) modifiers.add(KeyEvent.Modifier.Alt);
        if ((bits & 4) != 0) modifiers.add(KeyEvent.Modifier.Control);
        if ((bits & 8) != 0) modifiers.add(KeyEvent.Modifier.Super);
        if ((bits & 16) != 0) modifiers.add(KeyEvent.Modifier.Hyper);
        if ((bits & 32) != 0) modifiers.add(KeyEvent.Modifier.Meta);
        if ((bits & 64) != 0) modifiers.add(KeyEvent.Modifier.CapsLock);
        if ((bits & 128) != 0) modifiers.add(KeyEvent.Modifier.NumLock);
        return modifiers;
    }

    private static KeyEvent.EventType parseKittyEventType(int value) {
        switch (value) {
            case 2:
                return KeyEvent.EventType.Repeat;
            case 3:
                return KeyEvent.EventType.Release;
            default:
                return KeyEvent.EventType.Press;
        }
    }

    private static String parseTextCodepoints(String field) {
        String[] parts = field.split(":");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            int cp = parseIntSafe(part);
            if (cp > 0) {
                sb.appendCodePoint(cp);
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Builds a KeyEvent from a parsed kitty key code, mapping it to the
     * appropriate KeyEvent type (Character, Arrow, Special, Function, or Unknown).
     */
    private static KeyEvent buildKittyKeyEvent(
            int keyCode,
            EnumSet<KeyEvent.Modifier> modifiers,
            KeyEvent.EventType eventType,
            int shiftedKeyCode,
            int baseLayoutKeyCode,
            String associatedText,
            String rawSequence) {

        // Map special key codes to KeyEvent.Special
        KeyEvent.Special special = mapKittySpecialKey(keyCode);
        if (special != null) {
            return new KeyEvent(
                    KeyEvent.Type.Special,
                    '\0',
                    null,
                    special,
                    0,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Map function keys F13-F35 (F1-F12 use legacy CSI ~ format)
        int fKey = mapKittyFunctionKey(keyCode);
        if (fKey > 0) {
            return new KeyEvent(
                    KeyEvent.Type.Function,
                    '\0',
                    null,
                    null,
                    fKey,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Printable characters (Unicode codepoints below Private Use Area)
        if (keyCode >= 32 && keyCode < 57344) {
            char ch = (keyCode <= Character.MAX_VALUE) ? (char) keyCode : '\0';
            return new KeyEvent(
                    KeyEvent.Type.Character,
                    ch,
                    null,
                    null,
                    0,
                    modifiers,
                    rawSequence,
                    eventType,
                    keyCode,
                    shiftedKeyCode,
                    baseLayoutKeyCode,
                    associatedText);
        }

        // Unknown or unhandled private use area key codes (keypad, media, modifier keys)
        return new KeyEvent(
                KeyEvent.Type.Unknown,
                '\0',
                null,
                null,
                0,
                modifiers,
                rawSequence,
                eventType,
                keyCode,
                shiftedKeyCode,
                baseLayoutKeyCode,
                associatedText);
    }

    /**
     * Maps a kitty protocol key code to a KeyEvent.Special value.
     */
    private static KeyEvent.Special mapKittySpecialKey(int keyCode) {
        switch (keyCode) {
            case 13:
                return KeyEvent.Special.Enter;
            case 9:
                return KeyEvent.Special.Tab;
            case 27:
                return KeyEvent.Special.Escape;
            case 127:
                return KeyEvent.Special.Backspace;
            case 2:
                return KeyEvent.Special.Insert;
            case 3:
                return KeyEvent.Special.Delete;
            case 5:
                return KeyEvent.Special.PageUp;
            case 6:
                return KeyEvent.Special.PageDown;
            default:
                return null;
        }
    }

    /**
     * Maps a kitty protocol key code to a function key number (13-35).
     * F13-F35 use Unicode Private Use Area codes 57376-57398.
     */
    private static int mapKittyFunctionKey(int keyCode) {
        if (keyCode >= 57376 && keyCode <= 57398) {
            return keyCode - 57376 + 13;
        }
        return 0;
    }
}

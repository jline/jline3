/*
 * Copyright (c) 2002-2018, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import java.util.EnumSet;

import org.jline.keymap.KeyMap;
import org.jline.terminal.KeyEvent;
import org.jline.terminal.KeyParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.MouseSupport;
import org.jline.utils.Curses;
import org.jline.utils.InfoCmp;

/**
 * Utility class for building KeyMaps with pre-parsed KeyEvents.
 */
public class KeyMapBuilder {

    /**
     * Creates a KeyMap populated with common key bindings as KeyEvents.
     *
     * @param terminal the terminal to get key capabilities from
     * @return a KeyMap<InputEvent> with pre-parsed KeyEvents
     */
    public static KeyMap<InputEvent> createInputEventKeyMap(Terminal terminal) {
        KeyMap<InputEvent> map = new KeyMap<>();

        // Set handlers for unmatched keys
        // Unicode characters (regular printable chars) will be handled by this
        map.setUnicode(UNICODE_HANDLER);
        map.setNomatch(NOMATCH_HANDLER);

        // Bind mouse events
        map.bind(InputEvent.MOUSE, MouseSupport.keys(terminal));

        // Bind common key sequences to pre-parsed KeyEvents
        bindCommonKeys(map, terminal);

        return map;
    }

    // Special handlers for unmatched input
    public static final InputEvent UNICODE_HANDLER =
            new InputEvent(new KeyEvent('\0', EnumSet.noneOf(KeyEvent.Modifier.class), ""));
    public static final InputEvent NOMATCH_HANDLER = new InputEvent(new KeyEvent(""));

    private static void bindCommonKeys(KeyMap<InputEvent> map, Terminal terminal) {
        // Arrow keys
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_up,
                new KeyEvent(
                        KeyEvent.Arrow.Up,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_up)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_down,
                new KeyEvent(
                        KeyEvent.Arrow.Down,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_down)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_left,
                new KeyEvent(
                        KeyEvent.Arrow.Left,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_left)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_right,
                new KeyEvent(
                        KeyEvent.Arrow.Right,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_right)));

        // Function keys
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f1,
                new KeyEvent(
                        1,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f1)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f2,
                new KeyEvent(
                        2,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f2)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f3,
                new KeyEvent(
                        3,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f3)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f4,
                new KeyEvent(
                        4,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f4)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f5,
                new KeyEvent(
                        5,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f5)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f6,
                new KeyEvent(
                        6,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f6)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f7,
                new KeyEvent(
                        7,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f7)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f8,
                new KeyEvent(
                        8,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f8)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f9,
                new KeyEvent(
                        9,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f9)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f10,
                new KeyEvent(
                        10,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f10)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f11,
                new KeyEvent(
                        11,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f11)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_f12,
                new KeyEvent(
                        12,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_f12)));

        // Special keys
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_home,
                new KeyEvent(
                        KeyEvent.Special.Home,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_home)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_end,
                new KeyEvent(
                        KeyEvent.Special.End,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_end)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_ic,
                new KeyEvent(
                        KeyEvent.Special.Insert,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_ic)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_dc,
                new KeyEvent(
                        KeyEvent.Special.Delete,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_dc)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_ppage,
                new KeyEvent(
                        KeyEvent.Special.PageUp,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_ppage)));
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_npage,
                new KeyEvent(
                        KeyEvent.Special.PageDown,
                        EnumSet.noneOf(KeyEvent.Modifier.class),
                        getKeySequence(terminal, InfoCmp.Capability.key_npage)));

        // Common character sequences
        map.bind(
                new InputEvent(new KeyEvent(KeyEvent.Special.Enter, EnumSet.noneOf(KeyEvent.Modifier.class), "\r")),
                "\r");
        map.bind(
                new InputEvent(new KeyEvent(KeyEvent.Special.Enter, EnumSet.noneOf(KeyEvent.Modifier.class), "\n")),
                "\n");
        map.bind(
                new InputEvent(new KeyEvent(KeyEvent.Special.Tab, EnumSet.noneOf(KeyEvent.Modifier.class), "\t")),
                "\t");
        // Backtab (Shift+Tab)
        bindKey(
                map,
                terminal,
                InfoCmp.Capability.key_btab,
                new KeyEvent(KeyEvent.Special.Tab, EnumSet.of(KeyEvent.Modifier.Shift), "\u001b[Z"));
        map.bind(
                new InputEvent(
                        new KeyEvent(KeyEvent.Special.Escape, EnumSet.noneOf(KeyEvent.Modifier.class), "\u001b")),
                "\u001b");
        map.bind(
                new InputEvent(new KeyEvent(KeyEvent.Special.Backspace, EnumSet.noneOf(KeyEvent.Modifier.class), "\b")),
                "\b");
        map.bind(
                new InputEvent(
                        new KeyEvent(KeyEvent.Special.Backspace, EnumSet.noneOf(KeyEvent.Modifier.class), "\u007f")),
                "\u007f");

        // Bind common Ctrl+letter combinations
        bindControlKeys(map);

        // Space and other printable characters will be handled by the unicode handler
    }

    private static void bindKey(
            KeyMap<InputEvent> map, Terminal terminal, InfoCmp.Capability capability, KeyEvent keyEvent) {
        String sequence = getKeySequence(terminal, capability);
        if (sequence != null && !sequence.isEmpty()) {
            map.bind(new InputEvent(keyEvent), sequence);
        }
    }

    private static void bindControlKeys(KeyMap<InputEvent> map) {
        // Bind Ctrl+A through Ctrl+Z (excluding some that are handled specially)
        for (char c = 'a'; c <= 'z'; c++) {
            char ctrlChar = (char) (c - 'a' + 1);
            String sequence = String.valueOf(ctrlChar);

            // Skip some control chars that are handled specially above
            if (ctrlChar == '\t' || ctrlChar == '\n' || ctrlChar == '\r' || ctrlChar == '\u001b') {
                continue;
            }

            EnumSet<KeyEvent.Modifier> modifiers = EnumSet.of(KeyEvent.Modifier.Control);
            KeyEvent keyEvent = new KeyEvent(c, modifiers, sequence);
            map.bind(new InputEvent(keyEvent), sequence);
        }
    }

    private static String getKeySequence(Terminal terminal, InfoCmp.Capability capability) {
        String seq = terminal.getStringCapability(capability);
        if (seq != null) {
            return Curses.tputs(seq);
        } else {
            return null;
        }
    }

    /**
     * Creates an InputEvent for unmatched input by parsing it with KeyParser.
     * This is used as a fallback for keys not explicitly bound in the KeyMap.
     */
    public static InputEvent parseUnmatchedInput(String input) {
        KeyEvent keyEvent = KeyParser.parse(input);
        return new InputEvent(keyEvent);
    }
}

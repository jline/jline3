/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.demo.examples;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.EnumSet;

import org.jline.terminal.Attributes;
import org.jline.terminal.KeyEvent;
import org.jline.terminal.KeyParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

/**
 * Interactive key event viewer that displays parsed key events.
 *
 * <p>Enters raw mode with optional Kitty Keyboard Protocol support,
 * reads key presses, and displays the parsed {@link KeyEvent} for each.
 * Press {@code Escape} twice to exit.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * ./mvx demo KeyEventViewerExample            # default (legacy) mode
 * ./mvx demo KeyEventViewerExample -- --kitty  # enable kitty keyboard protocol
 * </pre>
 */
public class KeyEventViewerExample {

    public static void main(String[] args) throws IOException {
        boolean kitty = false;
        for (String arg : args) {
            if ("--kitty".equals(arg)) {
                kitty = true;
            }
        }

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            PrintWriter writer = terminal.writer();
            Attributes saved = terminal.getAttributes();
            terminal.enterRawMode();

            if (kitty) {
                if (terminal.hasKittyKeyboardSupport()) {
                    terminal.setKittyKeyboardMode(EnumSet.of(Terminal.KittyKeyboardMode.Disambiguate));
                    writeln(writer, "Kitty Keyboard Protocol enabled.");
                } else {
                    writeln(writer, "Terminal does not support Kitty Keyboard Protocol, using legacy mode.");
                    kitty = false;
                }
            }

            // Drain any pending input from probe responses
            NonBlockingReader reader = terminal.reader();
            while (reader.peek(100) >= 0) {
                reader.read();
            }

            writeln(writer, "Press keys to see parsed events. Press Escape twice to exit.");
            writeln(writer, "");
            writer.flush();
            boolean lastWasEscape = false;

            try {
                while (true) {
                    StringBuilder buf = new StringBuilder();
                    int c = reader.read();
                    if (c == -1) break;
                    buf.append((char) c);

                    if (c == 27) { // ESC — start of escape sequence
                        // Read ahead to collect the full sequence
                        while (true) {
                            int next = reader.peek(200);
                            if (next == -1 || next == -2) break;
                            next = reader.read();
                            buf.append((char) next);
                            // CSI sequences end with a letter in 0x40-0x7E range
                            if (buf.length() >= 3 && buf.charAt(1) == '[') {
                                if (next >= 0x40 && next <= 0x7E) break;
                            }
                            // SS3 sequences: ESC O <letter>
                            else if (buf.length() == 3 && buf.charAt(1) == 'O') {
                                break;
                            }
                            // ESC <letter> (alt-key), but not [ (CSI) or O (SS3)
                            else if (buf.length() == 2 && next >= 0x20 && next != '[' && next != 'O') {
                                break;
                            }
                        }
                    }

                    String raw = buf.toString();
                    KeyEvent event = KeyParser.parse(raw);

                    // Display hex dump of raw bytes
                    StringBuilder hex = new StringBuilder();
                    for (char ch : raw.toCharArray()) {
                        if (hex.length() > 0) hex.append(' ');
                        hex.append(String.format("%02X", (int) ch));
                    }

                    writeln(writer, "  raw: " + hex);
                    writeln(writer, "  " + formatEvent(event));
                    writeln(writer, "");
                    writer.flush();

                    // Exit on two consecutive Escapes (bare or kitty-encoded)
                    boolean isEscape = event.getType() == KeyEvent.Type.Special
                            && event.getSpecial() == KeyEvent.Special.Escape
                            && event.getModifiers().isEmpty();
                    if (isEscape) {
                        if (lastWasEscape) break;
                        lastWasEscape = true;
                    } else {
                        lastWasEscape = false;
                    }
                }
            } finally {
                if (kitty) {
                    terminal.resetKittyKeyboardMode();
                }
                terminal.setAttributes(saved);
                writeln(writer, "Done.");
                writer.flush();
            }
        }
    }

    private static void writeln(PrintWriter writer, String text) {
        writer.print(text);
        writer.print("\r\n");
    }

    private static String formatEvent(KeyEvent event) {
        StringBuilder sb = new StringBuilder("KeyEvent{type=").append(event.getType());
        switch (event.getType()) {
            case Character:
                sb.append(", character='").append(event.getCharacter()).append("'");
                break;
            case Arrow:
                sb.append(", arrow=").append(event.getArrow());
                break;
            case Special:
                sb.append(", special=").append(event.getSpecial());
                break;
            case Function:
                sb.append(", function=F").append(event.getFunctionKey());
                break;
            case Unknown:
                sb.append(", unknown");
                break;
        }
        if (!event.getModifiers().isEmpty()) {
            sb.append(", modifiers=").append(event.getModifiers());
        }
        if (event.getEventType() != KeyEvent.EventType.Press) {
            sb.append(", eventType=").append(event.getEventType());
        }
        if (event.getKeyCode() != 0) {
            sb.append(", keyCode=").append(event.getKeyCode());
        }
        if (event.getAssociatedText() != null) {
            sb.append(", text='").append(event.getAssociatedText()).append("'");
        }
        sb.append(", raw=").append(escapeRaw(event.getRawSequence()));
        sb.append("}");
        return sb.toString();
    }

    private static String escapeRaw(String raw) {
        if (raw == null) return "null";
        StringBuilder sb = new StringBuilder("'");
        for (char ch : raw.toCharArray()) {
            if (ch == 27) {
                sb.append("ESC");
            } else if (ch < 32) {
                sb.append("^").append((char) (ch + '@'));
            } else if (ch == 127) {
                sb.append("^?");
            } else {
                sb.append(ch);
            }
        }
        sb.append("'");
        return sb.toString();
    }
}

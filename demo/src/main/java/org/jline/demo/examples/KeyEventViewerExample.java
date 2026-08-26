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
import java.lang.reflect.Field;
import java.util.EnumSet;

import org.jline.terminal.Attributes;
import org.jline.terminal.KeyEvent;
import org.jline.terminal.KeyParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.KittyKeyboardSupport;
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
 * ./mvx demo KeyEventViewerExample                               # default (legacy) mode
 * ./mvx demo KeyEventViewerExample -- --kitty                     # kitty with Disambiguate only
 * ./mvx demo KeyEventViewerExample -- --kitty=disambiguate,events # choose specific flags
 * ./mvx demo KeyEventViewerExample -- --kitty=all                 # enable all flags
 * </pre>
 *
 * <p>Available kitty flags: {@code disambiguate}, {@code events}, {@code alternates},
 * {@code allkeys}, {@code text}, {@code all}.</p>
 */
public class KeyEventViewerExample {

    public static void main(String[] args) throws IOException {
        EnumSet<Terminal.KittyKeyboardMode> kittyModes = parseArgs(args);

        try (Terminal terminal = TerminalBuilder.builder().build()) {
            PrintWriter writer = terminal.writer();
            Attributes saved = terminal.getAttributes();
            terminal.enterRawMode();

            boolean kittyEnabled = enableKitty(terminal, writer, kittyModes);

            NonBlockingReader reader = terminal.reader();
            drainInput(reader);

            writeln(writer, "Press keys to see parsed events. Press Escape twice to exit.");
            writeln(writer, "");
            writer.flush();

            try {
                eventLoop(reader, writer);
            } finally {
                if (kittyEnabled) {
                    terminal.resetKittyKeyboardMode();
                }
                terminal.setAttributes(saved);
                writeln(writer, "Done.");
                writer.flush();
            }
        }
    }

    private static EnumSet<Terminal.KittyKeyboardMode> parseArgs(String[] args) {
        for (String arg : args) {
            if ("--kitty".equals(arg)) {
                return EnumSet.of(Terminal.KittyKeyboardMode.Disambiguate);
            } else if (arg.startsWith("--kitty=")) {
                return parseKittyFlags(arg.substring("--kitty=".length()));
            }
        }
        return null;
    }

    private static boolean enableKitty(
            Terminal terminal, PrintWriter writer, EnumSet<Terminal.KittyKeyboardMode> kittyModes) {
        if (kittyModes == null) {
            return false;
        }
        if (terminal.hasKittyKeyboardSupport()) {
            terminal.setKittyKeyboardMode(kittyModes);
            writeln(writer, "Kitty Keyboard Protocol enabled: " + kittyModes);
            return true;
        }
        writeln(writer, "Terminal does not support Kitty Keyboard Protocol, using legacy mode.");
        return false;
    }

    @SuppressWarnings("java:S2677")
    private static void drainInput(NonBlockingReader reader) throws IOException {
        while (reader.peek(100) >= 0) {
            reader.read();
        }
    }

    private static void eventLoop(NonBlockingReader reader, PrintWriter writer) throws IOException {
        boolean lastWasEscape = false;

        while (true) {
            int c = reader.read();
            if (c == -1) break;

            String raw = readSequence(reader, c);
            KeyEvent event = KeyParser.parse(raw);

            writeln(writer, "  raw: " + hexDump(raw));
            writeln(writer, "  " + formatEvent(event));
            writeln(writer, "");
            writer.flush();

            if (event.getEventType() == KeyEvent.EventType.Press) {
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
        }
    }

    private static String readSequence(NonBlockingReader reader, int firstChar) throws IOException {
        StringBuilder buf = new StringBuilder();
        buf.append((char) firstChar);

        if (firstChar != 27) {
            return buf.toString();
        }

        while (true) {
            int next = reader.peek(200);
            if (next == -1 || next == -2) break;
            next = reader.read();
            buf.append((char) next);

            if (isCsiTerminator(buf, next)) break;
            if (isSs3Complete(buf)) break;
            if (isAltKey(buf, next)) break;
        }
        return buf.toString();
    }

    private static boolean isCsiTerminator(StringBuilder buf, int next) {
        return buf.length() >= 3 && buf.charAt(1) == '[' && next >= 0x40 && next <= 0x7E;
    }

    private static boolean isSs3Complete(StringBuilder buf) {
        return buf.length() == 3 && buf.charAt(1) == 'O';
    }

    private static boolean isAltKey(StringBuilder buf, int next) {
        return buf.length() == 2 && next >= 0x20 && next != '[' && next != 'O';
    }

    private static String hexDump(String raw) {
        StringBuilder hex = new StringBuilder();
        for (char ch : raw.toCharArray()) {
            if (hex.length() > 0) hex.append(' ');
            hex.append(String.format("%02X", (int) ch));
        }
        return hex.toString();
    }

    private static void writeln(PrintWriter writer, String text) {
        writer.print(text);
        writer.print("\r\n");
    }

    private static String formatEvent(KeyEvent event) {
        StringBuilder sb = new StringBuilder("KeyEvent{type=").append(event.getType());
        appendTypeDetail(sb, event);
        if (!event.getModifiers().isEmpty()) {
            sb.append(", modifiers=").append(event.getModifiers());
        }
        if (event.getEventType() != KeyEvent.EventType.Press) {
            sb.append(", eventType=").append(event.getEventType());
        }
        if (event.getAssociatedText() != null) {
            sb.append(", text='").append(event.getAssociatedText()).append("'");
        }
        sb.append(", raw=").append(escapeRaw(event.getRawSequence()));
        sb.append("}");
        return sb.toString();
    }

    private static void appendTypeDetail(StringBuilder sb, KeyEvent event) {
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
            case Keypad:
                sb.append(", keypad=").append(event.getKeypad());
                break;
            case Media:
                sb.append(", media=").append(event.getMediaKey());
                break;
            case ModifierKey:
                sb.append(", modKey=").append(event.getModKey());
                break;
            case Unknown:
                appendUnknownDetail(sb, event);
                break;
            default:
                break;
        }
    }

    private static void appendUnknownDetail(StringBuilder sb, KeyEvent event) {
        if (event.getKeyCode() != 0) {
            String name = keyCodeName(event.getKeyCode());
            sb.append(", keyCode=").append(name != null ? name : event.getKeyCode());
        } else {
            sb.append(", unknown");
        }
    }

    private static EnumSet<Terminal.KittyKeyboardMode> parseKittyFlags(String spec) {
        if ("all".equalsIgnoreCase(spec)) {
            return EnumSet.allOf(Terminal.KittyKeyboardMode.class);
        }
        EnumSet<Terminal.KittyKeyboardMode> modes = EnumSet.noneOf(Terminal.KittyKeyboardMode.class);
        for (String flag : spec.split(",")) {
            switch (flag.trim().toLowerCase()) {
                case "disambiguate":
                    modes.add(Terminal.KittyKeyboardMode.Disambiguate);
                    break;
                case "events":
                    modes.add(Terminal.KittyKeyboardMode.ReportEvents);
                    break;
                case "alternates":
                    modes.add(Terminal.KittyKeyboardMode.ReportAlternates);
                    break;
                case "allkeys":
                    modes.add(Terminal.KittyKeyboardMode.ReportAllKeys);
                    break;
                case "text":
                    modes.add(Terminal.KittyKeyboardMode.ReportText);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown kitty flag: " + flag
                            + ". Valid flags: disambiguate, events, alternates, allkeys, text, all");
            }
        }
        return modes;
    }

    private static String keyCodeName(int keyCode) {
        for (Field f : KittyKeyboardSupport.class.getFields()) {
            if (f.getName().startsWith("KEY_") && f.getType() == int.class) {
                try {
                    if (f.getInt(null) == keyCode) {
                        return f.getName();
                    }
                } catch (IllegalAccessException e) {
                    // skip
                }
            }
        }
        return null;
    }

    private static String escapeRaw(String raw) {
        if (raw == null) return "null";
        StringBuilder sb = new StringBuilder("'");
        for (char ch : raw.toCharArray()) {
            if (ch == 27) {
                sb.append("\\E");
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

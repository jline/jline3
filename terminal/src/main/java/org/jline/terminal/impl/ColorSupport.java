/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jline.utils.NonBlockingReader;

/**
 * Parses OSC 10/11 color query responses from the terminal.
 *
 * <p>Terminals that support OSC color queries respond with an escape sequence
 * containing the current foreground (OSC 10) or background (OSC 11) color
 * as an {@code rgb:RRRR/GGGG/BBBB} value. This class reads and decodes
 * those responses into a packed 24-bit RGB integer.</p>
 */
public class ColorSupport {

    private ColorSupport() {}

    /**
     * Reads an OSC color response from the terminal and returns the color as a 24-bit RGB integer.
     *
     * @param reader    non-blocking reader connected to the terminal input
     * @param colorType the OSC color type to match (10 for foreground, 11 for background)
     * @return the color as a packed 24-bit RGB integer ({@code 0xRRGGBB}), or {@code -1} if
     *         no valid response was received
     * @throws IOException if an I/O error occurs while reading from the terminal
     */
    public static int parseColorResponse(NonBlockingReader reader, int colorType) throws IOException {
        if (reader.peek(50) < 0) {
            return -1;
        }
        if (!readOscHeader(reader, colorType)) {
            drainUntilTerminator(reader);
            return -1;
        }
        List<String> rgb = readRgbValues(reader);
        if (rgb.size() != 3
                || rgb.get(0).isEmpty()
                || rgb.get(1).isEmpty()
                || rgb.get(2).isEmpty()) {
            return -1;
        }
        return convertRgbToInt(rgb);
    }

    private static boolean readOscHeader(NonBlockingReader reader, int colorType) throws IOException {
        if (reader.read(10) != '\033' || reader.read(10) != ']') {
            return false;
        }
        int tens = reader.read(10);
        int ones = reader.read(10);
        if (tens != '1' || (ones != '0' && ones != '1')) {
            return false;
        }
        int type = (ones - '0') + 10;
        if (type != colorType) {
            return false;
        }
        if (reader.read(10) != ';') {
            return false;
        }
        return reader.read(10) == 'r' && reader.read(10) == 'g' && reader.read(10) == 'b' && reader.read(10) == ':';
    }

    private static List<String> readRgbValues(NonBlockingReader reader) throws IOException {
        StringBuilder sb = new StringBuilder(16);
        List<String> rgb = new ArrayList<>();
        while (true) {
            int c = reader.read(10);
            if (c == -1) {
                return Collections.emptyList();
            }
            if (c == NonBlockingReader.READ_EXPIRED) {
                return Collections.emptyList();
            }
            if (c == '\007') {
                rgb.add(sb.toString());
                return rgb;
            }
            if (c == '\033') {
                return readStTerminator(reader) ? addAndReturn(rgb, sb.toString()) : Collections.emptyList();
            }
            if (isHexChar(c)) {
                sb.append((char) c);
            } else if (c == '/') {
                rgb.add(sb.toString());
                sb.setLength(0);
            }
        }
    }

    private static boolean readStTerminator(NonBlockingReader reader) throws IOException {
        return reader.read(10) == '\\';
    }

    private static List<String> addAndReturn(List<String> list, String value) {
        list.add(value);
        return list;
    }

    private static void drainUntilTerminator(NonBlockingReader reader) throws IOException {
        while (true) {
            int c = reader.read(10);
            if (c == -1 || c == NonBlockingReader.READ_EXPIRED || c == '\007') {
                return;
            }
            if (c == '\033') {
                reader.read(10);
                return;
            }
        }
    }

    private static boolean isHexChar(int c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    private static int convertRgbToInt(List<String> rgb) {
        double r = Integer.parseInt(rgb.get(0), 16) / ((1 << (4 * rgb.get(0).length())) - 1.0);
        double g = Integer.parseInt(rgb.get(1), 16) / ((1 << (4 * rgb.get(1).length())) - 1.0);
        double b = Integer.parseInt(rgb.get(2), 16) / ((1 << (4 * rgb.get(2).length())) - 1.0);
        return (int) ((Math.round(r * 255) << 16) + (Math.round(g * 255) << 8) + Math.round(b * 255));
    }
}

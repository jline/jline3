/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.jansi.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.jline.jansi.AnsiColors;

/**
 * Ansi processor to process color conversion if needed.
 */
public class ColorsAnsiProcessor extends AnsiProcessor {

    private final AnsiColors colors;

    public ColorsAnsiProcessor(OutputStream os, AnsiColors colors) {
        super(os);
        this.colors = colors;
    }

    @Override
    protected boolean processEscapeCommand(ArrayList<Object> options, int command) throws IOException {
        if (command == 'm' && (colors == AnsiColors.Colors256 || colors == AnsiColors.Colors16)) {
            // Validate all options are ints...
            boolean has38or48 = false;
            for (Object next : options) {
                if (next != null && next.getClass() != Integer.class) {
                    throw new IllegalArgumentException();
                }
                Integer value = (Integer) next;
                has38or48 |= value == 38 || value == 48;
            }
            // SGR commands do not contain an extended color, so just transfer the buffer
            if (!has38or48) {
                return false;
            }
            StringBuilder sb = new StringBuilder(32);
            sb.append('\033').append('[');
            boolean first = true;
            Iterator<Object> optionsIterator = options.iterator();
            while (optionsIterator.hasNext()) {
                Object next = optionsIterator.next();
                if (next != null) {
                    int value = (Integer) next;
                    if (value == 38 || value == 48) {
                        // extended color like `esc[38;5;<index>m` or `esc[38;2;<r>;<g>;<b>m`
                        int arg2or5 = getNextOptionInt(optionsIterator);
                        if (arg2or5 == 2) {
                            // 24 bit color style like `esc[38;2;<r>;<g>;<b>m`
                            int r = getNextOptionInt(optionsIterator);
                            int g = getNextOptionInt(optionsIterator);
                            int b = getNextOptionInt(optionsIterator);
                            if (colors == AnsiColors.Colors256) {
                                int col = Colors.roundRgbColor(r, g, b, 256);
                                if (!first) {
                                    sb.append(';');
                                }
                                first = false;
                                sb.append(value);
                                sb.append(';');
                                sb.append(5);
                                sb.append(';');
                                sb.append(col);
                            } else {
                                int col = Colors.roundRgbColor(r, g, b, 16);
                                if (!first) {
                                    sb.append(';');
                                }
                                first = false;
                                sb.append(
                                        value == 38
                                                ? col >= 8 ? 90 + col - 8 : 30 + col
                                                : col >= 8 ? 100 + col - 8 : 40 + col);
                            }
                        } else if (arg2or5 == 5) {
                            // 256 color style like `esc[38;5;<index>m`
                            int paletteIndex = getNextOptionInt(optionsIterator);
                            if (colors == AnsiColors.Colors256) {
                                if (!first) {
                                    sb.append(';');
                                }
                                first = false;
                                sb.append(value);
                                sb.append(';');
                                sb.append(5);
                                sb.append(';');
                                sb.append(paletteIndex);
                            } else {
                                int col = Colors.roundColor(paletteIndex, 16);
                                if (!first) {
                                    sb.append(';');
                                }
                                first = false;
                                sb.append(
                                        value == 38
                                                ? col >= 8 ? 90 + col - 8 : 30 + col
                                                : col >= 8 ? 100 + col - 8 : 40 + col);
                            }
                        } else {
                            throw new IllegalArgumentException();
                        }
                    } else {
                        if (!first) {
                            sb.append(';');
                        }
                        first = false;
                        sb.append(value);
                    }
                }
            }
            sb.append('m');
            os.write(sb.toString().getBytes());
            return true;

        } else {
            return false;
        }
    }

    @Override
    protected boolean processOperatingSystemCommand(ArrayList<Object> options) {
        return false;
    }

    @Override
    protected boolean processCharsetSelect(ArrayList<Object> options) {
        return false;
    }
}

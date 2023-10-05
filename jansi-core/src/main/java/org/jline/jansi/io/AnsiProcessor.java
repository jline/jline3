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

/**
 * ANSI processor providing <code>process*</code> corresponding to ANSI escape codes.
 * This class methods implementations are empty: subclasses should actually perform the
 * ANSI escape behaviors by implementing active code in <code>process*</code> methods.
 *
 * <p>For more information about ANSI escape codes, see
 * <a href="http://en.wikipedia.org/wiki/ANSI_escape_code">Wikipedia article</a>
 *
 * @since 1.19
 */
@SuppressWarnings("unused")
public class AnsiProcessor {
    protected final OutputStream os;

    public AnsiProcessor(OutputStream os) {
        this.os = os;
    }

    /**
     * Helper for processEscapeCommand() to iterate over integer options
     * @param  optionsIterator  the underlying iterator
     * @throws IOException      if no more non-null values left
     */
    protected int getNextOptionInt(Iterator<Object> optionsIterator) throws IOException {
        for (; ; ) {
            if (!optionsIterator.hasNext()) throw new IllegalArgumentException();
            Object arg = optionsIterator.next();
            if (arg != null) return (Integer) arg;
        }
    }

    /**
     * @return true if the escape command was processed.
     */
    protected boolean processEscapeCommand(ArrayList<Object> options, int command) throws IOException {
        try {
            switch (command) {
                case 'A':
                    processCursorUp(optionInt(options, 0, 1));
                    return true;
                case 'B':
                    processCursorDown(optionInt(options, 0, 1));
                    return true;
                case 'C':
                    processCursorRight(optionInt(options, 0, 1));
                    return true;
                case 'D':
                    processCursorLeft(optionInt(options, 0, 1));
                    return true;
                case 'E':
                    processCursorDownLine(optionInt(options, 0, 1));
                    return true;
                case 'F':
                    processCursorUpLine(optionInt(options, 0, 1));
                    return true;
                case 'G':
                    processCursorToColumn(optionInt(options, 0));
                    return true;
                case 'H':
                case 'f':
                    processCursorTo(optionInt(options, 0, 1), optionInt(options, 1, 1));
                    return true;
                case 'J':
                    processEraseScreen(optionInt(options, 0, 0));
                    return true;
                case 'K':
                    processEraseLine(optionInt(options, 0, 0));
                    return true;
                case 'L':
                    processInsertLine(optionInt(options, 0, 1));
                    return true;
                case 'M':
                    processDeleteLine(optionInt(options, 0, 1));
                    return true;
                case 'S':
                    processScrollUp(optionInt(options, 0, 1));
                    return true;
                case 'T':
                    processScrollDown(optionInt(options, 0, 1));
                    return true;
                case 'm':
                    // Validate all options are ints...
                    for (Object next : options) {
                        if (next != null && next.getClass() != Integer.class) {
                            throw new IllegalArgumentException();
                        }
                    }

                    int count = 0;
                    Iterator<Object> optionsIterator = options.iterator();
                    while (optionsIterator.hasNext()) {
                        Object next = optionsIterator.next();
                        if (next != null) {
                            count++;
                            int value = (Integer) next;
                            if (30 <= value && value <= 37) {
                                processSetForegroundColor(value - 30);
                            } else if (40 <= value && value <= 47) {
                                processSetBackgroundColor(value - 40);
                            } else if (90 <= value && value <= 97) {
                                processSetForegroundColor(value - 90, true);
                            } else if (100 <= value && value <= 107) {
                                processSetBackgroundColor(value - 100, true);
                            } else if (value == 38 || value == 48) {
                                if (!optionsIterator.hasNext()) {
                                    continue;
                                }
                                // extended color like `esc[38;5;<index>m` or `esc[38;2;<r>;<g>;<b>m`
                                int arg2or5 = getNextOptionInt(optionsIterator);
                                if (arg2or5 == 2) {
                                    // 24 bit color style like `esc[38;2;<r>;<g>;<b>m`
                                    int r = getNextOptionInt(optionsIterator);
                                    int g = getNextOptionInt(optionsIterator);
                                    int b = getNextOptionInt(optionsIterator);
                                    if (r >= 0 && r <= 255 && g >= 0 && g <= 255 && b >= 0 && b <= 255) {
                                        if (value == 38) processSetForegroundColorExt(r, g, b);
                                        else processSetBackgroundColorExt(r, g, b);
                                    } else {
                                        throw new IllegalArgumentException();
                                    }
                                } else if (arg2or5 == 5) {
                                    // 256 color style like `esc[38;5;<index>m`
                                    int paletteIndex = getNextOptionInt(optionsIterator);
                                    if (paletteIndex >= 0 && paletteIndex <= 255) {
                                        if (value == 38) processSetForegroundColorExt(paletteIndex);
                                        else processSetBackgroundColorExt(paletteIndex);
                                    } else {
                                        throw new IllegalArgumentException();
                                    }
                                } else {
                                    throw new IllegalArgumentException();
                                }
                            } else {
                                switch (value) {
                                    case 39:
                                        processDefaultTextColor();
                                        break;
                                    case 49:
                                        processDefaultBackgroundColor();
                                        break;
                                    case 0:
                                        processAttributeReset();
                                        break;
                                    default:
                                        processSetAttribute(value);
                                }
                            }
                        }
                    }
                    if (count == 0) {
                        processAttributeReset();
                    }
                    return true;
                case 's':
                    processSaveCursorPosition();
                    return true;
                case 'u':
                    processRestoreCursorPosition();
                    return true;

                default:
                    if ('a' <= command && command <= 'z') {
                        processUnknownExtension(options, command);
                        return true;
                    }
                    if ('A' <= command && command <= 'Z') {
                        processUnknownExtension(options, command);
                        return true;
                    }
                    return false;
            }
        } catch (IllegalArgumentException ignore) {
        }
        return false;
    }

    /**
     * @return true if the operating system command was processed.
     */
    protected boolean processOperatingSystemCommand(ArrayList<Object> options) {
        int command = optionInt(options, 0);
        String label = (String) options.get(1);
        // for command > 2 label could be composed (i.e. contain ';'), but we'll leave
        // it to processUnknownOperatingSystemCommand implementations to handle that
        try {
            switch (command) {
                case 0:
                    processChangeIconNameAndWindowTitle(label);
                    return true;
                case 1:
                    processChangeIconName(label);
                    return true;
                case 2:
                    processChangeWindowTitle(label);
                    return true;

                default:
                    // not exactly unknown, but not supported through dedicated process methods:
                    processUnknownOperatingSystemCommand(command, label);
                    return true;
            }
        } catch (IllegalArgumentException ignore) {
        }
        return false;
    }

    /**
     * Process character set sequence.
     * @param options options
     * @return true if the charcter set select command was processed.
     */
    protected boolean processCharsetSelect(ArrayList<Object> options) {
        int set = optionInt(options, 0);
        char seq = (Character) options.get(1);
        processCharsetSelect(set, seq);
        return true;
    }

    private int optionInt(ArrayList<Object> options, int index) {
        if (options.size() <= index) throw new IllegalArgumentException();
        Object value = options.get(index);
        if (value == null) throw new IllegalArgumentException();
        if (!value.getClass().equals(Integer.class)) throw new IllegalArgumentException();
        return (Integer) value;
    }

    private int optionInt(ArrayList<Object> options, int index, int defaultValue) {
        if (options.size() > index) {
            Object value = options.get(index);
            if (value == null) {
                return defaultValue;
            }
            return (Integer) value;
        }
        return defaultValue;
    }

    /**
     * Process <code>CSI u</code> ANSI code, corresponding to <code>RCP – Restore Cursor Position</code>
     * @throws IOException IOException
     */
    protected void processRestoreCursorPosition() throws IOException {}

    /**
     * Process <code>CSI s</code> ANSI code, corresponding to <code>SCP – Save Cursor Position</code>
     * @throws IOException IOException
     */
    protected void processSaveCursorPosition() throws IOException {}

    /**
     * Process <code>CSI L</code> ANSI code, corresponding to <code>IL – Insert Line</code>
     * @param optionInt option
     * @throws IOException IOException
     * @since 1.16
     */
    protected void processInsertLine(int optionInt) throws IOException {}

    /**
     * Process <code>CSI M</code> ANSI code, corresponding to <code>DL – Delete Line</code>
     * @param optionInt option
     * @throws IOException IOException
     * @since 1.16
     */
    protected void processDeleteLine(int optionInt) throws IOException {}

    /**
     * Process <code>CSI n T</code> ANSI code, corresponding to <code>SD – Scroll Down</code>
     * @param optionInt option
     * @throws IOException IOException
     */
    protected void processScrollDown(int optionInt) throws IOException {}

    /**
     * Process <code>CSI n U</code> ANSI code, corresponding to <code>SU – Scroll Up</code>
     * @param optionInt option
     * @throws IOException IOException
     */
    protected void processScrollUp(int optionInt) throws IOException {}

    protected static final int ERASE_SCREEN_TO_END = 0;
    protected static final int ERASE_SCREEN_TO_BEGINING = 1;
    protected static final int ERASE_SCREEN = 2;

    /**
     * Process <code>CSI n J</code> ANSI code, corresponding to <code>ED – Erase in Display</code>
     * @param eraseOption eraseOption
     * @throws IOException IOException
     */
    protected void processEraseScreen(int eraseOption) throws IOException {}

    protected static final int ERASE_LINE_TO_END = 0;
    protected static final int ERASE_LINE_TO_BEGINING = 1;
    protected static final int ERASE_LINE = 2;

    /**
     * Process <code>CSI n K</code> ANSI code, corresponding to <code>ED – Erase in Line</code>
     * @param eraseOption eraseOption
     * @throws IOException IOException
     */
    protected void processEraseLine(int eraseOption) throws IOException {}

    protected static final int ATTRIBUTE_INTENSITY_BOLD = 1; // 	Intensity: Bold
    protected static final int ATTRIBUTE_INTENSITY_FAINT = 2; // 	Intensity; Faint 	not widely supported
    protected static final int ATTRIBUTE_ITALIC = 3; // 	Italic; on 	not widely supported. Sometimes treated as inverse.
    protected static final int ATTRIBUTE_UNDERLINE = 4; // 	Underline; Single
    protected static final int ATTRIBUTE_BLINK_SLOW = 5; // 	Blink; Slow 	less than 150 per minute
    protected static final int ATTRIBUTE_BLINK_FAST = 6; // 	Blink; Rapid 	MS-DOS ANSI.SYS; 150 per minute or more
    protected static final int ATTRIBUTE_NEGATIVE_ON =
            7; // 	Image; Negative 	inverse or reverse; swap foreground and background
    protected static final int ATTRIBUTE_CONCEAL_ON = 8; // 	Conceal on
    protected static final int ATTRIBUTE_UNDERLINE_DOUBLE = 21; // 	Underline; Double 	not widely supported
    protected static final int ATTRIBUTE_INTENSITY_NORMAL = 22; // 	Intensity; Normal 	not bold and not faint
    protected static final int ATTRIBUTE_UNDERLINE_OFF = 24; // 	Underline; None
    protected static final int ATTRIBUTE_BLINK_OFF = 25; // 	Blink; off
    protected static final int ATTRIBUTE_NEGATIVE_OFF = 27; // 	Image; Positive
    protected static final int ATTRIBUTE_CONCEAL_OFF = 28; // 	Reveal 	conceal off

    /**
     * process <code>SGR</code> other than <code>0</code> (reset), <code>30-39</code> (foreground),
     * <code>40-49</code> (background), <code>90-97</code> (foreground high intensity) or
     * <code>100-107</code> (background high intensity)
     * @param attribute attribute
     * @throws IOException IOException
     * @see #processAttributeReset()
     * @see #processSetForegroundColor(int)
     * @see #processSetForegroundColor(int, boolean)
     * @see #processSetForegroundColorExt(int)
     * @see #processSetForegroundColorExt(int, int, int)
     * @see #processDefaultTextColor()
     * @see #processDefaultBackgroundColor()
     */
    protected void processSetAttribute(int attribute) throws IOException {}

    protected static final int BLACK = 0;
    protected static final int RED = 1;
    protected static final int GREEN = 2;
    protected static final int YELLOW = 3;
    protected static final int BLUE = 4;
    protected static final int MAGENTA = 5;
    protected static final int CYAN = 6;
    protected static final int WHITE = 7;

    /**
     * process <code>SGR 30-37</code> corresponding to <code>Set text color (foreground)</code>.
     * @param color the text color
     * @throws IOException IOException
     */
    protected void processSetForegroundColor(int color) throws IOException {
        processSetForegroundColor(color, false);
    }

    /**
     * process <code>SGR 30-37</code> or <code>SGR 90-97</code> corresponding to
     * <code>Set text color (foreground)</code> either in normal mode or high intensity.
     * @param color the text color
     * @param bright is high intensity?
     * @throws IOException IOException
     */
    protected void processSetForegroundColor(int color, boolean bright) throws IOException {}

    /**
     * process <code>SGR 38</code> corresponding to <code>extended set text color (foreground)</code>
     * with a palette of 255 colors.
     * @param paletteIndex the text color in the palette
     * @throws IOException IOException
     */
    protected void processSetForegroundColorExt(int paletteIndex) throws IOException {}

    /**
     * process <code>SGR 38</code> corresponding to <code>extended set text color (foreground)</code>
     * with a 24 bits RGB definition of the color.
     * @param r red
     * @param g green
     * @param b blue
     * @throws IOException IOException
     */
    protected void processSetForegroundColorExt(int r, int g, int b) throws IOException {}

    /**
     * process <code>SGR 40-47</code> corresponding to <code>Set background color</code>.
     * @param color the background color
     * @throws IOException IOException
     */
    protected void processSetBackgroundColor(int color) throws IOException {
        processSetBackgroundColor(color, false);
    }

    /**
     * process <code>SGR 40-47</code> or <code>SGR 100-107</code> corresponding to
     * <code>Set background color</code> either in normal mode or high intensity.
     * @param color the background color
     * @param bright is high intensity?
     * @throws IOException IOException
     */
    protected void processSetBackgroundColor(int color, boolean bright) throws IOException {}

    /**
     * process <code>SGR 48</code> corresponding to <code>extended set background color</code>
     * with a palette of 255 colors.
     * @param paletteIndex the background color in the palette
     * @throws IOException IOException
     */
    protected void processSetBackgroundColorExt(int paletteIndex) throws IOException {}

    /**
     * process <code>SGR 48</code> corresponding to <code>extended set background color</code>
     * with a 24 bits RGB definition of the color.
     * @param r red
     * @param g green
     * @param b blue
     * @throws IOException IOException
     */
    protected void processSetBackgroundColorExt(int r, int g, int b) throws IOException {}

    /**
     * process <code>SGR 39</code> corresponding to <code>Default text color (foreground)</code>
     * @throws IOException IOException
     */
    protected void processDefaultTextColor() throws IOException {}

    /**
     * process <code>SGR 49</code> corresponding to <code>Default background color</code>
     * @throws IOException IOException
     */
    protected void processDefaultBackgroundColor() throws IOException {}

    /**
     * process <code>SGR 0</code> corresponding to <code>Reset / Normal</code>
     * @throws IOException IOException
     */
    protected void processAttributeReset() throws IOException {}

    /**
     * process <code>CSI n ; m H</code> corresponding to <code>CUP – Cursor Position</code> or
     * <code>CSI n ; m f</code> corresponding to <code>HVP – Horizontal and Vertical Position</code>
     * @param row row
     * @param col col
     * @throws IOException IOException
     */
    protected void processCursorTo(int row, int col) throws IOException {}

    /**
     * process <code>CSI n G</code> corresponding to <code>CHA – Cursor Horizontal Absolute</code>
     * @param x the column
     * @throws IOException IOException
     */
    protected void processCursorToColumn(int x) throws IOException {}

    /**
     * process <code>CSI n F</code> corresponding to <code>CPL – Cursor Previous Line</code>
     * @param count line count
     * @throws IOException IOException
     */
    protected void processCursorUpLine(int count) throws IOException {}

    /**
     * process <code>CSI n E</code> corresponding to <code>CNL – Cursor Next Line</code>
     * @param count line count
     * @throws IOException IOException
     */
    protected void processCursorDownLine(int count) throws IOException {
        // Poor mans impl..
        for (int i = 0; i < count; i++) {
            os.write('\n');
        }
    }

    /**
     * process <code>CSI n D</code> corresponding to <code>CUB – Cursor Back</code>
     * @param count count
     * @throws IOException IOException
     */
    protected void processCursorLeft(int count) throws IOException {}

    /**
     * process <code>CSI n C</code> corresponding to <code>CUF – Cursor Forward</code>
     * @param count count
     * @throws IOException IOException
     */
    protected void processCursorRight(int count) throws IOException {
        // Poor mans impl..
        for (int i = 0; i < count; i++) {
            os.write(' ');
        }
    }

    /**
     * process <code>CSI n B</code> corresponding to <code>CUD – Cursor Down</code>
     * @param count count
     * @throws IOException IOException
     */
    protected void processCursorDown(int count) throws IOException {}

    /**
     * process <code>CSI n A</code> corresponding to <code>CUU – Cursor Up</code>
     * @param count count
     * @throws IOException IOException
     */
    protected void processCursorUp(int count) throws IOException {}

    /**
     * Process Unknown Extension
     * @param options options
     * @param command command
     */
    protected void processUnknownExtension(ArrayList<Object> options, int command) {}

    /**
     * process <code>OSC 0;text BEL</code> corresponding to <code>Change Window and Icon label</code>
     * @param label window title name
     */
    protected void processChangeIconNameAndWindowTitle(String label) {
        processChangeIconName(label);
        processChangeWindowTitle(label);
    }

    /**
     * process <code>OSC 1;text BEL</code> corresponding to <code>Change Icon label</code>
     * @param label icon label
     */
    protected void processChangeIconName(String label) {}

    /**
     * process <code>OSC 2;text BEL</code> corresponding to <code>Change Window title</code>
     * @param label window title text
     */
    protected void processChangeWindowTitle(String label) {}

    /**
     * Process unknown <code>OSC</code> command.
     * @param command command
     * @param param param
     */
    protected void processUnknownOperatingSystemCommand(int command, String param) {}

    protected void processCharsetSelect(int set, char seq) {}
}

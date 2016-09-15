/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jna.win;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import static org.jline.terminal.impl.jna.win.Kernel32.BACKGROUND_BLUE;
import static org.jline.terminal.impl.jna.win.Kernel32.BACKGROUND_GREEN;
import static org.jline.terminal.impl.jna.win.Kernel32.BACKGROUND_INTENSITY;
import static org.jline.terminal.impl.jna.win.Kernel32.BACKGROUND_RED;
import static org.jline.terminal.impl.jna.win.Kernel32.FOREGROUND_BLUE;
import static org.jline.terminal.impl.jna.win.Kernel32.FOREGROUND_GREEN;
import static org.jline.terminal.impl.jna.win.Kernel32.FOREGROUND_INTENSITY;
import static org.jline.terminal.impl.jna.win.Kernel32.FOREGROUND_RED;


/**
 * A Windows ANSI escape processor, uses JNA to access native platform
 * API's to change the console attributes.
 *
 * @since 1.0
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author Joris Kuipers
 */
public final class WindowsAnsiOutputStream extends FilterOutputStream {

    private static final short FOREGROUND_BLACK   = 0;
    private static final short FOREGROUND_YELLOW  = (short) (FOREGROUND_RED|FOREGROUND_GREEN);
    private static final short FOREGROUND_MAGENTA = (short) (FOREGROUND_BLUE|FOREGROUND_RED);
    private static final short FOREGROUND_CYAN    = (short) (FOREGROUND_BLUE|FOREGROUND_GREEN);
    private static final short FOREGROUND_WHITE   = (short) (FOREGROUND_RED|FOREGROUND_GREEN|FOREGROUND_BLUE);

    private static final short BACKGROUND_BLACK   = 0;
    private static final short BACKGROUND_YELLOW  = (short) (BACKGROUND_RED|BACKGROUND_GREEN);
    private static final short BACKGROUND_MAGENTA = (short) (BACKGROUND_BLUE|BACKGROUND_RED);
    private static final short BACKGROUND_CYAN    = (short) (BACKGROUND_BLUE|BACKGROUND_GREEN);
    private static final short BACKGROUND_WHITE   = (short) (BACKGROUND_RED|BACKGROUND_GREEN|BACKGROUND_BLUE);

    private static final short ANSI_FOREGROUND_COLOR_MAP[] = {
            FOREGROUND_BLACK,
            FOREGROUND_RED,
            FOREGROUND_GREEN,
            FOREGROUND_YELLOW,
            FOREGROUND_BLUE,
            FOREGROUND_MAGENTA,
            FOREGROUND_CYAN,
            FOREGROUND_WHITE,
    };

    private static final short ANSI_BACKGROUND_COLOR_MAP[] = {
            BACKGROUND_BLACK,
            BACKGROUND_RED,
            BACKGROUND_GREEN,
            BACKGROUND_YELLOW,
            BACKGROUND_BLUE,
            BACKGROUND_MAGENTA,
            BACKGROUND_CYAN,
            BACKGROUND_WHITE,
    };

    public static final byte[] REST_CODE = "\033[0m".getBytes();

    private final static int MAX_ESCAPE_SEQUENCE_LENGTH = 100;

    private static final int LOOKING_FOR_FIRST_ESC_CHAR = 0;
    private static final int LOOKING_FOR_SECOND_ESC_CHAR = 1;
    private static final int LOOKING_FOR_NEXT_ARG = 2;
    private static final int LOOKING_FOR_STR_ARG_END = 3;
    private static final int LOOKING_FOR_INT_ARG_END = 4;
    private static final int LOOKING_FOR_OSC_COMMAND = 5;
    private static final int LOOKING_FOR_OSC_COMMAND_END = 6;
    private static final int LOOKING_FOR_OSC_PARAM = 7;
    private static final int LOOKING_FOR_ST = 8;

    private static final int FIRST_ESC_CHAR = 27;
    private static final int SECOND_ESC_CHAR = '[';
    private static final int SECOND_OSC_CHAR = ']';
    private static final int BEL = 7;
    private static final int SECOND_ST_CHAR = '\\';

    protected static final int ERASE_SCREEN_TO_END = 0;
    protected static final int ERASE_SCREEN_TO_BEGINING = 1;
    protected static final int ERASE_SCREEN = 2;

    protected static final int ERASE_LINE_TO_END = 0;
    protected static final int ERASE_LINE_TO_BEGINING = 1;
    protected static final int ERASE_LINE = 2;

    protected static final int ATTRIBUTE_INTENSITY_BOLD = 1; // 	Intensity: Bold
    protected static final int ATTRIBUTE_INTENSITY_FAINT = 2; // 	Intensity; Faint 	not widely supported
    protected static final int ATTRIBUTE_ITALIC = 3; // 	Italic; on 	not widely supported. Sometimes treated as inverse.
    protected static final int ATTRIBUTE_UNDERLINE = 4; // 	Underline; Single
    protected static final int ATTRIBUTE_BLINK_SLOW = 5; // 	Blink; Slow 	less than 150 per minute
    protected static final int ATTRIBUTE_BLINK_FAST = 6; // 	Blink; Rapid 	MS-DOS ANSI.SYS; 150 per minute or more
    protected static final int ATTRIBUTE_NEGATIVE_ON = 7; // 	Image; Negative 	inverse or reverse; swap foreground and background
    protected static final int ATTRIBUTE_CONCEAL_ON = 8; // 	Conceal on
    protected static final int ATTRIBUTE_UNDERLINE_DOUBLE = 21; // 	Underline; Double 	not widely supported
    protected static final int ATTRIBUTE_INTENSITY_NORMAL = 22; // 	Intensity; Normal 	not bold and not faint
    protected static final int ATTRIBUTE_UNDERLINE_OFF = 24; // 	Underline; None
    protected static final int ATTRIBUTE_BLINK_OFF = 25; // 	Blink; off
    protected static final int ATTRIBUTE_NEGATIVE_Off = 27; // 	Image; Positive
    protected static final int ATTRIBUTE_CONCEAL_OFF = 28; // 	Reveal 	conceal off

    protected static final int BLACK = 0;
    protected static final int RED = 1;
    protected static final int GREEN = 2;
    protected static final int YELLOW = 3;
    protected static final int BLUE = 4;
    protected static final int MAGENTA = 5;
    protected static final int CYAN = 6;
    protected static final int WHITE = 7;

    private final Pointer console = Kernel32.INSTANCE.GetStdHandle(Kernel32.STD_OUTPUT_HANDLE);

    private final Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
    private final short originalColors;

    private boolean negative;
    private short savedX = -1;
    private short savedY = -1;

    private byte buffer[] = new byte[MAX_ESCAPE_SEQUENCE_LENGTH];
    private int pos = 0;
    private int startOfValue;
    private final ArrayList<Object> options = new ArrayList<>();

    int state = LOOKING_FOR_FIRST_ESC_CHAR;


    // TODO: implement to get perf boost: public void write(byte[] b, int off, int len)

    public void write(int data) throws IOException {
        switch (state) {
            case LOOKING_FOR_FIRST_ESC_CHAR:
                if (data == FIRST_ESC_CHAR) {
                    buffer[pos++] = (byte) data;
                    state = LOOKING_FOR_SECOND_ESC_CHAR;
                } else {
                    out.write(data);
                }
                break;

            case LOOKING_FOR_SECOND_ESC_CHAR:
                buffer[pos++] = (byte) data;
                if (data == SECOND_ESC_CHAR) {
                    state = LOOKING_FOR_NEXT_ARG;
                } else if (data == SECOND_OSC_CHAR) {
                    state = LOOKING_FOR_OSC_COMMAND;
                } else {
                    reset(false);
                }
                break;

            case LOOKING_FOR_NEXT_ARG:
                buffer[pos++] = (byte) data;
                if ('"' == data) {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_STR_ARG_END;
                } else if ('0' <= data && data <= '9') {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_INT_ARG_END;
                } else if (';' == data) {
                    options.add(null);
                } else if ('?' == data) {
                    options.add('?');
                } else if ('=' == data) {
                    options.add('=');
                } else {
                    reset(processEscapeCommand(options, data));
                }
                break;

            case LOOKING_FOR_INT_ARG_END:
                buffer[pos++] = (byte) data;
                if (!('0' <= data && data <= '9')) {
                    String strValue = new String(buffer, startOfValue, (pos - 1) - startOfValue, "UTF-8");
                    Integer value = new Integer(strValue);
                    options.add(value);
                    if (data == ';') {
                        state = LOOKING_FOR_NEXT_ARG;
                    } else {
                        reset(processEscapeCommand(options, data));
                    }
                }
                break;

            case LOOKING_FOR_STR_ARG_END:
                buffer[pos++] = (byte) data;
                if ('"' != data) {
                    String value = new String(buffer, startOfValue, (pos - 1) - startOfValue, "UTF-8");
                    options.add(value);
                    if (data == ';') {
                        state = LOOKING_FOR_NEXT_ARG;
                    } else {
                        reset(processEscapeCommand(options, data));
                    }
                }
                break;

            case LOOKING_FOR_OSC_COMMAND:
                buffer[pos++] = (byte) data;
                if ('0' <= data && data <= '9') {
                    startOfValue = pos - 1;
                    state = LOOKING_FOR_OSC_COMMAND_END;
                } else {
                    reset(false);
                }
                break;

            case LOOKING_FOR_OSC_COMMAND_END:
                buffer[pos++] = (byte) data;
                if (';' == data) {
                    String strValue = new String(buffer, startOfValue, (pos - 1) - startOfValue, "UTF-8");
                    Integer value = new Integer(strValue);
                    options.add(value);
                    startOfValue = pos;
                    state = LOOKING_FOR_OSC_PARAM;
                } else if ('0' <= data && data <= '9') {
                    // already pushed digit to buffer, just keep looking
                } else {
                    // oops, did not expect this
                    reset(false);
                }
                break;

            case LOOKING_FOR_OSC_PARAM:
                buffer[pos++] = (byte) data;
                if (BEL == data) {
                    String value = new String(buffer, startOfValue, (pos - 1) - startOfValue, "UTF-8");
                    options.add(value);
                    reset(processOperatingSystemCommand(options));
                } else if (FIRST_ESC_CHAR == data) {
                    state = LOOKING_FOR_ST;
                } else {
                    // just keep looking while adding text
                }
                break;

            case LOOKING_FOR_ST:
                buffer[pos++] = (byte) data;
                if (SECOND_ST_CHAR == data) {
                    String value = new String(buffer, startOfValue, (pos - 2) - startOfValue, "UTF-8");
                    options.add(value);
                    reset(processOperatingSystemCommand(options));
                } else {
                    state = LOOKING_FOR_OSC_PARAM;
                }
                break;
        }

        // Is it just too long?
        if (pos >= buffer.length) {
            reset(false);
        }
    }

    /**
     * Resets all state to continue with regular parsing
     *
     * @param skipBuffer if current buffer should be skipped or written to out
     * @throws IOException
     */
    private void reset(boolean skipBuffer) throws IOException {
        if (!skipBuffer) {
            out.write(buffer, 0, pos);
        }
        pos = 0;
        startOfValue = 0;
        options.clear();
        state = LOOKING_FOR_FIRST_ESC_CHAR;
    }

    /**
     * @return true if the escape command was processed.
     */
    private boolean processEscapeCommand(ArrayList<Object> options, int command) throws IOException {
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
                    for (Object next : options) {
                        if (next != null) {
                            count++;
                            int value = (Integer) next;
                            if (30 <= value && value <= 37) {
                                processSetForegroundColor(value - 30);
                            } else if (40 <= value && value <= 47) {
                                processSetBackgroundColor(value - 40);
                            } else {
                                switch (value) {
                                    case 39:
                                        processDefaultTextColor();
                                        break;
                                    case 49:
                                        processDefaultBackgroundColor();
                                        break;
                                    case 0:
                                        processAttributeRest();
                                        break;
                                    default:
                                        processSetAttribute(value);
                                }
                            }
                        }
                    }
                    if (count == 0) {
                        processAttributeRest();
                    }
                    return true;
                case 's':
                    processSaveCursorPosition();
                    return true;
                case 'u':
                    processRestoreCursorPosition();
                    return true;

                default:
                    if ('a' <= command && 'z' <= command) {
                        processUnknownExtension(options, command);
                        return true;
                    }
                    if ('A' <= command && 'Z' <= command) {
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
    private boolean processOperatingSystemCommand(ArrayList<Object> options) throws IOException {
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

    protected void processScrollDown(int optionInt) throws IOException {
    }

    protected void processScrollUp(int optionInt) throws IOException {
    }

    protected void processDefaultTextColor() throws IOException {
        info.wAttributes = (short)((info.wAttributes & ~0x000F ) | (originalColors & 0x000F));
        applyAttribute();
    }

    protected void processDefaultBackgroundColor() throws IOException {
        info.wAttributes = (short)((info.wAttributes & ~0x00F0 ) | (originalColors & 0x00F0));
        applyAttribute();
    }

    protected void processUnknownExtension(ArrayList<Object> options, int command) {
    }

    protected void processChangeIconNameAndWindowTitle(String label) {
        processChangeIconName(label);
        processChangeWindowTitle(label);
    }

    protected void processChangeIconName(String label) {
    }

    protected void processUnknownOperatingSystemCommand(int command, String param) {
    }

    private int optionInt(ArrayList<Object> options, int index) {
        if (options.size() <= index)
            throw new IllegalArgumentException();
        Object value = options.get(index);
        if (value == null)
            throw new IllegalArgumentException();
        if (!value.getClass().equals(Integer.class))
            throw new IllegalArgumentException();
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

    @Override
    public void close() throws IOException {
        write(REST_CODE);
        flush();
        super.close();
    }


    public WindowsAnsiOutputStream(OutputStream os) throws IOException {
        super(os);
        getConsoleInfo();
        originalColors = info.wAttributes;
    }

    private void getConsoleInfo() throws IOException {
        out.flush();
        Kernel32.INSTANCE.GetConsoleScreenBufferInfo(console, info);
        if( negative ) {
            info.wAttributes = invertAttributeColors(info.wAttributes);
        }
    }

    private void applyAttribute() throws IOException {
        out.flush();
        short attributes = info.wAttributes;
        if( negative ) {
            attributes = invertAttributeColors(attributes);
        }
        Kernel32.INSTANCE.SetConsoleTextAttribute(console, attributes);
    }

    private short invertAttributeColors(short attributes) {
        // Swap the the Foreground and Background bits.
        int fg = 0x000F & attributes;
        fg <<= 8;
        int bg = 0X00F0 * attributes;
        bg >>=8;
        attributes = (short) ((attributes & 0xFF00) | fg | bg);
        return attributes;
    }

    private void applyCursorPosition() throws IOException {
        Kernel32.INSTANCE.SetConsoleCursorPosition(console, info.dwCursorPosition);
    }

    protected void processEraseScreen(int eraseOption) throws IOException {
        getConsoleInfo();
        IntByReference written = new IntByReference();
        switch(eraseOption) {
            case ERASE_SCREEN:
                Kernel32.COORD topLeft = new Kernel32.COORD();
                topLeft.X = 0;
                topLeft.Y = info.srWindow.Top;
                int screenLength = info.srWindow.height() * info.dwSize.X;
                Kernel32.INSTANCE.FillConsoleOutputCharacter(console, ' ', screenLength, topLeft, written);
                Kernel32.INSTANCE.FillConsoleOutputAttribute(console, info.wAttributes, screenLength, topLeft, written);
                break;
            case ERASE_SCREEN_TO_BEGINING:
                Kernel32.COORD topLeft2 = new Kernel32.COORD();
                topLeft2.X = 0;
                topLeft2.Y = info.srWindow.Top;
                int lengthToCursor = (info.dwCursorPosition.Y - info.srWindow.Top) * info.dwSize.X + info.dwCursorPosition.X;
                Kernel32.INSTANCE.FillConsoleOutputCharacter(console, ' ', lengthToCursor, topLeft2, written);
                Kernel32.INSTANCE.FillConsoleOutputAttribute(console, info.wAttributes, lengthToCursor, topLeft2, written);
                break;
            case ERASE_SCREEN_TO_END:
                int lengthToEnd = (info.srWindow.Bottom - info.dwCursorPosition.Y) * info.dwSize.X +
                        (info.dwSize.X - info.dwCursorPosition.X);
                Kernel32.INSTANCE.FillConsoleOutputCharacter(console, ' ', lengthToEnd, info.dwCursorPosition, written);
                Kernel32.INSTANCE.FillConsoleOutputAttribute(console, info.wAttributes, lengthToEnd, info.dwCursorPosition, written);
        }
    }

    protected void processEraseLine(int eraseOption) throws IOException {
        getConsoleInfo();
        IntByReference written = new IntByReference();
        switch(eraseOption) {
            case ERASE_LINE:
                Kernel32.COORD leftColCurrRow = new Kernel32.COORD((short) 0, info.dwCursorPosition.Y);
                Kernel32.INSTANCE.FillConsoleOutputCharacter(console, ' ', info.dwSize.X, leftColCurrRow, written);
                Kernel32.INSTANCE.FillConsoleOutputAttribute(console, info.wAttributes, info.dwSize.X, leftColCurrRow, written);
                break;
            case ERASE_LINE_TO_BEGINING:
                Kernel32.COORD leftColCurrRow2 = new Kernel32.COORD((short) 0, info.dwCursorPosition.Y);
                Kernel32.INSTANCE.FillConsoleOutputCharacter(console, ' ', info.dwCursorPosition.X, leftColCurrRow2, written);
                Kernel32.INSTANCE.FillConsoleOutputAttribute(console, info.wAttributes, info.dwCursorPosition.X, leftColCurrRow2, written);
                break;
            case ERASE_LINE_TO_END:
                int lengthToLastCol = info.dwSize.X - info.dwCursorPosition.X;
                Kernel32.INSTANCE.FillConsoleOutputCharacter(console, ' ', lengthToLastCol, info.dwCursorPosition, written);
                Kernel32.INSTANCE.FillConsoleOutputAttribute(console, info.wAttributes, lengthToLastCol, info.dwCursorPosition, written);
        }
    }

    protected void processCursorUpLine(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.X = 0;
        info.dwCursorPosition.Y = (short) Math.max(info.srWindow.Top, info.dwCursorPosition.Y-count);
        applyCursorPosition();
    }

    protected void processCursorDownLine(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.X = 0;
        info.dwCursorPosition.Y = (short) Math.min(info.dwSize.Y, info.dwCursorPosition.Y+count);
        applyCursorPosition();
    }

    protected void processCursorLeft(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.X = (short) Math.max(0, info.dwCursorPosition.X-count);
        applyCursorPosition();
    }

    protected void processCursorRight(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.X = (short)Math.min(info.srWindow.width(), info.dwCursorPosition.X+count);
        applyCursorPosition();
    }

    protected void processCursorDown(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.Y = (short) Math.min(info.dwSize.Y, info.dwCursorPosition.Y+count);
        applyCursorPosition();
    }

    protected void processCursorUp(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.Y = (short) Math.max(info.srWindow.Top, info.dwCursorPosition.Y-count);
        applyCursorPosition();
    }

    protected void processCursorTo(int row, int col) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.Y = (short) Math.max(info.srWindow.Top, Math.min(info.dwSize.Y, info.srWindow.Top+row-1));
        info.dwCursorPosition.X = (short) Math.max(0, Math.min(info.srWindow.width(), col-1));
        applyCursorPosition();
    }

    protected void processCursorToColumn(int x) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.X = (short) Math.max(0, Math.min(info.srWindow.width(), x-1));
        applyCursorPosition();
    }

    protected void processSetForegroundColor(int color) throws IOException {
        info.wAttributes = (short)((info.wAttributes & ~0x0007 ) | ANSI_FOREGROUND_COLOR_MAP[color]);
        applyAttribute();
    }

    protected void processSetBackgroundColor(int color) throws IOException {
        info.wAttributes = (short)((info.wAttributes & ~0x0070 ) | ANSI_BACKGROUND_COLOR_MAP[color]);
        applyAttribute();
    }

    protected void processAttributeRest() throws IOException {
        info.wAttributes = (short)((info.wAttributes & ~0x00FF ) | originalColors);
        this.negative = false;
        applyAttribute();
    }

    protected void processSetAttribute(int attribute) throws IOException {
        switch(attribute) {
            case ATTRIBUTE_INTENSITY_BOLD:
                info.wAttributes = (short)(info.wAttributes | FOREGROUND_INTENSITY );
                applyAttribute();
                break;
            case ATTRIBUTE_INTENSITY_NORMAL:
                info.wAttributes = (short)(info.wAttributes & ~FOREGROUND_INTENSITY );
                applyAttribute();
                break;

            // Yeah, setting the background intensity is not underlining.. but it's best we can do
            // using the Windows console API
            case ATTRIBUTE_UNDERLINE:
                info.wAttributes = (short)(info.wAttributes | BACKGROUND_INTENSITY );
                applyAttribute();
                break;
            case ATTRIBUTE_UNDERLINE_OFF:
                info.wAttributes = (short)(info.wAttributes & ~BACKGROUND_INTENSITY );
                applyAttribute();
                break;

            case ATTRIBUTE_NEGATIVE_ON:
                negative = true;
                applyAttribute();
                break;
            case ATTRIBUTE_NEGATIVE_Off:
                negative = false;
                applyAttribute();
                break;
        }
    }

    protected void processSaveCursorPosition() throws IOException {
        getConsoleInfo();
        savedX = info.dwCursorPosition.X;
        savedY = info.dwCursorPosition.Y;
    }

    protected void processRestoreCursorPosition() throws IOException {
        // restore only if there was a save operation first
        if (savedX != -1 && savedY != -1) {
            out.flush();
            info.dwCursorPosition.X = savedX;
            info.dwCursorPosition.Y = savedY;
            applyCursorPosition();
        }
    }

    protected void processChangeWindowTitle(String label) {
        Kernel32.INSTANCE.SetConsoleTitle(label);
    }
}

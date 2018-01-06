/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jna.win;

import java.io.IOException;
import java.io.Writer;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import org.jline.utils.AnsiWriter;

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
public final class WindowsAnsiWriter extends AnsiWriter {

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

    private final static int MAX_ESCAPE_SEQUENCE_LENGTH = 100;

    private final Pointer console;

    private final Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
    private final short originalColors;

    private boolean negative;
    private short savedX = -1;
    private short savedY = -1;

    public WindowsAnsiWriter(Writer out, Pointer console) throws IOException {
        super(out);
        this.console = console;
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
        fg <<= 4;
        int bg = 0X00F0 & attributes;
        bg >>= 4;
        attributes = (short) ((attributes & 0xFF00) | fg | bg);
        return attributes;
    }

    private void applyCursorPosition() throws IOException {
        info.dwCursorPosition.X = (short) Math.max(0, Math.min(info.dwSize.X - 1, info.dwCursorPosition.X));
        info.dwCursorPosition.Y = (short) Math.max(0, Math.min(info.dwSize.Y - 1, info.dwCursorPosition.Y));
        Kernel32.INSTANCE.SetConsoleCursorPosition(console, info.dwCursorPosition);
    }

    protected void processDefaultTextColor() throws IOException {
        info.wAttributes = (short)((info.wAttributes & ~0x000F ) | (originalColors & 0x000F));
        applyAttribute();
    }

    protected void processDefaultBackgroundColor() throws IOException {
        info.wAttributes = (short)((info.wAttributes & ~0x00F0 ) | (originalColors & 0x00F0));
        applyAttribute();
    }

    protected void processEraseScreen(int eraseOption) throws IOException {
        getConsoleInfo();
        IntByReference written = new IntByReference();
        switch(eraseOption) {
            case ERASE_SCREEN:
                Kernel32.COORD topLeft = new Kernel32.COORD();
                topLeft.X = 0;
                topLeft.Y = 0;
                int screenLength = info.dwSize.Y * info.dwSize.X;
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
        info.dwCursorPosition.Y -= count;
        applyCursorPosition();
    }

    protected void processCursorDownLine(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.X = 0;
        info.dwCursorPosition.Y += count;
        applyCursorPosition();
    }

    protected void processCursorLeft(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.X -= count;
        applyCursorPosition();
    }

    protected void processCursorRight(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.X += count;
        applyCursorPosition();
    }

    protected void processCursorDown(int count) throws IOException {
        getConsoleInfo();
        int nb = Math.max(0, info.dwCursorPosition.Y + count - info.dwSize.Y + 1);
        if (nb != count) {
            info.dwCursorPosition.Y += count;
            applyCursorPosition();
        }
        if (nb > 0) {
            Kernel32.SMALL_RECT scroll = new Kernel32.SMALL_RECT(info.srWindow);
            scroll.Top = 0;
            Kernel32.COORD org = new Kernel32.COORD();
            org.X = 0;
            org.Y = (short)(- nb);
            Kernel32.CHAR_INFO info = new Kernel32.CHAR_INFO(' ', originalColors);
            Kernel32.INSTANCE.ScrollConsoleScreenBuffer(console, scroll, scroll, org, info);
        }
    }

    protected void processCursorUp(int count) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.Y -= count;
        applyCursorPosition();
    }

    protected void processCursorTo(int row, int col) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.Y = (short) (row - 1);
        info.dwCursorPosition.X = (short) (col - 1);
        applyCursorPosition();
    }

    protected void processCursorToColumn(int x) throws IOException {
        getConsoleInfo();
        info.dwCursorPosition.X = (short) (x - 1);
        applyCursorPosition();
    }

    protected void processSetForegroundColor(int color, boolean bright) throws IOException {
        info.wAttributes = (short)((info.wAttributes & ~0x0007 ) | ANSI_FOREGROUND_COLOR_MAP[color]);
        info.wAttributes = (short) ((info.wAttributes & ~FOREGROUND_INTENSITY) | (bright ? FOREGROUND_INTENSITY : 0));
        applyAttribute();
    }

    protected void processSetBackgroundColor(int color, boolean bright) throws IOException {
        info.wAttributes = (short)((info.wAttributes & ~0x0070 ) | ANSI_BACKGROUND_COLOR_MAP[color]);
        info.wAttributes = (short) ((info.wAttributes & ~BACKGROUND_INTENSITY) | (bright ? BACKGROUND_INTENSITY : 0));
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
            case ATTRIBUTE_NEGATIVE_OFF:
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

    @Override
    protected void processInsertLine(int optionInt) throws IOException {
        getConsoleInfo();
        Kernel32.SMALL_RECT scroll = new Kernel32.SMALL_RECT(info.srWindow);
        scroll.Top = info.dwCursorPosition.Y;
        Kernel32.COORD org = new Kernel32.COORD();
        org.X = 0;
        org.Y = (short)(info.dwCursorPosition.Y + optionInt);
        Kernel32.CHAR_INFO info = new Kernel32.CHAR_INFO(' ', originalColors);
        Kernel32.INSTANCE.ScrollConsoleScreenBuffer(console, scroll, scroll, org, info);
    }

    @Override
    protected void processDeleteLine(int optionInt) throws IOException {
        getConsoleInfo();
        Kernel32.SMALL_RECT scroll = new Kernel32.SMALL_RECT(info.srWindow);
        scroll.Top = info.dwCursorPosition.Y;
        Kernel32.COORD org = new Kernel32.COORD();
        org.X = 0;
        org.Y = (short)(info.dwCursorPosition.Y - optionInt);
        Kernel32.CHAR_INFO info = new Kernel32.CHAR_INFO(' ', originalColors);
        Kernel32.INSTANCE.ScrollConsoleScreenBuffer(console, scroll, scroll, org, info);
    }

    protected void processChangeWindowTitle(String label) {
        Kernel32.INSTANCE.SetConsoleTitle(label);
    }
}

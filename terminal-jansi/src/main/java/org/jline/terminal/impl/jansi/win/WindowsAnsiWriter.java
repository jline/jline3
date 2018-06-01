/*
 * Copyright (C) 2009-2018 the original author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jline.terminal.impl.jansi.win;

import org.fusesource.jansi.internal.Kernel32.*;
import org.fusesource.jansi.internal.WindowsSupport;
import org.jline.utils.AnsiWriter;
import org.jline.utils.Colors;

import java.io.IOException;
import java.io.Writer;

import static org.fusesource.jansi.internal.Kernel32.*;

/**
 * A Windows ANSI escape processor, that uses JNA to access native platform
 * API's to change the console attributes.
 *
 * @since 1.0
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 * @author Joris Kuipers
 */
public final class WindowsAnsiWriter extends AnsiWriter {

    private static final long console = GetStdHandle(STD_OUTPUT_HANDLE);

    private static final short FOREGROUND_BLACK = 0;
    private static final short FOREGROUND_YELLOW = (short) (FOREGROUND_RED | FOREGROUND_GREEN);
    private static final short FOREGROUND_MAGENTA = (short) (FOREGROUND_BLUE | FOREGROUND_RED);
    private static final short FOREGROUND_CYAN = (short) (FOREGROUND_BLUE | FOREGROUND_GREEN);
    private static final short FOREGROUND_WHITE = (short) (FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE);

    private static final short BACKGROUND_BLACK = 0;
    private static final short BACKGROUND_YELLOW = (short) (BACKGROUND_RED | BACKGROUND_GREEN);
    private static final short BACKGROUND_MAGENTA = (short) (BACKGROUND_BLUE | BACKGROUND_RED);
    private static final short BACKGROUND_CYAN = (short) (BACKGROUND_BLUE | BACKGROUND_GREEN);
    private static final short BACKGROUND_WHITE = (short) (BACKGROUND_RED | BACKGROUND_GREEN | BACKGROUND_BLUE);

    private static final short[] ANSI_FOREGROUND_COLOR_MAP = {
            FOREGROUND_BLACK,
            FOREGROUND_RED,
            FOREGROUND_GREEN,
            FOREGROUND_YELLOW,
            FOREGROUND_BLUE,
            FOREGROUND_MAGENTA,
            FOREGROUND_CYAN,
            FOREGROUND_WHITE,
    };

    private static final short[] ANSI_BACKGROUND_COLOR_MAP = {
            BACKGROUND_BLACK,
            BACKGROUND_RED,
            BACKGROUND_GREEN,
            BACKGROUND_YELLOW,
            BACKGROUND_BLUE,
            BACKGROUND_MAGENTA,
            BACKGROUND_CYAN,
            BACKGROUND_WHITE,
    };

    private final CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
    private final short originalColors;

    private boolean negative;
    private boolean bold;
    private boolean underline;
    private short savedX = -1;
    private short savedY = -1;

    public WindowsAnsiWriter(Writer out) throws IOException {
        super(out);
        getConsoleInfo();
        originalColors = info.attributes;
    }

    private void getConsoleInfo() throws IOException {
        out.flush();
        if (GetConsoleScreenBufferInfo(console, info) == 0) {
            throw new IOException("Could not get the screen info: " + WindowsSupport.getLastErrorMessage());
        }
        if (negative) {
            info.attributes = invertAttributeColors(info.attributes);
        }
    }

    private void applyAttribute() throws IOException {
        out.flush();
        short attributes = info.attributes;
        // bold is simulated by high foreground intensity
        if (bold) {
            attributes |= FOREGROUND_INTENSITY;
        }
        // underline is simulated by high foreground intensity
        if (underline) {
            attributes |= BACKGROUND_INTENSITY;
        }
        if (negative) {
            attributes = invertAttributeColors(attributes);
        }
        if (SetConsoleTextAttribute(console, attributes) == 0) {
            throw new IOException(WindowsSupport.getLastErrorMessage());
        }
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
        info.cursorPosition.x = (short) Math.max(0, Math.min(info.size.x - 1, info.cursorPosition.x));
        info.cursorPosition.y = (short) Math.max(0, Math.min(info.size.y - 1, info.cursorPosition.y));
        if (SetConsoleCursorPosition(console, info.cursorPosition.copy()) == 0) {
            throw new IOException(WindowsSupport.getLastErrorMessage());
        }
    }

    @Override
    protected void processEraseScreen(int eraseOption) throws IOException {
        getConsoleInfo();
        int[] written = new int[1];
        switch (eraseOption) {
            case ERASE_SCREEN:
                COORD topLeft = new COORD();
                topLeft.x = 0;
                topLeft.y = info.window.top;
                int screenLength = info.window.height() * info.size.x;
                FillConsoleOutputAttribute(console, originalColors, screenLength, topLeft, written);
                FillConsoleOutputCharacterW(console, ' ', screenLength, topLeft, written);
                break;
            case ERASE_SCREEN_TO_BEGINING:
                COORD topLeft2 = new COORD();
                topLeft2.x = 0;
                topLeft2.y = info.window.top;
                int lengthToCursor = (info.cursorPosition.y - info.window.top) * info.size.x
                        + info.cursorPosition.x;
                FillConsoleOutputAttribute(console, originalColors, lengthToCursor, topLeft2, written);
                FillConsoleOutputCharacterW(console, ' ', lengthToCursor, topLeft2, written);
                break;
            case ERASE_SCREEN_TO_END:
                int lengthToEnd = (info.window.bottom - info.cursorPosition.y) * info.size.x +
                        (info.size.x - info.cursorPosition.x);
                FillConsoleOutputAttribute(console, originalColors, lengthToEnd, info.cursorPosition.copy(), written);
                FillConsoleOutputCharacterW(console, ' ', lengthToEnd, info.cursorPosition.copy(), written);
                break;
            default:
                break;
        }
    }

    @Override
    protected void processEraseLine(int eraseOption) throws IOException {
        getConsoleInfo();
        int[] written = new int[1];
        switch (eraseOption) {
            case ERASE_LINE:
                COORD leftColCurrRow = info.cursorPosition.copy();
                leftColCurrRow.x = 0;
                FillConsoleOutputAttribute(console, originalColors, info.size.x, leftColCurrRow, written);
                FillConsoleOutputCharacterW(console, ' ', info.size.x, leftColCurrRow, written);
                break;
            case ERASE_LINE_TO_BEGINING:
                COORD leftColCurrRow2 = info.cursorPosition.copy();
                leftColCurrRow2.x = 0;
                FillConsoleOutputAttribute(console, originalColors, info.cursorPosition.x, leftColCurrRow2, written);
                FillConsoleOutputCharacterW(console, ' ', info.cursorPosition.x, leftColCurrRow2, written);
                break;
            case ERASE_LINE_TO_END:
                int lengthToLastCol = info.size.x - info.cursorPosition.x;
                FillConsoleOutputAttribute(console, originalColors, lengthToLastCol, info.cursorPosition.copy(), written);
                FillConsoleOutputCharacterW(console, ' ', lengthToLastCol, info.cursorPosition.copy(), written);
                break;
            default:
                break;
        }
    }

    protected void processCursorUpLine(int count) throws IOException {
        getConsoleInfo();
        info.cursorPosition.x = 0;
        info.cursorPosition.y -= count;
        applyCursorPosition();
    }

    protected void processCursorDownLine(int count) throws IOException {
        getConsoleInfo();
        info.cursorPosition.x = 0;
        info.cursorPosition.y += count;
        applyCursorPosition();
    }

    @Override
    protected void processCursorLeft(int count) throws IOException {
        getConsoleInfo();
        info.cursorPosition.x -= count;
        applyCursorPosition();
    }

    @Override
    protected void processCursorRight(int count) throws IOException {
        getConsoleInfo();
        info.cursorPosition.x += count;
        applyCursorPosition();
    }

    @Override
    protected void processCursorDown(int count) throws IOException {
        getConsoleInfo();
        int nb = Math.max(0, info.cursorPosition.y + count - info.size.y + 1);
        if (nb != count) {
            info.cursorPosition.y += count;
            applyCursorPosition();
        }
        if (nb > 0) {
            SMALL_RECT scroll = info.window.copy();
            scroll.top = 0;
            COORD org = new COORD();
            org.x = 0;
            org.y = (short)(- nb);
            CHAR_INFO info = new CHAR_INFO();
            info.unicodeChar = ' ';
            info.attributes = originalColors;
            ScrollConsoleScreenBuffer(console, scroll, scroll, org, info);
        }
    }

    @Override
    protected void processCursorUp(int count) throws IOException {
        getConsoleInfo();
        info.cursorPosition.y -= count;
        applyCursorPosition();
    }

    @Override
    protected void processCursorTo(int row, int col) throws IOException {
        getConsoleInfo();
        info.cursorPosition.y = (short) (info.window.top + row - 1);
        info.cursorPosition.x = (short) (col - 1);
        applyCursorPosition();
    }

    @Override
    protected void processCursorToColumn(int x) throws IOException {
        getConsoleInfo();
        info.cursorPosition.x = (short) (x - 1);
        applyCursorPosition();
    }

    @Override
    protected void processSetForegroundColorExt(int paletteIndex) throws IOException {
        int color = Colors.roundColor(paletteIndex, 16);
        info.attributes = (short) ((info.attributes & ~0x0007) | ANSI_FOREGROUND_COLOR_MAP[color & 0x07]);
        info.attributes = (short) ((info.attributes & ~FOREGROUND_INTENSITY) | (color >= 8 ? FOREGROUND_INTENSITY : 0));
        applyAttribute();
    }

    @Override
    protected void processSetBackgroundColorExt(int paletteIndex) throws IOException {
        int color = Colors.roundColor(paletteIndex, 16);
        info.attributes = (short) ((info.attributes & ~0x0070) | ANSI_BACKGROUND_COLOR_MAP[color & 0x07]);
        info.attributes = (short) ((info.attributes & ~BACKGROUND_INTENSITY) | (color >= 8 ? BACKGROUND_INTENSITY : 0));
        applyAttribute();
    }

    @Override
    protected void processDefaultTextColor() throws IOException {
        info.attributes = (short) ((info.attributes & ~0x000F) | (originalColors & 0xF));
        info.attributes = (short) (info.attributes & ~FOREGROUND_INTENSITY);
        applyAttribute();
    }

    @Override
    protected void processDefaultBackgroundColor() throws IOException {
        info.attributes = (short) ((info.attributes & ~0x00F0) | (originalColors & 0xF0));
        info.attributes = (short) (info.attributes & ~BACKGROUND_INTENSITY);
        applyAttribute();
    }

    @Override
    protected void processAttributeRest() throws IOException {
        info.attributes = (short) ((info.attributes & ~0x00FF) | originalColors);
        this.negative = false;
        this.bold = false;
        this.underline = false;
        applyAttribute();
    }

    @Override
    protected void processSetAttribute(int attribute) throws IOException {
        switch (attribute) {
            case ATTRIBUTE_INTENSITY_BOLD:
                bold = true;
                applyAttribute();
                break;
            case ATTRIBUTE_INTENSITY_NORMAL:
                bold = false;
                applyAttribute();
                break;

            case ATTRIBUTE_UNDERLINE:
                underline = true;
                applyAttribute();
                break;
            case ATTRIBUTE_UNDERLINE_OFF:
                underline = false;
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
            default:
                break;
        }
    }

    @Override
    protected void processSaveCursorPosition() throws IOException {
        getConsoleInfo();
        savedX = info.cursorPosition.x;
        savedY = info.cursorPosition.y;
    }

    @Override
    protected void processRestoreCursorPosition() throws IOException {
        // restore only if there was a save operation first
        if (savedX != -1 && savedY != -1) {
            out.flush();
            info.cursorPosition.x = savedX;
            info.cursorPosition.y = savedY;
            applyCursorPosition();
        }
    }

    @Override
    protected void processInsertLine(int optionInt) throws IOException {
        getConsoleInfo();
        SMALL_RECT scroll = info.window.copy();
        scroll.top = info.cursorPosition.y;
        COORD org = new COORD();
        org.x = 0;
        org.y = (short)(info.cursorPosition.y + optionInt);
        CHAR_INFO info = new CHAR_INFO();
        info.attributes = originalColors;
        info.unicodeChar = ' ';
        if (ScrollConsoleScreenBuffer(console, scroll, scroll, org, info) == 0) {
            throw new IOException(WindowsSupport.getLastErrorMessage());
        }
    }

    @Override
    protected void processDeleteLine(int optionInt) throws IOException {
        getConsoleInfo();
        SMALL_RECT scroll = info.window.copy();
        scroll.top = info.cursorPosition.y;
        COORD org = new COORD();
        org.x = 0;
        org.y = (short)(info.cursorPosition.y - optionInt);
        CHAR_INFO info = new CHAR_INFO();
        info.attributes = originalColors;
        info.unicodeChar = ' ';
        if (ScrollConsoleScreenBuffer(console, scroll, scroll, org, info) == 0) {
            throw new IOException(WindowsSupport.getLastErrorMessage());
        }
    }

    @Override
    protected void processChangeWindowTitle(String title) {
        SetConsoleTitle(title);
    }
}

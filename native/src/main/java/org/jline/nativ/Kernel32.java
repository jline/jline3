/*
 * Copyright (c) 2009-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.nativ;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Interface to access Win32 base APIs.
 */
@SuppressWarnings("unused")
public class Kernel32 {

    /**
     * Private constructor to prevent instantiation.
     */
    private Kernel32() {
        // Utility class
    }

    static {
        if (JLineNativeLoader.initialize()) {
            init();
        }
    }

    private static native void init();

    public static short FOREGROUND_BLUE;
    public static short FOREGROUND_GREEN;
    public static short FOREGROUND_RED;
    public static short FOREGROUND_INTENSITY;
    public static short BACKGROUND_BLUE;
    public static short BACKGROUND_GREEN;
    public static short BACKGROUND_RED;
    public static short BACKGROUND_INTENSITY;
    public static short COMMON_LVB_LEADING_BYTE;
    public static short COMMON_LVB_TRAILING_BYTE;
    public static short COMMON_LVB_GRID_HORIZONTAL;
    public static short COMMON_LVB_GRID_LVERTICAL;
    public static short COMMON_LVB_GRID_RVERTICAL;
    public static short COMMON_LVB_REVERSE_VIDEO;
    public static short COMMON_LVB_UNDERSCORE;
    public static int FORMAT_MESSAGE_FROM_SYSTEM;
    public static int STD_INPUT_HANDLE;
    public static int STD_OUTPUT_HANDLE;
    public static int STD_ERROR_HANDLE;
    public static long INVALID_HANDLE_VALUE;

    public static native long malloc(long size);

    public static native void free(long ptr);

    /**
     * http://msdn.microsoft.com/en-us/library/ms686311%28VS.85%29.aspx
     */
    public static class SMALL_RECT {
        /**
         * Default constructor.
         */
        public SMALL_RECT() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;

        public short left;
        public short top;
        public short right;
        public short bottom;

        public short width() {
            return (short) (right - left);
        }

        public short height() {
            return (short) (bottom - top);
        }

        public SMALL_RECT copy() {
            SMALL_RECT rc = new SMALL_RECT();
            rc.left = left;
            rc.top = top;
            rc.right = right;
            rc.bottom = bottom;
            return rc;
        }
    }

    /**
     * see http://msdn.microsoft.com/en-us/library/ms686047%28VS.85%29.aspx
     */
    public static native int SetConsoleTextAttribute(long consoleOutput, short attributes);

    public static class COORD {

        /**
         * Default constructor.
         */
        public COORD() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;

        public short x;
        public short y;

        public COORD copy() {
            COORD rc = new COORD();
            rc.x = x;
            rc.y = y;
            return rc;
        }
    }

    /**
     * http://msdn.microsoft.com/en-us/library/ms682093%28VS.85%29.aspx
     */
    public static class CONSOLE_SCREEN_BUFFER_INFO {

        /**
         * Default constructor.
         */
        public CONSOLE_SCREEN_BUFFER_INFO() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;

        public COORD size = new COORD();
        public COORD cursorPosition = new COORD();
        public short attributes;
        public SMALL_RECT window = new SMALL_RECT();
        public COORD maximumWindowSize = new COORD();

        public int windowWidth() {
            return window.width() + 1;
        }

        public int windowHeight() {
            return window.height() + 1;
        }
    }

    // DWORD WINAPI WaitForSingleObject(
    //  _In_ HANDLE hHandle,
    //  _In_ DWORD  dwMilliseconds
    // );
    public static native int WaitForSingleObject(long hHandle, int dwMilliseconds);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms724211%28VS.85%29.aspx
     */
    public static native int CloseHandle(long handle);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms679360(VS.85).aspx
     */
    public static native int GetLastError();

    public static native int FormatMessageW(
            int flags, long source, int messageId, int languageId, byte[] buffer, int size, long[] args);

    /**
     * See: http://msdn.microsoft.com/en-us/library/ms683171%28VS.85%29.aspx
     */
    public static native int GetConsoleScreenBufferInfo(
            long consoleOutput, CONSOLE_SCREEN_BUFFER_INFO consoleScreenBufferInfo);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms683231%28VS.85%29.aspx
     */
    public static native long GetStdHandle(int stdHandle);

    /**
     * http://msdn.microsoft.com/en-us/library/ms686025%28VS.85%29.aspx
     */
    public static native int SetConsoleCursorPosition(long consoleOutput, COORD cursorPosition);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms682663%28VS.85%29.aspx
     */
    public static native int FillConsoleOutputCharacterW(
            long consoleOutput, char character, int length, COORD writeCoord, int[] numberOfCharsWritten);

    /**
     * see: https://msdn.microsoft.com/en-us/library/ms682662%28VS.85%29.aspx
     */
    public static native int FillConsoleOutputAttribute(
            long consoleOutput, short attribute, int length, COORD writeCoord, int[] numberOfAttrsWritten);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms687401(v=VS.85).aspx
     */
    public static native int WriteConsoleW(
            long consoleOutput, char[] buffer, int numberOfCharsToWrite, int[] numberOfCharsWritten, long reserved);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms683167%28VS.85%29.aspx
     */
    public static native int GetConsoleMode(long handle, int[] mode);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms686033%28VS.85%29.aspx
     */
    public static native int SetConsoleMode(long handle, int mode);

    /**
     * see: http://msdn.microsoft.com/en-us/library/078sfkak(VS.80).aspx
     */
    public static native int _getch();

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms686050%28VS.85%29.aspx
     *
     * @return 0 if title was set successfully
     */
    public static native int SetConsoleTitle(String title);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms683169(v=VS.85).aspx
     *
     * @return the current output code page
     */
    public static native int GetConsoleOutputCP();

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms686036(v=VS.85).aspx
     *
     * @return non 0 if code page was set
     */
    public static native int SetConsoleOutputCP(int codePageID);

    /**
     * see: https://msdn.microsoft.com/en-us/library/windows/desktop/ms682013(v=vs.85).aspx
     */
    public static class CHAR_INFO {

        /**
         * Default constructor.
         */
        public CHAR_INFO() {
            // Default constructor
        }

        static {
            JLineNativeLoader.initialize();
            init();
        }

        private static native void init();

        public static int SIZEOF;

        public short attributes;
        public char unicodeChar;
    }

    /**
     * see: https://msdn.microsoft.com/en-us/library/windows/desktop/ms685107(v=vs.85).aspx
     */
    public static native int ScrollConsoleScreenBuffer(
            long consoleOutput,
            SMALL_RECT scrollRectangle,
            SMALL_RECT clipRectangle,
            COORD destinationOrigin,
            CHAR_INFO fill);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms684166(v=VS.85).aspx
     */
    public static class KEY_EVENT_RECORD {

        /**
         * Default constructor.
         */
        public KEY_EVENT_RECORD() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;
        public static int CAPSLOCK_ON;
        public static int NUMLOCK_ON;
        public static int SCROLLLOCK_ON;
        public static int ENHANCED_KEY;
        public static int LEFT_ALT_PRESSED;
        public static int LEFT_CTRL_PRESSED;
        public static int RIGHT_ALT_PRESSED;
        public static int RIGHT_CTRL_PRESSED;
        public static int SHIFT_PRESSED;

        public boolean keyDown;
        public short repeatCount;
        public short keyCode;
        public short scanCode;
        public char uchar;
        public int controlKeyState;

        public String toString() {
            return "KEY_EVENT_RECORD{" + "keyDown="
                    + keyDown + ", repeatCount="
                    + repeatCount + ", keyCode="
                    + keyCode + ", scanCode="
                    + scanCode + ", uchar="
                    + uchar + ", controlKeyState="
                    + controlKeyState + '}';
        }
    }

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms684239(v=VS.85).aspx
     */
    public static class MOUSE_EVENT_RECORD {

        /**
         * Default constructor.
         */
        public MOUSE_EVENT_RECORD() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;
        public static int FROM_LEFT_1ST_BUTTON_PRESSED;
        public static int FROM_LEFT_2ND_BUTTON_PRESSED;
        public static int FROM_LEFT_3RD_BUTTON_PRESSED;
        public static int FROM_LEFT_4TH_BUTTON_PRESSED;
        public static int RIGHTMOST_BUTTON_PRESSED;

        public static int CAPSLOCK_ON;
        public static int NUMLOCK_ON;
        public static int SCROLLLOCK_ON;
        public static int ENHANCED_KEY;
        public static int LEFT_ALT_PRESSED;
        public static int LEFT_CTRL_PRESSED;
        public static int RIGHT_ALT_PRESSED;
        public static int RIGHT_CTRL_PRESSED;
        public static int SHIFT_PRESSED;

        public static int DOUBLE_CLICK;
        public static int MOUSE_HWHEELED;
        public static int MOUSE_MOVED;
        public static int MOUSE_WHEELED;

        public COORD mousePosition = new COORD();
        public int buttonState;
        public int controlKeyState;
        public int eventFlags;

        public String toString() {
            return "MOUSE_EVENT_RECORD{" + "mousePosition="
                    + mousePosition + ", buttonState="
                    + buttonState + ", controlKeyState="
                    + controlKeyState + ", eventFlags="
                    + eventFlags + '}';
        }
    }

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms687093(v=VS.85).aspx
     */
    public static class WINDOW_BUFFER_SIZE_RECORD {

        /**
         * Default constructor.
         */
        public WINDOW_BUFFER_SIZE_RECORD() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;

        public COORD size = new COORD();

        public String toString() {
            return "WINDOW_BUFFER_SIZE_RECORD{size=" + size + '}';
        }
    }

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms683149(v=VS.85).aspx
     */
    public static class FOCUS_EVENT_RECORD {
        /**
         * Default constructor.
         */
        public FOCUS_EVENT_RECORD() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;
        public boolean setFocus;
    }

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms684213(v=VS.85).aspx
     */
    public static class MENU_EVENT_RECORD {
        /**
         * Default constructor.
         */
        public MENU_EVENT_RECORD() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;
        public int commandId;
    }

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms683499(v=VS.85).aspx
     */
    public static class INPUT_RECORD {

        /**
         * Default constructor.
         */
        public INPUT_RECORD() {
            // Default constructor
        }

        static {
            if (JLineNativeLoader.initialize()) {
                init();
            }
        }

        private static native void init();

        public static int SIZEOF;
        public static short KEY_EVENT;
        public static short MOUSE_EVENT;
        public static short WINDOW_BUFFER_SIZE_EVENT;
        public static short FOCUS_EVENT;
        public static short MENU_EVENT;
        public short eventType;
        public KEY_EVENT_RECORD keyEvent = new KEY_EVENT_RECORD();
        public MOUSE_EVENT_RECORD mouseEvent = new MOUSE_EVENT_RECORD();
        public WINDOW_BUFFER_SIZE_RECORD windowBufferSizeEvent = new WINDOW_BUFFER_SIZE_RECORD();
        public MENU_EVENT_RECORD menuEvent = new MENU_EVENT_RECORD();
        public FOCUS_EVENT_RECORD focusEvent = new FOCUS_EVENT_RECORD();

        public static native void memmove(INPUT_RECORD dest, long src, long size);
    }

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms684961(v=VS.85).aspx
     */
    private static native int ReadConsoleInputW(long handle, long inputRecord, int length, int[] eventsCount);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms684344(v=VS.85).aspx
     */
    private static native int PeekConsoleInputW(long handle, long inputRecord, int length, int[] eventsCount);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms683207(v=VS.85).aspx
     */
    public static native int GetNumberOfConsoleInputEvents(long handle, int[] numberOfEvents);

    /**
     * see: http://msdn.microsoft.com/en-us/library/ms683147(v=VS.85).aspx
     */
    public static native int FlushConsoleInputBuffer(long handle);

    /**
     * Return console input events.
     */
    public static INPUT_RECORD[] readConsoleInputHelper(long handle, int count, boolean peek) throws IOException {
        int[] length = new int[1];
        int res;
        long inputRecordPtr = 0;
        try {
            inputRecordPtr = malloc(INPUT_RECORD.SIZEOF * count);
            if (inputRecordPtr == 0) {
                throw new IOException("cannot allocate memory with JNI");
            }
            res = peek
                    ? PeekConsoleInputW(handle, inputRecordPtr, count, length)
                    : ReadConsoleInputW(handle, inputRecordPtr, count, length);
            if (res == 0) {
                int bufferSize = 160;
                byte[] data = new byte[bufferSize];
                FormatMessageW(FORMAT_MESSAGE_FROM_SYSTEM, 0, GetLastError(), 0, data, bufferSize, null);
                String lastErrorMessage = new String(data, StandardCharsets.UTF_16LE).trim();
                throw new IOException("ReadConsoleInputW failed: " + lastErrorMessage);
            }
            if (length[0] <= 0) {
                return new INPUT_RECORD[0];
            }
            INPUT_RECORD[] records = new INPUT_RECORD[length[0]];
            for (int i = 0; i < records.length; i++) {
                records[i] = new INPUT_RECORD();
                INPUT_RECORD.memmove(records[i], inputRecordPtr + i * INPUT_RECORD.SIZEOF, INPUT_RECORD.SIZEOF);
            }
            return records;
        } finally {
            if (inputRecordPtr != 0) {
                free(inputRecordPtr);
            }
        }
    }

    /**
     * Return console input key events (discard other events).
     *
     * @param count requested number of events
     * @return array possibly of size smaller then count
     */
    public static INPUT_RECORD[] readConsoleKeyInput(long handle, int count, boolean peek) throws IOException {
        while (true) {
            // read events until we have keyboard events, the queue could be full
            // of mouse events.
            INPUT_RECORD[] evts = readConsoleInputHelper(handle, count, peek);
            int keyEvtCount = 0;
            for (INPUT_RECORD evt : evts) {
                if (evt.eventType == INPUT_RECORD.KEY_EVENT) keyEvtCount++;
            }
            if (keyEvtCount > 0) {
                INPUT_RECORD[] res = new INPUT_RECORD[keyEvtCount];
                int i = 0;
                for (INPUT_RECORD evt : evts) {
                    if (evt.eventType == INPUT_RECORD.KEY_EVENT) {
                        res[i++] = evt;
                    }
                }
                return res;
            }
        }
    }

    public static String getLastErrorMessage() {
        int bufferSize = 160;
        byte[] data = new byte[bufferSize];
        FormatMessageW(Kernel32.FORMAT_MESSAGE_FROM_SYSTEM, 0, GetLastError(), 0, data, bufferSize, null);
        return new String(data, StandardCharsets.UTF_16LE).trim();
    }

    public static native int isatty(int fd);
}

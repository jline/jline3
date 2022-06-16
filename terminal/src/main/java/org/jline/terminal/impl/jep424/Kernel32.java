/*
 * Copyright (C) 2022 the original author(s).
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
package org.jline.terminal.impl.jep424;

import java.io.IOException;
import java.lang.foreign.Addressable;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.util.Objects;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.OfAddress;
import static java.lang.foreign.ValueLayout.OfBoolean;
import static java.lang.foreign.ValueLayout.OfByte;
import static java.lang.foreign.ValueLayout.OfChar;
import static java.lang.foreign.ValueLayout.OfDouble;
import static java.lang.foreign.ValueLayout.OfFloat;
import static java.lang.foreign.ValueLayout.OfInt;
import static java.lang.foreign.ValueLayout.OfLong;
import static java.lang.foreign.ValueLayout.OfShort;

@SuppressWarnings( {"unused", "CopyConstructorMissesField"} )
class Kernel32
{

    public static final int FORMAT_MESSAGE_FROM_SYSTEM = 0x00001000;

    public static final int INVALID_HANDLE_VALUE = -1;
    public static final int STD_INPUT_HANDLE = -10;
    public static final int STD_OUTPUT_HANDLE = -11;
    public static final int STD_ERROR_HANDLE = -12;

    public static final int ENABLE_PROCESSED_INPUT = 0x0001;
    public static final int ENABLE_LINE_INPUT = 0x0002;
    public static final int ENABLE_ECHO_INPUT = 0x0004;
    public static final int ENABLE_WINDOW_INPUT = 0x0008;
    public static final int ENABLE_MOUSE_INPUT = 0x0010;
    public static final int ENABLE_INSERT_MODE = 0x0020;
    public static final int ENABLE_QUICK_EDIT_MODE = 0x0040;
    public static final int ENABLE_EXTENDED_FLAGS = 0x0080;

    public static final int RIGHT_ALT_PRESSED = 0x0001;
    public static final int LEFT_ALT_PRESSED = 0x0002;
    public static final int RIGHT_CTRL_PRESSED = 0x0004;
    public static final int LEFT_CTRL_PRESSED = 0x0008;
    public static final int SHIFT_PRESSED = 0x0010;

    public static final int FOREGROUND_BLUE = 0x0001;
    public static final int FOREGROUND_GREEN = 0x0002;
    public static final int FOREGROUND_RED = 0x0004;
    public static final int FOREGROUND_INTENSITY = 0x0008;
    public static final int BACKGROUND_BLUE = 0x0010;
    public static final int BACKGROUND_GREEN = 0x0020;
    public static final int BACKGROUND_RED = 0x0040;
    public static final int BACKGROUND_INTENSITY = 0x0080;

    // Button state
    public static final int FROM_LEFT_1ST_BUTTON_PRESSED = 0x0001;
    public static final int RIGHTMOST_BUTTON_PRESSED = 0x0002;
    public static final int FROM_LEFT_2ND_BUTTON_PRESSED = 0x0004;
    public static final int FROM_LEFT_3RD_BUTTON_PRESSED = 0x0008;
    public static final int FROM_LEFT_4TH_BUTTON_PRESSED = 0x0010;

    // Event flags
    public static final int MOUSE_MOVED = 0x0001;
    public static final int DOUBLE_CLICK = 0x0002;
    public static final int MOUSE_WHEELED = 0x0004;
    public static final int MOUSE_HWHEELED = 0x0008;

    // Event types
    public static final short KEY_EVENT = 0x0001;
    public static final short MOUSE_EVENT = 0x0002;
    public static final short WINDOW_BUFFER_SIZE_EVENT = 0x0004;
    public static final short MENU_EVENT = 0x0008;
    public static final short FOCUS_EVENT = 0x0010;

    public static int WaitForSingleObject( Addressable hHandle, int dwMilliseconds )
    {
        var mh$ = requireNonNull( WaitForSingleObject$MH, "WaitForSingleObject" );
        try
        {
            return (int) mh$.invokeExact( hHandle, dwMilliseconds );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static MemoryAddress GetStdHandle( int nStdHandle )
    {
        var mh$ = requireNonNull( GetStdHandle$MH, "GetStdHandle" );
        try
        {
            return (MemoryAddress) mh$.invokeExact( nStdHandle );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int FormatMessageW( int dwFlags, Addressable lpSource, int dwMessageId, int dwLanguageId,
                                      Addressable lpBuffer, int nSize, Addressable Arguments )
    {
        var mh$ = requireNonNull( FormatMessageW$MH, "FormatMessageW" );
        try
        {
            return (int) mh$.invokeExact( dwFlags, lpSource, dwMessageId, dwLanguageId, lpBuffer, nSize, Arguments );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int SetConsoleTextAttribute( Addressable hConsoleOutput, short wAttributes )
    {
        var mh$ = requireNonNull( SetConsoleTextAttribute$MH, "SetConsoleTextAttribute" );
        try
        {
            return (int) mh$.invokeExact( hConsoleOutput, wAttributes );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int SetConsoleMode( Addressable hConsoleHandle, int dwMode )
    {
        var mh$ = requireNonNull( SetConsoleMode$MH, "SetConsoleMode" );
        try
        {
            return (int) mh$.invokeExact( hConsoleHandle, dwMode );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int GetConsoleMode( Addressable hConsoleHandle, Addressable lpMode )
    {
        var mh$ = requireNonNull( GetConsoleMode$MH, "GetConsoleMode" );
        try
        {
            return (int) mh$.invokeExact( hConsoleHandle, lpMode );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int SetConsoleTitleW( Addressable lpConsoleTitle )
    {
        var mh$ = requireNonNull( SetConsoleTitleW$MH, "SetConsoleTitleW" );
        try
        {
            return (int) mh$.invokeExact( lpConsoleTitle );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int SetConsoleCursorPosition( Addressable hConsoleOutput, COORD dwCursorPosition )
    {
        var mh$ = requireNonNull( SetConsoleCursorPosition$MH, "SetConsoleCursorPosition" );
        try
        {
            return (int) mh$.invokeExact( hConsoleOutput, dwCursorPosition.seg );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int FillConsoleOutputCharacterW( Addressable hConsoleOutput, char cCharacter, int nLength,
                                                   COORD dwWriteCoord, Addressable lpNumberOfCharsWritten )
    {
        var mh$ = requireNonNull( FillConsoleOutputCharacterW$MH, "FillConsoleOutputCharacterW" );
        try
        {
            return (int) mh$.invokeExact( hConsoleOutput, cCharacter, nLength, dwWriteCoord.seg, lpNumberOfCharsWritten );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int FillConsoleOutputAttribute( Addressable hConsoleOutput, short wAttribute, int nLength,
                                                  COORD dwWriteCoord, Addressable lpNumberOfAttrsWritten )
    {
        var mh$ = requireNonNull( FillConsoleOutputAttribute$MH, "FillConsoleOutputAttribute" );
        try
        {
            return (int) mh$.invokeExact( hConsoleOutput, wAttribute, nLength, dwWriteCoord.seg, lpNumberOfAttrsWritten );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int WriteConsoleW( Addressable hConsoleOutput, Addressable lpBuffer, int nNumberOfCharsToWrite,
                                     Addressable lpNumberOfCharsWritten, Addressable lpReserved )
    {
        var mh$ = requireNonNull( WriteConsoleW$MH, "WriteConsoleW" );
        try
        {
            return (int) mh$.invokeExact( hConsoleOutput, lpBuffer, nNumberOfCharsToWrite, lpNumberOfCharsWritten,
                    lpReserved );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int ReadConsoleInputW( Addressable hConsoleInput, Addressable lpBuffer, int nLength,
                                         Addressable lpNumberOfEventsRead )
    {
        var mh$ = requireNonNull( ReadConsoleInputW$MH, "ReadConsoleInputW" );
        try
        {
            return (int) mh$.invokeExact( hConsoleInput, lpBuffer, nLength, lpNumberOfEventsRead );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int PeekConsoleInputW( Addressable hConsoleInput, Addressable lpBuffer, int nLength,
                                         Addressable lpNumberOfEventsRead )
    {
        var mh$ = requireNonNull( PeekConsoleInputW$MH, "PeekConsoleInputW" );
        try
        {
            return (int) mh$.invokeExact( hConsoleInput, lpBuffer, nLength, lpNumberOfEventsRead );
        }
        catch ( Throwable ex$ )
        {
            throw new AssertionError( "should not reach here", ex$ );
        }
    }

    public static int GetConsoleScreenBufferInfo ( Addressable hConsoleOutput,  CONSOLE_SCREEN_BUFFER_INFO lpConsoleScreenBufferInfo ) {
        var mh$ = requireNonNull( GetConsoleScreenBufferInfo$MH, "GetConsoleScreenBufferInfo" );
        try {
            return (int)mh$.invokeExact(hConsoleOutput, lpConsoleScreenBufferInfo.seg);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    public static int ScrollConsoleScreenBuffer ( Addressable hConsoleOutput,  SMALL_RECT lpScrollRectangle,
                                                  SMALL_RECT lpClipRectangle,  COORD dwDestinationOrigin,
                                                  CHAR_INFO lpFill) {
        var mh$ = requireNonNull( ScrollConsoleScreenBuffer$MH, "ScrollConsoleScreenBuffer" );
        try {
            return (int)mh$.invokeExact(hConsoleOutput, lpScrollRectangle, lpClipRectangle, dwDestinationOrigin, lpFill);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }

    public static int GetLastError (Object... x0) {
        var mh$ = requireNonNull( GetLastError$MH, "GetLastError" );
        try {
            return (int)mh$.invokeExact(x0);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }


    public static INPUT_RECORD[] readConsoleInputHelper( MemoryAddress handle, int count, boolean peek ) throws IOException
    {
        try ( MemorySession session = MemorySession.openImplicit() )
        {
            MemorySegment inputRecordPtr = session.allocateArray( INPUT_RECORD.LAYOUT, count );
            MemorySegment length = session.allocate( JAVA_INT, 0 );
            int res = peek ? PeekConsoleInputW( handle, inputRecordPtr, count, length )
                           : ReadConsoleInputW( handle, inputRecordPtr, count, length );
            if ( res == 0 )
            {
                throw new IOException( "ReadConsoleInputW failed: " + getLastErrorMessage() );
            }
            int len = length.get( JAVA_INT, 0 );
            return inputRecordPtr.elements( INPUT_RECORD.LAYOUT )
                    .map( INPUT_RECORD::new )
                    .limit( len )
                    .toArray( INPUT_RECORD[]::new );
        }
    }

    public static String getLastErrorMessage()
    {
        int errorCode = GetLastError();
        return getErrorMessage( errorCode );
    }

    public static String getErrorMessage( int errorCode )
    {
        int bufferSize = 160;
        MemorySegment data = MemorySegment.allocateNative( bufferSize, MemorySession.openImplicit() );
        FormatMessageW( FORMAT_MESSAGE_FROM_SYSTEM, null, errorCode, 0, data, bufferSize, null );
        return data.getUtf8String( 0 ).trim();
    }

    static final OfBoolean C_BOOL$LAYOUT = ValueLayout.JAVA_BOOLEAN;
    static final OfByte C_CHAR$LAYOUT = ValueLayout.JAVA_BYTE;
    static final OfChar C_WCHAR$LAYOUT = ValueLayout.JAVA_CHAR.withBitAlignment( 16 );
    static final OfShort C_SHORT$LAYOUT = ValueLayout.JAVA_SHORT.withBitAlignment( 16 );
    static final OfShort C_WORD$LAYOUT = ValueLayout.JAVA_SHORT.withBitAlignment( 16 );
    static final OfInt C_DWORD$LAYOUT = ValueLayout.JAVA_INT.withBitAlignment( 32 );
    static final OfInt C_INT$LAYOUT = JAVA_INT.withBitAlignment( 32 );
    static final OfLong C_LONG$LAYOUT = ValueLayout.JAVA_LONG.withBitAlignment( 64 );
    static final OfLong C_LONG_LONG$LAYOUT = ValueLayout.JAVA_LONG.withBitAlignment( 64 );
    static final OfFloat C_FLOAT$LAYOUT = ValueLayout.JAVA_FLOAT.withBitAlignment( 32 );
    static final OfDouble C_DOUBLE$LAYOUT = ValueLayout.JAVA_DOUBLE.withBitAlignment( 64 );
    static final OfAddress C_POINTER$LAYOUT = ValueLayout.ADDRESS.withBitAlignment( 64 );

    static final MethodHandle WaitForSingleObject$MH = downcallHandle(
            "WaitForSingleObject",
            FunctionDescriptor.of( C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_INT$LAYOUT
            )
    );
    static final MethodHandle GetStdHandle$MH = downcallHandle(
            "GetStdHandle",
            FunctionDescriptor.of( C_POINTER$LAYOUT,
                    C_INT$LAYOUT
            )
    );
    static final MethodHandle FormatMessageW$MH = downcallHandle(
            "FormatMessageW",
            FunctionDescriptor.of(
                    C_INT$LAYOUT,
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_INT$LAYOUT,
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT
            )
    );
    static final MethodHandle SetConsoleTextAttribute$MH = downcallHandle(
            "SetConsoleTextAttribute",
            FunctionDescriptor.of( C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_SHORT$LAYOUT
            )
    );
    static final MethodHandle SetConsoleMode$MH = downcallHandle(
            "SetConsoleMode",
            FunctionDescriptor.of( C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_INT$LAYOUT
            )
    );
    static final MethodHandle GetConsoleMode$MH = downcallHandle(
            "GetConsoleMode",
            FunctionDescriptor.of( C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_POINTER$LAYOUT
            )
    );

    static final MethodHandle SetConsoleTitleW$MH = downcallHandle(
            "SetConsoleTitleW",
            FunctionDescriptor.of(
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT
            )
    );
    static final MethodHandle SetConsoleCursorPosition$MH = downcallHandle(
            "SetConsoleCursorPosition",
            FunctionDescriptor.of(
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    COORD.LAYOUT
            )
    );
    static final MethodHandle FillConsoleOutputCharacterW$MH = downcallHandle(
            "FillConsoleOutputCharacterW",
            FunctionDescriptor.of(
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_SHORT$LAYOUT,
                    C_INT$LAYOUT,
                    COORD.LAYOUT,
                    C_POINTER$LAYOUT
            )
    );
    static final MethodHandle FillConsoleOutputAttribute$MH = downcallHandle(
            "FillConsoleOutputAttribute",
            FunctionDescriptor.of(
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_SHORT$LAYOUT,
                    C_INT$LAYOUT,
                    COORD.LAYOUT,
                    C_POINTER$LAYOUT
            )
    );
    static final MethodHandle WriteConsoleW$MH = downcallHandle(
            "WriteConsoleW",
            FunctionDescriptor.of(
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_POINTER$LAYOUT
            )
    );

    static final MethodHandle ReadConsoleInputW$MH = downcallHandle(
            "ReadConsoleInputW",
            FunctionDescriptor.of(
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT
            )
    );
    static final MethodHandle PeekConsoleInputW$MH = downcallHandle(
            "PeekConsoleInputW",
            FunctionDescriptor.of(
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_INT$LAYOUT,
                    C_POINTER$LAYOUT
            )
    );

    static final MethodHandle GetConsoleScreenBufferInfo$MH = downcallHandle(
            "GetConsoleScreenBufferInfo",
            FunctionDescriptor.of( C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_POINTER$LAYOUT
            )
    );

    static final MethodHandle ScrollConsoleScreenBuffer$MH = downcallHandle(
            "ScrollConsoleScreenBuffer",
            FunctionDescriptor.of(C_INT$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_POINTER$LAYOUT,
                    C_POINTER$LAYOUT,
                    COORD.LAYOUT,
                    C_POINTER$LAYOUT
            )
    );
    static final MethodHandle GetLastError$MH = downcallHandle(
            "GetLastError",
            FunctionDescriptor.of(C_INT$LAYOUT)
    );

    
    public static class INPUT_RECORD
    {
        static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
                ValueLayout.JAVA_SHORT.withName( "EventType" ),
                MemoryLayout.unionLayout(
                        KEY_EVENT_RECORD.LAYOUT.withName( "KeyEvent" ),
                        MOUSE_EVENT_RECORD.LAYOUT.withName( "MouseEvent" ),
                        WINDOW_BUFFER_SIZE_RECORD.LAYOUT.withName( "WindowBufferSizeEvent" ),
                        MENU_EVENT_RECORD.LAYOUT.withName( "MenuEvent" ),
                        FOCUS_EVENT_RECORD.LAYOUT.withName( "FocusEvent" )
                ).withName( "Event" )
        );
        static final VarHandle EventType$VH = varHandle( LAYOUT, "EventType" );
        static final long Event$OFFSET = byteOffset( LAYOUT, "Event" );

        private final MemorySegment seg;

        public INPUT_RECORD()
        {
            this( MemorySegment.allocateNative( LAYOUT, MemorySession.openImplicit() ) );
        }

        INPUT_RECORD( MemorySegment seg ) {
            this.seg = seg;
        }

        public short eventType()
        {
            return (short) EventType$VH.get( seg );
        }

        public KEY_EVENT_RECORD keyEvent()
        {
            return new KEY_EVENT_RECORD( seg, Event$OFFSET );
        }

        public MOUSE_EVENT_RECORD mouseEvent()
        {
            return new MOUSE_EVENT_RECORD( seg, Event$OFFSET );
        }

        public FOCUS_EVENT_RECORD focusEvent()
        {
            return new FOCUS_EVENT_RECORD( seg, Event$OFFSET );
        }

    }

    public static class MENU_EVENT_RECORD
    {

        static final GroupLayout LAYOUT = MemoryLayout.structLayout(
                C_DWORD$LAYOUT.withName( "dwCommandId" )
        );
        static final VarHandle COMMAND_ID = varHandle( LAYOUT, "dwCommandId" );

        private final MemorySegment seg;

        public MENU_EVENT_RECORD()
        {
            this( MemorySegment.allocateNative( LAYOUT.byteSize(), MemorySession.openImplicit() ) );
        }

        MENU_EVENT_RECORD( MemorySegment seg )
        {
            this.seg = seg;
        }

        public int commandId()
        {
            return (int) MENU_EVENT_RECORD.COMMAND_ID.get( seg );
        }

        public void commandId( int commandId )
        {
            MENU_EVENT_RECORD.COMMAND_ID.set( seg, commandId );
        }

    }

    public static class FOCUS_EVENT_RECORD
    {

        static final GroupLayout LAYOUT = MemoryLayout.structLayout(
                C_BOOL$LAYOUT.withName( "bSetFocus" )
        );
        static final VarHandle SET_FOCUS = varHandle( LAYOUT, "bSetFocus" );

        private final MemorySegment seg;

        public FOCUS_EVENT_RECORD()
        {
            this( MemorySegment.allocateNative( LAYOUT.byteSize(), MemorySession.openImplicit() ) );
        }

        FOCUS_EVENT_RECORD( MemorySegment seg )
        {
            this.seg = Objects.requireNonNull( seg );
        }

        FOCUS_EVENT_RECORD( MemorySegment seg, long offset )
        {
            this.seg = Objects.requireNonNull( seg ).asSlice( offset, LAYOUT.byteSize() );
        }

        public boolean setFocus()
        {
            return (boolean) FOCUS_EVENT_RECORD.SET_FOCUS.get( seg );
        }

        public void setFocus( boolean setFocus )
        {
            FOCUS_EVENT_RECORD.SET_FOCUS.set( seg, setFocus );
        }

    }

    public static class WINDOW_BUFFER_SIZE_RECORD
    {

        static final GroupLayout LAYOUT = MemoryLayout.structLayout(
                COORD.LAYOUT.withName( "size" )
        );
        static final long SIZE_OFFSET = byteOffset( LAYOUT, "size" );

        private final MemorySegment seg;

        public WINDOW_BUFFER_SIZE_RECORD()
        {
            this( MemorySegment.allocateNative( LAYOUT.byteSize(), MemorySession.openImplicit() ) );
        }

        WINDOW_BUFFER_SIZE_RECORD( MemorySegment seg )
        {
            this.seg = seg;
        }

        public COORD size()
        {
            return new COORD( seg, SIZE_OFFSET );
        }

        public String toString()
        {
            return "WINDOW_BUFFER_SIZE_RECORD{size=" + this.size() + '}';
        }

    }

    public static class MOUSE_EVENT_RECORD
    {

        private static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
                COORD.LAYOUT.withName( "dwMousePosition" ),
                C_DWORD$LAYOUT.withName( "dwButtonState" ),
                C_DWORD$LAYOUT.withName( "dwControlKeyState" ),
                C_DWORD$LAYOUT.withName( "dwEventFlags" )
        );
        private static final long MOUSE_POSITION_OFFSET = byteOffset( LAYOUT, "dwMousePosition" );
        private static final VarHandle BUTTON_STATE = varHandle( LAYOUT, "dwButtonState" );
        private static final VarHandle CONTROL_KEY_STATE = varHandle( LAYOUT, "dwControlKeyState" );
        private static final VarHandle EVENT_FLAGS = varHandle( LAYOUT, "dwEventFlags" );

        private final MemorySegment seg;

        public MOUSE_EVENT_RECORD()
        {
            this( MemorySegment.allocateNative( LAYOUT, MemorySession.openImplicit() ) );
        }

        MOUSE_EVENT_RECORD( MemorySegment seg )
        {
            this.seg = Objects.requireNonNull( seg );
        }

        MOUSE_EVENT_RECORD( MemorySegment seg, long offset )
        {
            this.seg = Objects.requireNonNull( seg ).asSlice( offset, LAYOUT.byteSize() );
        }

        public COORD mousePosition()
        {
            return new COORD( seg, MOUSE_POSITION_OFFSET );
        }

        public int buttonState()
        {
            return (int) BUTTON_STATE.get( seg );
        }

        public int controlKeyState()
        {
            return (int) CONTROL_KEY_STATE.get( seg );
        }

        public int eventFlags()
        {
            return (int) EVENT_FLAGS.get( seg );
        }

        public String toString()
        {
            return "MOUSE_EVENT_RECORD{mousePosition=" + mousePosition() + ", buttonState=" + buttonState()
                    + ", controlKeyState=" + controlKeyState() + ", eventFlags=" + eventFlags() + '}';
        }
    }

    public static class KEY_EVENT_RECORD
    {

        static final MemoryLayout LAYOUT = MemoryLayout.structLayout(
                JAVA_INT.withName( "bKeyDown" ),
                ValueLayout.JAVA_SHORT.withName( "wRepeatCount" ),
                ValueLayout.JAVA_SHORT.withName( "wVirtualKeyCode" ),
                ValueLayout.JAVA_SHORT.withName( "wVirtualScanCode" ),
                MemoryLayout.unionLayout(
                        ValueLayout.JAVA_CHAR.withName( "UnicodeChar" ),
                        ValueLayout.JAVA_BYTE.withName( "AsciiChar" )
                ).withName( "uChar" ),
                JAVA_INT.withName( "dwControlKeyState" )
        );
        static final VarHandle bKeyDown$VH = varHandle( LAYOUT, "bKeyDown" );
        static final VarHandle wRepeatCount$VH = varHandle( LAYOUT, "wRepeatCount" );
        static final VarHandle wVirtualKeyCode$VH = varHandle( LAYOUT, "wVirtualKeyCode" );
        static final VarHandle wVirtualScanCode$VH = varHandle( LAYOUT, "wVirtualScanCode" );
        static final VarHandle UnicodeChar$VH = varHandle( LAYOUT, "uChar", "UnicodeChar" );
        static final VarHandle AsciiChar$VH = varHandle( LAYOUT, "uChar", "AsciiChar" );
        static final VarHandle dwControlKeyState$VH = varHandle( LAYOUT, "dwControlKeyState" );

        final MemorySegment seg;

        public KEY_EVENT_RECORD()
        {
            this( MemorySegment.allocateNative( LAYOUT, MemorySession.openImplicit() ) );
        }

        KEY_EVENT_RECORD( MemorySegment seg )
        {
            this.seg = seg;
        }

        KEY_EVENT_RECORD( MemorySegment seg, long offset )
        {
            this.seg = Objects.requireNonNull( seg ).asSlice( offset, LAYOUT.byteSize() );
        }

        public boolean keyDown()
        {
            return (boolean) bKeyDown$VH.get( seg );
        }

        public int repeatCount()
        {
            return (int) wRepeatCount$VH.get( seg );
        }

        public short keyCode()
        {
            return (short) wVirtualKeyCode$VH.get( seg );
        }

        public short scanCode()
        {
            return (short) wVirtualScanCode$VH.get( seg );
        }

        public char uchar()
        {
            return (char) UnicodeChar$VH.get( seg );
        }

        public int controlKeyState()
        {
            return (int) dwControlKeyState$VH.get( seg );
        }

        public String toString()
        {
            return "KEY_EVENT_RECORD{keyDown=" + this.keyDown() + ", repeatCount=" + this.repeatCount() + ", keyCode="
                    + this.keyCode() + ", scanCode=" + this.scanCode() + ", uchar=" + this.uchar()
                    + ", controlKeyState="
                    + this.controlKeyState() + '}';
        }
    }

    public static class CHAR_INFO
    {

        static final GroupLayout LAYOUT = MemoryLayout.structLayout(
                MemoryLayout.unionLayout(
                        C_WCHAR$LAYOUT.withName( "UnicodeChar" ),
                        C_CHAR$LAYOUT.withName( "AsciiChar" )
                ).withName( "Char" ),
                C_WORD$LAYOUT.withName( "Attributes" )
        );
        static final VarHandle UnicodeChar$VH = varHandle( LAYOUT, "Char", "UnicodeChar" );
        static final VarHandle Attributes$VH = varHandle( LAYOUT, "Attributes" );

        final MemorySegment seg;

        public CHAR_INFO()
        {
            this( MemorySegment.allocateNative( LAYOUT.byteSize(), MemorySession.openImplicit() ) );
        }

        public CHAR_INFO( char c, short a )
        {
            this();
            UnicodeChar$VH.set( seg, c );
            Attributes$VH.set( seg, a );
        }

        CHAR_INFO( MemorySegment seg )
        {
            this.seg = seg;
        }

        public char unicodeChar()
        {
            return (char) UnicodeChar$VH.get( seg );
        }

    }

    public static class CONSOLE_SCREEN_BUFFER_INFO
    {
        static final GroupLayout LAYOUT = MemoryLayout.structLayout(
                COORD.LAYOUT.withName( "dwSize" ),
                COORD.LAYOUT.withName( "dwCursorPosition" ),
                C_WORD$LAYOUT.withName( "wAttributes" ),
                SMALL_RECT.LAYOUT.withName( "srWindow" ),
                COORD.LAYOUT.withName( "dwMaximumWindowSize" )
        );
        static final long dwSize$OFFSET = byteOffset( LAYOUT, "dwSize" );
        static final long dwCursorPosition$OFFSET = byteOffset( LAYOUT, "dwCursorPosition" );
        static final VarHandle wAttributes$VH = varHandle( LAYOUT, "wAttributes" );
        static final long srWindow$OFFSET = byteOffset( LAYOUT, "srWindow" );

        private final MemorySegment seg;

        public CONSOLE_SCREEN_BUFFER_INFO()
        {
            this( MemorySegment.allocateNative( LAYOUT.byteSize(), MemorySession.openImplicit() ) );
        }

        CONSOLE_SCREEN_BUFFER_INFO( MemorySegment seg )
        {
            this.seg = seg;
        }

        public COORD size()
        {
            return new COORD( seg, dwSize$OFFSET );
        }

        public COORD cursorPosition()
        {
            return new COORD( seg, dwCursorPosition$OFFSET );
        }

        public short attributes()
        {
            return (short) wAttributes$VH.get( seg );
        }

        public SMALL_RECT window()
        {
            return new SMALL_RECT( seg, srWindow$OFFSET );
        }

        public int windowWidth()
        {
            return this.window().width() + 1;
        }

        public int windowHeight()
        {
            return this.window().height() + 1;
        }

        public void attributes( short attr )
        {
            wAttributes$VH.set( seg, attr );
        }
    }

    public static class COORD
    {

        static final GroupLayout LAYOUT = MemoryLayout.structLayout(
                C_SHORT$LAYOUT.withName( "x" ),
                C_SHORT$LAYOUT.withName( "y" )
        );
        static final VarHandle x$VH = varHandle( LAYOUT, "x" );
        static final VarHandle y$VH = varHandle( LAYOUT, "y" );

        private final MemorySegment seg;

        public COORD()
        {
            this( MemorySegment.allocateNative( LAYOUT, MemorySession.openImplicit() ) );
        }

        public COORD( short x, short y )
        {
            this( MemorySegment.allocateNative( LAYOUT, MemorySession.openImplicit() ) );
            x( x );
            y( y );
        }

        public COORD( COORD from )
        {
            this( MemorySegment.allocateNative( LAYOUT, MemorySession.openImplicit() )
                    .copyFrom( Objects.requireNonNull( from ).seg ) );
        }

        COORD( MemorySegment seg )
        {
            this.seg = seg;
        }

        COORD( MemorySegment seg, long offset )
        {
            this.seg = Objects.requireNonNull( seg ).asSlice( offset, LAYOUT.byteSize() );
        }

        public short x()
        {
            return (short) COORD.x$VH.get( seg );
        }

        public void x( short x )
        {
            COORD.x$VH.set( seg, x );
        }

        public short y()
        {
            return (short) COORD.y$VH.get( seg );
        }

        public void y( short y )
        {
            COORD.y$VH.set( seg, y );
        }

        public COORD copy()
        {
            return new COORD( this );
        }
    }

    public static class SMALL_RECT
    {

        static final GroupLayout LAYOUT = MemoryLayout.structLayout(
                C_SHORT$LAYOUT.withName( "Left" ),
                C_SHORT$LAYOUT.withName( "Top" ),
                C_SHORT$LAYOUT.withName( "Right" ),
                C_SHORT$LAYOUT.withName( "Bottom" )
        );
        static final VarHandle Left$VH = varHandle( LAYOUT, "Left" );
        static final VarHandle Top$VH = varHandle( LAYOUT, "Top" );
        static final VarHandle Right$VH = varHandle( LAYOUT, "Right" );
        static final VarHandle Bottom$VH = varHandle( LAYOUT, "Bottom" );

        private final MemorySegment seg;

        public SMALL_RECT()
        {
            this( MemorySegment.allocateNative( LAYOUT, MemorySession.openImplicit() ) );
        }

        public SMALL_RECT( SMALL_RECT from )
        {
            this( MemorySegment.allocateNative( LAYOUT, MemorySession.openImplicit() )
                    .copyFrom( from.seg ) );
        }

        SMALL_RECT( MemorySegment seg, long offset )
        {
            this( seg.asSlice( offset, LAYOUT.byteSize() ) );
        }

        SMALL_RECT( MemorySegment seg )
        {
            this.seg = seg;
        }

        public short left()
        {
            return (short) Left$VH.get( seg );
        }

        public short top()
        {
            return (short) Top$VH.get( seg );
        }

        public short right()
        {
            return (short) Right$VH.get( seg );
        }

        public short bottom()
        {
            return (short) Bottom$VH.get( seg );
        }

        public short width()
        {
            return (short) ( this.right() - this.left() );
        }

        public short height()
        {
            return (short) ( this.bottom() - this.top() );
        }

        public void left( short l )
        {
            Left$VH.set( seg, l );
        }

        public void top( short t )
        {
            Top$VH.set( seg, t );
        }

        public SMALL_RECT copy()
        {
            return new SMALL_RECT( this );
        }
    }

    private final static Linker LINKER = Linker.nativeLinker();

    private final static SymbolLookup SYMBOL_LOOKUP;

    static
    {
        SymbolLookup loaderLookup = SymbolLookup.loaderLookup();
        SYMBOL_LOOKUP = name -> loaderLookup.lookup( name ).or( () -> LINKER.defaultLookup().lookup( name ) );
    }

    static MethodHandle downcallHandle( String name, FunctionDescriptor fdesc )
    {
        return SYMBOL_LOOKUP.lookup( name )
                        .map( addr -> LINKER.downcallHandle( addr, fdesc ) )
                        .orElse( null );
    }

    static <T> T requireNonNull( T obj, String symbolName )
    {
        if ( obj == null )
        {
            throw new UnsatisfiedLinkError( "unresolved symbol: " + symbolName );
        }
        return obj;
    }

    static VarHandle varHandle( MemoryLayout layout, String e1 ) {
        return layout.varHandle( MemoryLayout.PathElement.groupElement( e1 ) );
    }

    static VarHandle varHandle( MemoryLayout layout, String e1, String e2 ) {
        return layout.varHandle( MemoryLayout.PathElement.groupElement( e1 ),
                                 MemoryLayout.PathElement.groupElement( e2 ) );
    }

    static long byteOffset( MemoryLayout layout, String e1 ) {
        return layout.byteOffset( MemoryLayout.PathElement.groupElement( e1 ) );
    }

}

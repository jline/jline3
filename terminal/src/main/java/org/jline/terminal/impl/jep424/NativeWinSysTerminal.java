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

import java.io.BufferedWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.Writer;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.nio.charset.Charset;
import java.util.function.IntConsumer;

import org.jline.terminal.Cursor;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractWindowsTerminal;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;

import static java.lang.foreign.ValueLayout.JAVA_INT;
import static org.jline.terminal.impl.jep424.Kernel32.*;
import static org.jline.terminal.impl.jep424.Kernel32.GetConsoleMode;
import static org.jline.terminal.impl.jep424.Kernel32.GetConsoleScreenBufferInfo;
import static org.jline.terminal.impl.jep424.Kernel32.GetStdHandle;
import static org.jline.terminal.impl.jep424.Kernel32.INPUT_RECORD;
import static org.jline.terminal.impl.jep424.Kernel32.INVALID_HANDLE_VALUE;
import static org.jline.terminal.impl.jep424.Kernel32.KEY_EVENT_RECORD;
import static org.jline.terminal.impl.jep424.Kernel32.MOUSE_EVENT_RECORD;
import static org.jline.terminal.impl.jep424.Kernel32.STD_ERROR_HANDLE;
import static org.jline.terminal.impl.jep424.Kernel32.STD_INPUT_HANDLE;
import static org.jline.terminal.impl.jep424.Kernel32.STD_OUTPUT_HANDLE;
import static org.jline.terminal.impl.jep424.Kernel32.SetConsoleMode;
import static org.jline.terminal.impl.jep424.Kernel32.WaitForSingleObject;
import static org.jline.terminal.impl.jep424.Kernel32.getLastErrorMessage;
import static org.jline.terminal.impl.jep424.Kernel32.readConsoleInputHelper;

public class NativeWinSysTerminal extends AbstractWindowsTerminal
{

    public static NativeWinSysTerminal createTerminal( String name, String type, boolean ansiPassThrough,
                                                       Charset encoding,
                                                       boolean nativeSignals, SignalHandler signalHandler,
                                                       boolean paused,
                                                       TerminalProvider.Stream consoleStream ) throws IOException
    {
        Writer writer;
        MemorySegment mode = MemorySegment.allocateNative( JAVA_INT, MemorySession.openImplicit() );
        MemoryAddress consoleIn = GetStdHandle( STD_INPUT_HANDLE );
        MemoryAddress console;
        switch ( consoleStream )
        {
            case Output:
                console = GetStdHandle( STD_OUTPUT_HANDLE );
                break;
            case Error:
                console = GetStdHandle( STD_ERROR_HANDLE );
                break;
            default:
                throw new IllegalArgumentException( "Unsupport stream for console: " + consoleStream );
        }
        if ( ansiPassThrough )
        {
            if ( type == null )
            {
                type = OSUtils.IS_CONEMU ? TYPE_WINDOWS_CONEMU : TYPE_WINDOWS;
            }
            writer = new NativeWinConsoleWriter();
        }
        else
        {
            if ( GetConsoleMode( console, mode ) == 0 )
            {
                throw new IOException( "Failed to get console mode: " + getLastErrorMessage() );
            }
            int m = mode.get( JAVA_INT, 0 );
            if ( SetConsoleMode( console,
                    m | AbstractWindowsTerminal.ENABLE_VIRTUAL_TERMINAL_PROCESSING ) != 0 )
            {
                if ( type == null )
                {
                    type = TYPE_WINDOWS_VTP;
                }
                writer = new NativeWinConsoleWriter();
            }
            else if ( OSUtils.IS_CONEMU )
            {
                if ( type == null )
                {
                    type = TYPE_WINDOWS_CONEMU;
                }
                writer = new NativeWinConsoleWriter();
            }
            else
            {
                if ( type == null )
                {
                    type = TYPE_WINDOWS;
                }
                writer = new WindowsAnsiWriter( new BufferedWriter( new NativeWinConsoleWriter() ) );
            }
        }
        if ( GetConsoleMode( consoleIn, mode ) == 0 )
        {
            throw new IOException( "Failed to get console mode: " + getLastErrorMessage() );
        }
        NativeWinSysTerminal terminal = new NativeWinSysTerminal( writer, name, type, encoding, nativeSignals,
                signalHandler, consoleIn, console );
        // Start input pump thread
        if ( !paused )
        {
            terminal.resume();
        }
        return terminal;
    }

    public static boolean isWindowsSystemStream( TerminalProvider.Stream stream )
    {
        MemoryAddress console;
        MemorySegment mode = MemorySegment.allocateNative( JAVA_INT, MemorySession.openImplicit() );
        switch ( stream )
        {
            case Input:
                console = GetStdHandle( STD_INPUT_HANDLE );
                break;
            case Output:
                console = GetStdHandle( STD_OUTPUT_HANDLE );
                break;
            case Error:
                console = GetStdHandle( STD_ERROR_HANDLE );
                break;
            default:
                return false;
        }
        return GetConsoleMode( console, mode ) != 0;
    }

    private final MemoryAddress console;
    private final MemoryAddress outputHandle;

    NativeWinSysTerminal( Writer writer, String name, String type, Charset encoding, boolean nativeSignals,
                          SignalHandler signalHandler,
                          MemoryAddress console, MemoryAddress outputHandle ) throws IOException
    {
        super( writer, name, type, encoding, nativeSignals, signalHandler );
        this.console = console;
        this.outputHandle = outputHandle;
    }

    @Override
    protected int getConsoleMode()
    {
        try (MemorySession session = MemorySession.openImplicit() )
        {
            MemorySegment mode = session.allocate( JAVA_INT );
            if ( GetConsoleMode( console, mode ) == 0 )
            {
                return -1;
            }
            return mode.get( JAVA_INT, 0 );
        }
    }

    @Override
    protected void setConsoleMode( int mode )
    {
        SetConsoleMode( console, mode );
    }

    public Size getSize()
    {
        CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
        GetConsoleScreenBufferInfo( outputHandle, info );
        return new Size( info.windowWidth(), info.windowHeight() );
    }

    @Override
    public Size getBufferSize()
    {
        CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
        GetConsoleScreenBufferInfo( outputHandle, info );
        return new Size( info.size().x(), info.size().y() );
    }

    protected boolean processConsoleInput() throws IOException
    {
        INPUT_RECORD[] events;
        if ( console != null && console.toRawLongValue() != INVALID_HANDLE_VALUE
                && WaitForSingleObject( console, 100 ) == 0 )
        {
            events = readConsoleInputHelper( console, 1, false );
        }
        else
        {
            return false;
        }

        boolean flush = false;
        for ( INPUT_RECORD event : events )
        {
            int eventType = event.eventType();
            if ( eventType == KEY_EVENT )
            {
                KEY_EVENT_RECORD keyEvent = event.keyEvent();
                processKeyEvent( keyEvent.keyDown(), keyEvent.keyCode(), keyEvent.uchar(), keyEvent.controlKeyState() );
                flush = true;
            }
            else if ( eventType == WINDOW_BUFFER_SIZE_EVENT )
            {
                raise( Signal.WINCH );
            }
            else if ( eventType == MOUSE_EVENT )
            {
                processMouseEvent( event.mouseEvent() );
                flush = true;
            }
            else if ( eventType == FOCUS_EVENT )
            {
                processFocusEvent( event.focusEvent().setFocus() );
            }
        }

        return flush;
    }

    private final char[] focus = new char[] {'\033', '[', ' '};

    private void processFocusEvent( boolean hasFocus ) throws IOException
    {
        if ( focusTracking )
        {
            focus[2] = hasFocus ? 'I' : 'O';
            slaveInputPipe.write( focus );
        }
    }

    private final char[] mouse = new char[] {'\033', '[', 'M', ' ', ' ', ' '};

    private void processMouseEvent( MOUSE_EVENT_RECORD mouseEvent ) throws IOException
    {
        int dwEventFlags = mouseEvent.eventFlags();
        int dwButtonState = mouseEvent.buttonState();
        if ( tracking == MouseTracking.Off
                || tracking == MouseTracking.Normal && dwEventFlags == MOUSE_MOVED
                || tracking == MouseTracking.Button && dwEventFlags == MOUSE_MOVED
                && dwButtonState == 0 )
        {
            return;
        }
        int cb = 0;
        dwEventFlags &= ~DOUBLE_CLICK; // Treat double-clicks as normal
        if ( dwEventFlags == MOUSE_WHEELED )
        {
            cb |= 64;
            if ( ( dwButtonState >> 16 ) < 0 )
            {
                cb |= 1;
            }
        }
        else if ( dwEventFlags == MOUSE_HWHEELED )
        {
            return;
        }
        else if ( ( dwButtonState & FROM_LEFT_1ST_BUTTON_PRESSED ) != 0 )
        {
            cb |= 0x00;
        }
        else if ( ( dwButtonState & RIGHTMOST_BUTTON_PRESSED ) != 0 )
        {
            cb |= 0x01;
        }
        else if ( ( dwButtonState & FROM_LEFT_2ND_BUTTON_PRESSED ) != 0 )
        {
            cb |= 0x02;
        }
        else
        {
            cb |= 0x03;
        }
        int cx = mouseEvent.mousePosition().x();
        int cy = mouseEvent.mousePosition().y();
        mouse[3] = (char) ( ' ' + cb );
        mouse[4] = (char) ( ' ' + cx + 1 );
        mouse[5] = (char) ( ' ' + cy + 1 );
        slaveInputPipe.write( mouse );
    }

    @Override
    public Cursor getCursorPosition( IntConsumer discarded )
    {
        CONSOLE_SCREEN_BUFFER_INFO info = new CONSOLE_SCREEN_BUFFER_INFO();
        if ( GetConsoleScreenBufferInfo( outputHandle, info ) == 0 )
        {
            throw new IOError( new IOException( "Could not get the cursor position: " + getLastErrorMessage() ) );
        }
        return new Cursor( info.cursorPosition().x(), info.cursorPosition().y() );
    }

}

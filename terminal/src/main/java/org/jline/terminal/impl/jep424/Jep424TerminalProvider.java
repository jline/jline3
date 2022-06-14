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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.GroupLayout;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryAddress;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.MemorySession;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.EnumMap;
import java.util.EnumSet;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.AbstractPty;
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;


public class Jep424TerminalProvider implements TerminalProvider
{
    @Override
    public String name()
    {
        return "jep424";
    }

    @Override
    public Terminal     sysTerminal(String name, String type, boolean ansiPassThrough, Charset encoding,
                                boolean nativeSignals, Terminal.SignalHandler signalHandler, boolean paused,
                                Stream consoleStream) throws IOException {
        if ( OSUtils.IS_WINDOWS) {
//            return NativeWinSysTerminal.createTerminal(name, type, ansiPassThrough, encoding,
//                    nativeSignals, signalHandler, paused, consoleStream);
            throw new UnsupportedOperationException();
        } else {
            Pty pty = new NativePty(-1, null, 0, FileDescriptor.in,
                    consoleStream == Stream.Output ? 1 : 2,
                    consoleStream == Stream.Output ? FileDescriptor.out : FileDescriptor.err,
                    CLibrary.ttyName(0));
            return new PosixSysTerminal(name, type, pty, encoding, nativeSignals, signalHandler);
        }
    }

    @Override
    public Terminal newTerminal(String name, String type, InputStream in, OutputStream out,
                                Charset encoding, Terminal.SignalHandler signalHandler, boolean paused,
                                Attributes attributes, Size size) throws IOException
    {
        Pty pty = CLibrary.openpty(attributes, size);
        return new PosixPtyTerminal(name, type, pty, in, out, encoding, signalHandler, paused);
    }

    @Override
    public boolean isSystemStream(Stream stream) {
        if (OSUtils.IS_WINDOWS) {
            return isWindowsSystemStream(stream);
        } else {
            return isPosixSystemStream(stream);
        }
    }

    public boolean isWindowsSystemStream(Stream stream) {
        //return NativeWinSysTerminal.isWindowsSystemStream(stream);
        throw new UnsupportedOperationException();
    }

    public boolean isPosixSystemStream(Stream stream) {
        return NativePty.isPosixSystemStream(stream);
    }

    @Override
    public String systemStreamName(Stream stream) {
        return NativePty.posixSystemStreamName(stream);
    }

    static class NativePty extends AbstractPty
    {
        private final int master;
        private final int slave;
        private final int slaveOut;
        private final String name;
        private final FileDescriptor masterFD;
        private final FileDescriptor slaveFD;
        private final FileDescriptor slaveOutFD;

        public NativePty(int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, String name) {
            this(master, masterFD, slave, slaveFD, slave, slaveFD, name);
        }

        public NativePty(int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, int slaveOut, FileDescriptor slaveOutFD, String name) {
            this.master = master;
            this.slave = slave;
            this.slaveOut = slaveOut;
            this.name = name;
            this.masterFD = masterFD;
            this.slaveFD = slaveFD;
            this.slaveOutFD = slaveOutFD;
        }

        @Override
        public void close() throws IOException {
            if (master > 0) {
                getMasterInput().close();
            }
            if (slave > 0) {
                getSlaveInput().close();
            }
        }

        public int getMaster() {
            return master;
        }

        public int getSlave() {
            return slave;
        }

        public int getSlaveOut() {
            return slaveOut;
        }

        public String getName() {
            return name;
        }

        public FileDescriptor getMasterFD() {
            return masterFD;
        }

        public FileDescriptor getSlaveFD() {
            return slaveFD;
        }

        public FileDescriptor getSlaveOutFD() {
            return slaveOutFD;
        }

        public InputStream getMasterInput() {
            return new FileInputStream(getMasterFD());
        }

        public OutputStream getMasterOutput() {
            return new FileOutputStream(getMasterFD());
        }

        protected InputStream doGetSlaveInput() {
            return new FileInputStream(getSlaveFD());
        }

        public OutputStream getSlaveOutput() {
            return new FileOutputStream(getSlaveOutFD());
        }

        @Override
        public Attributes getAttr() throws IOException {
            return CLibrary.getAttributes(slave);
        }

        @Override
        protected void doSetAttr(Attributes attr) throws IOException {
            CLibrary.setAttributes(slave, attr);
        }

        @Override
        public Size getSize() throws IOException {
            return CLibrary.getTerminalSize(slave);
        }

        @Override
        public void setSize(Size size) throws IOException {
            CLibrary.setTerminalSize(slave, size);
        }

        @Override
        public String toString() {
            return "NativePty[" + getName() + "]";
        }

        protected static FileDescriptor newDescriptor(int fd) {
            try {
                Constructor<FileDescriptor> cns = FileDescriptor.class.getDeclaredConstructor(int.class);
                cns.setAccessible(true);
                return cns.newInstance(fd);
            } catch (Throwable e) {
                throw new RuntimeException("Unable to create FileDescriptor", e);
            }
        }

        public static boolean isPosixSystemStream(TerminalProvider.Stream stream) {
            return switch ( stream ) {
                case Input -> CLibrary.isTty( 0 );
                case Output -> CLibrary.isTty( 1 );
                case Error -> CLibrary.isTty( 2 );
            };
        }

        public static String posixSystemStreamName(TerminalProvider.Stream stream) {
            return switch ( stream ) {
                case Input -> CLibrary.ttyName( 0 );
                case Output -> CLibrary.ttyName( 1 );
                case Error -> CLibrary.ttyName( 2 );
            };
        }
    }

    /*
    public static class NativeWinSysTerminal extends AbstractWindowsTerminal
    {

        private static final long consoleIn = Kernel32.GetStdHandle( STD_INPUT_HANDLE );
        private static final long consoleOut = Kernel32.GetStdHandle( STD_OUTPUT_HANDLE );
        private static final long consoleErr = Kernel32.GetStdHandle( STD_ERROR_HANDLE );

        public static NativeWinSysTerminal createTerminal( String name, String type, boolean ansiPassThrough,
                                                          Charset encoding,
                                                          boolean nativeSignals, SignalHandler signalHandler,
                                                          boolean paused,
                                                          TerminalProvider.Stream consoleStream ) throws IOException
        {
            Writer writer;
            int[] mode = new int[1];
            long console;
            switch ( consoleStream )
            {
                case Output:
                    console = consoleOut;
                    break;
                case Error:
                    console = consoleErr;
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
                writer = new JansiWinConsoleWriter();
            }
            else
            {
                if ( Kernel32.GetConsoleMode( console, mode ) == 0 )
                {
                    throw new IOException( "Failed to get console mode: " + getLastErrorMessage() );
                }
                if ( Kernel32.SetConsoleMode( console,
                        mode[0] | AbstractWindowsTerminal.ENABLE_VIRTUAL_TERMINAL_PROCESSING ) != 0 )
                {
                    if ( type == null )
                    {
                        type = TYPE_WINDOWS_VTP;
                    }
                    writer = new JansiWinConsoleWriter();
                }
                else if ( OSUtils.IS_CONEMU )
                {
                    if ( type == null )
                    {
                        type = TYPE_WINDOWS_CONEMU;
                    }
                    writer = new JansiWinConsoleWriter();
                }
                else
                {
                    if ( type == null )
                    {
                        type = TYPE_WINDOWS;
                    }
                    writer = new WindowsAnsiWriter( new BufferedWriter( new JansiWinConsoleWriter() ) );
                }
            }
            if ( Kernel32.GetConsoleMode( consoleIn, mode ) == 0 )
            {
                throw new IOException( "Failed to get console mode: " + getLastErrorMessage() );
            }
            WinSysTerminal terminal = new WinSysTerminal( writer, name, type, encoding, nativeSignals,
                    signalHandler, consoleIn, console );
            // Start input pump thread
            if ( !paused )
            {
                terminal.resume();
            }
            return terminal;
        }

        public static boolean isWindowsSystemStream( JansiSupport.Stream stream )
        {
            int[] mode = new int[1];
            long console;
            switch ( stream )
            {
                case Input:
                    console = consoleIn;
                    break;
                case Output:
                    console = consoleOut;
                    break;
                case Error:
                    console = consoleErr;
                    break;
                default:
                    return false;
            }
            return Kernel32.GetConsoleMode( console, mode ) != 0;
        }

        private long console;
        private long outputHandle;

        WinSysTerminal( Writer writer, String name, String type, Charset encoding, boolean nativeSignals,
                             SignalHandler signalHandler,
                             long console, long outputHandle ) throws IOException
        {
            super( writer, name, type, encoding, nativeSignals, signalHandler );
            this.console = console;
            this.outputHandle = outputHandle;
        }

        @Override
        protected int getConsoleMode()
        {
            int[] mode = new int[1];
            if ( Kernel32.GetConsoleMode( console, mode ) == 0 )
            {
                return -1;
            }
            return mode[0];
        }

        @Override
        protected void setConsoleMode( int mode )
        {
            Kernel32.SetConsoleMode( console, mode );
        }

        public Size getSize()
        {
            Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
            Kernel32.GetConsoleScreenBufferInfo( outputHandle, info );
            return new Size( info.windowWidth(), info.windowHeight() );
        }

        @Override
        public Size getBufferSize()
        {
            Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
            Kernel32.GetConsoleScreenBufferInfo( outputHandle, info );
            return new Size( info.size.x, info.size.y );
        }

        protected boolean processConsoleInput() throws IOException
        {
            Kernel32.INPUT_RECORD[] events;
            if ( console != Kernel32.INVALID_HANDLE_VALUE
                    && Kernel32.WaitForSingleObject( console, 100 ) == 0 )
            {
                events = readConsoleInputHelper( console, 1, false );
            }
            else
            {
                return false;
            }

            boolean flush = false;
            for ( Kernel32.INPUT_RECORD event : events )
            {
                if ( event.eventType == Kernel32.INPUT_RECORD.KEY_EVENT )
                {
                    Kernel32.KEY_EVENT_RECORD keyEvent = event.keyEvent;
                    processKeyEvent( keyEvent.keyDown, keyEvent.keyCode, keyEvent.uchar, keyEvent.controlKeyState );
                    flush = true;
                }
                else if ( event.eventType == Kernel32.INPUT_RECORD.WINDOW_BUFFER_SIZE_EVENT )
                {
                    raise( Signal.WINCH );
                }
                else if ( event.eventType == Kernel32.INPUT_RECORD.MOUSE_EVENT )
                {
                    processMouseEvent( event.mouseEvent );
                    flush = true;
                }
                else if ( event.eventType == Kernel32.INPUT_RECORD.FOCUS_EVENT )
                {
                    processFocusEvent( event.focusEvent.setFocus );
                }
            }

            return flush;
        }

        private char[] focus = new char[] {'\033', '[', ' '};

        private void processFocusEvent( boolean hasFocus ) throws IOException
        {
            if ( focusTracking )
            {
                focus[2] = hasFocus ? 'I' : 'O';
                slaveInputPipe.write( focus );
            }
        }

        private char[] mouse = new char[] {'\033', '[', 'M', ' ', ' ', ' '};

        private void processMouseEvent( Kernel32.MOUSE_EVENT_RECORD mouseEvent ) throws IOException
        {
            int dwEventFlags = mouseEvent.eventFlags;
            int dwButtonState = mouseEvent.buttonState;
            if ( tracking == MouseTracking.Off
                    || tracking == MouseTracking.Normal && dwEventFlags == Kernel32.MOUSE_EVENT_RECORD.MOUSE_MOVED
                    || tracking == MouseTracking.Button && dwEventFlags == Kernel32.MOUSE_EVENT_RECORD.MOUSE_MOVED
                    && dwButtonState == 0 )
            {
                return;
            }
            int cb = 0;
            dwEventFlags &= ~Kernel32.MOUSE_EVENT_RECORD.DOUBLE_CLICK; // Treat double-clicks as normal
            if ( dwEventFlags == Kernel32.MOUSE_EVENT_RECORD.MOUSE_WHEELED )
            {
                cb |= 64;
                if ( ( dwButtonState >> 16 ) < 0 )
                {
                    cb |= 1;
                }
            }
            else if ( dwEventFlags == Kernel32.MOUSE_EVENT_RECORD.MOUSE_HWHEELED )
            {
                return;
            }
            else if ( ( dwButtonState & Kernel32.MOUSE_EVENT_RECORD.FROM_LEFT_1ST_BUTTON_PRESSED ) != 0 )
            {
                cb |= 0x00;
            }
            else if ( ( dwButtonState & Kernel32.MOUSE_EVENT_RECORD.RIGHTMOST_BUTTON_PRESSED ) != 0 )
            {
                cb |= 0x01;
            }
            else if ( ( dwButtonState & Kernel32.MOUSE_EVENT_RECORD.FROM_LEFT_2ND_BUTTON_PRESSED ) != 0 )
            {
                cb |= 0x02;
            }
            else
            {
                cb |= 0x03;
            }
            int cx = mouseEvent.mousePosition.x;
            int cy = mouseEvent.mousePosition.y;
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
            return new Cursor( info.cursorPosition.x, info.cursorPosition.y );
        }

        public void disableScrolling()
        {
            strings.remove( InfoCmp.Capability.insert_line );
            strings.remove( InfoCmp.Capability.parm_insert_line );
            strings.remove( InfoCmp.Capability.delete_line );
            strings.remove( InfoCmp.Capability.parm_delete_line );
        }

        static String getLastErrorMessage()
        {
            int errorCode = GetLastError();
            return getErrorMessage( errorCode );
        }

        static String getErrorMessage( int errorCode )
        {
            int bufferSize = 160;
            byte[] data = new byte[bufferSize];
            FormatMessageW( FORMAT_MESSAGE_FROM_SYSTEM, 0, errorCode, 0, data, bufferSize, null );
            return new String( data, StandardCharsets.UTF_16LE ).trim();
        }
    }
     */

    static class CLibrary
    {
        // Window sizes.
        // @see <a href="http://man7.org/linux/man-pages/man4/tty_ioctl.4.html">IOCTL_TTY(2) man-page</a>
        static class winsize
        {
            static GroupLayout layout;
            static VarHandle ws_col;
            static VarHandle ws_row;
            static {
                layout = MemoryLayout.structLayout(
                        ValueLayout.JAVA_SHORT.withName("ws_row"),
                        ValueLayout.JAVA_SHORT.withName("ws_col"),
                        ValueLayout.JAVA_SHORT,
                        ValueLayout.JAVA_SHORT
                );
                ws_row = layout.varHandle(MemoryLayout.PathElement.groupElement("ws_row"));
                ws_col = layout.varHandle(MemoryLayout.PathElement.groupElement("ws_col"));
            }
            private final MemorySegment seg;
            winsize() {
                seg = MemorySegment.allocateNative( layout, MemorySession.openImplicit());
            }
            winsize(short ws_col, short ws_row) {
                this();
                ws_col(ws_col);
                ws_row(ws_row);
            }
            MemoryAddress address() {
                return seg.address();
            }
            short ws_col() {
                return (short) ws_col.get(seg.address());
            }
            void ws_col(short col) {
                ws_col.set(seg.address(), col);
            }
            short ws_row() {
                return (short) ws_row.get(seg.address());
            }
            void ws_row(short row) {
                ws_row.set(seg.address(), row);
            }
        }
        // termios structure for termios functions, describing a general terminal interface that is
        // provided to control asynchronous communications ports
        // @see <a href="http://man7.org/linux/man-pages/man3/termios.3.html">TERMIOS(3) man-page</a>
        static class termios {
            static GroupLayout layout;
            static VarHandle c_iflag;
            static VarHandle c_oflag;
            static VarHandle c_cflag;
            static VarHandle c_lflag;
            static VarHandle c_ispeed;
            static VarHandle c_ospeed;
            static {
                layout = MemoryLayout.structLayout(
                        ValueLayout.JAVA_LONG.withName("c_iflag"),
                        ValueLayout.JAVA_LONG.withName("c_oflag"),
                        ValueLayout.JAVA_LONG.withName("c_cflag"),
                        ValueLayout.JAVA_LONG.withName("c_lflag"),
                        MemoryLayout.sequenceLayout(32, ValueLayout.JAVA_BYTE).withName( "c_cc" ),
                        ValueLayout.JAVA_LONG.withName("c_ispeed"),
                        ValueLayout.JAVA_LONG.withName("c_ospeed")
                );
                c_iflag = layout.varHandle(MemoryLayout.PathElement.groupElement("c_iflag"));
                c_oflag = layout.varHandle(MemoryLayout.PathElement.groupElement("c_oflag"));
                c_cflag = layout.varHandle(MemoryLayout.PathElement.groupElement("c_cflag"));
                c_lflag = layout.varHandle(MemoryLayout.PathElement.groupElement("c_lflag"));
                c_ispeed = layout.varHandle(MemoryLayout.PathElement.groupElement("c_ispeed"));
                c_ospeed = layout.varHandle(MemoryLayout.PathElement.groupElement("c_ospeed"));
            }
            private final MemorySegment seg;
            termios() {
                seg = MemorySegment.allocateNative(layout, MemorySession.openImplicit());
            }
            termios(Attributes t) {
                this();
                // Input flags
                long c_iflag = 0;
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IGNBRK), IGNBRK, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.BRKINT), BRKINT, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IGNPAR), IGNPAR, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.PARMRK), PARMRK, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.INPCK), INPCK, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.ISTRIP), ISTRIP, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.INLCR), INLCR, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IGNCR), IGNCR, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.ICRNL), ICRNL, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IXON), IXON, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IXOFF), IXOFF, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IXANY), IXANY, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IMAXBEL), IMAXBEL, c_iflag);
                c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IUTF8), IUTF8, c_iflag);
                c_iflag(c_iflag);
                // Output flags
                long c_oflag = 0;
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OPOST), OPOST, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.ONLCR), ONLCR, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OXTABS), OXTABS, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.ONOEOT), ONOEOT, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OCRNL), OCRNL, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.ONOCR), ONOCR, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.ONLRET), ONLRET, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OFILL), OFILL, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.NLDLY), NLDLY, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.TABDLY), TABDLY, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.CRDLY), CRDLY, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.FFDLY), FFDLY, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.BSDLY), BSDLY, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.VTDLY), VTDLY, c_oflag);
                c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OFDEL), OFDEL, c_oflag);
                c_oflag(c_oflag);
                // Control flags
                long c_cflag = 0;
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CIGNORE), CIGNORE, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CS5), CS5, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CS6), CS6, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CS7), CS7, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CS8), CS8, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CSTOPB), CSTOPB, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CREAD), CREAD, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.PARENB), PARENB, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.PARODD), PARODD, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.HUPCL), HUPCL, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CLOCAL), CLOCAL, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CCTS_OFLOW), CCTS_OFLOW, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CRTS_IFLOW), CRTS_IFLOW, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CDTR_IFLOW), CDTR_IFLOW, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CDSR_OFLOW), CDSR_OFLOW, c_cflag);
                c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CCAR_OFLOW), CCAR_OFLOW, c_cflag);
                c_cflag(c_cflag);
                // Local flags
                long c_lflag = 0;
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOKE), ECHOKE, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOE), ECHOE, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOK), ECHOK, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHO), ECHO, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHONL), ECHONL, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOPRT), ECHOPRT, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOCTL), ECHOCTL, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ISIG), ISIG, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ICANON), ICANON, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ALTWERASE), ALTWERASE, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.IEXTEN), IEXTEN, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.EXTPROC), EXTPROC, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.TOSTOP), TOSTOP, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.FLUSHO), FLUSHO, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.NOKERNINFO), NOKERNINFO, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.PENDIN), PENDIN, c_lflag);
                c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.NOFLSH), NOFLSH, c_lflag);
                c_lflag(c_lflag);
                // Control chars
                byte[] c_cc = new byte[20];
                c_cc[VEOF] = (byte) t.getControlChar(Attributes.ControlChar.VEOF);
                c_cc[VEOL] = (byte) t.getControlChar(Attributes.ControlChar.VEOL);
                c_cc[VEOL2] = (byte) t.getControlChar(Attributes.ControlChar.VEOL2);
                c_cc[VERASE] = (byte) t.getControlChar(Attributes.ControlChar.VERASE);
                c_cc[VWERASE] = (byte) t.getControlChar(Attributes.ControlChar.VWERASE);
                c_cc[VKILL] = (byte) t.getControlChar(Attributes.ControlChar.VKILL);
                c_cc[VREPRINT] = (byte) t.getControlChar(Attributes.ControlChar.VREPRINT);
                c_cc[VINTR] = (byte) t.getControlChar(Attributes.ControlChar.VINTR);
                c_cc[VQUIT] = (byte) t.getControlChar(Attributes.ControlChar.VQUIT);
                c_cc[VSUSP] = (byte) t.getControlChar(Attributes.ControlChar.VSUSP);
                c_cc[VDSUSP] = (byte) t.getControlChar(Attributes.ControlChar.VDSUSP);
                c_cc[VSTART] = (byte) t.getControlChar(Attributes.ControlChar.VSTART);
                c_cc[VSTOP] = (byte) t.getControlChar(Attributes.ControlChar.VSTOP);
                c_cc[VLNEXT] = (byte) t.getControlChar(Attributes.ControlChar.VLNEXT);
                c_cc[VDISCARD] = (byte) t.getControlChar(Attributes.ControlChar.VDISCARD);
                c_cc[VMIN] = (byte) t.getControlChar(Attributes.ControlChar.VMIN);
                c_cc[VTIME] = (byte) t.getControlChar(Attributes.ControlChar.VTIME);
                c_cc[VSTATUS] = (byte) t.getControlChar(Attributes.ControlChar.VSTATUS);
                c_cc().copyFrom(MemorySegment.ofArray(c_cc));
            }
            MemoryAddress address() {
                return seg.address();
            }
            long c_iflag() {
                return (long) c_iflag.get(seg);
            }
            void c_iflag(long f) {
                c_iflag.set(seg, f);
            }
            long c_oflag() {
                return (long) c_oflag.get(seg);
            }
            void c_oflag(long f) {
                c_oflag.set(seg, f);
            }
            long c_cflag() {
                return (long) c_cflag.get(seg);
            }
            void c_cflag(long f) {
                c_cflag.set(seg, f);
            }
            long c_lflag() {
                return (long) c_lflag.get(seg);
            }
            void c_lflag(long f) {
                c_lflag.set(seg, f);
            }
            MemorySegment c_cc() {
                return seg.asSlice(32, 20);
            }
            long c_ispeed() {
                return (long) c_ispeed.get(seg);
            }
            void c_ispeed(long f) {
                c_ispeed.set(seg, f);
            }
            long c_ospeed() {
                return (long) c_ospeed.get(seg);
            }
            void c_ospeed(long f) {
                c_ospeed.set(seg, f);
            }

            private static long setFlag(boolean flag, long value, long org) {
                return flag ? org | value : org;
            }

            private static <T extends Enum<T>> void addFlag( long value, EnumSet<T> flags, T flag, int v) {
                if ((value & v) != 0) {
                    flags.add(flag);
                }
            }

            public Attributes asAttributes() {
                Attributes attr = new Attributes();
                // Input flags
                long c_iflag = c_iflag();
                EnumSet<Attributes.InputFlag> iflag = attr.getInputFlags();
                addFlag(c_iflag, iflag, Attributes.InputFlag.IGNBRK, IGNBRK);
                addFlag(c_iflag, iflag, Attributes.InputFlag.IGNBRK, IGNBRK);
                addFlag(c_iflag, iflag, Attributes.InputFlag.BRKINT, BRKINT);
                addFlag(c_iflag, iflag, Attributes.InputFlag.IGNPAR, IGNPAR);
                addFlag(c_iflag, iflag, Attributes.InputFlag.PARMRK, PARMRK);
                addFlag(c_iflag, iflag, Attributes.InputFlag.INPCK, INPCK);
                addFlag(c_iflag, iflag, Attributes.InputFlag.ISTRIP, ISTRIP);
                addFlag(c_iflag, iflag, Attributes.InputFlag.INLCR, INLCR);
                addFlag(c_iflag, iflag, Attributes.InputFlag.IGNCR, IGNCR);
                addFlag(c_iflag, iflag, Attributes.InputFlag.ICRNL, ICRNL);
                addFlag(c_iflag, iflag, Attributes.InputFlag.IXON, IXON);
                addFlag(c_iflag, iflag, Attributes.InputFlag.IXOFF, IXOFF);
                addFlag(c_iflag, iflag, Attributes.InputFlag.IXANY, IXANY);
                addFlag(c_iflag, iflag, Attributes.InputFlag.IMAXBEL, IMAXBEL);
                addFlag(c_iflag, iflag, Attributes.InputFlag.IUTF8, IUTF8);
                // Output flags
                long c_oflag = c_oflag();
                EnumSet<Attributes.OutputFlag> oflag = attr.getOutputFlags();
                addFlag(c_oflag, oflag, Attributes.OutputFlag.OPOST, OPOST);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.ONLCR, ONLCR);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.OXTABS, OXTABS);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.ONOEOT, ONOEOT);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.OCRNL, OCRNL);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.ONOCR, ONOCR);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.ONLRET, ONLRET);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.OFILL, OFILL);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.NLDLY, NLDLY);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.TABDLY, TABDLY);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.CRDLY, CRDLY);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.FFDLY, FFDLY);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.BSDLY, BSDLY);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.VTDLY, VTDLY);
                addFlag(c_oflag, oflag, Attributes.OutputFlag.OFDEL, OFDEL);
                // Control flags
                long c_cflag = c_cflag();
                EnumSet<Attributes.ControlFlag> cflag = attr.getControlFlags();
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CIGNORE, CIGNORE);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CS5, CS5);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CS6, CS6);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CS7, CS7);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CS8, CS8);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CSTOPB, CSTOPB);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CREAD, CREAD);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.PARENB, PARENB);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.PARODD, PARODD);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.HUPCL, HUPCL);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CLOCAL, CLOCAL);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CCTS_OFLOW, CCTS_OFLOW);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CRTS_IFLOW, CRTS_IFLOW);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CDSR_OFLOW, CDSR_OFLOW);
                addFlag(c_cflag, cflag, Attributes.ControlFlag.CCAR_OFLOW, CCAR_OFLOW);
                // Local flags
                long c_lflag = c_lflag();
                EnumSet<Attributes.LocalFlag> lflag = attr.getLocalFlags();
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ECHOKE, ECHOKE);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ECHOE, ECHOE);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ECHOK, ECHOK);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ECHO, ECHO);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ECHONL, ECHONL);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ECHOPRT, ECHOPRT);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ECHOCTL, ECHOCTL);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ISIG, ISIG);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ICANON, ICANON);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.ALTWERASE, ALTWERASE);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.IEXTEN, IEXTEN);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.EXTPROC, EXTPROC);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.TOSTOP, TOSTOP);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.FLUSHO, FLUSHO);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.NOKERNINFO, NOKERNINFO);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.PENDIN, PENDIN);
                addFlag(c_lflag, lflag, Attributes.LocalFlag.NOFLSH, NOFLSH);
                // Control chars
                byte[] c_cc = c_cc().toArray(ValueLayout.JAVA_BYTE);
                EnumMap<Attributes.ControlChar, Integer> cc = attr.getControlChars();
                cc.put(Attributes.ControlChar.VEOF, (int) c_cc[VEOF]);
                cc.put(Attributes.ControlChar.VEOL, (int) c_cc[VEOL]);
                cc.put(Attributes.ControlChar.VEOL2, (int) c_cc[VEOL2]);
                cc.put(Attributes.ControlChar.VERASE, (int) c_cc[VERASE]);
                cc.put(Attributes.ControlChar.VWERASE, (int) c_cc[VWERASE]);
                cc.put(Attributes.ControlChar.VKILL, (int) c_cc[VKILL]);
                cc.put(Attributes.ControlChar.VREPRINT, (int) c_cc[VREPRINT]);
                cc.put(Attributes.ControlChar.VINTR, (int) c_cc[VINTR]);
                cc.put(Attributes.ControlChar.VQUIT, (int) c_cc[VQUIT]);
                cc.put(Attributes.ControlChar.VSUSP, (int) c_cc[VSUSP]);
                cc.put(Attributes.ControlChar.VDSUSP, (int) c_cc[VDSUSP]);
                cc.put(Attributes.ControlChar.VSTART, (int) c_cc[VSTART]);
                cc.put(Attributes.ControlChar.VSTOP, (int) c_cc[VSTOP]);
                cc.put(Attributes.ControlChar.VLNEXT, (int) c_cc[VLNEXT]);
                cc.put(Attributes.ControlChar.VDISCARD, (int) c_cc[VDISCARD]);
                cc.put(Attributes.ControlChar.VMIN, (int) c_cc[VMIN]);
                cc.put(Attributes.ControlChar.VTIME, (int) c_cc[VTIME]);
                cc.put(Attributes.ControlChar.VSTATUS, (int) c_cc[VSTATUS]);
                // Return
                return attr;
            }
        }

        static MethodHandle ioctl;
        static MethodHandle isatty;
        static MethodHandle openpty;
        static MethodHandle tcsetattr;
        static MethodHandle tcgetattr;
        static MethodHandle ttyname_r;
        static {
            // methods
            Linker linker = Linker.nativeLinker();
            // https://man7.org/linux/man-pages/man2/ioctl.2.html
            ioctl = linker.downcallHandle(
                    linker.defaultLookup().lookup("ioctl").get(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
            // https://www.man7.org/linux/man-pages/man3/isatty.3.html
            isatty = linker.downcallHandle(
                    linker.defaultLookup().lookup("isatty").get(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT));
            // https://man7.org/linux/man-pages/man3/openpty.3.html
            openpty = linker.downcallHandle(
                    linker.defaultLookup().lookup("openpty").get(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            // https://man7.org/linux/man-pages/man3/tcsetattr.3p.html
            tcsetattr = linker.downcallHandle(
                    linker.defaultLookup().lookup("tcsetattr").get(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            // https://man7.org/linux/man-pages/man3/tcgetattr.3p.html
            tcgetattr = linker.downcallHandle(
                    linker.defaultLookup().lookup("tcgetattr").get(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS));
            // https://man7.org/linux/man-pages/man3/ttyname.3.html
            ttyname_r = linker.downcallHandle(
                    linker.defaultLookup().lookup( "ttyname_r" ).get(),
                    FunctionDescriptor.of( ValueLayout.JAVA_INT, ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        }

        static Size getTerminalSize(int fd) {
            try {
                winsize ws = new winsize();
                int res = (int) ioctl.invoke(fd, TIOCGWINSZ, ws.address());
                return new Size( ws.ws_col(), ws.ws_row() );
            } catch (Throwable e) {
                throw new RuntimeException("Unable to ioctl(TIOCGWINSZ)", e);
            }
        }

        static void setTerminalSize(int fd, Size size) {
            try {
                winsize ws = new winsize();
                ws.ws_row((short) size.getRows());
                ws.ws_col((short) size.getColumns());
                int res = (int) ioctl.invoke(fd, TIOCSWINSZ, ws.address());
            } catch (Throwable e) {
                throw new RuntimeException("Unable to ioctl(TIOCGWINSZ)", e);
            }
        }

        static Attributes getAttributes(int fd) {
            try {
                termios t = new termios();
                int res = (int) tcgetattr.invoke(fd, t.address());
                return t.asAttributes();
            } catch (Throwable e) {
                throw new RuntimeException("Unable to ioctl(TIOCGWINSZ)", e);
            }
        }

        static void setAttributes(int fd, Attributes attr) {
            try {
                termios t = new termios(attr);
                int res = (int) tcsetattr.invoke(fd, TCSANOW, t.address());
            } catch (Throwable e) {
                throw new RuntimeException("Unable to tcsetattr()", e);
            }
        }

        static boolean isTty(int fd) {
            try {
                return (int) isatty.invoke(fd) == 1;
            } catch (Throwable e) {
                throw new RuntimeException("Unable to call isatty", e);
            }
        }

        static String ttyName(int fd) {
            try {
                MemorySegment buf = MemorySegment.allocateNative( 64, MemorySession.openImplicit() );
                int res = (int) ttyname_r.invoke(fd, buf, buf.byteSize());
                byte[] data = buf.toArray(ValueLayout.JAVA_BYTE);
                int len = 0;
                while (data[len] != 0) {
                    len++;
                }
                return new String(data, 0, len);
            } catch (Throwable e) {
                throw new RuntimeException("Unable to call ttyname_r()", e);
            }
        }

        static Pty openpty(Attributes attr, Size size) {
            try {
                winsize ws = new winsize();
                termios t = new termios();

                int[] master = new int[1];
                int[] slave = new int[1];
                byte[] buf = new byte[64];
                int res = (int) openpty.invoke(master, slave, buf,
                        attr != null ? new termios(attr) : null,
                        size != null ? new winsize((short) size.getRows(), (short) size.getColumns()) : null);
                int len = 0;
                while (buf[len] != 0) {
                    len++;
                }
                String device = new String(buf, 0, len);
                return new NativePty(master[0], NativePty.newDescriptor(master[0]), slave[0], NativePty.newDescriptor(slave[0]), device);
            } catch (Throwable e) {
                throw new RuntimeException("Unable to call openpty()", e);
            }
        }

        // CONSTANTS

        private static int TIOCGWINSZ;
        private static int TIOCSWINSZ;

        private static int TCSANOW;
        private static int TCSADRAIN;
        private static int TCSAFLUSH;

        private static int VEOF;
        private static int VEOL;
        private static int VEOL2;
        private static int VERASE;
        private static int VWERASE;
        private static int VKILL;
        private static int VREPRINT;
        private static int VERASE2;
        private static int VINTR;
        private static int VQUIT;
        private static int VSUSP;
        private static int VDSUSP;
        private static int VSTART;
        private static int VSTOP;
        private static int VLNEXT;
        private static int VDISCARD;
        private static int VMIN;
        private static int VSWTC;
        private static int VTIME;
        private static int VSTATUS;

        private static int IGNBRK;
        private static int BRKINT;
        private static int IGNPAR;
        private static int PARMRK;
        private static int INPCK;
        private static int ISTRIP;
        private static int INLCR;
        private static int IGNCR;
        private static int ICRNL;
        private static int IUCLC;
        private static int IXON;
        private static int IXOFF;
        private static int IXANY;
        private static int IMAXBEL;
        private static int IUTF8;

        private static int OPOST;
        private static int OLCUC;
        private static int ONLCR;
        private static int OXTABS;
        private static int NLDLY;
        private static int NL0;
        private static int NL1;
        private static int TABDLY;
        private static int TAB0;
        private static int TAB1;
        private static int TAB2;
        private static int TAB3;
        private static int CRDLY;
        private static int CR0;
        private static int CR1;
        private static int CR2;
        private static int CR3;
        private static int FFDLY;
        private static int FF0;
        private static int FF1;
        private static int XTABS;
        private static int BSDLY;
        private static int BS0;
        private static int BS1;
        private static int VTDLY;
        private static int VT0;
        private static int VT1;
        private static int CBAUD;
        private static int B0;
        private static int B50;
        private static int B75;
        private static int B110;
        private static int B134;
        private static int B150;
        private static int B200;
        private static int B300;
        private static int B600;
        private static int B1200;
        private static int B1800;
        private static int B2400;
        private static int B4800;
        private static int B9600;
        private static int B19200;
        private static int B38400;
        private static int EXTA;
        private static int EXTB;
        private static int OFDEL;
        private static int ONOEOT;
        private static int OCRNL;
        private static int ONOCR;
        private static int ONLRET;
        private static int OFILL;

        private static int CIGNORE;
        private static int CSIZE;
        private static int CS5;
        private static int CS6;
        private static int CS7;
        private static int CS8;
        private static int CSTOPB;
        private static int CREAD;
        private static int PARENB;
        private static int PARODD;
        private static int HUPCL;
        private static int CLOCAL;
        private static int CCTS_OFLOW;
        private static int CRTS_IFLOW;
        private static int CDTR_IFLOW;
        private static int CDSR_OFLOW;
        private static int CCAR_OFLOW;

        private static int ECHOKE;
        private static int ECHOE;
        private static int ECHOK;
        private static int ECHO;
        private static int ECHONL;
        private static int ECHOPRT;
        private static int ECHOCTL;
        private static int ISIG;
        private static int ICANON;
        private static int XCASE;
        private static int ALTWERASE;
        private static int IEXTEN;
        private static int EXTPROC;
        private static int TOSTOP;
        private static int FLUSHO;
        private static int NOKERNINFO;
        private static int PENDIN;
        private static int NOFLSH;

        static {
            String osName = System.getProperty("os.name");
            if (osName.startsWith("Linux")) {
                String arch = System.getProperty("os.arch");
                boolean isMipsPpcOrSparc = arch.equals("mips") || arch.equals("mips64")
                        || arch.equals("mipsel") || arch.equals("mips64el")
                        || arch.startsWith("ppc") || arch.startsWith("sparc");
                TIOCGWINSZ = isMipsPpcOrSparc ? 0x40087468 : 0x00005413;
                TIOCSWINSZ = isMipsPpcOrSparc ? 0x80087467 : 0x00005414;

                TCSANOW =          0x0;
                TCSADRAIN =        0x1;
                TCSAFLUSH =        0x2;

                VINTR       = 0;
                VQUIT       = 1;
                VERASE      = 2;
                VKILL       = 3;
                VEOF        = 4;
                VTIME       = 5;
                VMIN        = 6;
                VSWTC       = 7;
                VSTART      = 8;
                VSTOP       = 9;
                VSUSP       = 10;
                VEOL        = 11;
                VREPRINT    = 12;
                VDISCARD    = 13;
                VWERASE     = 14;
                VLNEXT      = 15;
                VEOL2       = 16;

                IGNBRK =   0x0000001;
                BRKINT =   0x0000002;
                IGNPAR =   0x0000004;
                PARMRK =   0x0000008;
                INPCK =    0x0000010;
                ISTRIP =   0x0000020;
                INLCR =    0x0000040;
                IGNCR =    0x0000080;
                ICRNL =    0x0000100;
                IUCLC =    0x0000200;
                IXON =     0x0000400;
                IXANY =    0x0000800;
                IXOFF =    0x0001000;
                IMAXBEL =  0x0002000;
                IUTF8 =    0x0004000;

                OPOST =    0x0000001;
                OLCUC =    0x0000002;
                ONLCR =    0x0000004;
                OCRNL =    0x0000008;
                ONOCR =    0x0000010;
                ONLRET =   0x0000020;
                OFILL =    0x0000040;
                OFDEL =    0x0000080;
                NLDLY =    0x0000100;
                  NL0 =    0x0000000;
                  NL1 =    0x0000100;
                CRDLY =    0x0000600;
                  CR0 =    0x0000000;
                  CR1 =    0x0000200;
                  CR2 =    0x0000400;
                  CR3 =    0x0000600;
                TABDLY =   0x0001800;
                  TAB0 =   0x0000000;
                  TAB1 =   0x0000800;
                  TAB2 =   0x0001000;
                  TAB3 =   0x0001800;
                  XTABS =  0x0001800;
                BSDLY =    0x0002000;
                  BS0 =    0x0000000;
                  BS1 =    0x0002000;
                VTDLY =    0x0004000;
                  VT0 =    0x0000000;
                  VT1 =    0x0004000;
                FFDLY =    0x0008000;
                  FF0 =    0x0000000;
                  FF1 =    0x0008000;

                CBAUD =    0x000100f;
                 B0 =      0x0000000;
                 B50 =     0x0000001;
                 B75 =     0x0000002;
                 B110 =    0x0000003;
                 B134 =    0x0000004;
                 B150 =    0x0000005;
                 B200 =    0x0000006;
                 B300 =    0x0000007;
                 B600 =    0x0000008;
                 B1200 =   0x0000009;
                 B1800 =   0x000000a;
                 B2400 =   0x000000b;
                 B4800 =   0x000000c;
                 B9600 =   0x000000d;
                 B19200 =  0x000000e;
                 B38400 =  0x000000f;
                EXTA =  B19200;
                EXTB =  B38400;
                CSIZE =    0x0000030;
                  CS5 =    0x0000000;
                  CS6 =    0x0000010;
                  CS7 =    0x0000020;
                  CS8 =    0x0000030;
                CSTOPB =   0x0000040;
                CREAD =    0x0000080;
                PARENB =   0x0000100;
                PARODD =   0x0000200;
                HUPCL =    0x0000400;
                CLOCAL =   0x0000800;

                ISIG =     0x0000001;
                ICANON =   0x0000002;
                XCASE =    0x0000004;
                ECHO =     0x0000008;
                ECHOE =    0x0000010;
                ECHOK =    0x0000020;
                ECHONL =   0x0000040;
                NOFLSH =   0x0000080;
                TOSTOP =   0x0000100;
                ECHOCTL =  0x0000200;
                ECHOPRT =  0x0000400;
                ECHOKE =   0x0000800;
                FLUSHO =   0x0001000;
                PENDIN =   0x0002000;
                IEXTEN =   0x0008000;
                EXTPROC =  0x0010000;
            }
            else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
                int _TIOC = ( 'T' << 8 );
                TIOCGWINSZ = ( _TIOC | 104 );
                TIOCSWINSZ = ( _TIOC | 103 );

                TCSANOW =          0x0;
                TCSADRAIN =        0x1;
                TCSAFLUSH =        0x2;

                VINTR       = 0;
                VQUIT       = 1;
                VERASE      = 2;
                VKILL       = 3;
                VEOF        = 4;
                VTIME       = 5;
                VMIN        = 6;
                VSWTC       = 7;
                VSTART      = 8;
                VSTOP       = 9;
                VSUSP       = 10;
                VEOL        = 11;
                VREPRINT    = 12;
                VDISCARD    = 13;
                VWERASE     = 14;
                VLNEXT      = 15;
                VEOL2       = 16;

                IGNBRK =   0x0000001;
                BRKINT =   0x0000002;
                IGNPAR =   0x0000004;
                PARMRK =   0x0000010;
                INPCK =    0x0000020;
                ISTRIP =   0x0000040;
                INLCR =    0x0000100;
                IGNCR =    0x0000200;
                ICRNL =    0x0000400;
                IUCLC =    0x0001000;
                IXON =     0x0002000;
                IXANY =    0x0004000;
                IXOFF =    0x0010000;
                IMAXBEL =  0x0020000;
                IUTF8 =    0x0040000;

                OPOST =    0x0000001;
                OLCUC =    0x0000002;
                ONLCR =    0x0000004;
                OCRNL =    0x0000010;
                ONOCR =    0x0000020;
                ONLRET =   0x0000040;
                OFILL =    0x0000100;
                OFDEL =    0x0000200;
                NLDLY =    0x0000400;
                NL0 =    0x0000000;
                NL1 =    0x0000400;
                CRDLY =    0x0003000;
                CR0 =    0x0000000;
                CR1 =    0x0001000;
                CR2 =    0x0002000;
                CR3 =    0x0003000;
                TABDLY =   0x0014000;
                TAB0 =   0x0000000;
                TAB1 =   0x0004000;
                TAB2 =   0x0010000;
                TAB3 =   0x0014000;
                XTABS =  0x0014000;
                BSDLY =    0x0020000;
                BS0 =    0x0000000;
                BS1 =    0x0020000;
                VTDLY =    0x0040000;
                VT0 =    0x0000000;
                VT1 =    0x0040000;
                FFDLY =    0x0100000;
                FF0 =    0x0000000;
                FF1 =    0x0100000;

                CBAUD =    0x0010017;
                B0 =      0x0000000;
                B50 =     0x0000001;
                B75 =     0x0000002;
                B110 =    0x0000003;
                B134 =    0x0000004;
                B150 =    0x0000005;
                B200 =    0x0000006;
                B300 =    0x0000007;
                B600 =    0x0000010;
                B1200 =   0x0000011;
                B1800 =   0x0000012;
                B2400 =   0x0000013;
                B4800 =   0x0000014;
                B9600 =   0x0000015;
                B19200 =  0x0000016;
                B38400 =  0x0000017;
                EXTA =  0xB19200;
                EXTB =  0xB38400;
                CSIZE =    0x0000060;
                CS5 =    0x0000000;
                CS6 =    0x0000020;
                CS7 =    0x0000040;
                CS8 =    0x0000060;
                CSTOPB =   0x0000100;
                CREAD =    0x0000200;
                PARENB =   0x0000400;
                PARODD =   0x0001000;
                HUPCL =    0x0002000;
                CLOCAL =   0x0004000;

                ISIG =     0x0000001;
                ICANON =   0x0000002;
                XCASE =    0x0000004;
                ECHO =     0x0000010;
                ECHOE =    0x0000020;
                ECHOK =    0x0000040;
                ECHONL =   0x0000100;
                NOFLSH =   0x0000200;
                TOSTOP =   0x0000400;
                ECHOCTL =  0x0001000;
                ECHOPRT =  0x0002000;
                ECHOKE =   0x0004000;
                FLUSHO =   0x0010000;
                PENDIN =   0x0040000;
                IEXTEN =   0x0100000;
                EXTPROC =  0x0200000;
            }
            else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
                TIOCGWINSZ = 0x40087468;
                TIOCSWINSZ = 0x80087467;

                TCSANOW     = 0x00000000;

                VEOF        = 0;
                VEOL        = 1;
                VEOL2       = 2;
                VERASE      = 3;
                VWERASE     = 4;
                VKILL       = 5;
                VREPRINT    = 6;
                VINTR       = 8;
                VQUIT       = 9;
                VSUSP       = 10;
                VDSUSP      = 11;
                VSTART      = 12;
                VSTOP       = 13;
                VLNEXT      = 14;
                VDISCARD    = 15;
                VMIN        = 16;
                VTIME       = 17;
                VSTATUS     = 18;

                IGNBRK      = 0x00000001;
                BRKINT      = 0x00000002;
                IGNPAR      = 0x00000004;
                PARMRK      = 0x00000008;
                INPCK       = 0x00000010;
                ISTRIP      = 0x00000020;
                INLCR       = 0x00000040;
                IGNCR       = 0x00000080;
                ICRNL       = 0x00000100;
                IXON        = 0x00000200;
                IXOFF       = 0x00000400;
                IXANY       = 0x00000800;
                IMAXBEL     = 0x00002000;
                IUTF8       = 0x00004000;

                OPOST       = 0x00000001;
                ONLCR       = 0x00000002;
                OXTABS      = 0x00000004;
                ONOEOT      = 0x00000008;
                OCRNL       = 0x00000010;
                ONOCR       = 0x00000020;
                ONLRET      = 0x00000040;
                OFILL       = 0x00000080;
                NLDLY       = 0x00000300;
                TABDLY      = 0x00000c04;
                CRDLY       = 0x00003000;
                FFDLY       = 0x00004000;
                BSDLY       = 0x00008000;
                VTDLY       = 0x00010000;
                OFDEL       = 0x00020000;

                CIGNORE     = 0x00000001;
                CS5         = 0x00000000;
                CS6         = 0x00000100;
                CS7         = 0x00000200;
                CS8         = 0x00000300;
                CSTOPB      = 0x00000400;
                CREAD       = 0x00000800;
                PARENB      = 0x00001000;
                PARODD      = 0x00002000;
                HUPCL       = 0x00004000;
                CLOCAL      = 0x00008000;
                CCTS_OFLOW  = 0x00010000;
                CRTS_IFLOW  = 0x00020000;
                CDTR_IFLOW  = 0x00040000;
                CDSR_OFLOW  = 0x00080000;
                CCAR_OFLOW  = 0x00100000;

                ECHOKE      = 0x00000001;
                ECHOE       = 0x00000002;
                ECHOK       = 0x00000004;
                ECHO        = 0x00000008;
                ECHONL      = 0x00000010;
                ECHOPRT     = 0x00000020;
                ECHOCTL     = 0x00000040;
                ISIG        = 0x00000080;
                ICANON      = 0x00000100;
                ALTWERASE   = 0x00000200;
                IEXTEN      = 0x00000400;
                EXTPROC     = 0x00000800;
                TOSTOP      = 0x00400000;
                FLUSHO      = 0x00800000;
                NOKERNINFO  = 0x02000000;
                PENDIN      = 0x20000000;
                NOFLSH      = 0x80000000;
            }
            else if (osName.startsWith("FreeBSD")) {
                TIOCGWINSZ = 0x40087468;
                TIOCSWINSZ = 0x80087467;

                TCSANOW =          0x0;
                TCSADRAIN =        0x1;
                TCSAFLUSH =        0x2;

                VEOF        = 0;
                VEOL        = 1;
                VEOL2       = 2;
                VERASE      = 3;
                VWERASE     = 4;
                VKILL       = 5;
                VREPRINT    = 6;
                VERASE2     = 7;
                VINTR       = 8;
                VQUIT       = 9;
                VSUSP       = 10;
                VDSUSP      = 11;
                VSTART      = 12;
                VSTOP       = 13;
                VLNEXT      = 14;
                VDISCARD    = 15;
                VMIN        = 16;
                VTIME       = 17;
                VSTATUS     = 18;

                IGNBRK =   0x0000001;
                BRKINT =   0x0000002;
                IGNPAR =   0x0000004;
                PARMRK =   0x0000008;
                INPCK =    0x0000010;
                ISTRIP =   0x0000020;
                INLCR =    0x0000040;
                IGNCR =    0x0000080;
                ICRNL =    0x0000100;
                IXON =     0x0000200;
                IXOFF =    0x0000400;
                IXANY =    0x0000800;
                IMAXBEL =  0x0002000;

                OPOST =    0x0000001;
                ONLCR =    0x0000002;
                TABDLY =   0x0000004;
                TAB0 =     0x0000000;
                TAB3 =     0x0000004;
                ONOEOT =   0x0000008;
                OCRNL =    0x0000010;
                ONLRET =   0x0000040;

                CIGNORE =  0x0000001;
                CSIZE =    0x0000300;
                CS5 =      0x0000000;
                CS6 =      0x0000100;
                CS7 =      0x0000200;
                CS8 =      0x0000300;
                CSTOPB =   0x0000400;
                CREAD =    0x0000800;
                PARENB =   0x0001000;
                PARODD =   0x0002000;
                HUPCL =    0x0004000;
                CLOCAL =   0x0008000;

                ECHOKE =   0x0000001;
                ECHOE =    0x0000002;
                ECHOK =    0x0000004;
                ECHO =     0x0000008;
                ECHONL =   0x0000010;
                ECHOPRT =  0x0000020;
                ECHOCTL =  0x0000040;
                ISIG =     0x0000080;
                ICANON =   0x0000100;
                ALTWERASE = 0x000200;
                IEXTEN =   0x0000400;
                EXTPROC =  0x0000800;
                TOSTOP =   0x0400000;
                FLUSHO =   0x0800000;
                PENDIN =   0x2000000;
                NOFLSH =   0x8000000;
            }
            else {
                throw new UnsupportedOperationException();
            }
        }
    }

    static class Kernel32 {


    }

}

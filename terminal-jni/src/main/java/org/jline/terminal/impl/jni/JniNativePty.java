/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jni;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jline.nativ.CLibrary;
import org.jline.nativ.Kernel32;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractPty;
import org.jline.terminal.impl.jni.win.NativeWinSysTerminal;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;

import static org.jline.nativ.CLibrary.TCSANOW;

public abstract class JniNativePty extends AbstractPty implements Pty {

    private final int master;
    private final int slave;
    private final int slaveOut;
    private final String name;
    private final FileDescriptor masterFD;
    private final FileDescriptor slaveFD;
    private final FileDescriptor slaveOutFD;

    public JniNativePty(
            TerminalProvider provider,
            SystemStream systemStream,
            int master,
            FileDescriptor masterFD,
            int slave,
            FileDescriptor slaveFD,
            String name) {
        this(provider, systemStream, master, masterFD, slave, slaveFD, slave, slaveFD, name);
    }

    public JniNativePty(
            TerminalProvider provider,
            SystemStream systemStream,
            int master,
            FileDescriptor masterFD,
            int slave,
            FileDescriptor slaveFD,
            int slaveOut,
            FileDescriptor slaveOutFD,
            String name) {
        super(provider, systemStream);
        this.master = master;
        this.slave = slave;
        this.slaveOut = slaveOut;
        this.name = name;
        this.masterFD = masterFD;
        this.slaveFD = slaveFD;
        this.slaveOutFD = slaveOutFD;
    }

    protected static String ttyname(int fd) throws IOException {
        String name = CLibrary.ttyname(fd);
        if (name != null) {
            name = name.trim();
        }
        if (name == null || name.isEmpty()) {
            throw new IOException("Not a tty");
        }
        return name;
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
        CLibrary.Termios tios = new CLibrary.Termios();
        CLibrary.tcgetattr(slave, tios);
        return toAttributes(tios);
    }

    @Override
    protected void doSetAttr(Attributes attr) throws IOException {
        CLibrary.Termios tios = toTermios(attr);
        CLibrary.tcsetattr(slave, TCSANOW, tios);
    }

    @Override
    public Size getSize() throws IOException {
        CLibrary.WinSize sz = new CLibrary.WinSize();
        int res = CLibrary.ioctl(slave, CLibrary.TIOCGWINSZ, sz);
        if (res != 0) {
            throw new IOException("Error calling ioctl(TIOCGWINSZ): return code is " + res);
        }
        return new Size(sz.ws_col, sz.ws_row);
    }

    @Override
    public void setSize(Size size) throws IOException {
        CLibrary.WinSize sz = new CLibrary.WinSize((short) size.getRows(), (short) size.getColumns());
        int res = CLibrary.ioctl(slave, CLibrary.TIOCSWINSZ, sz);
        if (res != 0) {
            throw new IOException("Error calling ioctl(TIOCSWINSZ): return code is " + res);
        }
    }

    protected abstract CLibrary.Termios toTermios(Attributes t);

    protected abstract Attributes toAttributes(CLibrary.Termios tios);

    @Override
    public String toString() {
        return "NativePty[" + getName() + "]";
    }

    public static boolean isPosixSystemStream(SystemStream stream) {
        return CLibrary.isatty(fd(stream)) == 1;
    }

    public static String posixSystemStreamName(SystemStream systemStream) {
        return CLibrary.ttyname(fd(systemStream));
    }

    public static int systemStreamWidth(SystemStream systemStream) {
        try {
            if (OSUtils.IS_WINDOWS) {
                Kernel32.CONSOLE_SCREEN_BUFFER_INFO info = new Kernel32.CONSOLE_SCREEN_BUFFER_INFO();
                long outConsole = NativeWinSysTerminal.getConsole(systemStream);
                Kernel32.GetConsoleScreenBufferInfo(outConsole, info);
                return info.windowWidth();
            } else {
                CLibrary.WinSize sz = new CLibrary.WinSize();
                int res = CLibrary.ioctl(fd(systemStream), CLibrary.TIOCGWINSZ, sz);
                if (res != 0) {
                    throw new IOException("Error calling ioctl(TIOCGWINSZ): return code is " + res);
                }
                return sz.ws_col;
            }
        } catch (Throwable t) {
            return -1;
        }
    }

    private static int fd(SystemStream systemStream) {
        switch (systemStream) {
            case Input:
                return 0;
            case Output:
                return 1;
            case Error:
                return 2;
            default:
                return -1;
        }
    }
}

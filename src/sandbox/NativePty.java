/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;

import org.fusesource.jansi.internal.CLibrary;
import org.fusesource.jansi.internal.CLibrary.Termios;
import org.fusesource.jansi.internal.CLibrary.WinSize;

public class NativePty implements Pty {

    private final int master;
    private final int slave;
    private final String name;
    private final FileDescriptor masterFD;
    private final FileDescriptor slaveFD;

    public static NativePty current() throws IOException {
        int slave = 0;
        String name = CLibrary.ttyname(slave);
        if (name != null) {
            return new NativePty(-1, null, slave, FileDescriptor.in, name);
        }
        throw new IOException("Not a tty");
    }

    public static NativePty open(Attributes attr, Size size) throws IOException {
        int[] master = new int[1];
        int[] slave = new int[1];
        byte[] name = new byte[64];
        Termios termios;
        if (attr != null) {
            termios = new Termios();
            termios.c_iflag = attr.c_iflag;
            termios.c_oflag = attr.c_oflag;
            termios.c_cflag = attr.c_cflag;
            termios.c_lflag = attr.c_lflag;
            termios.c_cc = attr.c_cc;
        } else {
            termios = null;
        }
        verify("openpty", CLibrary.openpty(master, slave, name, termios != null ? termios : null,
                size != null ? new WinSize((short) size.getRows(), (short) size.getColumns()) : null));
        int i = 0;
        while (name[i++] != 0) ;
        return new NativePty(master[0], newDescriptor(master[0]), slave[0], newDescriptor(slave[0]), new String(name, 0, i - 1));
    }

    /*
    public static NativePty openUnix98(Attributes attr, Size size) throws IOException {
        int master = CLibrary.open("/dev/ptmx", CLibrary.O_RDWR | CLibrary.O_NOCTTY);
        verify("open", master < 0 ? 1 : 0);
        verify("granpt", CLibrary.grantpt(master));
        verify("unlockpt", CLibrary.unlockpt(master));
        String name = CLibrary.ptsname(master);
        int slave = CLibrary.open(name, CLibrary.O_RDWR);
        verify("open", slave < 0 ? 1 : 0);
        NativePty pty = new NativePty(master, slave, name);
        if (attr != null) {
            pty.setAttr(attr);
        }
        if (size != null) {
            pty.setSize(size);
        }
        return pty;
    }
    */

    protected NativePty(int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, String name) {
        this.master = master;
        this.slave = slave;
        this.name = name;
        this.masterFD = masterFD;
        this.slaveFD = slaveFD;
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

    protected int getMaster() {
        return master;
    }

    protected int getSlave() {
        return slave;
    }

    protected String getName() {
        return name;
    }

    protected FileDescriptor getMasterFD() {
        return masterFD;
    }

    protected FileDescriptor getSlaveFD() {
        return slaveFD;
    }

    public InputStream getMasterInput() {
        return new FileInputStream(getMasterFD());
    }

    public OutputStream getMasterOutput() {
        return new FileOutputStream(getMasterFD());
    }

    public InputStream getSlaveInput() {
        return new FileInputStream(getSlaveFD());
    }

    public OutputStream getSlaveOutput() {
        return new FileOutputStream(getSlaveFD());
    }

    private static FileDescriptor newDescriptor(int fd) {
        try {
            Constructor<FileDescriptor> cns = FileDescriptor.class.getDeclaredConstructor(int.class);
            cns.setAccessible(true);
            return cns.newInstance(fd);
        } catch (Exception e) {
            throw new RuntimeException("Unable to create FileDescriptor", e);
        }
    }

    public Attributes getAttr() throws IOException {
        Termios termios = new Termios();
        verify("tcgetattr", CLibrary.tcgetattr(getSlave(), termios));
        Attributes attr = new Attributes();
        attr.c_iflag = termios.c_iflag;
        attr.c_oflag = termios.c_oflag;
        attr.c_cflag = termios.c_cflag;
        attr.c_lflag = termios.c_lflag;
        attr.c_cc = termios.c_cc;
        return attr;
    }

    public void setAttr(Attributes attr) throws IOException {
        Termios termios = new Termios();
        termios.c_iflag = attr.c_iflag;
        termios.c_oflag = attr.c_oflag;
        termios.c_cflag = attr.c_cflag;
        termios.c_lflag = attr.c_lflag;
        termios.c_cc = attr.c_cc;
        verify("tcsetattr", CLibrary.tcsetattr(getSlave(), CLibrary.TCSANOW, termios));
    }

    public Size getSize() throws IOException {
        WinSize size = new WinSize();
        verify("ioctl", CLibrary.ioctl(getSlave(), CLibrary.TIOCGWINSZ, size));
        return new Size(size.ws_col, size.ws_row);
    }

    public void setSize(Size size) throws IOException {
        verify("ioctl", CLibrary.ioctl(getSlave(), CLibrary.TIOCSWINSZ,
                new WinSize((short) size.getRows(), (short) size.getColumns())));
    }

    private static void verify(String func, int res) throws IOException {
        if (res != 0) {
            throw new IOException("Error calling " + func + ", result code: " + res);
        }
    }

}

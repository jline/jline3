/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi;

import org.fusesource.jansi.internal.CLibrary;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.impl.AbstractPty;
import org.jline.terminal.spi.Pty;
import org.jline.utils.OSUtils;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;

import static org.fusesource.jansi.internal.CLibrary.TCSANOW;
import static org.jline.terminal.impl.jansi.JansiSupportImpl.JANSI_MAJOR_VERSION;
import static org.jline.terminal.impl.jansi.JansiSupportImpl.JANSI_MINOR_VERSION;
import static org.jline.utils.ExecHelper.exec;

public abstract class JansiNativePty extends AbstractPty implements Pty {

    private final int master;
    private final int slave;
    private final int slaveOut;
    private final String name;
    private final FileDescriptor masterFD;
    private final FileDescriptor slaveFD;
    private final FileDescriptor slaveOutFD;

    public JansiNativePty(int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, String name) {
        this(master, masterFD, slave, slaveFD, slave, slaveFD, name);
    }

    public JansiNativePty(int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, int slaveOut, FileDescriptor slaveOutFD, String name) {
        this.master = master;
        this.slave = slave;
        this.slaveOut = slaveOut;
        this.name = name;
        this.masterFD = masterFD;
        this.slaveFD = slaveFD;
        this.slaveOutFD = slaveOutFD;
    }

    protected static String ttyname() throws IOException {
        String name;
        if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 16) {
            name = CLibrary.ttyname(0);
        } else {
            try {
                name = exec(true, OSUtils.TTY_COMMAND);
            } catch (IOException e) {
                throw new IOException("Not a tty", e);
            }
        }
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
        CLibrary.ioctl(slave, CLibrary.TIOCGWINSZ, sz);
        return new Size(sz.ws_col, sz.ws_row);
    }

    @Override
    public void setSize(Size size) throws IOException {
        CLibrary.WinSize sz = new CLibrary.WinSize((short) size.getRows(), (short) size.getColumns());
        CLibrary.ioctl(slave, CLibrary.TIOCSWINSZ, sz);
    }

    protected abstract CLibrary.Termios toTermios(Attributes t);

    protected abstract Attributes toAttributes(CLibrary.Termios tios);

    @Override
    public String toString() {
        return "JansiNativePty[" + getName() + "]";
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

}

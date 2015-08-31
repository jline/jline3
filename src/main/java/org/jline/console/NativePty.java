/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.console;

import java.io.IOException;

import org.fusesource.jansi.internal.CLibrary;
import org.fusesource.jansi.internal.CLibrary.Termios;
import org.fusesource.jansi.internal.CLibrary.WinSize;

public class NativePty extends Pty {

    NativePty(int master, int slave, String name) {
        super(master, slave, name);
    }

    public static NativePty current() throws IOException {
        if (CLibrary.isatty(0) == 1) {
            return new NativePty(-1, 0, "/dev/tty");
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
        return new NativePty(master[0], slave[0], new String(name, 0, i - 1));
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

/*
 * Copyright (c) 2002-2017, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jansi.osx;

import org.fusesource.jansi.internal.CLibrary;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.impl.jansi.JansiNativePty;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;

public class OsXNativePty extends JansiNativePty {

    public static OsXNativePty current() throws IOException {
        try {
            String name = ttyname();
            return new OsXNativePty(-1, null, 0, FileDescriptor.in, 1, FileDescriptor.out, name);
        } catch (IOException e) {
            throw new IOException("Not a tty", e);
        }
    }

    public static OsXNativePty open(Attributes attr, Size size) throws IOException {
        int[] master = new int[1];
        int[] slave = new int[1];
        byte[] buf = new byte[64];
        CLibrary.openpty(master, slave, buf,
                attr != null ? termios(attr) : null,
                size != null ? new CLibrary.WinSize((short) size.getRows(), (short) size.getColumns()) : null);
        int len = 0;
        while (buf[len] != 0) {
            len++;
        }
        String name = new String(buf, 0, len);
        return new OsXNativePty(master[0], newDescriptor(master[0]), slave[0], newDescriptor(slave[0]), name);
    }

    public OsXNativePty(int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, String name) {
        super(master, masterFD, slave, slaveFD, name);
    }

    public OsXNativePty(int master, FileDescriptor masterFD, int slave, FileDescriptor slaveFD, int slaveOut, FileDescriptor slaveOutFD, String name) {
        super(master, masterFD, slave, slaveFD, slaveOut, slaveOutFD, name);
    }
    // CONSTANTS

    private static final int VEOF        = 0;
    private static final int VEOL        = 1;
    private static final int VEOL2       = 2;
    private static final int VERASE      = 3;
    private static final int VWERASE     = 4;
    private static final int VKILL       = 5;
    private static final int VREPRINT    = 6;
    private static final int VINTR       = 8;
    private static final int VQUIT       = 9;
    private static final int VSUSP       = 10;
    private static final int VDSUSP      = 11;
    private static final int VSTART      = 12;
    private static final int VSTOP       = 13;
    private static final int VLNEXT      = 14;
    private static final int VDISCARD    = 15;
    private static final int VMIN        = 16;
    private static final int VTIME       = 17;
    private static final int VSTATUS     = 18;

    private static final int IGNBRK      = 0x00000001;
    private static final int BRKINT      = 0x00000002;
    private static final int IGNPAR      = 0x00000004;
    private static final int PARMRK      = 0x00000008;
    private static final int INPCK       = 0x00000010;
    private static final int ISTRIP      = 0x00000020;
    private static final int INLCR       = 0x00000040;
    private static final int IGNCR       = 0x00000080;
    private static final int ICRNL       = 0x00000100;
    private static final int IXON        = 0x00000200;
    private static final int IXOFF       = 0x00000400;
    private static final int IXANY       = 0x00000800;
    private static final int IMAXBEL     = 0x00002000;
    private static final int IUTF8       = 0x00004000;

    private static final int OPOST       = 0x00000001;
    private static final int ONLCR       = 0x00000002;
    private static final int OXTABS      = 0x00000004;
    private static final int ONOEOT      = 0x00000008;
    private static final int OCRNL       = 0x00000010;
    private static final int ONOCR       = 0x00000020;
    private static final int ONLRET      = 0x00000040;
    private static final int OFILL       = 0x00000080;
    private static final int NLDLY       = 0x00000300;
    private static final int TABDLY      = 0x00000c04;
    private static final int CRDLY       = 0x00003000;
    private static final int FFDLY       = 0x00004000;
    private static final int BSDLY       = 0x00008000;
    private static final int VTDLY       = 0x00010000;
    private static final int OFDEL       = 0x00020000;

    private static final int CIGNORE     = 0x00000001;
    private static final int CS5         = 0x00000000;
    private static final int CS6         = 0x00000100;
    private static final int CS7         = 0x00000200;
    private static final int CS8         = 0x00000300;
    private static final int CSTOPB      = 0x00000400;
    private static final int CREAD       = 0x00000800;
    private static final int PARENB      = 0x00001000;
    private static final int PARODD      = 0x00002000;
    private static final int HUPCL       = 0x00004000;
    private static final int CLOCAL      = 0x00008000;
    private static final int CCTS_OFLOW  = 0x00010000;
    private static final int CRTS_IFLOW  = 0x00020000;
    private static final int CDTR_IFLOW  = 0x00040000;
    private static final int CDSR_OFLOW  = 0x00080000;
    private static final int CCAR_OFLOW  = 0x00100000;

    private static final int ECHOKE      = 0x00000001;
    private static final int ECHOE       = 0x00000002;
    private static final int ECHOK       = 0x00000004;
    private static final int ECHO        = 0x00000008;
    private static final int ECHONL      = 0x00000010;
    private static final int ECHOPRT     = 0x00000020;
    private static final int ECHOCTL     = 0x00000040;
    private static final int ISIG        = 0x00000080;
    private static final int ICANON      = 0x00000100;
    private static final int ALTWERASE   = 0x00000200;
    private static final int IEXTEN      = 0x00000400;
    private static final int EXTPROC     = 0x00000800;
    private static final int TOSTOP      = 0x00400000;
    private static final int FLUSHO      = 0x00800000;
    private static final int NOKERNINFO  = 0x02000000;
    private static final int PENDIN      = 0x20000000;
    private static final int NOFLSH      = 0x80000000;

    protected CLibrary.Termios toTermios(Attributes t) {
        return termios(t);
    }

    static CLibrary.Termios termios(Attributes t) {
        CLibrary.Termios tio = new CLibrary.Termios();
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IGNBRK), IGNBRK, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.BRKINT), BRKINT, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IGNPAR), IGNPAR, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.PARMRK), PARMRK, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.INPCK), INPCK, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.ISTRIP), ISTRIP, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.INLCR), INLCR, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IGNCR), IGNCR, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.ICRNL), ICRNL, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IXON), IXON, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IXOFF), IXOFF, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IXANY), IXANY, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IMAXBEL), IMAXBEL, tio.c_iflag);
        tio.c_iflag = setFlag(t.getInputFlag(Attributes.InputFlag.IUTF8), IUTF8, tio.c_iflag);
        // Output flags
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OPOST), OPOST, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.ONLCR), ONLCR, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OXTABS), OXTABS, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.ONOEOT), ONOEOT, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OCRNL), OCRNL, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.ONOCR), ONOCR, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.ONLRET), ONLRET, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OFILL), OFILL, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.NLDLY), NLDLY, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.TABDLY), TABDLY, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.CRDLY), CRDLY, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.FFDLY), FFDLY, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.BSDLY), BSDLY, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.VTDLY), VTDLY, tio.c_oflag);
        tio.c_oflag = setFlag(t.getOutputFlag(Attributes.OutputFlag.OFDEL), OFDEL, tio.c_oflag);
        // Control flags
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CIGNORE), CIGNORE, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CS5), CS5, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CS6), CS6, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CS7), CS7, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CS8), CS8, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CSTOPB), CSTOPB, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CREAD), CREAD, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.PARENB), PARENB, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.PARODD), PARODD, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.HUPCL), HUPCL, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CLOCAL), CLOCAL, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CCTS_OFLOW), CCTS_OFLOW, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CRTS_IFLOW), CRTS_IFLOW, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CDTR_IFLOW), CDTR_IFLOW, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CDSR_OFLOW), CDSR_OFLOW, tio.c_cflag);
        tio.c_cflag = setFlag(t.getControlFlag(Attributes.ControlFlag.CCAR_OFLOW), CCAR_OFLOW, tio.c_cflag);
        // Local flags
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOKE), ECHOKE, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOE), ECHOE, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOK), ECHOK, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHO), ECHO, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHONL), ECHONL, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOPRT), ECHOPRT, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ECHOCTL), ECHOCTL, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ISIG), ISIG, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ICANON), ICANON, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.ALTWERASE), ALTWERASE, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.IEXTEN), IEXTEN, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.EXTPROC), EXTPROC, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.TOSTOP), TOSTOP, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.FLUSHO), FLUSHO, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.NOKERNINFO), NOKERNINFO, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.PENDIN), PENDIN, tio.c_lflag);
        tio.c_lflag = setFlag(t.getLocalFlag(Attributes.LocalFlag.NOFLSH), NOFLSH, tio.c_lflag);
        // Control chars
        tio.c_cc[VEOF] = (byte) t.getControlChar(Attributes.ControlChar.VEOF);
        tio.c_cc[VEOL] = (byte) t.getControlChar(Attributes.ControlChar.VEOL);
        tio.c_cc[VEOL2] = (byte) t.getControlChar(Attributes.ControlChar.VEOL2);
        tio.c_cc[VERASE] = (byte) t.getControlChar(Attributes.ControlChar.VERASE);
        tio.c_cc[VWERASE] = (byte) t.getControlChar(Attributes.ControlChar.VWERASE);
        tio.c_cc[VKILL] = (byte) t.getControlChar(Attributes.ControlChar.VKILL);
        tio.c_cc[VREPRINT] = (byte) t.getControlChar(Attributes.ControlChar.VREPRINT);
        tio.c_cc[VINTR] = (byte) t.getControlChar(Attributes.ControlChar.VINTR);
        tio.c_cc[VQUIT] = (byte) t.getControlChar(Attributes.ControlChar.VQUIT);
        tio.c_cc[VSUSP] = (byte) t.getControlChar(Attributes.ControlChar.VSUSP);
        tio.c_cc[VDSUSP] = (byte) t.getControlChar(Attributes.ControlChar.VDSUSP);
        tio.c_cc[VSTART] = (byte) t.getControlChar(Attributes.ControlChar.VSTART);
        tio.c_cc[VSTOP] = (byte) t.getControlChar(Attributes.ControlChar.VSTOP);
        tio.c_cc[VLNEXT] = (byte) t.getControlChar(Attributes.ControlChar.VLNEXT);
        tio.c_cc[VDISCARD] = (byte) t.getControlChar(Attributes.ControlChar.VDISCARD);
        tio.c_cc[VMIN] = (byte) t.getControlChar(Attributes.ControlChar.VMIN);
        tio.c_cc[VTIME] = (byte) t.getControlChar(Attributes.ControlChar.VTIME);
        tio.c_cc[VSTATUS] = (byte) t.getControlChar(Attributes.ControlChar.VSTATUS);
        return tio;
    }
    
    protected Attributes toAttributes(CLibrary.Termios tio) {
        Attributes attr = new Attributes();
        // Input flags
        EnumSet<Attributes.InputFlag> iflag = attr.getInputFlags();
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.IGNBRK, IGNBRK);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.IGNBRK, IGNBRK);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.BRKINT, BRKINT);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.IGNPAR, IGNPAR);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.PARMRK, PARMRK);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.INPCK, INPCK);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.ISTRIP, ISTRIP);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.INLCR, INLCR);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.IGNCR, IGNCR);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.ICRNL, ICRNL);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.IXON, IXON);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.IXOFF, IXOFF);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.IXANY, IXANY);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.IMAXBEL, IMAXBEL);
        addFlag(tio.c_iflag, iflag, Attributes.InputFlag.IUTF8, IUTF8);
        // Output flags
        EnumSet<Attributes.OutputFlag> oflag = attr.getOutputFlags();
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.OPOST, OPOST);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.ONLCR, ONLCR);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.OXTABS, OXTABS);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.ONOEOT, ONOEOT);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.OCRNL, OCRNL);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.ONOCR, ONOCR);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.ONLRET, ONLRET);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.OFILL, OFILL);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.NLDLY, NLDLY);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.TABDLY, TABDLY);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.CRDLY, CRDLY);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.FFDLY, FFDLY);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.BSDLY, BSDLY);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.VTDLY, VTDLY);
        addFlag(tio.c_oflag, oflag, Attributes.OutputFlag.OFDEL, OFDEL);
        // Control flags
        EnumSet<Attributes.ControlFlag> cflag = attr.getControlFlags();
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CIGNORE, CIGNORE);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CS5, CS5);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CS6, CS6);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CS7, CS7);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CS8, CS8);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CSTOPB, CSTOPB);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CREAD, CREAD);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.PARENB, PARENB);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.PARODD, PARODD);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.HUPCL, HUPCL);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CLOCAL, CLOCAL);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CCTS_OFLOW, CCTS_OFLOW);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CRTS_IFLOW, CRTS_IFLOW);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CDSR_OFLOW, CDSR_OFLOW);
        addFlag(tio.c_cflag, cflag, Attributes.ControlFlag.CCAR_OFLOW, CCAR_OFLOW);
        // Local flags
        EnumSet<Attributes.LocalFlag> lflag = attr.getLocalFlags();
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ECHOKE, ECHOKE);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ECHOE, ECHOE);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ECHOK, ECHOK);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ECHO, ECHO);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ECHONL, ECHONL);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ECHOPRT, ECHOPRT);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ECHOCTL, ECHOCTL);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ISIG, ISIG);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ICANON, ICANON);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.ALTWERASE, ALTWERASE);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.IEXTEN, IEXTEN);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.EXTPROC, EXTPROC);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.TOSTOP, TOSTOP);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.FLUSHO, FLUSHO);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.NOKERNINFO, NOKERNINFO);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.PENDIN, PENDIN);
        addFlag(tio.c_lflag, lflag, Attributes.LocalFlag.NOFLSH, NOFLSH);
        // Control chars
        EnumMap<Attributes.ControlChar, Integer> cc = attr.getControlChars();
        cc.put(Attributes.ControlChar.VEOF, (int) tio.c_cc[VEOF]);
        cc.put(Attributes.ControlChar.VEOL, (int) tio.c_cc[VEOL]);
        cc.put(Attributes.ControlChar.VEOL2, (int) tio.c_cc[VEOL2]);
        cc.put(Attributes.ControlChar.VERASE, (int) tio.c_cc[VERASE]);
        cc.put(Attributes.ControlChar.VWERASE, (int) tio.c_cc[VWERASE]);
        cc.put(Attributes.ControlChar.VKILL, (int) tio.c_cc[VKILL]);
        cc.put(Attributes.ControlChar.VREPRINT, (int) tio.c_cc[VREPRINT]);
        cc.put(Attributes.ControlChar.VINTR, (int) tio.c_cc[VINTR]);
        cc.put(Attributes.ControlChar.VQUIT, (int) tio.c_cc[VQUIT]);
        cc.put(Attributes.ControlChar.VSUSP, (int) tio.c_cc[VSUSP]);
        cc.put(Attributes.ControlChar.VDSUSP, (int) tio.c_cc[VDSUSP]);
        cc.put(Attributes.ControlChar.VSTART, (int) tio.c_cc[VSTART]);
        cc.put(Attributes.ControlChar.VSTOP, (int) tio.c_cc[VSTOP]);
        cc.put(Attributes.ControlChar.VLNEXT, (int) tio.c_cc[VLNEXT]);
        cc.put(Attributes.ControlChar.VDISCARD, (int) tio.c_cc[VDISCARD]);
        cc.put(Attributes.ControlChar.VMIN, (int) tio.c_cc[VMIN]);
        cc.put(Attributes.ControlChar.VTIME, (int) tio.c_cc[VTIME]);
        cc.put(Attributes.ControlChar.VSTATUS, (int) tio.c_cc[VSTATUS]);
        // Return
        return attr;
    }

    private static long setFlag(boolean flag, long value, long org) {
        return flag ? org | value : org;
    }

    private static <T extends Enum<T>> void addFlag(long value, EnumSet<T> flags, T flag, int v) {
        if ((value & v) != 0) {
            flags.add(flag);
        }
    }
}

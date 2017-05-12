/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal.impl.jna.freebsd;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

import com.sun.jna.LastErrorException;
import com.sun.jna.Structure;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.ControlFlag;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Size;

public interface CLibrary extends com.sun.jna.Library {

    void tcgetattr(int fd, termios termios) throws LastErrorException;

    void tcsetattr(int fd, int cmd, termios termios) throws LastErrorException;

    void ioctl(int fd, long cmd, winsize data) throws LastErrorException;

    void ttyname_r(int fd, byte[] buf, int len) throws LastErrorException;

    class winsize extends Structure {
        public short ws_row;
        public short ws_col;
        public short ws_xpixel;
        public short ws_ypixel;

        public winsize() {
        }

        public winsize(Size ws) {
            ws_row = (short) ws.getRows();
            ws_col = (short) ws.getColumns();
        }

        public Size toSize() {
            return new Size(ws_col, ws_row);
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(//
                    "ws_row",//
                    "ws_col",//
                    "ws_xpixel",//
                    "ws_ypixel"//
            );
        }

    }

    class termios extends Structure {

        public int c_iflag;
        public int c_oflag;
        public int c_cflag;
        public int c_lflag;
        public byte[] c_cc = new byte[20];
        public int c_ispeed;
        public int c_ospeed;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(//
                    "c_iflag",//
                    "c_oflag",//
                    "c_cflag",//
                    "c_lflag",//
                    "c_cc",//
                    "c_ispeed",//
                    "c_ospeed"//
            );
        }

        public termios() {
        }

        public termios(Attributes t) {
            // Input flags
            c_iflag = setFlag(t.getInputFlag(InputFlag.IGNBRK),           IGNBRK,     c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.BRKINT),           BRKINT,     c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.IGNPAR),           IGNPAR,     c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.PARMRK),           PARMRK,     c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.INPCK),            INPCK,      c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.ISTRIP),           ISTRIP,     c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.INLCR),            INLCR,      c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.IGNCR),            IGNCR,      c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.ICRNL),            ICRNL,      c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.IXON),             IXON,       c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.IXOFF),            IXOFF,      c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.IXANY),            IXANY,      c_iflag);
            c_iflag = setFlag(t.getInputFlag(InputFlag.IMAXBEL),          IMAXBEL,    c_iflag);
            // Output flags
            c_oflag = setFlag(t.getOutputFlag(OutputFlag.OPOST),          OPOST,      c_oflag);
            c_oflag = setFlag(t.getOutputFlag(OutputFlag.ONLCR),          ONLCR,      c_oflag);
            c_oflag = setFlag(t.getOutputFlag(OutputFlag.OCRNL),          OCRNL,      c_oflag);
            c_oflag = setFlag(t.getOutputFlag(OutputFlag.ONLRET),         ONLRET,     c_oflag);
            c_oflag = setFlag(t.getOutputFlag(OutputFlag.TABDLY),         TABDLY,     c_oflag);
            // Control flags
            c_cflag = setFlag(t.getControlFlag(ControlFlag.CS5),          CS5,        c_cflag);
            c_cflag = setFlag(t.getControlFlag(ControlFlag.CS6),          CS6,        c_cflag);
            c_cflag = setFlag(t.getControlFlag(ControlFlag.CS7),          CS7,        c_cflag);
            c_cflag = setFlag(t.getControlFlag(ControlFlag.CS8),          CS8,        c_cflag);
            c_cflag = setFlag(t.getControlFlag(ControlFlag.CSTOPB),       CSTOPB,     c_cflag);
            c_cflag = setFlag(t.getControlFlag(ControlFlag.CREAD),        CREAD,      c_cflag);
            c_cflag = setFlag(t.getControlFlag(ControlFlag.PARENB),       PARENB,     c_cflag);
            c_cflag = setFlag(t.getControlFlag(ControlFlag.PARODD),       PARODD,     c_cflag);
            c_cflag = setFlag(t.getControlFlag(ControlFlag.HUPCL),        HUPCL,      c_cflag);
            c_cflag = setFlag(t.getControlFlag(ControlFlag.CLOCAL),       CLOCAL,     c_cflag);
            // Local flags
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.ECHOKE),           ECHOKE,     c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.ECHOE),            ECHOE,      c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.ECHOK),            ECHOK,      c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.ECHO),             ECHO,       c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.ECHONL),           ECHONL,     c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.ECHOPRT),          ECHOPRT,    c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.ECHOCTL),          ECHOCTL,    c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.ISIG),             ISIG,       c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.ICANON),           ICANON,     c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.IEXTEN),           IEXTEN,     c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.EXTPROC),          EXTPROC,    c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.TOSTOP),           TOSTOP,     c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.FLUSHO),           FLUSHO,     c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.PENDIN),           PENDIN,     c_lflag);
            c_lflag = setFlag(t.getLocalFlag(LocalFlag.NOFLSH),           NOFLSH,     c_lflag);
            // Control chars
            c_cc[VEOF]      = (byte) t.getControlChar(ControlChar.VEOF);
            c_cc[VEOL]      = (byte) t.getControlChar(ControlChar.VEOL);
            c_cc[VEOL2]     = (byte) t.getControlChar(ControlChar.VEOL2);
            c_cc[VERASE]    = (byte) t.getControlChar(ControlChar.VERASE);
            c_cc[VWERASE]   = (byte) t.getControlChar(ControlChar.VWERASE);
            c_cc[VKILL]     = (byte) t.getControlChar(ControlChar.VKILL);
            c_cc[VREPRINT]  = (byte) t.getControlChar(ControlChar.VREPRINT);
            c_cc[VINTR]     = (byte) t.getControlChar(ControlChar.VINTR);
            c_cc[VQUIT]     = (byte) t.getControlChar(ControlChar.VQUIT);
            c_cc[VSUSP]     = (byte) t.getControlChar(ControlChar.VSUSP);
            c_cc[VSTART]    = (byte) t.getControlChar(ControlChar.VSTART);
            c_cc[VSTOP]     = (byte) t.getControlChar(ControlChar.VSTOP);
            c_cc[VLNEXT]    = (byte) t.getControlChar(ControlChar.VLNEXT);
            c_cc[VDISCARD]  = (byte) t.getControlChar(ControlChar.VDISCARD);
            c_cc[VMIN]      = (byte) t.getControlChar(ControlChar.VMIN);
            c_cc[VTIME]     = (byte) t.getControlChar(ControlChar.VTIME);
        }

        private int setFlag(boolean flag, int value, int org) {
            return flag ? (org | value) : org;
        }

        public Attributes toAttributes() {
            Attributes attr = new Attributes();
            // Input flags
            EnumSet<InputFlag> iflag = attr.getInputFlags();
            addFlag(c_iflag, iflag, InputFlag.IGNBRK,   IGNBRK);
            addFlag(c_iflag, iflag, InputFlag.IGNBRK, IGNBRK);
            addFlag(c_iflag, iflag, InputFlag.BRKINT, BRKINT);
            addFlag(c_iflag, iflag, InputFlag.IGNPAR, IGNPAR);
            addFlag(c_iflag, iflag, InputFlag.PARMRK, PARMRK);
            addFlag(c_iflag, iflag, InputFlag.INPCK, INPCK);
            addFlag(c_iflag, iflag, InputFlag.ISTRIP, ISTRIP);
            addFlag(c_iflag, iflag, InputFlag.INLCR, INLCR);
            addFlag(c_iflag, iflag, InputFlag.IGNCR, IGNCR);
            addFlag(c_iflag, iflag, InputFlag.ICRNL, ICRNL);
            addFlag(c_iflag, iflag, InputFlag.IXON, IXON);
            addFlag(c_iflag, iflag, InputFlag.IXOFF, IXOFF);
            addFlag(c_iflag, iflag, InputFlag.IXANY, IXANY);
            addFlag(c_iflag, iflag, InputFlag.IMAXBEL, IMAXBEL);
            // Output flags
            EnumSet<OutputFlag> oflag = attr.getOutputFlags();
            addFlag(c_oflag, oflag, OutputFlag.OPOST, OPOST);
            addFlag(c_oflag, oflag, OutputFlag.ONLCR, ONLCR);
            addFlag(c_oflag, oflag, OutputFlag.OCRNL, OCRNL);
            addFlag(c_oflag, oflag, OutputFlag.ONLRET, ONLRET);
            addFlag(c_oflag, oflag, OutputFlag.TABDLY, TABDLY);
            // Control flags
            EnumSet<ControlFlag> cflag = attr.getControlFlags();
            addFlag(c_cflag, cflag, ControlFlag.CS5, CS5);
            addFlag(c_cflag, cflag, ControlFlag.CS6, CS6);
            addFlag(c_cflag, cflag, ControlFlag.CS7, CS7);
            addFlag(c_cflag, cflag, ControlFlag.CS8, CS8);
            addFlag(c_cflag, cflag, ControlFlag.CSTOPB, CSTOPB);
            addFlag(c_cflag, cflag, ControlFlag.CREAD, CREAD);
            addFlag(c_cflag, cflag, ControlFlag.PARENB, PARENB);
            addFlag(c_cflag, cflag, ControlFlag.PARODD, PARODD);
            addFlag(c_cflag, cflag, ControlFlag.HUPCL, HUPCL);
            addFlag(c_cflag, cflag, ControlFlag.CLOCAL, CLOCAL);
            // Local flags
            EnumSet<LocalFlag> lflag = attr.getLocalFlags();
            addFlag(c_lflag, lflag, LocalFlag.ECHOKE, ECHOKE);
            addFlag(c_lflag, lflag, LocalFlag.ECHOE, ECHOE);
            addFlag(c_lflag, lflag, LocalFlag.ECHOK, ECHOK);
            addFlag(c_lflag, lflag, LocalFlag.ECHO, ECHO);
            addFlag(c_lflag, lflag, LocalFlag.ECHONL, ECHONL);
            addFlag(c_lflag, lflag, LocalFlag.ECHOPRT, ECHOPRT);
            addFlag(c_lflag, lflag, LocalFlag.ECHOCTL, ECHOCTL);
            addFlag(c_lflag, lflag, LocalFlag.ISIG, ISIG);
            addFlag(c_lflag, lflag, LocalFlag.ICANON, ICANON);
            addFlag(c_lflag, lflag, LocalFlag.IEXTEN, IEXTEN);
            addFlag(c_lflag, lflag, LocalFlag.EXTPROC, EXTPROC);
            addFlag(c_lflag, lflag, LocalFlag.TOSTOP, TOSTOP);
            addFlag(c_lflag, lflag, LocalFlag.FLUSHO, FLUSHO);
            addFlag(c_lflag, lflag, LocalFlag.PENDIN, PENDIN);
            addFlag(c_lflag, lflag, LocalFlag.NOFLSH, NOFLSH);
            // Control chars
            EnumMap<ControlChar, Integer> cc = attr.getControlChars();
            cc.put(ControlChar.VEOF, 	    (int) c_cc[VEOF]);
            cc.put(ControlChar.VEOL, 	    (int) c_cc[VEOL]);
            cc.put(ControlChar.VEOL2, 	    (int) c_cc[VEOL2]);
            cc.put(ControlChar.VERASE, 	    (int) c_cc[VERASE]);
            cc.put(ControlChar.VWERASE,     (int) c_cc[VWERASE]);
            cc.put(ControlChar.VKILL, 	    (int) c_cc[VKILL]);
            cc.put(ControlChar.VREPRINT, 	(int) c_cc[VREPRINT]);
            cc.put(ControlChar.VINTR, 	    (int) c_cc[VINTR]);
            cc.put(ControlChar.VQUIT, 	    (int) c_cc[VQUIT]);
            cc.put(ControlChar.VSUSP, 	    (int) c_cc[VSUSP]);
            cc.put(ControlChar.VSTART, 	    (int) c_cc[VSTART]);
            cc.put(ControlChar.VSTOP, 	    (int) c_cc[VSTOP]);
            cc.put(ControlChar.VLNEXT, 	    (int) c_cc[VLNEXT]);
            cc.put(ControlChar.VDISCARD,    (int) c_cc[VDISCARD]);
            cc.put(ControlChar.VMIN, 	    (int) c_cc[VMIN]);
            cc.put(ControlChar.VTIME, 	    (int) c_cc[VTIME]);
            // Return
            return attr;
        }

        private <T extends Enum<T>> void addFlag(int value, EnumSet<T> flags, T flag, int v) {
            if ((value & v) != 0) {
                flags.add(flag);
            }
        }
    }

    // CONSTANTS

    int TIOCGWINSZ = 0x40087468;
    int TIOCSWINSZ = 0x80087467;

    int VEOF        = 0;
    int VEOL        = 1;
    int VEOL2       = 2;
    int VERASE      = 3;
    int VWERASE     = 4;
    int VKILL       = 5;
    int VREPRINT    = 6;
    int VERASE2     = 7;
    int VINTR       = 8;
    int VQUIT       = 9;
    int VSUSP       = 10;
    int VDSUSP      = 11;
    int VSTART      = 12;
    int VSTOP       = 13;
    int VLNEXT      = 14;
    int VDISCARD    = 15;
    int VMIN        = 16;
    int VTIME       = 17;
    int VSTATUS     = 18;

    int IGNBRK =   0x0000001;
    int BRKINT =   0x0000002;
    int IGNPAR =   0x0000004;
    int PARMRK =   0x0000008;
    int INPCK =    0x0000010;
    int ISTRIP =   0x0000020;
    int INLCR =    0x0000040;
    int IGNCR =    0x0000080;
    int ICRNL =    0x0000100;
    int IXON =     0x0000200;
    int IXOFF =    0x0000400;
    int IXANY =    0x0000800;
    int IMAXBEL =  0x0002000;

    int OPOST =    0x0000001;
    int ONLCR =    0x0000002;
    int TABDLY =   0x0000004;
      int TAB0 =   0x0000000;
      int TAB3 =   0x0000004;
    int ONOEOT =   0x0000008;
    int OCRNL =    0x0000010;
    int ONLRET =   0x0000040;

    int CIGNORE =  0x0000001;
    int CSIZE =    0x0000300;
      int CS5 =    0x0000000;
      int CS6 =    0x0000100;
      int CS7 =    0x0000200;
      int CS8 =    0x0000300;
    int CSTOPB =   0x0000400;
    int CREAD =    0x0000800;
    int PARENB =   0x0001000;
    int PARODD =   0x0002000;
    int HUPCL =    0x0004000;
    int CLOCAL =   0x0008000;

    int ECHOKE =   0x0000001;
    int ECHOE =    0x0000002;
    int ECHOK =    0x0000004;
    int ECHO =     0x0000008;
    int ECHONL =   0x0000010;
    int ECHOPRT =  0x0000020;
    int ECHOCTL =  0x0000040;
    int ISIG =     0x0000080;
    int ICANON =   0x0000100;
    int ALTWERASE = 0x000200;
    int IEXTEN =   0x0000400;
    int EXTPROC =  0x0000800;
    int TOSTOP =   0x0400000;
    int FLUSHO =   0x0800000;
    int PENDIN =   0x2000000;
    int NOFLSH =   0x8000000;

    int TCSANOW =          0x0;
    int TCSADRAIN =        0x1;
    int TCSAFLUSH =        0x2;

}

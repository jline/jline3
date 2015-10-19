/*
 * Copyright (c) 2002-2015, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * http://www.opensource.org/licenses/bsd-license.php
 */
package org.jline.terminal;

import java.util.EnumSet;

public class Attributes {

    /**
     * Control characters
     */
    public enum ControlChar {
        VEOF(0),
        VEOL(1),
        VEOL2(2),
        VERASE(3),
        VWERASE(4),
        VKILL(5),
        VREPRINT(6),
        VINTR(8),
        VQUIT(9),
        VSUSP(10),
        VDSUSP(11),
        VSTART(12),
        VSTOP(13),
        VLNEXT(14),
        VDISCARD(15),
        VMIN(16),
        VTIME(17),
        VSTATUS(18);
        
        final int value;

        ControlChar(int value) {
            this.value = value;
        }
        
    }

    /**
     * Input flags - software input processing
     */
    public enum InputFlag {
        IGNBRK (0x00000001),      /* ignore BREAK condition */
        BRKINT (0x00000002),      /* map BREAK to SIGINTR */
        IGNPAR (0x00000004),      /* ignore (discard) parity errors */
        PARMRK (0x00000008),      /* mark parity and framing errors */
        INPCK  (0x00000010),      /* enable checking of parity errors */
        ISTRIP (0x00000020),      /* strip 8th bit off chars */
        INLCR  (0x00000040),      /* map NL into CR */
        IGNCR  (0x00000080),      /* ignore CR */
        ICRNL  (0x00000100),      /* map CR to NL (ala CRMOD) */
        IXON   (0x00000200),      /* enable output flow control */
        IXOFF  (0x00000400),      /* enable input flow control */
        IXANY  (0x00000800),      /* any char will restart after stop */
        IMAXBEL(0x00002000),      /* ring bell on input queue full */
        IUTF8  (0x00004000);      /* maintain state for UTF-8 VERASE */
        
        final int value;

        InputFlag(int value) {
            this.value = value;
        }
    }

    /*
     * Output flags - software output processing
     */
    public enum OutputFlag {
        OPOST (0x00000001),      /* enable following output processing */
        ONLCR (0x00000002),      /* map NL to CR-NL (ala CRMOD) */
        OXTABS(0x00000004),      /* expand tabs to spaces */
        ONOEOT(0x00000008),      /* discard EOT's (^D) on output) */
        OCRNL (0x00000010),      /* map CR to NL on output */
        ONOCR (0x00000020),      /* no CR output at column 0 */
        ONLRET(0x00000040),      /* NL performs CR function */
        OFILL (0x00000080),      /* use fill characters for delay */
        NLDLY (0x00000300),      /* \n delay */
        TABDLY(0x00000c04),      /* horizontal tab delay */
        CRDLY (0x00003000),      /* \r delay */
        FFDLY (0x00004000),      /* form feed delay */
        BSDLY (0x00008000),      /* \b delay */
        VTDLY (0x00010000),      /* vertical tab delay */
        OFDEL (0x00020000);      /* fill is DEL, else NUL */

        final int value;

        OutputFlag(int value) {
            this.value = value;
        }
    }

    /*
     * Control flags - hardware control of terminal
     */
    public enum ControlFlag {
        CIGNORE    (0x00000001),      /* ignore control flags */
        CSIZE      (0x00000300),      /* character size mask */
            CS5    (0x00000000),      /* 5 bits    (pseudo) */
            CS6    (0x00000100),      /* 6 bits */
            CS7    (0x00000200),      /* 7 bits */
            CS8    (0x00000300),      /* 8 bits */
        CSTOPB     (0x00000400),      /* send 2 stop bits */
        CREAD      (0x00000800),      /* enable receiver */
        PARENB     (0x00001000),      /* parity enable */
        PARODD     (0x00002000),      /* odd parity, else even */
        HUPCL      (0x00004000),      /* hang up on last close */
        CLOCAL     (0x00008000),      /* ignore modem status lines */
        CCTS_OFLOW (0x00010000),      /* CTS flow control of output */
        CRTS_IFLOW (0x00020000),      /* RTS flow control of input */
        CRTSCTS    (CCTS_OFLOW.value | CRTS_IFLOW.value),
        CDTR_IFLOW (0x00040000),      /* DTR flow control of input */
        CDSR_OFLOW (0x00080000),      /* DSR flow control of output */
        CCAR_OFLOW (0x00100000);      /* DCD flow control of output */

        final int value;

        ControlFlag(int value) {
            this.value = value;
        }
    }

    /*
     * "Local" flags - dumping ground for other state
     *
     * Warning: some flags in this structure begin with
     * the letter "I" and look like they belong in the
     * input flag.
     */
    public enum LocalFlag {
        ECHOKE     (0x00000001),      /* visual erase for line kill */
        ECHOE      (0x00000002),      /* visually erase chars */
        ECHOK      (0x00000004),      /* echo NL after line kill */
        ECHO       (0x00000008),      /* enable echoing */
        ECHONL     (0x00000010),      /* echo NL even if ECHO is off */
        ECHOPRT    (0x00000020),      /* visual erase mode for hardcopy */
        ECHOCTL    (0x00000040),      /* echo control chars as ^(Char) */
        ISIG       (0x00000080),      /* enable signals INTR, QUIT, [D]SUSP */
        ICANON     (0x00000100),      /* canonicalize input lines */
        ALTWERASE  (0x00000200),      /* use alternate WERASE algorithm */
        IEXTEN     (0x00000400),      /* enable DISCARD and LNEXT */
        EXTPROC    (0x00000800),      /* external processing */
        TOSTOP     (0x00400000),      /* stop background jobs from output */
        FLUSHO     (0x00800000),      /* output being flushed (state) */
        NOKERNINFO (0x02000000),      /* no kernel output from VSTATUS */
        PENDIN     (0x20000000),      /* XXX retype pending input (state) */
        NOFLSH     (0x80000000);      /* don't flush after interrupt */

        final int value;

        LocalFlag(int value) {
            this.value = value;
        }
    }

    long c_iflag;
    long c_oflag;
    long c_cflag;
    long c_lflag;
    byte[] c_cc = new byte[20];

    public Attributes() {
    }

    public Attributes(Attributes attr) {
        copy(attr);
    }

    //
    // Input flags
    //
    
    public EnumSet<InputFlag> getInputFlags() {
        EnumSet<InputFlag> flags = EnumSet.noneOf(InputFlag.class);
        for (InputFlag flag : InputFlag.values()) {
            if (getInputFlag(flag)) {
                flags.add(flag);
            }
        }
        return flags;
    }

    public void setInputFlags(EnumSet<InputFlag> flags) {
        int v = 0;
        for (InputFlag f : flags) {
            v |= f.value;
        }
        c_iflag = v;
    }

    public boolean getInputFlag(InputFlag flag) {
        return (c_iflag & flag.value) == flag.value;
    }

    public void setInputFlags(EnumSet<InputFlag> flags, boolean value) {
        int v = 0;
        for (InputFlag f : flags) {
            v |= f.value;
        }
        if (value) {
            c_iflag |= v;
        } else {
            c_iflag &= ~v;
        }
    }
    
    public void setInputFlag(InputFlag flag, boolean value) {
        if (value) {
            c_iflag |= flag.value;
        } else {
            c_iflag &= ~flag.value;
        }
    }

    //
    // Output flags
    //
    
    public EnumSet<OutputFlag> getOutputFlags() {
        EnumSet<OutputFlag> flags = EnumSet.noneOf(OutputFlag.class);
        for (OutputFlag flag : OutputFlag.values()) {
            if (getOutputFlag(flag)) {
                flags.add(flag);
            }
        }
        return flags;
    }

    public void setOutputFlags(EnumSet<OutputFlag> flags) {
        int v = 0;
        for (OutputFlag f : flags) {
            v |= f.value;
        }
        c_oflag = v;
    }

    public boolean getOutputFlag(OutputFlag flag) {
        return (c_oflag & flag.value) == flag.value;
    }

    public void setOutputFlags(EnumSet<OutputFlag> flags, boolean value) {
        int v = 0;
        for (OutputFlag f : flags) {
            v |= f.value;
        }
        if (value) {
            c_oflag |= v;
        } else {
            c_oflag &= ~v;
        }
    }

    public void setOutputFlag(OutputFlag flag, boolean value) {
        if (value) {
            c_oflag |= flag.value;
        } else {
            c_oflag &= ~flag.value;
        }
    }

    //
    // Control flags
    //
    
    public EnumSet<ControlFlag> getControlFlags() {
        EnumSet<ControlFlag> flags = EnumSet.noneOf(ControlFlag.class);
        for (ControlFlag flag : ControlFlag.values()) {
            if (getControlFlag(flag)) {
                flags.add(flag);
            }
        }
        return flags;
    }

    public void setControlFlags(EnumSet<ControlFlag> flags) {
        int v = 0;
        for (ControlFlag f : flags) {
            v |= f.value;
        }
        c_cflag = v;
    }

    public boolean getControlFlag(ControlFlag flag) {
        switch (flag) {
            case CS5:
            case CS6:
            case CS7:
            case CS8:
                return (c_cflag & ControlFlag.CSIZE.value) == flag.value;
            case CSIZE:
                return false;
            default:
                return (c_cflag & flag.value) == flag.value;
        }
    }

    public void setControlFlags(EnumSet<ControlFlag> flags, boolean value) {
        int v = 0;
        for (ControlFlag f : flags) {
            v |= f.value;
        }
        if (value) {
            c_cflag |= v;
        } else {
            c_cflag &= ~v;
        }
    }

    public void setControlFlag(ControlFlag flag, boolean value) {
        if (value) {
            c_cflag |= flag.value;
        } else {
            c_cflag &= ~flag.value;
        }
    }
    
    //
    // Local flags
    //

    public EnumSet<LocalFlag> getLocalFlags() {
        EnumSet<LocalFlag> flags = EnumSet.noneOf(LocalFlag.class);
        for (LocalFlag flag : LocalFlag.values()) {
            if (getLocalFlag(flag)) {
                flags.add(flag);
            }
        }
        return flags;
    }

    public void setLocalFlags(EnumSet<LocalFlag> flags) {
        int v = 0;
        for (LocalFlag f : flags) {
            v |= f.value;
        }
        c_lflag = v;
    }

    public boolean getLocalFlag(LocalFlag flag) {
        return (c_lflag & flag.value) == flag.value;
    }

    public void setLocalFlags(EnumSet<LocalFlag> flags, boolean value) {
        int v = 0;
        for (LocalFlag f : flags) {
            v |= f.value;
        }
        if (value) {
            c_lflag |= v;
        } else {
            c_lflag &= ~v;
        }
    }

    public void setLocalFlag(LocalFlag flag, boolean value) {
        if (value) {
            c_lflag |= flag.value;
        } else {
            c_lflag &= ~flag.value;
        }
    }

    //
    // Control chars
    //

    public int getControlChar(ControlChar c) {
        return c_cc[c.value] & 0xff;
    }

    public void setControlChar(ControlChar c, int value) {
        c_cc[c.value] = (byte) value;
    }

    //
    // Miscellaneous methods
    //

    public void copy(Attributes attributes) {
        System.arraycopy(attributes.c_cc, 0, c_cc, 0, c_cc.length);
        c_cflag = attributes.c_cflag;
        c_iflag = attributes.c_iflag;
        c_lflag = attributes.c_lflag;
        c_oflag = attributes.c_oflag;
    }
}

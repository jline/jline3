/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.util.EnumMap;

import org.jline.terminal.Attributes.*;

/**
 * Solaris (SunOS) specific termios flag constants and control character indices.
 *
 * @see TermiosMapping
 */
@SuppressWarnings("java:S6548")
public class SolarisTermiosMapping extends TermiosMapping {

    /** Singleton instance. */
    public static final SolarisTermiosMapping INSTANCE = new SolarisTermiosMapping();

    private SolarisTermiosMapping() {
        super(inputFlags(), outputFlags(), controlFlags(), localFlags(), controlChars());
    }

    private static EnumMap<InputFlag, Long> inputFlags() {
        var map = new EnumMap<InputFlag, Long>(InputFlag.class);
        map.put(InputFlag.IGNBRK, 0x0000001L);
        map.put(InputFlag.BRKINT, 0x0000002L);
        map.put(InputFlag.IGNPAR, 0x0000004L);
        map.put(InputFlag.PARMRK, 0x0000008L);
        map.put(InputFlag.INPCK, 0x0000010L);
        map.put(InputFlag.ISTRIP, 0x0000020L);
        map.put(InputFlag.INLCR, 0x0000040L);
        map.put(InputFlag.IGNCR, 0x0000080L);
        map.put(InputFlag.ICRNL, 0x0000100L);
        map.put(InputFlag.IUCLC, 0x0000200L);
        map.put(InputFlag.IXON, 0x0000400L);
        map.put(InputFlag.IXANY, 0x0000800L);
        map.put(InputFlag.IXOFF, 0x0001000L);
        map.put(InputFlag.IMAXBEL, 0x0002000L);
        map.put(InputFlag.IUTF8, 0x0004000L);
        return map;
    }

    private static EnumMap<OutputFlag, Long> outputFlags() {
        var map = new EnumMap<OutputFlag, Long>(OutputFlag.class);
        map.put(OutputFlag.OPOST, 0x0000001L);
        map.put(OutputFlag.OLCUC, 0x0000002L);
        map.put(OutputFlag.ONLCR, 0x0000004L);
        map.put(OutputFlag.OCRNL, 0x0000008L);
        map.put(OutputFlag.ONOCR, 0x0000010L);
        map.put(OutputFlag.ONLRET, 0x0000020L);
        map.put(OutputFlag.OFILL, 0x0000040L);
        map.put(OutputFlag.OFDEL, 0x0000080L);
        map.put(OutputFlag.NLDLY, 0x0000100L);
        map.put(OutputFlag.CRDLY, 0x0000600L);
        map.put(OutputFlag.TABDLY, 0x0001800L);
        map.put(OutputFlag.BSDLY, 0x0002000L);
        map.put(OutputFlag.VTDLY, 0x0004000L);
        map.put(OutputFlag.FFDLY, 0x0008000L);
        return map;
    }

    private static EnumMap<ControlFlag, Long> controlFlags() {
        var map = new EnumMap<ControlFlag, Long>(ControlFlag.class);
        map.put(ControlFlag.CS5, 0x0000000L);
        map.put(ControlFlag.CS6, 0x0000010L);
        map.put(ControlFlag.CS7, 0x0000020L);
        map.put(ControlFlag.CS8, 0x0000030L);
        map.put(ControlFlag.CSTOPB, 0x0000040L);
        map.put(ControlFlag.CREAD, 0x0000080L);
        map.put(ControlFlag.PARENB, 0x0000100L);
        map.put(ControlFlag.PARODD, 0x0000200L);
        map.put(ControlFlag.HUPCL, 0x0000400L);
        map.put(ControlFlag.CLOCAL, 0x0000800L);
        return map;
    }

    private static EnumMap<LocalFlag, Long> localFlags() {
        var map = new EnumMap<LocalFlag, Long>(LocalFlag.class);
        map.put(LocalFlag.ISIG, 0x0000001L);
        map.put(LocalFlag.ICANON, 0x0000002L);
        map.put(LocalFlag.XCASE, 0x0000004L);
        map.put(LocalFlag.ECHO, 0x0000008L);
        map.put(LocalFlag.ECHOE, 0x0000010L);
        map.put(LocalFlag.ECHOK, 0x0000020L);
        map.put(LocalFlag.ECHONL, 0x0000040L);
        map.put(LocalFlag.NOFLSH, 0x0000080L);
        map.put(LocalFlag.TOSTOP, 0x0000100L);
        map.put(LocalFlag.ECHOCTL, 0x0000200L);
        map.put(LocalFlag.ECHOPRT, 0x0000400L);
        map.put(LocalFlag.ECHOKE, 0x0000800L);
        map.put(LocalFlag.FLUSHO, 0x0002000L);
        map.put(LocalFlag.PENDIN, 0x0004000L);
        map.put(LocalFlag.IEXTEN, 0x0008000L);
        map.put(LocalFlag.EXTPROC, 0x0010000L);
        return map;
    }

    private static EnumMap<ControlChar, Integer> controlChars() {
        var map = new EnumMap<ControlChar, Integer>(ControlChar.class);
        map.put(ControlChar.VINTR, 0);
        map.put(ControlChar.VQUIT, 1);
        map.put(ControlChar.VERASE, 2);
        map.put(ControlChar.VKILL, 3);
        map.put(ControlChar.VEOF, 4);
        map.put(ControlChar.VTIME, 5);
        map.put(ControlChar.VMIN, 6);
        map.put(ControlChar.VSWTC, 7);
        map.put(ControlChar.VSTART, 8);
        map.put(ControlChar.VSTOP, 9);
        map.put(ControlChar.VSUSP, 10);
        map.put(ControlChar.VEOL, 11);
        map.put(ControlChar.VREPRINT, 12);
        map.put(ControlChar.VDISCARD, 13);
        map.put(ControlChar.VWERASE, 14);
        map.put(ControlChar.VLNEXT, 15);
        map.put(ControlChar.VEOL2, 16);
        return map;
    }
}

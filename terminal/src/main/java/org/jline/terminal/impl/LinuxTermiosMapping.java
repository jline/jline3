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
 * Linux-specific termios flag constants and control character indices.
 * <p>
 * Linux on PowerPC (ppc, ppc64, ppc64le) has its own layout in
 * {@code asm/termbits.h}: other control character indices (for example
 * {@code VMIN} is 5 and {@code VTIME} is 7) and other flag bits (for example
 * {@code ICANON} is {@code 0x100}). {@link #POWERPC} holds those values.
 *
 * @see TermiosMapping
 */
@SuppressWarnings("java:S6548")
public class LinuxTermiosMapping extends TermiosMapping {

    /** Singleton instance for the generic Linux layout. */
    public static final LinuxTermiosMapping INSTANCE = new LinuxTermiosMapping(false);

    /** Singleton instance for Linux on PowerPC. */
    public static final LinuxTermiosMapping POWERPC = new LinuxTermiosMapping(true);

    private LinuxTermiosMapping(boolean ppc) {
        super(inputFlags(ppc), outputFlags(ppc), controlFlags(ppc), localFlags(ppc), controlChars(ppc));
    }

    private static EnumMap<InputFlag, Long> inputFlags(boolean ppc) {
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
        map.put(InputFlag.IUCLC, ppc ? 0x0001000L : 0x0000200L);
        map.put(InputFlag.IXON, ppc ? 0x0000200L : 0x0000400L);
        map.put(InputFlag.IXANY, 0x0000800L);
        map.put(InputFlag.IXOFF, ppc ? 0x0000400L : 0x0001000L);
        map.put(InputFlag.IMAXBEL, 0x0002000L);
        map.put(InputFlag.IUTF8, 0x0004000L);
        return map;
    }

    private static EnumMap<OutputFlag, Long> outputFlags(boolean ppc) {
        var map = new EnumMap<OutputFlag, Long>(OutputFlag.class);
        map.put(OutputFlag.OPOST, 0x0000001L);
        map.put(OutputFlag.OLCUC, ppc ? 0x0000004L : 0x0000002L);
        map.put(OutputFlag.ONLCR, ppc ? 0x0000002L : 0x0000004L);
        map.put(OutputFlag.OCRNL, 0x0000008L);
        map.put(OutputFlag.ONOCR, 0x0000010L);
        map.put(OutputFlag.ONLRET, 0x0000020L);
        map.put(OutputFlag.OFILL, 0x0000040L);
        map.put(OutputFlag.OFDEL, 0x0000080L);
        map.put(OutputFlag.NLDLY, ppc ? 0x0000300L : 0x0000100L);
        map.put(OutputFlag.CRDLY, ppc ? 0x0003000L : 0x0000600L);
        map.put(OutputFlag.TABDLY, ppc ? 0x0000c00L : 0x0001800L);
        map.put(OutputFlag.BSDLY, ppc ? 0x0008000L : 0x0002000L);
        map.put(OutputFlag.VTDLY, ppc ? 0x0010000L : 0x0004000L);
        map.put(OutputFlag.FFDLY, ppc ? 0x0004000L : 0x0008000L);
        return map;
    }

    private static EnumMap<ControlFlag, Long> controlFlags(boolean ppc) {
        var map = new EnumMap<ControlFlag, Long>(ControlFlag.class);
        map.put(ControlFlag.CS5, 0x0000000L);
        map.put(ControlFlag.CS6, ppc ? 0x0000100L : 0x0000010L);
        map.put(ControlFlag.CS7, ppc ? 0x0000200L : 0x0000020L);
        map.put(ControlFlag.CS8, ppc ? 0x0000300L : 0x0000030L);
        map.put(ControlFlag.CSTOPB, ppc ? 0x0000400L : 0x0000040L);
        map.put(ControlFlag.CREAD, ppc ? 0x0000800L : 0x0000080L);
        map.put(ControlFlag.PARENB, ppc ? 0x0001000L : 0x0000100L);
        map.put(ControlFlag.PARODD, ppc ? 0x0002000L : 0x0000200L);
        map.put(ControlFlag.HUPCL, ppc ? 0x0004000L : 0x0000400L);
        map.put(ControlFlag.CLOCAL, ppc ? 0x0008000L : 0x0000800L);
        return map;
    }

    private static EnumMap<LocalFlag, Long> localFlags(boolean ppc) {
        var map = new EnumMap<LocalFlag, Long>(LocalFlag.class);
        map.put(LocalFlag.ISIG, ppc ? 0x0000080L : 0x0000001L);
        map.put(LocalFlag.ICANON, ppc ? 0x0000100L : 0x0000002L);
        map.put(LocalFlag.XCASE, ppc ? 0x0004000L : 0x0000004L);
        map.put(LocalFlag.ECHO, 0x0000008L);
        map.put(LocalFlag.ECHOE, ppc ? 0x0000002L : 0x0000010L);
        map.put(LocalFlag.ECHOK, ppc ? 0x0000004L : 0x0000020L);
        map.put(LocalFlag.ECHONL, ppc ? 0x0000010L : 0x0000040L);
        map.put(LocalFlag.NOFLSH, ppc ? 0x80000000L : 0x0000080L);
        map.put(LocalFlag.TOSTOP, ppc ? 0x0400000L : 0x0000100L);
        map.put(LocalFlag.ECHOCTL, ppc ? 0x0000040L : 0x0000200L);
        map.put(LocalFlag.ECHOPRT, ppc ? 0x0000020L : 0x0000400L);
        map.put(LocalFlag.ECHOKE, ppc ? 0x0000001L : 0x0000800L);
        map.put(LocalFlag.FLUSHO, ppc ? 0x0800000L : 0x0001000L);
        map.put(LocalFlag.PENDIN, ppc ? 0x20000000L : 0x0004000L);
        map.put(LocalFlag.IEXTEN, ppc ? 0x0000400L : 0x0008000L);
        map.put(LocalFlag.EXTPROC, ppc ? 0x10000000L : 0x0010000L);
        return map;
    }

    private static EnumMap<ControlChar, Integer> controlChars(boolean ppc) {
        var map = new EnumMap<ControlChar, Integer>(ControlChar.class);
        map.put(ControlChar.VINTR, 0);
        map.put(ControlChar.VQUIT, 1);
        map.put(ControlChar.VERASE, 2);
        map.put(ControlChar.VKILL, 3);
        map.put(ControlChar.VEOF, 4);
        map.put(ControlChar.VTIME, ppc ? 7 : 5);
        map.put(ControlChar.VMIN, ppc ? 5 : 6);
        map.put(ControlChar.VSWTC, ppc ? 9 : 7);
        map.put(ControlChar.VSTART, ppc ? 13 : 8);
        map.put(ControlChar.VSTOP, ppc ? 14 : 9);
        map.put(ControlChar.VSUSP, ppc ? 12 : 10);
        map.put(ControlChar.VEOL, ppc ? 6 : 11);
        map.put(ControlChar.VREPRINT, ppc ? 11 : 12);
        map.put(ControlChar.VDISCARD, ppc ? 16 : 13);
        map.put(ControlChar.VWERASE, ppc ? 10 : 14);
        map.put(ControlChar.VLNEXT, 15);
        map.put(ControlChar.VEOL2, ppc ? 8 : 16);
        return map;
    }
}

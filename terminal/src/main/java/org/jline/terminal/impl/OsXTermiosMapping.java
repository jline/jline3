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
 * macOS (Darwin) specific termios flag constants and control character indices.
 *
 * @see TermiosMapping
 */
@SuppressWarnings("java:S6548")
public class OsXTermiosMapping extends TermiosMapping {

    /** Singleton instance. */
    public static final OsXTermiosMapping INSTANCE = new OsXTermiosMapping();

    private OsXTermiosMapping() {
        super(inputFlags(), outputFlags(), controlFlags(), localFlags(), controlChars());
    }

    private static EnumMap<InputFlag, Long> inputFlags() {
        var map = new EnumMap<InputFlag, Long>(InputFlag.class);
        map.put(InputFlag.IGNBRK, 0x00000001L);
        map.put(InputFlag.BRKINT, 0x00000002L);
        map.put(InputFlag.IGNPAR, 0x00000004L);
        map.put(InputFlag.PARMRK, 0x00000008L);
        map.put(InputFlag.INPCK, 0x00000010L);
        map.put(InputFlag.ISTRIP, 0x00000020L);
        map.put(InputFlag.INLCR, 0x00000040L);
        map.put(InputFlag.IGNCR, 0x00000080L);
        map.put(InputFlag.ICRNL, 0x00000100L);
        map.put(InputFlag.IXON, 0x00000200L);
        map.put(InputFlag.IXOFF, 0x00000400L);
        map.put(InputFlag.IXANY, 0x00000800L);
        map.put(InputFlag.IMAXBEL, 0x00002000L);
        map.put(InputFlag.IUTF8, 0x00004000L);
        return map;
    }

    private static EnumMap<OutputFlag, Long> outputFlags() {
        var map = new EnumMap<OutputFlag, Long>(OutputFlag.class);
        map.put(OutputFlag.OPOST, 0x00000001L);
        map.put(OutputFlag.ONLCR, 0x00000002L);
        map.put(OutputFlag.OXTABS, 0x00000004L);
        map.put(OutputFlag.ONOEOT, 0x00000008L);
        map.put(OutputFlag.OCRNL, 0x00000010L);
        map.put(OutputFlag.ONOCR, 0x00000020L);
        map.put(OutputFlag.ONLRET, 0x00000040L);
        map.put(OutputFlag.OFILL, 0x00000080L);
        map.put(OutputFlag.NLDLY, 0x00000300L);
        map.put(OutputFlag.TABDLY, 0x00000c04L);
        map.put(OutputFlag.CRDLY, 0x00003000L);
        map.put(OutputFlag.FFDLY, 0x00004000L);
        map.put(OutputFlag.BSDLY, 0x00008000L);
        map.put(OutputFlag.VTDLY, 0x00010000L);
        map.put(OutputFlag.OFDEL, 0x00020000L);
        return map;
    }

    private static EnumMap<ControlFlag, Long> controlFlags() {
        var map = new EnumMap<ControlFlag, Long>(ControlFlag.class);
        map.put(ControlFlag.CIGNORE, 0x00000001L);
        map.put(ControlFlag.CS5, 0x00000000L);
        map.put(ControlFlag.CS6, 0x00000100L);
        map.put(ControlFlag.CS7, 0x00000200L);
        map.put(ControlFlag.CS8, 0x00000300L);
        map.put(ControlFlag.CSTOPB, 0x00000400L);
        map.put(ControlFlag.CREAD, 0x00000800L);
        map.put(ControlFlag.PARENB, 0x00001000L);
        map.put(ControlFlag.PARODD, 0x00002000L);
        map.put(ControlFlag.HUPCL, 0x00004000L);
        map.put(ControlFlag.CLOCAL, 0x00008000L);
        map.put(ControlFlag.CCTS_OFLOW, 0x00010000L);
        map.put(ControlFlag.CRTS_IFLOW, 0x00020000L);
        map.put(ControlFlag.CDTR_IFLOW, 0x00040000L);
        map.put(ControlFlag.CDSR_OFLOW, 0x00080000L);
        map.put(ControlFlag.CCAR_OFLOW, 0x00100000L);
        return map;
    }

    private static EnumMap<LocalFlag, Long> localFlags() {
        var map = new EnumMap<LocalFlag, Long>(LocalFlag.class);
        map.put(LocalFlag.ECHOKE, 0x00000001L);
        map.put(LocalFlag.ECHOE, 0x00000002L);
        map.put(LocalFlag.ECHOK, 0x00000004L);
        map.put(LocalFlag.ECHO, 0x00000008L);
        map.put(LocalFlag.ECHONL, 0x00000010L);
        map.put(LocalFlag.ECHOPRT, 0x00000020L);
        map.put(LocalFlag.ECHOCTL, 0x00000040L);
        map.put(LocalFlag.ISIG, 0x00000080L);
        map.put(LocalFlag.ICANON, 0x00000100L);
        map.put(LocalFlag.ALTWERASE, 0x00000200L);
        map.put(LocalFlag.IEXTEN, 0x00000400L);
        map.put(LocalFlag.EXTPROC, 0x00000800L);
        map.put(LocalFlag.TOSTOP, 0x00400000L);
        map.put(LocalFlag.FLUSHO, 0x00800000L);
        map.put(LocalFlag.NOKERNINFO, 0x02000000L);
        map.put(LocalFlag.PENDIN, 0x20000000L);
        map.put(LocalFlag.NOFLSH, 0x80000000L);
        return map;
    }

    private static EnumMap<ControlChar, Integer> controlChars() {
        var map = new EnumMap<ControlChar, Integer>(ControlChar.class);
        map.put(ControlChar.VEOF, 0);
        map.put(ControlChar.VEOL, 1);
        map.put(ControlChar.VEOL2, 2);
        map.put(ControlChar.VERASE, 3);
        map.put(ControlChar.VWERASE, 4);
        map.put(ControlChar.VKILL, 5);
        map.put(ControlChar.VREPRINT, 6);
        map.put(ControlChar.VINTR, 8);
        map.put(ControlChar.VQUIT, 9);
        map.put(ControlChar.VSUSP, 10);
        map.put(ControlChar.VDSUSP, 11);
        map.put(ControlChar.VSTART, 12);
        map.put(ControlChar.VSTOP, 13);
        map.put(ControlChar.VLNEXT, 14);
        map.put(ControlChar.VDISCARD, 15);
        map.put(ControlChar.VMIN, 16);
        map.put(ControlChar.VTIME, 17);
        map.put(ControlChar.VSTATUS, 18);
        return map;
    }
}

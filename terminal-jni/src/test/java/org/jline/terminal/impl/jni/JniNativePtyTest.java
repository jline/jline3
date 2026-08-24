/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.jni;

import org.jline.nativ.CLibrary;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.impl.TermiosMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JniNativePtyTest {

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void applyAttributesPreservesBaudAndSpeed() {
        CLibrary.Termios tios = new CLibrary.Termios();
        Attributes seed = new Attributes();
        seed.setControlFlag(ControlFlag.CS8, true);
        seed.setControlFlag(ControlFlag.CREAD, true);
        seed.setLocalFlag(LocalFlag.ECHO, true);
        JniNativePty.applyAttributes(tios, seed);

        TermiosMapping mapping = TermiosMapping.forCurrentPlatform();
        Attributes allControl = new Attributes();
        for (ControlFlag flag : ControlFlag.values()) {
            allControl.setControlFlag(flag, true);
        }
        long mappedCflag = mapping.toTermios(allControl).cflag();
        long baudBits = 0x000DL & ~mappedCflag;
        if (baudBits == 0) {
            baudBits = Long.lowestOneBit(~mappedCflag);
        }
        tios.c_cflag |= baudBits;
        tios.c_ispeed = 9600;
        tios.c_ospeed = 9600;
        tios.c_cc[31] = 0x5A;

        Attributes attr = mapping.toAttributes(JniNativePty.fromNativeTermios(tios));
        attr.setLocalFlag(LocalFlag.ECHO, false);
        JniNativePty.applyAttributes(tios, attr);

        assertEquals(baudBits, tios.c_cflag & baudBits);
        assertEquals(9600, tios.c_ispeed);
        assertEquals(9600, tios.c_ospeed);
        assertEquals((byte) 0x5A, tios.c_cc[31]);
        Attributes applied = mapping.toAttributes(JniNativePty.fromNativeTermios(tios));
        assertFalse(applied.getLocalFlag(LocalFlag.ECHO));
        assertTrue(applied.getControlFlag(ControlFlag.CS8));
        assertTrue(applied.getControlFlag(ControlFlag.CREAD));
    }
}

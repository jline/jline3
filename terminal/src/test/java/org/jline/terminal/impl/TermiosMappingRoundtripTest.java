/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermiosMappingRoundtripTest {

    @Test
    void testLinuxRoundtrip() {
        verifyRoundtrip("Linux", LinuxTermiosMapping.INSTANCE);
    }

    @Test
    void testLinuxPowerPcRoundtrip() {
        verifyRoundtrip("LinuxPowerPc", LinuxTermiosMapping.POWERPC);
    }

    @Test
    void testLinuxPowerPcAbiValues() {
        // ICANON must encode as 0x100 on PowerPC (not the generic 0x002)
        Attributes attr = new Attributes();
        attr.setLocalFlag(LocalFlag.ICANON, true);
        TermiosData tio = LinuxTermiosMapping.POWERPC.toTermios(attr);
        assertEquals(0x100L, tio.lflag(), "ICANON must be 0x100 on PowerPC");

        // VMIN -> cc[5], VTIME -> cc[7] (PowerPC ABI; generic Linux has VMIN=6, VTIME=5)
        Attributes ccAttr = new Attributes();
        ccAttr.setControlChar(ControlChar.VMIN, 42);
        ccAttr.setControlChar(ControlChar.VTIME, 99);
        TermiosData tio2 = LinuxTermiosMapping.POWERPC.toTermios(ccAttr);
        assertEquals(42, tio2.cc()[5], "VMIN must map to cc[5] on PowerPC");
        assertEquals(99, tio2.cc()[7], "VTIME must map to cc[7] on PowerPC");
    }

    @Test
    void testDetectPlatformLinuxPowerPc() {
        assertSame(LinuxTermiosMapping.POWERPC, TermiosMapping.detectPlatform("Linux", "ppc64le"));
        assertSame(LinuxTermiosMapping.POWERPC, TermiosMapping.detectPlatform("Linux", "ppc64"));
        assertSame(LinuxTermiosMapping.POWERPC, TermiosMapping.detectPlatform("Linux", "ppc"));
        assertSame(LinuxTermiosMapping.INSTANCE, TermiosMapping.detectPlatform("Linux", "amd64"));
        assertSame(LinuxTermiosMapping.INSTANCE, TermiosMapping.detectPlatform("Linux", "aarch64"));
        assertSame(OsXTermiosMapping.INSTANCE, TermiosMapping.detectPlatform("Mac OS X", "aarch64"));
    }

    @Test
    void testOsXRoundtrip() {
        verifyRoundtrip("OsX", OsXTermiosMapping.INSTANCE);
    }

    @Test
    void testFreeBsdRoundtrip() {
        verifyRoundtrip("FreeBsd", FreeBsdTermiosMapping.INSTANCE);
    }

    @Test
    void testSolarisRoundtrip() {
        verifyRoundtrip("Solaris", SolarisTermiosMapping.INSTANCE);
    }

    @Test
    void testLinuxPreservesBaudWhenApplyingAttributes() {
        verifyPreservesUnmappedBaudAndSpeed("Linux", LinuxTermiosMapping.INSTANCE, 0x000DL);
    }

    @Test
    void testLinuxPowerPcPreservesBaudWhenApplyingAttributes() {
        verifyPreservesUnmappedBaudAndSpeed("LinuxPowerPc", LinuxTermiosMapping.POWERPC, 0x000DL);
    }

    @Test
    void testOsXPreservesBaudWhenApplyingAttributes() {
        verifyPreservesUnmappedBaudAndSpeed("OsX", OsXTermiosMapping.INSTANCE, 0x000DL);
    }

    @Test
    void testFreeBsdPreservesBaudWhenApplyingAttributes() {
        verifyPreservesUnmappedBaudAndSpeed("FreeBsd", FreeBsdTermiosMapping.INSTANCE, 0x000DL);
    }

    @Test
    void testSolarisPreservesBaudWhenApplyingAttributes() {
        verifyPreservesUnmappedBaudAndSpeed("Solaris", SolarisTermiosMapping.INSTANCE, 0x000DL);
    }

    private void verifyRoundtrip(String platform, TermiosMapping mapping) {
        verifyFlagRoundtrip(platform, mapping);
        verifyControlCharRoundtrip(platform, mapping);
        verifyDoubleRoundtripStability(platform, mapping);
    }

    private void verifyFlagRoundtrip(String platform, TermiosMapping mapping) {
        for (InputFlag flag : InputFlag.values()) {
            Attributes attr = new Attributes();
            attr.setInputFlag(flag, true);
            TermiosData tio = mapping.toTermios(attr);
            if (tio.iflag() != 0) {
                Attributes result = mapping.toAttributes(tio);
                assertTrue(result.getInputFlag(flag), platform + ": " + flag + " input flag roundtrip");
            }
        }
        for (OutputFlag flag : OutputFlag.values()) {
            Attributes attr = new Attributes();
            attr.setOutputFlag(flag, true);
            TermiosData tio = mapping.toTermios(attr);
            if (tio.oflag() != 0) {
                Attributes result = mapping.toAttributes(tio);
                assertTrue(result.getOutputFlag(flag), platform + ": " + flag + " output flag roundtrip");
            }
        }
        for (ControlFlag flag : ControlFlag.values()) {
            Attributes attr = new Attributes();
            attr.setControlFlag(flag, true);
            TermiosData tio = mapping.toTermios(attr);
            if (tio.cflag() != 0) {
                Attributes result = mapping.toAttributes(tio);
                assertTrue(result.getControlFlag(flag), platform + ": " + flag + " control flag roundtrip");
            }
        }
        for (LocalFlag flag : LocalFlag.values()) {
            Attributes attr = new Attributes();
            attr.setLocalFlag(flag, true);
            TermiosData tio = mapping.toTermios(attr);
            if (tio.lflag() != 0) {
                Attributes result = mapping.toAttributes(tio);
                assertTrue(result.getLocalFlag(flag), platform + ": " + flag + " local flag roundtrip");
            }
        }
    }

    private void verifyControlCharRoundtrip(String platform, TermiosMapping mapping) {
        Attributes attr = new Attributes();
        int value = 1;
        for (ControlChar cc : ControlChar.values()) {
            attr.setControlChar(cc, value++);
        }
        TermiosData tio = mapping.toTermios(attr);
        Attributes result = mapping.toAttributes(tio);
        for (ControlChar cc : ControlChar.values()) {
            int expected = attr.getControlChar(cc);
            int actual = result.getControlChar(cc);
            if (actual >= 0) {
                assertEquals(expected, actual, platform + ": " + cc + " control char roundtrip");
            }
        }
    }

    private void verifyDoubleRoundtripStability(String platform, TermiosMapping mapping) {
        Attributes attr = new Attributes();
        for (InputFlag flag : InputFlag.values()) {
            attr.setInputFlag(flag, true);
        }
        for (OutputFlag flag : OutputFlag.values()) {
            attr.setOutputFlag(flag, true);
        }
        for (ControlFlag flag : ControlFlag.values()) {
            attr.setControlFlag(flag, true);
        }
        for (LocalFlag flag : LocalFlag.values()) {
            attr.setLocalFlag(flag, true);
        }

        TermiosData tio1 = mapping.toTermios(attr);
        Attributes result1 = mapping.toAttributes(tio1);
        TermiosData tio2 = mapping.toTermios(result1);
        Attributes result2 = mapping.toAttributes(tio2);
        TermiosData tio3 = mapping.toTermios(result2);

        assertEquals(tio2.iflag(), tio3.iflag(), platform + ": c_iflag stable after double roundtrip");
        assertEquals(tio2.oflag(), tio3.oflag(), platform + ": c_oflag stable after double roundtrip");
        assertEquals(tio2.cflag(), tio3.cflag(), platform + ": c_cflag stable after double roundtrip");
        assertEquals(tio2.lflag(), tio3.lflag(), platform + ": c_lflag stable after double roundtrip");
    }

    private void verifyPreservesUnmappedBaudAndSpeed(String platform, TermiosMapping mapping, long preferredBaud) {
        Attributes mapped = new Attributes();
        for (ControlFlag flag : ControlFlag.values()) {
            mapped.setControlFlag(flag, true);
        }
        long mappedCflag = mapping.toTermios(mapped).cflag();
        long baudBits = preferredBaud & ~mappedCflag;
        if (baudBits == 0) {
            baudBits = Long.lowestOneBit(~mappedCflag);
        }

        Attributes seed = new Attributes();
        seed.setControlFlag(ControlFlag.CS8, true);
        seed.setControlFlag(ControlFlag.CREAD, true);
        seed.setLocalFlag(LocalFlag.ECHO, true);
        TermiosData existing = mapping.toTermios(seed);
        existing.cflag(existing.cflag() | baudBits);
        existing.ispeed(9600);
        existing.ospeed(9600);
        existing.cc()[31] = 0x5A;

        Attributes attr = mapping.toAttributes(existing);
        attr.setLocalFlag(LocalFlag.ECHO, false);

        TermiosData result = mapping.toTermios(attr, existing);
        assertEquals(baudBits, result.cflag() & baudBits, platform + ": unmapped baud bits preserved");
        assertEquals(9600, result.ispeed(), platform + ": c_ispeed preserved");
        assertEquals(9600, result.ospeed(), platform + ": c_ospeed preserved");
        assertEquals((byte) 0x5A, result.cc()[31], platform + ": unmapped c_cc bytes preserved");
        Attributes applied = mapping.toAttributes(result);
        assertFalse(applied.getLocalFlag(LocalFlag.ECHO), platform + ": mapped ECHO cleared");
        assertTrue(applied.getControlFlag(ControlFlag.CS8), platform + ": mapped CS8 kept");
        assertTrue(applied.getControlFlag(ControlFlag.CREAD), platform + ": mapped CREAD kept");
    }
}

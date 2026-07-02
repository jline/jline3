/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Mode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the batch mode probing infrastructure: DECRQM,
 * Kitty Keyboard Protocol, and Sixel (DA1) detection.
 */
class DecModeProbeTest {

    // --- isModeSupported batch probing ---

    @Test
    void testAllModesFromBatchResponse() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed responses for all three DECRQM modes + DA1 with Sixel attribute (4)
        terminal.slaveInputPipe.write(
                ("\033[?2026;2$y\033[?2027;1$y\033[?2048;3$y\033[?64;1;2;4;6;22c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.isModeSupported(Mode.SYNCHRONIZED_OUTPUT));
        assertTrue(terminal.isModeSupported(Mode.GRAPHEME_CLUSTER));
        assertTrue(terminal.isModeSupported(Mode.IN_BAND_RESIZE));
        assertTrue(terminal.isModeSupported(Mode.SIXEL));

        // Batch query should contain all DECRQM queries and DA1
        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2026$p"));
        assertTrue(output.contains("\033[?2027$p"));
        assertTrue(output.contains("\033[?2048$p"));
        assertTrue(output.contains("\033[c"));

        terminal.close();
    }

    @Test
    void testPartialModeSupport() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Only mode 2027 supported, 2026 not recognized, 2048 permanently reset
        terminal.slaveInputPipe.write(
                ("\033[?2026;0$y\033[?2027;2$y\033[?2048;4$y\033[?64c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertFalse(terminal.isModeSupported(Mode.SYNCHRONIZED_OUTPUT));
        assertTrue(terminal.isModeSupported(Mode.GRAPHEME_CLUSTER));
        assertFalse(terminal.isModeSupported(Mode.IN_BAND_RESIZE));

        terminal.close();
    }

    @Test
    void testNoModeSupportedWhenDa1Only() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // DA1 only — terminal doesn't understand DECRQM, no Sixel attribute
        terminal.slaveInputPipe.write("\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertFalse(terminal.isModeSupported(Mode.SYNCHRONIZED_OUTPUT));
        assertFalse(terminal.isModeSupported(Mode.GRAPHEME_CLUSTER));
        assertFalse(terminal.isModeSupported(Mode.IN_BAND_RESIZE));
        assertFalse(terminal.isModeSupported(Mode.SIXEL));

        terminal.close();
    }

    @Test
    void testNoModeSupportedOnDumbTerminal() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.isModeSupported(Mode.SYNCHRONIZED_OUTPUT));
        assertFalse(terminal.isModeSupported(Mode.GRAPHEME_CLUSTER));
        assertFalse(terminal.isModeSupported(Mode.IN_BAND_RESIZE));
        assertFalse(terminal.isModeSupported(Mode.SIXEL));

        // No queries should have been sent
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    void testNoModeSupportedOnDumbColorTerminal() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB_COLOR, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.isModeSupported(Mode.SYNCHRONIZED_OUTPUT));
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    void testProbeResultsCached() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        terminal.slaveInputPipe.write(("\033[?2027;1$y\033[?64c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.isModeSupported(Mode.GRAPHEME_CLUSTER));

        // Record output length after first probe
        int firstProbeLen = masterOutput.size();

        // Second call should not re-probe
        assertTrue(terminal.isModeSupported(Mode.GRAPHEME_CLUSTER));
        assertFalse(terminal.isModeSupported(Mode.SYNCHRONIZED_OUTPUT));
        assertEquals(firstProbeLen, masterOutput.size());

        terminal.close();
    }

    @Test
    void testBatchQueryIncludesKittyQuery() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DA1 only
        terminal.slaveInputPipe.write("\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        terminal.isModeSupported(Mode.GRAPHEME_CLUSTER);

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?u"), "Batch query should include Kitty keyboard query");

        terminal.close();
    }

    @Test
    void testIsModeConsistentWithSupportsGraphemeCluster() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        terminal.slaveInputPipe.write(("\033[?2027;1$y\033[?64c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        // Both should agree when mode 2027 is supported via DECRQM
        assertTrue(terminal.isModeSupported(Mode.GRAPHEME_CLUSTER));
        assertTrue(terminal.supportsGraphemeClusterMode());

        terminal.close();
    }

    // --- Kitty Keyboard Protocol detection ---

    @Test
    void testKittyKeyboardSupported() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Kitty response (flags=1) + mode 2027 DECRPM + DA1
        terminal.slaveInputPipe.write(("\033[?1u\033[?2027;2$y\033[?64c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.isModeSupported(Mode.KITTY_KEYBOARD));
        assertTrue(terminal.isModeSupported(Mode.GRAPHEME_CLUSTER));

        terminal.close();
    }

    @Test
    void testKittyKeyboardSupportedWithHigherFlags() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Kitty flags=31 (all flags) + DA1
        terminal.slaveInputPipe.write(("\033[?31u\033[?64c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.isModeSupported(Mode.KITTY_KEYBOARD));

        terminal.close();
    }

    @Test
    void testKittyKeyboardNotSupportedWhenNoResponse() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // No Kitty response, just DA1
        terminal.slaveInputPipe.write("\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertFalse(terminal.isModeSupported(Mode.KITTY_KEYBOARD));

        terminal.close();
    }

    @Test
    void testKittyKeyboardNotSupportedOnDumbTerminal() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.isModeSupported(Mode.KITTY_KEYBOARD));
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    void testKittyKeyboardCachedWithModes() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        terminal.slaveInputPipe.write(("\033[?1u\033[?64c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        // Trigger via isModeSupported — Kitty result should also be cached
        terminal.isModeSupported(Mode.GRAPHEME_CLUSTER);
        assertTrue(terminal.isModeSupported(Mode.KITTY_KEYBOARD));

        terminal.close();
    }

    // --- Sixel (DA1 attribute 4) detection ---

    @Test
    void testSixelSupportedFromDa1() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // DA1 with attribute 4 (Sixel) among parameters
        terminal.slaveInputPipe.write(("\033[?64;1;2;4;6;22c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.isModeSupported(Mode.SIXEL));

        terminal.close();
    }

    @Test
    void testSixelSupportedWhenAttribute4AtEnd() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // DA1 with attribute 4 as last parameter
        terminal.slaveInputPipe.write(("\033[?64;4c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.isModeSupported(Mode.SIXEL));

        terminal.close();
    }

    @Test
    void testSixelSupportedWhenAttribute4IsFirstAttribute() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // DA1 with attribute 4 as first attribute after device type (Pp=1)
        terminal.slaveInputPipe.write(("\033[?1;4;6c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.isModeSupported(Mode.SIXEL));

        terminal.close();
    }

    @Test
    void testSixelNotSupportedWhenDeviceTypeIs4() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // DA1 with Pp=4 (device type), not attribute 4 — should not be treated as Sixel
        terminal.slaveInputPipe.write(("\033[?4;6c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertFalse(terminal.isModeSupported(Mode.SIXEL));

        terminal.close();
    }

    @Test
    void testSixelNotSupportedWhenNoAttribute4() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // DA1 without attribute 4
        terminal.slaveInputPipe.write(("\033[?64;1;2;6;22c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertFalse(terminal.isModeSupported(Mode.SIXEL));

        terminal.close();
    }

    @Test
    void testSixelNotSupportedOnDumbTerminal() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.isModeSupported(Mode.SIXEL));
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    void testSixelCachedWithOtherModes() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // DA1 with Sixel + DECRPM for mode 2027
        terminal.slaveInputPipe.write(("\033[?2027;1$y\033[?64;4c").getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        // Trigger probe via any mode
        assertTrue(terminal.isModeSupported(Mode.GRAPHEME_CLUSTER));

        // Sixel should also be cached from the same batch
        assertTrue(terminal.isModeSupported(Mode.SIXEL));

        terminal.close();
    }

    // --- parseDecrpm unit tests ---

    @Test
    void testParseDecrpmSupported() {
        String response = "\033[?2027;1$y\033[?64c";
        assertEquals(AbstractTerminal.ProbeResult.SUPPORTED, AbstractTerminal.parseDecrpm(response, 2027));
    }

    @Test
    void testParseDecrpmResetButSettable() {
        String response = "\033[?2026;2$y\033[?64c";
        assertEquals(AbstractTerminal.ProbeResult.SUPPORTED, AbstractTerminal.parseDecrpm(response, 2026));
    }

    @Test
    void testParseDecrpmPermanentlySet() {
        String response = "\033[?2048;3$y\033[?64c";
        assertEquals(AbstractTerminal.ProbeResult.SUPPORTED, AbstractTerminal.parseDecrpm(response, 2048));
    }

    @Test
    void testParseDecrpmNotRecognized() {
        String response = "\033[?2027;0$y\033[?64c";
        assertEquals(AbstractTerminal.ProbeResult.NOT_SUPPORTED, AbstractTerminal.parseDecrpm(response, 2027));
    }

    @Test
    void testParseDecrpmPermanentlyReset() {
        String response = "\033[?2027;4$y\033[?64c";
        assertEquals(AbstractTerminal.ProbeResult.NOT_SUPPORTED, AbstractTerminal.parseDecrpm(response, 2027));
    }

    @Test
    void testParseDecrpmModeNotPresent() {
        String response = "\033[?2027;1$y\033[?64c";
        assertEquals(AbstractTerminal.ProbeResult.NOT_SUPPORTED, AbstractTerminal.parseDecrpm(response, 2048));
    }

    @Test
    void testParseDecrpmMultipleModes() {
        String response = "\033[?2026;2$y\033[?2027;1$y\033[?2048;0$y\033[?64c";
        assertEquals(AbstractTerminal.ProbeResult.SUPPORTED, AbstractTerminal.parseDecrpm(response, 2026));
        assertEquals(AbstractTerminal.ProbeResult.SUPPORTED, AbstractTerminal.parseDecrpm(response, 2027));
        assertEquals(AbstractTerminal.ProbeResult.NOT_SUPPORTED, AbstractTerminal.parseDecrpm(response, 2048));
    }

    // --- parseKittyResponse unit tests ---

    @Test
    void testParseKittyResponsePresent() {
        assertTrue(AbstractTerminal.parseKittyResponse("\033[?1u\033[?2027;1$y\033[?64c"));
    }

    @Test
    void testParseKittyResponseHighFlags() {
        assertTrue(AbstractTerminal.parseKittyResponse("\033[?31u\033[?64c"));
    }

    @Test
    void testParseKittyResponseZeroFlags() {
        assertTrue(AbstractTerminal.parseKittyResponse("\033[?0u\033[?64c"));
    }

    @Test
    void testParseKittyResponseAbsent() {
        assertFalse(AbstractTerminal.parseKittyResponse("\033[?2027;1$y\033[?64c"));
    }

    @Test
    void testParseKittyResponseDoesNotMatchDecrpm() {
        // CSI ? 2027 ; 1 $ y  — digits are followed by ';', not 'u'
        assertFalse(AbstractTerminal.parseKittyResponse("\033[?2027;1$y\033[?64c"));
    }

    @Test
    void testParseKittyResponseEmptyString() {
        assertFalse(AbstractTerminal.parseKittyResponse(""));
    }

    // --- parseSixelFromDa1 unit tests ---

    @Test
    void testParseSixelFromDa1MiddleParam() {
        // Attribute 4 in the middle of the parameter list
        assertTrue(AbstractTerminal.parseSixelFromDa1("\033[?64;1;2;4;6;22c"));
    }

    @Test
    void testParseSixelFromDa1LastParam() {
        // Attribute 4 at the end: ;4c
        assertTrue(AbstractTerminal.parseSixelFromDa1("\033[?64;4c"));
    }

    @Test
    void testParseSixelFromDa1FirstAttribute() {
        // Attribute 4 as first attribute after device type (Pp=1)
        assertTrue(AbstractTerminal.parseSixelFromDa1("\033[?1;4;6c"));
    }

    @Test
    void testParseSixelFromDa1DeviceTypeNotAttribute() {
        // Pp=4 is the device type, NOT attribute 4 — must not match
        assertFalse(AbstractTerminal.parseSixelFromDa1("\033[?4c"));
    }

    @Test
    void testParseSixelFromDa1DeviceType4WithOtherAttrs() {
        // Pp=4 with attributes=[6] — device type 4 is not attribute 4
        assertFalse(AbstractTerminal.parseSixelFromDa1("\033[?4;6c"));
    }

    @Test
    void testParseSixelFromDa1Absent() {
        // No attribute 4
        assertFalse(AbstractTerminal.parseSixelFromDa1("\033[?64;1;2;6;22c"));
    }

    @Test
    void testParseSixelFromDa1DoesNotMatchSubstring() {
        // "64" contains "4" but not ";4;" or ";4c"
        assertFalse(AbstractTerminal.parseSixelFromDa1("\033[?64c"));
    }

    @Test
    void testParseSixelFromDa1EmptyString() {
        assertFalse(AbstractTerminal.parseSixelFromDa1(""));
    }

    @Test
    void testParseSixelFromDa1WithDecrpmInResponse() {
        // Combined response: DECRPM with Ps=4 (permanently reset) + DA1 without Sixel
        // The ";4$" in DECRPM should NOT match ";4;" or ";4c"
        assertFalse(AbstractTerminal.parseSixelFromDa1("\033[?2048;4$y\033[?64c"));
    }

    @Test
    void testParseSixelFromDa1WithDecrpmAndSixel() {
        // Combined response: DECRPM + DA1 with Sixel attribute
        assertTrue(AbstractTerminal.parseSixelFromDa1("\033[?2027;1$y\033[?64;4c"));
    }
}

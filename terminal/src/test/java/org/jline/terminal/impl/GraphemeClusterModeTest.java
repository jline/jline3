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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Size;
import org.jline.terminal.Sized;
import org.jline.terminal.Terminal;
import org.jline.utils.ColorPalette;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GraphemeClusterModeTest {

    // Test emoji for isClusterGrouped assertions
    private static final String FLAG_FR = "\uD83C\uDDEB\uD83C\uDDF7"; // 🇫🇷
    private static final String ZWJ_EMOJI = "\uD83D\uDC69\u200D\uD83D\uDD2C"; // 👩‍🔬

    @Test
    void testSupportedWhenTerminalRespondsSet() throws Exception {
        // DECRPM response: mode 2027 currently set (Ps=1)
        assertProbeResult("\033[?2027;1$y", "xterm-256color", true);
    }

    @Test
    void testSupportedWhenTerminalRespondsReset() throws Exception {
        // DECRPM response: mode 2027 currently reset but recognized (Ps=2)
        assertProbeResult("\033[?2027;2$y", "xterm-256color", true);
    }

    @Test
    void testSupportedWhenTerminalRespondsPermanentlySet() throws Exception {
        // DECRPM response: mode 2027 permanently set (Ps=3)
        assertProbeResult("\033[?2027;3$y", "xterm-256color", true);
    }

    @Test
    void testNotSupportedWhenTerminalRespondsNotRecognized() throws Exception {
        // DECRPM response: mode 2027 not recognized (Ps=0)
        assertProbeResult("\033[?2027;0$y", "xterm-256color", false);
    }

    @Test
    void testNotSupportedWhenTerminalRespondsPermanentlyReset() throws Exception {
        // DECRPM response: mode 2027 permanently reset (Ps=4)
        assertProbeResult("\033[?2027;4$y", "xterm-256color", false);
    }

    @Test
    void testNotSupportedOnDumbTerminal() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.supportsGraphemeClusterMode());
        // No query should have been sent
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    void testNotSupportedOnDumbColorTerminal() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB_COLOR, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.supportsGraphemeClusterMode());
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    void testNotSupportedWhenOnlyDa1Responds() throws Exception {
        // Terminal doesn't support DECRQM but responds to DA1 sentinel
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DA1 response (no DECRPM), then CPR col=5 for cursor probe fallback
        terminal.slaveInputPipe.write("\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        // Cursor probe runs after DECRQM fails — feed CPR (no clustering)
        ResponderHandle cprResponder = startCprResponder(terminal, masterOutput);
        cprResponder.start();

        assertFalse(terminal.supportsGraphemeClusterMode());

        cprResponder.joinAndAssert();
        terminal.close();
    }

    @Test
    void testProbeSentForNonXtermTerminal() throws Exception {
        // Non-xterm terminals should now be probed (DA1 sentinel makes it safe)
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "vt100", masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.supportsGraphemeClusterMode());
        // Probe should have been sent
        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027$p"));

        terminal.close();
    }

    @Test
    void testNotSupportedWhenNoResponse() throws Exception {
        // No response written to slaveInputPipe → peek() times out
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.supportsGraphemeClusterMode());

        // Query and DA1 sentinel should have been sent
        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027$p"));
        assertTrue(output.contains("\033[c"));

        terminal.close();
    }

    @Test
    void testResultIsCached() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DECRPM response followed by mock DA1 response
        terminal.slaveInputPipe.write("\033[?2027;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.supportsGraphemeClusterMode());
        // Second call should return cached result without re-probing
        assertTrue(terminal.supportsGraphemeClusterMode());

        terminal.close();
    }

    @Test
    void testSetGraphemeClusterModeEnable() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DECRPM response followed by mock DA1 response
        terminal.slaveInputPipe.write("\033[?2027;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.setGraphemeClusterMode(true, false));
        assertTrue(terminal.getGraphemeClusterMode());

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027h"));

        terminal.close();
    }

    @Test
    void testSetGraphemeClusterModeDisable() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DECRPM response followed by mock DA1 response
        terminal.slaveInputPipe.write("\033[?2027;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.setGraphemeClusterMode(true, false));
        assertTrue(terminal.getGraphemeClusterMode());

        assertTrue(terminal.setGraphemeClusterMode(false, false));
        assertFalse(terminal.getGraphemeClusterMode());

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027l"));

        terminal.close();
    }

    @Test
    void testSetGraphemeClusterModeReturnsFalseWhenNotSupported() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.setGraphemeClusterMode(true, false));
        assertFalse(terminal.getGraphemeClusterMode());

        terminal.close();
    }

    @Test
    void testModeDisabledOnClose() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DECRPM response followed by mock DA1 response
        terminal.slaveInputPipe.write("\033[?2027;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.setGraphemeClusterMode(true, false));
        assertTrue(terminal.getGraphemeClusterMode());

        // Close should disable the mode
        terminal.close();

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        // Should contain both enable and disable sequences
        assertTrue(output.contains("\033[?2027h"));
        assertTrue(output.contains("\033[?2027l"));
    }

    @Test
    void testModeNotDisabledOnCloseIfNeverEnabled() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DECRPM response followed by mock DA1 response
        terminal.slaveInputPipe.write("\033[?2027;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.supportsGraphemeClusterMode());

        terminal.close();

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        // Should contain probe but no enable/disable sequences
        assertTrue(output.contains("\033[?2027$p"));
        assertFalse(output.contains("\033[?2027h"));
        assertFalse(output.contains("\033[?2027l"));
    }

    @Test
    void testDefaultInterfaceMethodsReturnFalse() {
        // Verify the Terminal interface default methods
        Terminal terminal = new Terminal() {
            public String getName() {
                return "test";
            }

            public SignalHandler handle(Signal signal, SignalHandler handler) {
                return null;
            }

            public void raise(Signal signal) {}

            public NonBlockingReader reader() {
                return null;
            }

            public PrintWriter writer() {
                return null;
            }

            public Charset encoding() {
                return StandardCharsets.UTF_8;
            }

            public InputStream input() {
                return null;
            }

            public OutputStream output() {
                return null;
            }

            public boolean canPauseResume() {
                return false;
            }

            public void pause() {}

            public void pause(boolean wait) {}

            public void resume() {}

            public boolean paused() {
                return false;
            }

            public Attributes enterRawMode() {
                return null;
            }

            public boolean echo() {
                return false;
            }

            public boolean echo(boolean echo) {
                return false;
            }

            public Attributes getAttributes() {
                return null;
            }

            public void setAttributes(Attributes attr) {}

            public Size getSize() {
                return Size.of(80, 24);
            }

            public void setSize(Sized size) {}

            public void flush() {}

            public String getType() {
                return "test";
            }

            public boolean puts(Capability capability, Object... params) {
                return false;
            }

            public boolean getBooleanCapability(Capability capability) {
                return false;
            }

            public Integer getNumericCapability(Capability capability) {
                return null;
            }

            public String getStringCapability(Capability capability) {
                return null;
            }

            public Cursor getCursorPosition(IntConsumer discarded) {
                return null;
            }

            public boolean hasMouseSupport() {
                return false;
            }

            public boolean trackMouse(MouseTracking tracking) {
                return false;
            }

            public MouseTracking getCurrentMouseTracking() {
                return MouseTracking.Off;
            }

            public MouseEvent readMouseEvent() {
                return null;
            }

            public MouseEvent readMouseEvent(IntSupplier reader) {
                return null;
            }

            public MouseEvent readMouseEvent(String prefix) {
                return null;
            }

            public MouseEvent readMouseEvent(IntSupplier reader, String prefix) {
                return null;
            }

            public boolean hasFocusSupport() {
                return false;
            }

            public boolean trackFocus(boolean tracking) {
                return false;
            }

            public ColorPalette getPalette() {
                return null;
            }

            public void close() {}
        };

        assertFalse(terminal.supportsGraphemeClusterMode());
        assertFalse(terminal.getGraphemeClusterMode());
        assertFalse(terminal.setGraphemeClusterMode(true, false));
    }

    // --- Force-enable via setGraphemeClusterMode(true, true) ---

    @Test
    void testForceEnableSkipsProbing() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Force-enable without any terminal responses
        assertTrue(terminal.setGraphemeClusterMode(true, true));
        assertTrue(terminal.getGraphemeClusterMode());
        assertTrue(terminal.supportsGraphemeClusterMode());

        // Should not send Mode 2027 enable sequence
        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains("\033[?2027h"), "Force-enable should not send Mode 2027 escape");

        terminal.close();
    }

    // --- Cursor position probe fallback tests ---

    @Test
    void testCursorProbeDetectsGraphemeClusters() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Background thread feeds responses when probe queries appear in output
        ResponderHandle responder = startResponder(terminal, masterOutput);
        responder.start();

        assertTrue(terminal.supportsGraphemeClusterMode());
        // Native detection auto-enables grapheme cluster mode
        assertTrue(terminal.getGraphemeClusterMode());

        responder.joinAndAssert();
        terminal.close();
    }

    @Test
    void testCursorProbeNotSupportedWhenNoGrouping() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DA1 for DECRQM failure, then 4 CPR col=5 (no grouping for any category)
        ResponderHandle responder = startResponder(terminal, masterOutput, NONE_GROUPED);
        responder.start();

        assertFalse(terminal.supportsGraphemeClusterMode());
        assertFalse(terminal.getGraphemeClusterMode());

        responder.joinAndAssert();
        terminal.close();
    }

    @Test
    void testCursorProbeNoResponseFallsBack() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // No responses at all — both probes time out
        assertFalse(terminal.supportsGraphemeClusterMode());
        assertFalse(terminal.getGraphemeClusterMode());

        terminal.close();
    }

    @Test
    void testCursorProbeNativeSkipsEscapeSequences() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        ResponderHandle responder = startResponder(terminal, masterOutput);
        responder.start();

        assertTrue(terminal.supportsGraphemeClusterMode());
        assertTrue(terminal.getGraphemeClusterMode());
        responder.joinAndAssert();

        // setGraphemeClusterMode should succeed but not send Mode 2027 escapes
        assertTrue(terminal.setGraphemeClusterMode(true, false));
        assertTrue(terminal.setGraphemeClusterMode(false, false));

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        // Should NOT contain Mode 2027 enable/disable sequences
        assertFalse(output.contains("\033[?2027h"), "Should not send Mode 2027 enable for native mode");
        assertFalse(output.contains("\033[?2027l"), "Should not send Mode 2027 disable for native mode");

        terminal.close();
    }

    @Test
    void testCursorProbeNativeCloseDoesNotSendDisable() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        ResponderHandle responder = startResponder(terminal, masterOutput);
        responder.start();

        assertTrue(terminal.supportsGraphemeClusterMode());
        assertTrue(terminal.getGraphemeClusterMode());
        responder.joinAndAssert();

        terminal.close();

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains("\033[?2027l"), "Close should not send Mode 2027 disable for native mode");
    }

    // CPR responses: initial startCol query + flag probe + ZWJ probe.
    // Displacement of 2 = grouped (single cluster), 4 = ungrouped.

    /** All grouped: start=1, flag=3 (delta=2), ZWJ=3 (delta=2). */
    private static final String ALL_GROUPED = "\033[1;1R\033[1;3R\033[1;3R";

    /** None grouped: start=1, flag=5 (delta=4), ZWJ=5 (delta=4). */
    private static final String NONE_GROUPED = "\033[1;1R\033[1;5R\033[1;5R";

    private ResponderHandle startResponder(LineDisciplineTerminal terminal, ByteArrayOutputStream masterOutput) {
        return startResponder(terminal, masterOutput, ALL_GROUPED);
    }

    /**
     * Starts a responder thread that feeds DA1 (for DECRQM failure) and CPR
     * responses to the terminal's slave input pipe when the corresponding
     * probe queries appear in the master output. The returned {@code error}
     * reference captures any exception so tests can assert the responder
     * completed cleanly.
     */
    private ResponderHandle startResponder(
            LineDisciplineTerminal terminal, ByteArrayOutputStream masterOutput, String cprResponses) {
        AtomicReference<Exception> error = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                // Wait for DECRQM query, then feed DA1 response (not supported)
                waitForOutput(masterOutput, "\033[?2027$p");
                terminal.slaveInputPipe.write("\033[?64c".getBytes(StandardCharsets.UTF_8));
                terminal.slaveInputPipe.flush();

                // Wait for DSR query, then feed CPR responses
                waitForOutput(masterOutput, "\033[6n");
                terminal.slaveInputPipe.write(cprResponses.getBytes(StandardCharsets.UTF_8));
                terminal.slaveInputPipe.flush();
            } catch (Exception e) {
                error.set(e);
            }
        });
        t.setDaemon(true);
        return new ResponderHandle(t, error);
    }

    private static class ResponderHandle {
        final Thread thread;
        final AtomicReference<Exception> error;

        ResponderHandle(Thread thread, AtomicReference<Exception> error) {
            this.thread = thread;
            this.error = error;
        }

        void start() {
            thread.start();
        }

        void joinAndAssert() throws Exception {
            thread.join(5000);
            assertFalse(thread.isAlive(), "Responder thread should have completed");
            Exception e = error.get();
            if (e != null) {
                throw new AssertionError("Responder thread failed", e);
            }
        }
    }

    /**
     * Starts a responder thread that only feeds a CPR response (no DA1) when
     * the DSR query appears in the master output. Used when the DECRPM/DA1
     * response is already pre-loaded in the pipe.
     */
    private ResponderHandle startCprResponder(LineDisciplineTerminal terminal, ByteArrayOutputStream masterOutput) {
        AtomicReference<Exception> error = new AtomicReference<>();
        Thread t = new Thread(() -> {
            try {
                waitForOutput(masterOutput, "\033[6n");
                terminal.slaveInputPipe.write(NONE_GROUPED.getBytes(StandardCharsets.UTF_8));
                terminal.slaveInputPipe.flush();
            } catch (Exception e) {
                error.set(e);
            }
        });
        t.setDaemon(true);
        return new ResponderHandle(t, error);
    }

    @SuppressWarnings("java:S2925") // ByteArrayOutputStream has no notification mechanism; polling is necessary
    private void waitForOutput(ByteArrayOutputStream masterOutput, String trigger) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!masterOutput.toString(StandardCharsets.UTF_8).contains(trigger)) {
            if (System.currentTimeMillis() > deadline) {
                throw new RuntimeException("Timed out waiting for: " + trigger);
            }
            Thread.sleep(5);
        }
    }

    // --- Cursor probe failure mode tests ---

    @Test
    void testCursorProbeReturnsNullWhenTerminalLacksU6U7() throws Exception {
        // screen-256color has no u6/u7 capabilities, so getCursorPosition returns null
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "screen-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DA1 response (DECRQM not supported) so cursor probe is attempted
        terminal.slaveInputPipe.write("\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertFalse(terminal.supportsGraphemeClusterMode());
        assertFalse(terminal.getGraphemeClusterMode());

        // Verify DECRQM was sent but no Mode 2027 enable
        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027$p"));
        assertFalse(output.contains("\033[?2027h"));

        terminal.close();
    }

    @Test
    void testCursorProbeReturnsNullOnEof() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DA1 response, then close the pipe before the CPR response arrives
        // so getCursorPosition reads EOF and returns null
        ResponderHandle responder = new ResponderHandle(
                new Thread(() -> {
                    try {
                        waitForOutput(masterOutput, "\033[6n");
                        terminal.slaveInputPipe.close();
                    } catch (Exception e) {
                        // pipe close may throw; ignore since the test validates the outcome
                    }
                }),
                new AtomicReference<>());
        responder.start();

        // Pre-load DA1 for DECRQM failure
        terminal.slaveInputPipe.write("\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertFalse(terminal.supportsGraphemeClusterMode());
        assertFalse(terminal.getGraphemeClusterMode());

        responder.joinAndAssert();
        terminal.close();
    }

    @Test
    void testCursorProbeUnexpectedPosition() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DA1 for DECRQM failure, then start CPR + 2 CPR with unexpected width (delta=3)
        ResponderHandle responder = startResponder(terminal, masterOutput, "\033[1;1R\033[1;4R\033[1;4R");
        responder.start();

        assertFalse(terminal.supportsGraphemeClusterMode());
        assertFalse(terminal.getGraphemeClusterMode());

        responder.joinAndAssert();
        terminal.close();
    }

    @Test
    void testCursorProbeWorksWhenCursorNotAtColumnOne() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Cursor starts at column 10 (e.g., after a prompt); grouped = displacement of 2
        String nonZeroStart = "\033[1;10R\033[1;12R\033[1;12R";
        ResponderHandle responder = startResponder(terminal, masterOutput, nonZeroStart);
        responder.start();

        assertTrue(terminal.supportsGraphemeClusterMode());
        assertTrue(terminal.getGraphemeClusterMode());
        assertTrue(terminal.isClusterGrouped(FLAG_FR, 0, FLAG_FR.length()));
        assertTrue(terminal.isClusterGrouped(ZWJ_EMOJI, 0, ZWJ_EMOJI.length()));

        responder.joinAndAssert();
        terminal.close();
    }

    @Test
    void testPartialGroupingFlagsOnly() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Flag grouped (delta=2), ZWJ not grouped (delta=4) — e.g. Tabby, Alacritty
        String flagsOnly = "\033[1;1R\033[1;3R\033[1;5R";
        ResponderHandle responder = startResponder(terminal, masterOutput, flagsOnly);
        responder.start();

        assertTrue(terminal.supportsGraphemeClusterMode());
        assertTrue(terminal.getGraphemeClusterMode());

        // Flag grouped, ZWJ not grouped
        assertTrue(terminal.isClusterGrouped(FLAG_FR, 0, FLAG_FR.length()));
        assertFalse(terminal.isClusterGrouped(ZWJ_EMOJI, 0, ZWJ_EMOJI.length()));

        responder.joinAndAssert();
        terminal.close();
    }

    @Test
    void testAllGroupedWhenMode2027() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DECRPM response (mode 2027 set) + DA1
        terminal.slaveInputPipe.write("\033[?2027;1$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.setGraphemeClusterMode(true, false));
        assertTrue(terminal.isClusterGrouped(FLAG_FR, 0, FLAG_FR.length()));
        assertTrue(terminal.isClusterGrouped(ZWJ_EMOJI, 0, ZWJ_EMOJI.length()));

        terminal.close();
    }

    @Test
    void testNothingGroupedWhenNotSupported() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.isClusterGrouped(FLAG_FR, 0, FLAG_FR.length()));
        assertFalse(terminal.isClusterGrouped(ZWJ_EMOJI, 0, ZWJ_EMOJI.length()));

        terminal.close();
    }

    @Test
    void testForceEnableGroupsAll() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        assertTrue(terminal.setGraphemeClusterMode(true, true));
        assertTrue(terminal.isClusterGrouped(FLAG_FR, 0, FLAG_FR.length()));
        assertTrue(terminal.isClusterGrouped(ZWJ_EMOJI, 0, ZWJ_EMOJI.length()));

        terminal.close();
    }

    @Test
    void testForceDisableThenNonForceEnablePreservesGrouping() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Force-enable (marks capability as supported/native, sets grouping flags)
        assertTrue(terminal.setGraphemeClusterMode(true, true));
        assertTrue(terminal.getGraphemeClusterMode());
        assertTrue(terminal.isClusterGrouped(FLAG_FR, 0, FLAG_FR.length()));

        // Force-disable (should NOT clear grouping capability flags, but
        // isClusterGrouped should return false while mode is disabled)
        assertTrue(terminal.setGraphemeClusterMode(false, true));
        assertFalse(terminal.getGraphemeClusterMode());
        assertFalse(terminal.isClusterGrouped(FLAG_FR, 0, FLAG_FR.length()));
        assertFalse(terminal.isClusterGrouped(ZWJ_EMOJI, 0, ZWJ_EMOJI.length()));

        // Non-force enable should succeed (supportsGraphemeClusterMode is true)
        // and should NOT send Mode 2027 escapes (graphemeClusterNative is true)
        masterOutput.reset();
        assertTrue(terminal.setGraphemeClusterMode(true, false));
        assertTrue(terminal.getGraphemeClusterMode());
        assertTrue(terminal.isClusterGrouped(FLAG_FR, 0, FLAG_FR.length()));
        assertTrue(terminal.isClusterGrouped(ZWJ_EMOJI, 0, ZWJ_EMOJI.length()));

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertFalse(output.contains("\033[?2027h"), "Native mode should not send Mode 2027 enable");

        terminal.close();
    }

    @Test
    void testForceDisableSendsMode2027DisableWhenPreviouslyEnabled() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Enable Mode 2027 via normal (non-force) path
        terminal.slaveInputPipe.write("\033[?2027;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();
        assertTrue(terminal.setGraphemeClusterMode(true, false));
        assertTrue(terminal.getGraphemeClusterMode());

        // Force-disable should send Mode 2027 disable escape before switching to native
        masterOutput.reset();
        assertTrue(terminal.setGraphemeClusterMode(false, true));
        assertFalse(terminal.getGraphemeClusterMode());

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027l"), "Force-disable should send Mode 2027 disable");

        // isClusterGrouped returns false while mode is disabled,
        // but capability flags are preserved internally for re-enabling
        assertFalse(terminal.isClusterGrouped(FLAG_FR, 0, FLAG_FR.length()));
        assertFalse(terminal.isClusterGrouped(ZWJ_EMOJI, 0, ZWJ_EMOJI.length()));

        terminal.close();
    }

    private void assertProbeResult(String response, String terminalType, boolean expectedSupport) throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", terminalType, masterOutput, StandardCharsets.UTF_8)) {

            // Feed the DECRPM response followed by mock DA1 response
            terminal.slaveInputPipe.write((response + "\033[?64c").getBytes(StandardCharsets.UTF_8));
            terminal.slaveInputPipe.flush();

            // When DECRPM says not supported, cursor probe runs as fallback
            ResponderHandle cprResponder = null;
            if (!expectedSupport) {
                cprResponder = startCprResponder(terminal, masterOutput);
                cprResponder.start();
            }

            assertEquals(expectedSupport, terminal.supportsGraphemeClusterMode());

            if (cprResponder != null) {
                cprResponder.joinAndAssert();
            }

            // Verify the DECRQM query and DA1 sentinel were sent
            String output = masterOutput.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("\033[?2027$p"));
            assertTrue(output.contains("\033[c"));
        }
    }
}

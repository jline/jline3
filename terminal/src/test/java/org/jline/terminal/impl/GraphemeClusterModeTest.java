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
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.jline.terminal.Attributes;
import org.jline.terminal.Cursor;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.utils.ColorPalette;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GraphemeClusterModeTest {

    @Test
    public void testSupportedWhenTerminalRespondsSet() throws Exception {
        // DECRPM response: mode 2027 currently set (Ps=1)
        assertProbeResult("\033[?2027;1$y", "xterm-256color", true);
    }

    @Test
    public void testSupportedWhenTerminalRespondsReset() throws Exception {
        // DECRPM response: mode 2027 currently reset but recognized (Ps=2)
        assertProbeResult("\033[?2027;2$y", "xterm-256color", true);
    }

    @Test
    public void testSupportedWhenTerminalRespondsPermanentlySet() throws Exception {
        // DECRPM response: mode 2027 permanently set (Ps=3)
        assertProbeResult("\033[?2027;3$y", "xterm-256color", true);
    }

    @Test
    public void testNotSupportedWhenTerminalRespondsNotRecognized() throws Exception {
        // DECRPM response: mode 2027 not recognized (Ps=0)
        assertProbeResult("\033[?2027;0$y", "xterm-256color", false);
    }

    @Test
    public void testNotSupportedWhenTerminalRespondsPermanentlyReset() throws Exception {
        // DECRPM response: mode 2027 permanently reset (Ps=4)
        assertProbeResult("\033[?2027;4$y", "xterm-256color", false);
    }

    @Test
    public void testNotSupportedOnDumbTerminal() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.supportsGraphemeClusterMode());
        // No query should have been sent
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    public void testNotSupportedOnDumbColorTerminal() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB_COLOR, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.supportsGraphemeClusterMode());
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    public void testNotSupportedOnNonXtermTerminal() throws Exception {
        // Terminals that don't start with "xterm" should not be probed
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "vt100", masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.supportsGraphemeClusterMode());
        // No query should have been sent
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    public void testNotSupportedWhenNoResponse() throws Exception {
        // No response written to slaveInputPipe → peek() times out
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.supportsGraphemeClusterMode());

        // Query should still have been sent
        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027$p"));

        terminal.close();
    }

    @Test
    public void testResultIsCached() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed response
        terminal.slaveInputPipe.write("\033[?2027;2$y".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.supportsGraphemeClusterMode());
        // Second call should return cached result without re-probing
        assertTrue(terminal.supportsGraphemeClusterMode());

        terminal.close();
    }

    @Test
    public void testSetGraphemeClusterModeEnable() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed probe response
        terminal.slaveInputPipe.write("\033[?2027;2$y".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.setGraphemeClusterMode(true));
        assertTrue(terminal.getGraphemeClusterMode());

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027h"));

        terminal.close();
    }

    @Test
    public void testSetGraphemeClusterModeDisable() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed probe response
        terminal.slaveInputPipe.write("\033[?2027;2$y".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.setGraphemeClusterMode(true));
        assertTrue(terminal.getGraphemeClusterMode());

        assertTrue(terminal.setGraphemeClusterMode(false));
        assertFalse(terminal.getGraphemeClusterMode());

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027l"));

        terminal.close();
    }

    @Test
    public void testSetGraphemeClusterModeReturnsFalseWhenNotSupported() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.setGraphemeClusterMode(true));
        assertFalse(terminal.getGraphemeClusterMode());

        terminal.close();
    }

    @Test
    public void testModeDisabledOnClose() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed probe response
        terminal.slaveInputPipe.write("\033[?2027;2$y".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.setGraphemeClusterMode(true));
        assertTrue(terminal.getGraphemeClusterMode());

        // Close should disable the mode
        terminal.close();

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        // Should contain both enable and disable sequences
        assertTrue(output.contains("\033[?2027h"));
        assertTrue(output.contains("\033[?2027l"));
    }

    @Test
    public void testModeNotDisabledOnCloseIfNeverEnabled() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed probe response but don't enable the mode
        terminal.slaveInputPipe.write("\033[?2027;2$y".getBytes(StandardCharsets.UTF_8));
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
    public void testDefaultInterfaceMethodsReturnFalse() {
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
                return new Size(80, 24);
            }

            public void setSize(Size size) {}

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
        assertFalse(terminal.setGraphemeClusterMode(true));
    }

    private void assertProbeResult(String response, String terminalType, boolean expectedSupport) throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", terminalType, masterOutput, StandardCharsets.UTF_8);

        // Feed the DECRPM response into the slave input pipe
        terminal.slaveInputPipe.write(response.getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertEquals(expectedSupport, terminal.supportsGraphemeClusterMode());

        // Verify the DECRQM query was sent
        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2027$p"));

        terminal.close();
    }
}

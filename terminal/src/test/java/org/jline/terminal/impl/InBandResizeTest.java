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
import org.jline.terminal.Sized;
import org.jline.terminal.Terminal;
import org.jline.utils.ColorPalette;
import org.jline.utils.InfoCmp.Capability;
import org.jline.utils.NonBlockingReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for in-band window resize support (mode 2048) via the
 * {@link Terminal#hasInBandResizeSupport()} and
 * {@link Terminal#trackInBandResize(boolean)} API.
 *
 * <p>Low-level DECRPM parsing and batch probe mechanics are covered
 * by {@link DecModeProbeTest}; this class focuses on the higher-level
 * enable/disable/close lifecycle.</p>
 */
class InBandResizeTest {

    @Test
    void testSupportedWhenBatchProbeFindsMode2048() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        // Feed DECRPM response for mode 2048 (Ps=2 → recognized, reset) + DA1
        terminal.slaveInputPipe.write("\033[?2048;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.hasInBandResizeSupport());

        terminal.close();
    }

    @Test
    void testNotSupportedOnDumbTerminal() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.hasInBandResizeSupport());
        // No query should have been sent
        assertEquals(0, masterOutput.size());

        terminal.close();
    }

    @Test
    void testNotSupportedWhenNoResponse() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.hasInBandResizeSupport());

        // Batch query and DA1 sentinel should have been sent
        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2048$p"));
        assertTrue(output.contains("\033[c"));

        terminal.close();
    }

    @Test
    void testTrackInBandResizeEnable() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        terminal.slaveInputPipe.write("\033[?2048;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.trackInBandResize(true));

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2048h"));

        terminal.close();
    }

    @Test
    void testTrackInBandResizeDisable() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        terminal.slaveInputPipe.write("\033[?2048;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.trackInBandResize(true));
        assertTrue(terminal.trackInBandResize(false));

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2048h"));
        assertTrue(output.contains("\033[?2048l"));

        terminal.close();
    }

    @Test
    void testTrackInBandResizeReturnsFalseWhenNotSupported() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", Terminal.TYPE_DUMB, masterOutput, StandardCharsets.UTF_8);

        assertFalse(terminal.trackInBandResize(true));

        terminal.close();
    }

    @Test
    void testModeDisabledOnClose() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        terminal.slaveInputPipe.write("\033[?2048;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.trackInBandResize(true));

        // Close should disable the mode
        terminal.close();

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2048h"));
        assertTrue(output.contains("\033[?2048l"));
    }

    @Test
    void testModeNotDisabledOnCloseIfNeverEnabled() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8);

        terminal.slaveInputPipe.write("\033[?2048;2$y\033[?64c".getBytes(StandardCharsets.UTF_8));
        terminal.slaveInputPipe.flush();

        assertTrue(terminal.hasInBandResizeSupport());

        terminal.close();

        String output = masterOutput.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains("\033[?2048$p"));
        assertFalse(output.contains("\033[?2048h"));
        assertFalse(output.contains("\033[?2048l"));
    }

    @Test
    void testDefaultInterfaceMethodsReturnFalse() {
        Terminal terminal = new Terminal() {
            public String getName() {
                return "test";
            }

            public SignalHandler handle(Signal signal, SignalHandler handler) {
                return null;
            }

            public void raise(Signal signal) {
                // no-op for stub
            }

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

            public void pause() {
                // no-op for stub
            }

            public void pause(boolean wait) {
                // no-op for stub
            }

            public void resume() {
                // no-op for stub
            }

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

            public void setAttributes(Attributes attr) {
                // no-op for stub
            }

            public Size getSize() {
                return Size.of(80, 24);
            }

            public void setSize(Sized size) {
                // no-op for stub
            }

            public void flush() {
                // no-op for stub
            }

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

            public void close() {
                // no-op for stub
            }
        };

        assertFalse(terminal.hasInBandResizeSupport());
        assertFalse(terminal.trackInBandResize(true));
    }
}

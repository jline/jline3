/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.keymap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jline.reader.impl.ReaderTestSupport.EofPipedInputStream;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link InBandResize} — the shared utility for parsing
 * DEC private mode 2048 in-band resize reports.
 *
 * <p>These tests exercise the static helper methods directly, without
 * going through {@code LineReaderImpl} or any builtin.  The existing
 * {@code InBandResizeWidgetTest} covers the integration path through
 * the {@code terminal-resize} widget.</p>
 */
class InBandResizeTest {

    private Terminal terminal;
    private EofPipedInputStream in;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setUp() throws Exception {
        in = new EofPipedInputStream();
        out = new ByteArrayOutputStream();
        terminal = new DumbTerminal("dumb", "dumb", in, out, StandardCharsets.UTF_8);
        terminal.setSize(Size.of(160, 80));
    }

    @AfterEach
    void tearDown() throws IOException {
        if (terminal != null) {
            terminal.close();
        }
    }

    private BindingReader readerFor(String input) {
        in.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
        return new BindingReader(terminal.reader());
    }

    // ---- readResizeParams ----

    @Test
    void readResizeParams_rowsAndCols() {
        BindingReader reader = readerFor("24;80t");
        assertEquals("24;80", InBandResize.readResizeParams(reader));
    }

    @Test
    void readResizeParams_withPixelDimensions() {
        BindingReader reader = readerFor("30;120;480;960t");
        assertEquals("30;120;480;960", InBandResize.readResizeParams(reader));
    }

    @Test
    void readResizeParams_malformedCharReturnsNull() {
        // 'x' is invalid — drained to 't', returns null
        BindingReader reader = readerFor("10x30;80t");
        assertNull(InBandResize.readResizeParams(reader));
    }

    @Test
    void readResizeParams_tooLongReturnsNull() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            sb.append('1');
        }
        sb.append('t');
        BindingReader reader = readerFor(sb.toString());
        assertNull(InBandResize.readResizeParams(reader));
    }

    @Test
    void readResizeParams_emptyParamsBeforeT() {
        BindingReader reader = readerFor("t");
        assertEquals("", InBandResize.readResizeParams(reader));
    }

    @Test
    void readResizeParams_eofWithoutT() {
        // No terminating 't' — EOF reached
        BindingReader reader = readerFor("24;80");
        assertNull(InBandResize.readResizeParams(reader));
    }

    @Test
    void readResizeParams_consumesExactlyUpToT() {
        // After reading params up to 't', remaining data should still be readable
        BindingReader reader = readerFor("24;80tAB");
        assertEquals("24;80", InBandResize.readResizeParams(reader));
        // 'A' and 'B' should remain
        assertEquals('A', reader.readCharacter());
        assertEquals('B', reader.readCharacter());
    }

    // ---- applyResizeParams ----

    @Test
    void applyResizeParams_updatesTerminalSize() {
        InBandResize.applyResizeParams("30;120", terminal);
        assertEquals(Size.of(120, 30), terminal.getSize());
    }

    @Test
    void applyResizeParams_withPixelDimensions() {
        // Pixel dimensions are ignored — only rows and cols applied
        InBandResize.applyResizeParams("50;200;800;1600", terminal);
        assertEquals(Size.of(200, 50), terminal.getSize());
    }

    @Test
    void applyResizeParams_raisesWINCH() {
        List<Signal> signals = new ArrayList<>();
        terminal.handle(Signal.WINCH, signals::add);

        InBandResize.applyResizeParams("25;80", terminal);

        assertEquals(1, signals.size());
        assertEquals(Signal.WINCH, signals.get(0));
    }

    @Test
    void applyResizeParams_tooFewPartsIgnored() {
        Size before = terminal.getSize();
        InBandResize.applyResizeParams("30", terminal);
        assertEquals(before, terminal.getSize());
    }

    @Test
    void applyResizeParams_emptyStringIgnored() {
        Size before = terminal.getSize();
        InBandResize.applyResizeParams("", terminal);
        assertEquals(before, terminal.getSize());
    }

    @Test
    void applyResizeParams_zeroRowsIgnored() {
        Size before = terminal.getSize();
        InBandResize.applyResizeParams("0;80", terminal);
        assertEquals(before, terminal.getSize());
    }

    @Test
    void applyResizeParams_zeroColsIgnored() {
        Size before = terminal.getSize();
        InBandResize.applyResizeParams("30;0", terminal);
        assertEquals(before, terminal.getSize());
    }

    @Test
    void applyResizeParams_nonNumericIgnored() {
        Size before = terminal.getSize();
        InBandResize.applyResizeParams("abc;def", terminal);
        assertEquals(before, terminal.getSize());
    }

    // ---- handleResize (end-to-end) ----

    @Test
    void handleResize_validSequence() {
        BindingReader reader = readerFor("40;132t");
        InBandResize.handleResize(reader, terminal);
        assertEquals(Size.of(132, 40), terminal.getSize());
    }

    @Test
    void handleResize_malformedSequenceNoChange() {
        Size before = terminal.getSize();
        BindingReader reader = readerFor("bad;datat");
        InBandResize.handleResize(reader, terminal);
        assertEquals(before, terminal.getSize());
    }

    @Test
    void handleResize_raisesWINCHOnValidSequence() {
        List<Signal> signals = new ArrayList<>();
        terminal.handle(Signal.WINCH, signals::add);

        BindingReader reader = readerFor("25;80t");
        InBandResize.handleResize(reader, terminal);

        assertEquals(1, signals.size());
    }

    @Test
    void handleResize_noWINCHOnMalformedSequence() {
        List<Signal> signals = new ArrayList<>();
        terminal.handle(Signal.WINCH, signals::add);

        BindingReader reader = readerFor("x;yt");
        InBandResize.handleResize(reader, terminal);

        assertTrue(signals.isEmpty());
    }

    // ---- KeyMap integration ----

    @Test
    void resizeSeqBindsInKeyMap() {
        in.setIn(new ByteArrayInputStream(("\033[48;25;80t").getBytes(StandardCharsets.UTF_8)));
        BindingReader reader = new BindingReader(terminal.reader());

        KeyMap<String> keyMap = new KeyMap<>();
        keyMap.setUnicode("insert");
        keyMap.bind("resize", InBandResize.RESIZE_SEQ);

        // The KeyMap should match the CSI 48; prefix
        String binding = reader.readBinding(keyMap);
        assertEquals("resize", binding);
        assertEquals(InBandResize.RESIZE_SEQ, reader.getLastBinding());

        // Now read the remaining params + apply
        InBandResize.handleResize(reader, terminal);
        assertEquals(Size.of(80, 25), terminal.getSize());
    }
}

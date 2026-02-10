/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.widget;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jline.reader.impl.BufferImpl;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AutopairWidgets}.
 */
public class AutopairWidgetsTest {

    private Terminal terminal;
    private LineReaderImpl reader;
    private AutopairWidgets autopairWidgets;

    @BeforeEach
    public void setUp() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        terminal = new DumbTerminal("terminal", "ansi", in, out, StandardCharsets.UTF_8);
        terminal.setSize(new Size(160, 80));
        reader = new LineReaderImpl(terminal, "JLine", null);
        autopairWidgets = new AutopairWidgets(reader);
        autopairWidgets.enable();
    }

    @Test
    public void testWidgetCreation() {
        // Test that AutopairWidgets can be created and enabled
        assertNotNull(autopairWidgets);
    }

    @Test
    public void testEnableDisable() {
        // Test that enable/disable works without throwing exceptions
        autopairWidgets.disable();
        autopairWidgets.enable();
        // If we get here without exceptions, the test passes
        assertTrue(true);
    }

    /**
     * Test that BufferImpl.currChar() returns 0 at end of buffer.
     * This is critical for the fix in PR #1568 - the autopair logic checks
     * for end-of-buffer by comparing currChar() == 0, not -1.
     */
    @Test
    public void testBufferImplCurrCharReturnsZeroAtEnd() {
        BufferImpl buffer = new BufferImpl();
        buffer.write("test");
        assertEquals(0, buffer.currChar(), "currChar() should return 0 at end of buffer");
    }

    /**
     * Test that BufferImpl.currChar() returns the actual character when not at end.
     */
    @Test
    public void testBufferImplCurrCharReturnsCharInMiddle() {
        BufferImpl buffer = new BufferImpl();
        buffer.write("test");
        buffer.cursor(0);
        assertEquals('t', buffer.currChar(), "currChar() should return 't' at position 0");
    }

    /**
     * Test that BufferImpl.currChar() returns 0 for empty buffer.
     */
    @Test
    public void testBufferImplCurrCharReturnsZeroForEmptyBuffer() {
        BufferImpl buffer = new BufferImpl();
        assertEquals(0, buffer.currChar(), "currChar() should return 0 for empty buffer");
    }

    /**
     * Test that BufferImpl.currChar() returns newline character correctly.
     */
    @Test
    public void testBufferImplCurrCharReturnsNewline() {
        BufferImpl buffer = new BufferImpl();
        buffer.write("test\nmore");
        buffer.cursor(4); // Position at the newline
        assertEquals('\n', buffer.currChar(), "currChar() should return newline character");
    }

    /**
     * Test that BufferImpl.currChar() returns 0 when cursor is moved past the end.
     */
    @Test
    public void testBufferImplCurrCharAfterMovingToEnd() {
        BufferImpl buffer = new BufferImpl();
        buffer.write("abc");
        buffer.cursor(0);
        buffer.cursor(3); // Move to end
        assertEquals(0, buffer.currChar(), "currChar() should return 0 when cursor is at end");
    }
}

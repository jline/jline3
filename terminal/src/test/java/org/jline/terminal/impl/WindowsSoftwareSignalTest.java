/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal.Signal;
import org.jline.terminal.Terminal.SignalHandler;
import org.jline.utils.AnsiWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.jline.terminal.TerminalBuilder.PROP_SOFTWARE_SIGNALS;
import static org.jline.terminal.impl.AbstractWindowsTerminal.TYPE_WINDOWS;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for software signal interception in {@link AbstractWindowsTerminal}
 * when ISIG is cleared (raw mode).
 *
 * <p>Verifies that signal control characters (VINTR, VQUIT, VSUSP) raise the
 * corresponding signal even in raw mode when {@code softwareSignals} is enabled,
 * mirroring the behavior of {@link SignalInterceptingInputStream} on POSIX
 * terminals (fixes #2127).
 */
class WindowsSoftwareSignalTest {

    private String originalSoftwareSignals;

    @BeforeEach
    void setUp() {
        originalSoftwareSignals = System.getProperty(PROP_SOFTWARE_SIGNALS);
    }

    @AfterEach
    void tearDown() {
        if (originalSoftwareSignals != null) {
            System.setProperty(PROP_SOFTWARE_SIGNALS, originalSoftwareSignals);
        } else {
            System.clearProperty(PROP_SOFTWARE_SIGNALS);
        }
    }

    private AbstractWindowsTerminal<?> createTestTerminal(boolean nativeSignals) throws Exception {
        System.setProperty("org.jline.terminal.conemu.disable-activate", "true");
        StringWriter sw = new StringWriter();
        return new AbstractWindowsTerminal<>(
                null,
                null,
                new AnsiWriter(new BufferedWriter(sw)),
                "test",
                TYPE_WINDOWS,
                Charset.defaultCharset(),
                nativeSignals,
                SignalHandler.SIG_DFL,
                null,
                0,
                null,
                0) {
            @Override
            protected int getConsoleMode(Object console) {
                return 0;
            }

            @Override
            protected void setConsoleMode(Object console, int mode) {}

            @Override
            public int getDefaultForegroundColor() {
                return -1;
            }

            @Override
            public int getDefaultBackgroundColor() {
                return -1;
            }

            @Override
            protected boolean processConsoleInput() {
                return false;
            }

            @Override
            public Size getSize() {
                return Size.of(80, 25);
            }
        };
    }

    @Test
    void testCtrlCRaisesSignalWhenIsigEnabled() throws Exception {
        try (AbstractWindowsTerminal<?> terminal = createTestTerminal(false)) {
            // ISIG is on by default
            Attributes attr = terminal.getAttributes();
            assertTrue(attr.getLocalFlag(LocalFlag.ISIG));

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.INT, received::set);

            terminal.processInputChar('\3'); // Ctrl-C

            assertEquals(Signal.INT, received.get());
        }
    }

    @Test
    void testCtrlCRaisesSignalInRawModeWithSoftwareSignals() throws Exception {
        System.setProperty(PROP_SOFTWARE_SIGNALS, "true");
        try (AbstractWindowsTerminal<?> terminal = createTestTerminal(false)) {
            // Enter raw mode — clears ISIG
            Attributes attr = terminal.getAttributes();
            attr.setLocalFlag(LocalFlag.ISIG, false);
            terminal.setAttributes(attr);
            assertFalse(terminal.getAttributes().getLocalFlag(LocalFlag.ISIG));

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.INT, received::set);

            terminal.processInputChar('\3'); // Ctrl-C

            // Signal should be raised even with ISIG cleared
            assertEquals(Signal.INT, received.get());
        }
    }

    @Test
    void testCtrlCPassesThroughInRawModeWithSoftwareSignals() throws Exception {
        System.setProperty(PROP_SOFTWARE_SIGNALS, "true");
        try (AbstractWindowsTerminal<?> terminal = createTestTerminal(false)) {
            // Enter raw mode
            Attributes attr = terminal.getAttributes();
            attr.setLocalFlag(LocalFlag.ISIG, false);
            terminal.setAttributes(attr);

            terminal.handle(Signal.INT, s -> {});

            terminal.processInputChar('\3');

            // Character should also be written to the reader (passed through)
            int ch = terminal.reader().read(100);
            assertEquals('\3', ch);
        }
    }

    @Test
    void testCtrlCNotRaisedInRawModeWhenSoftwareSignalsDisabled() throws Exception {
        System.setProperty(PROP_SOFTWARE_SIGNALS, "false");
        try (AbstractWindowsTerminal<?> terminal = createTestTerminal(false)) {
            // Enter raw mode
            Attributes attr = terminal.getAttributes();
            attr.setLocalFlag(LocalFlag.ISIG, false);
            terminal.setAttributes(attr);

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.INT, received::set);

            terminal.processInputChar('\3');

            // Signal should NOT be raised when softwareSignals is disabled
            assertNull(received.get());

            // But character should still be written to the pipe
            int ch = terminal.reader().read(100);
            assertEquals('\3', ch);
        }
    }

    @Test
    void testCtrlZRaisesSignalInRawModeWithSoftwareSignals() throws Exception {
        System.setProperty(PROP_SOFTWARE_SIGNALS, "true");
        try (AbstractWindowsTerminal<?> terminal = createTestTerminal(false)) {
            // Enter raw mode
            Attributes attr = terminal.getAttributes();
            attr.setLocalFlag(LocalFlag.ISIG, false);
            terminal.setAttributes(attr);

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.TSTP, received::set);

            terminal.processInputChar('\032'); // Ctrl-Z (VSUSP)

            assertEquals(Signal.TSTP, received.get());
        }
    }

    @Test
    void testCtrlCConsumedWhenIsigEnabled() throws Exception {
        try (AbstractWindowsTerminal<?> terminal = createTestTerminal(false)) {
            // ISIG is on — signal chars should be consumed (not passed through)
            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.INT, received::set);

            terminal.processInputChar('\3');

            assertEquals(Signal.INT, received.get());

            // Character should NOT be in the reader (consumed by signal handling)
            int ch = terminal.reader().read(100);
            assertEquals(-2, ch); // -2 = no data available (timeout)
        }
    }

    @Test
    void testSoftwareSignalsDefaultsToTrue() throws Exception {
        // Clear the property to test the default
        System.clearProperty(PROP_SOFTWARE_SIGNALS);
        try (AbstractWindowsTerminal<?> terminal = createTestTerminal(false)) {
            // Enter raw mode
            Attributes attr = terminal.getAttributes();
            attr.setLocalFlag(LocalFlag.ISIG, false);
            terminal.setAttributes(attr);

            AtomicReference<Signal> received = new AtomicReference<>();
            terminal.handle(Signal.INT, received::set);

            terminal.processInputChar('\3');

            // Default should be true, so signal should be raised
            assertEquals(Signal.INT, received.get());
        }
    }
}

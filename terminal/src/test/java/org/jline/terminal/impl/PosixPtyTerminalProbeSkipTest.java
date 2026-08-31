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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.jline.terminal.Terminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that {@link PosixPtyTerminal} skips escape-sequence probing to prevent
 * indefinite hangs when no terminal emulator is attached to the PTY.
 *
 * <p>When a PTY is created via {@code openpty()} and the external streams are
 * null or connected to nothing, the native {@code read()} on the slave fd can
 * block indefinitely despite {@code VMIN=0/VTIME=0} being set. The fix is to
 * skip probing entirely for PTY terminals, since probe responses depend on an
 * external terminal emulator that may not be present.</p>
 *
 * @see <a href="https://github.com/jline/jline3/issues/2209">#2209</a>
 */
class PosixPtyTerminalProbeSkipTest {

    /**
     * Verifies that a terminal with {@code canProbeTerminalModes() == false}
     * skips probing and returns immediately from {@code isModeSupported()},
     * without blocking in {@code readProbeChar()}.
     *
     * <p>Uses a {@link LineDisciplineTerminal} subclass that overrides
     * {@code canProbeTerminalModes()} to simulate the behavior of
     * {@link PosixPtyTerminal}, which cannot respond to probes.</p>
     */
    @Test
    void probingSkippedWhenCanProbeReturnsFalse() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        // A terminal that never responds to reads (simulates stuck PTY).
        // With probing disabled, isModeSupported() should return immediately.
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8) {
                    @Override
                    protected boolean canProbeTerminalModes() {
                        return false;
                    }
                }) {
            // This must complete quickly — if probing is NOT skipped, it would
            // block in readProbeChar() waiting for a response that never comes.
            CompletableFuture<Boolean> result =
                    CompletableFuture.supplyAsync(() -> terminal.isModeSupported(Terminal.Mode.GRAPHEME_CLUSTER));

            // 2 seconds is generous — the call should return in < 10ms.
            // Without the fix, it would block for the full probe timeout (200ms+)
            // and potentially hang indefinitely on a real PTY.
            boolean supported = result.get(2, TimeUnit.SECONDS);
            assertFalse(supported, "modes should not be supported when probing is skipped");

            // No probe escape sequences should have been written
            String output = masterOutput.toString(StandardCharsets.UTF_8);
            assertFalse(output.contains("\033[?2027$p"), "DECRQM probe should not be sent when probing is skipped");
        }
    }

    /**
     * Verifies that a terminal with {@code canProbeTerminalModes() == false}
     * also skips grapheme cluster probing and returns {@code false} from
     * {@code supportsGraphemeClusterMode()}.
     */
    @Test
    void graphemeClusterProbeSkippedWhenCanProbeReturnsFalse() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8) {
                    @Override
                    protected boolean canProbeTerminalModes() {
                        return false;
                    }
                }) {
            CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(terminal::supportsGraphemeClusterMode);

            boolean supported = result.get(2, TimeUnit.SECONDS);
            assertFalse(supported, "grapheme cluster mode should not be supported when probing is skipped");
        }
    }

    /**
     * Verifies that the default {@code canProbeTerminalModes()} returns
     * {@code true}, preserving normal probe behavior for system terminals.
     */
    @Test
    void defaultCanProbeReturnsTrue() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8)) {
            assertTrue(terminal.canProbeTerminalModes(), "default canProbeTerminalModes() should return true");
        }
    }

    /**
     * Verifies that when probing is skipped, {@code isModeSupported()} returns
     * {@code false} for all modes (all default to NO_RESPONSE).
     */
    @Test
    void allModesUnsupportedWhenProbingSkipped() throws Exception {
        ByteArrayOutputStream masterOutput = new ByteArrayOutputStream();
        try (LineDisciplineTerminal terminal =
                new LineDisciplineTerminal("test", "xterm-256color", masterOutput, StandardCharsets.UTF_8) {
                    @Override
                    protected boolean canProbeTerminalModes() {
                        return false;
                    }
                }) {
            for (Terminal.Mode mode : Terminal.Mode.values()) {
                assertFalse(
                        terminal.isModeSupported(mode),
                        "mode " + mode + " should not be supported when probing is skipped");
            }
        }
    }
}

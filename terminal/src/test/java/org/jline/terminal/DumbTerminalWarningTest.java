/*
 * Copyright (c) 2026, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the "Unable to create a system terminal" warning is suppressed
 * when providers are available but no streams are TTYs (e.g., CI environments),
 * and is emitted when no providers could be loaded.
 */
@SuppressWarnings("missing-explicit-ctor")
public class DumbTerminalWarningTest {

    @Test
    public void testNoWarningWhenNoTtyAndProvidersAvailable() throws IOException {
        // In a CI/test environment, no streams are TTYs.
        // With providers available, this is an expected dumb fallback — no warning should be emitted.
        List<LogRecord> records = new ArrayList<>();
        Logger logger = Logger.getLogger("org.jline");
        Handler handler = new CapturingHandler(records);
        logger.addHandler(handler);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.WARNING);
        try {
            Terminal terminal = TerminalBuilder.builder().system(true).build();
            terminal.close();
            boolean hasWarning = records.stream()
                    .filter(r -> r.getLevel() == Level.WARNING)
                    .anyMatch(r -> r.getMessage().contains("Unable to create a system terminal"));
            assertFalse(hasWarning, "Should not warn about dumb terminal fallback when no streams are TTYs");
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(oldLevel);
        }
    }

    @Test
    public void testWarningWhenNoProvidersLoaded() throws IOException {
        // When no providers can be loaded, we can't determine TTY status,
        // so the warning should still be emitted.
        List<LogRecord> records = new ArrayList<>();
        Logger logger = Logger.getLogger("org.jline");
        Handler handler = new CapturingHandler(records);
        logger.addHandler(handler);
        Level oldLevel = logger.getLevel();
        logger.setLevel(Level.WARNING);
        try {
            Terminal terminal = TerminalBuilder.builder()
                    .system(true)
                    .ffm(false)
                    .jni(false)
                    .exec(false)
                    .build();
            terminal.close();
            boolean hasWarning = records.stream()
                    .filter(r -> r.getLevel() == Level.WARNING)
                    .anyMatch(r -> r.getMessage().contains("Unable to create a system terminal"));
            assertTrue(hasWarning, "Should warn about dumb terminal fallback when no providers are loaded");
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(oldLevel);
        }
    }

    private static class CapturingHandler extends Handler {
        private final List<LogRecord> records;

        CapturingHandler(List<LogRecord> records) {
            this.records = records;
        }

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }
}

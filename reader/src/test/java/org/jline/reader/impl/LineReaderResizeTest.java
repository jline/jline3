/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jline.terminal.Terminal;
import org.jline.utils.Status;
import org.jline.utils.VirtualTerminal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LineReaderResizeTest {

    @Test
    void winchRedisplayDoesNotDuplicateMultiRowPromptBuffer() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("terminal", "ansi", StandardCharsets.UTF_8, 20, 8)) {
            ExposedLineReader reader = new ExposedLineReader(terminal);

            reader.setPrompt("-- user --\n");
            reader.getBuffer().write("abc def ghi jkl mno pqr");

            String previousOutput = "previous output";
            terminal.writer().print(previousOutput + "\r\n");
            terminal.flush();

            reader.redisplay();
            terminal.flush();

            assertEquals(1, countOccurrences(terminal.screenContent(), "-- user --"), terminal::screenContent);
            assertEquals(1, countOccurrences(terminal.screenContent(), previousOutput), terminal::screenContent);

            terminal.resizeScreen(18, 9);
            terminal.startCapture();
            reader.handleWinch();
            terminal.flush();
            byte[] captured = terminal.stopCapture();
            assertEquals(0, captured.length, "WINCH should not repaint without status rows");

            terminal.startCapture();
            reader.redisplay();
            terminal.flush();
            captured = terminal.stopCapture();
            assertEquals(0, captured.length, "resized display model should already be current");

            assertEquals(1, countOccurrences(terminal.screenContent(), "-- user --"), terminal::screenContent);
            assertEquals(1, countOccurrences(terminal.screenContent(), previousOutput), terminal::screenContent);
        }
    }

    @Test
    void winchWithEmptyStatusDoesNotRedrawPromptBuffer() throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("terminal", "ansi", StandardCharsets.UTF_8, 20, 8)) {
            Status status = Status.getStatus(terminal);
            ExposedLineReader reader = new ExposedLineReader(terminal);

            assertEquals(0, status.size());

            reader.setPrompt("-- user --\n");
            reader.getBuffer().write("abc def ghi jkl mno pqr");
            reader.redisplay();
            terminal.flush();

            terminal.startCapture();
            terminal.resizeScreen(18, 9);
            reader.handleWinch();
            terminal.flush();
            byte[] captured = terminal.stopCapture();

            assertEquals(0, captured.length, "WINCH with empty status should not emit any output");
        }
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int from = 0;
        int found;
        while ((found = text.indexOf(needle, from)) >= 0) {
            count++;
            from = found + needle.length();
        }
        return count;
    }

    private static final class ExposedLineReader extends LineReaderImpl {
        private ExposedLineReader(Terminal terminal) throws IOException {
            super(terminal);
        }

        private void handleWinch() {
            handleSignal(Terminal.Signal.WINCH);
        }
    }
}

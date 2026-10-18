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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.Reference;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.Status;
import org.jline.utils.VirtualTerminal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void winchWithStatusAndNarrowerTallerTerminal() throws IOException {
        assertStatusResize(36, 14, false, 0);
    }

    @Test
    void winchWithStatusAndWiderTerminal() throws IOException {
        assertStatusResize(48, 12, false, 0);
    }

    @Test
    void winchWithStatusAndShorterTerminal() throws IOException {
        assertStatusResize(40, 10, false, 0);
    }

    @Test
    void winchWithStatusAndTallerTerminal() throws IOException {
        assertStatusResize(40, 14, false, 0);
    }

    @Test
    void winchWithStatusAndBorder() throws IOException {
        assertStatusResize(36, 10, true, 0);
    }

    @Test
    void winchWithStatusAndUnchangedSize() throws IOException {
        assertStatusResize(40, 12, true, 0);
    }

    @Test
    void winchWithStatusClearingPromptRows() throws IOException {
        assertStatusResize(40, 9, false, 4);
    }

    @Test
    void winchWithStatusAndWrappedInput() throws IOException {
        assertStatusResize(40, 14, false, 0, "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Test
    @Timeout(15)
    void readLineWithStatusAcceptsInputAfterWinch() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (VirtualTerminal terminal = new VirtualTerminal("terminal", "xterm", StandardCharsets.UTF_8, 40, 12)) {
            Status status = Status.getStatus(terminal);
            status.update(List.of(new AttributedString("status one"), new AttributedString("status two")));
            LineReader reader = LineReaderBuilder.builder().terminal(terminal).build();
            CountDownLatch ready = new CountDownLatch(1);
            AtomicReference<String> resizedScreen = new AtomicReference<>();
            reader.getWidgets().put("resize-ready", () -> {
                reader.callWidget(LineReader.REDISPLAY);
                terminal.resizeScreen(36, 14);
                terminal.raise(Terminal.Signal.WINCH);
                resizedScreen.set(terminal.screenContent());
                ready.countDown();
                return true;
            });
            reader.getKeyMaps().get(LineReader.MAIN).bind(new Reference("resize-ready"), "\u0002");
            reader.runMacro("abc\u0002");
            Future<String> line = executor.submit(() -> reader.readLine("-- user --\n"));
            assertTrue(ready.await(5, TimeUnit.SECONDS));

            terminal.processInputBytes("def\n".getBytes(StandardCharsets.UTF_8));

            assertEquals("abcdef", line.get(5, TimeUnit.SECONDS));
            assertEquals(1, countOccurrences(resizedScreen.get(), "-- user --"), resizedScreen::get);
            assertEquals(1, countOccurrences(terminal.screenContent(), "abcdef"), terminal::screenContent);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    @Timeout(15)
    void winchWaitsForReaderBeforeResizingDisplay() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (VirtualTerminal terminal = new VirtualTerminal("terminal", "xterm", StandardCharsets.UTF_8, 40, 12)) {
            Status.getStatus(terminal).update(List.of(new AttributedString("status")));
            ExposedLineReader reader = new ExposedLineReader(terminal);
            reader.setPrompt("> ");
            reader.getBuffer().write("abc");
            reader.redisplay();
            CountDownLatch started = new CountDownLatch(1);
            AtomicReference<Thread> signalThread = new AtomicReference<>();
            Future<?> signal;
            reader.lock.lock();
            try {
                terminal.resizeScreen(36, 14);
                signal = executor.submit(() -> {
                    signalThread.set(Thread.currentThread());
                    started.countDown();
                    reader.handleWinch();
                });
                assertTrue(started.await(5, TimeUnit.SECONDS));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while (!reader.lock.hasQueuedThread(signalThread.get())) {
                    assertTrue(System.nanoTime() < deadline, "WINCH should wait for the reader lock");
                    Thread.sleep(1);
                }
                assertEquals(40, reader.display.getColumns(), "Display must remain unchanged while the reader owns it");
            } finally {
                reader.lock.unlock();
            }
            signal.get(5, TimeUnit.SECONDS);
            assertEquals(36, reader.display.getColumns());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private void assertStatusResize(int columns, int rows, boolean border, int blankLines) throws IOException {
        assertStatusResize(columns, rows, border, blankLines, "abc def ghi");
    }

    private void assertStatusResize(int columns, int rows, boolean border, int blankLines, String input)
            throws IOException {
        try (VirtualTerminal terminal = new VirtualTerminal("terminal", "xterm", StandardCharsets.UTF_8, 40, 12)) {
            Status status = Status.getStatus(terminal);
            status.setBorder(border);
            status.update(List.of(new AttributedString("status one"), new AttributedString("status two")));
            terminal.writer().print("\rprevious output\r\n");
            terminal.writer().print("\r\n".repeat(blankLines));
            ExposedLineReader reader = new ExposedLineReader(terminal);
            reader.setPrompt("-- user --\n");
            reader.getBuffer().write(input);
            reader.redisplay();
            terminal.flush();

            assertEquals(1, countOccurrences(terminal.screenContent(), "previous output"), terminal::screenContent);
            assertEquals(1, countOccurrences(terminal.screenContent(), "-- user --"), terminal::screenContent);

            terminal.resizeScreen(columns, rows);
            terminal.startCapture();
            reader.handleWinch();
            terminal.flush();
            byte[] captured = terminal.stopCapture();
            if (columns == 40 && rows == 12) {
                assertEquals(0, captured.length, "WINCH without a grid change should not emit output");
            }

            assertEquals(1, countOccurrences(terminal.screenContent(), "-- user --"), terminal::screenContent);
            String expectedInput = input;
            if (input.length() > columns) {
                expectedInput = input.substring(0, columns) + "\n" + input.substring(columns);
            }
            String expectedBlock = "\nprevious output\n" + "\n".repeat(blankLines) + "-- user --\n" + expectedInput;
            assertEquals(
                    1,
                    countOccurrences("\n" + unpadded(terminal.screenContent()), expectedBlock),
                    terminal::screenContent);
            assertEquals(1, countOccurrences(terminal.screenContent(), "previous output"), terminal::screenContent);
            assertEquals(
                    "status one", terminal.screenContent().split("\n")[rows - 2].trim());
            assertEquals(
                    "status two", terminal.screenContent().split("\n")[rows - 1].trim());

            reader.getBuffer().write("!");
            reader.redisplay();
            terminal.flush();
            assertEquals(
                    1,
                    countOccurrences("\n" + unpadded(terminal.screenContent()), expectedBlock + "!"),
                    terminal::screenContent);
        }
    }

    private static String unpadded(String text) {
        return text.replaceAll(" +\\n", "\n");
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

/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.reader.impl;

import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Verifies that the thread interrupt flag is cleared after a
 * {@link UserInterruptException} is thrown from {@link LineReader#readLine()}.
 *
 * @see <a href="https://github.com/jline/jline3/issues/2039">#2039</a>
 */
class LineReaderInterruptTest {

    private static final byte CTRL_C = 0x03;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long INPUT_DELAY_MS = 200;

    /**
     * Pressing Ctrl-C during readLine() should throw UserInterruptException
     * and leave the thread's interrupt flag clear so that a subsequent
     * readLine() call works without requiring Thread.interrupted() first.
     *
     * <p>This simulates the behaviour of {@code SignalInterceptingInputStream}
     * used on native (FFM/JNI) terminals: it raises {@code Signal.INT} on the
     * reader thread (which sets the interrupt flag via the LineReader's signal
     * handler) and then passes 0x03 through so the INTERRUPT widget fires.
     * With a plain {@code LineDisciplineTerminal} created via
     * {@code TerminalBuilder.streams()}, ISIG is cleared in raw mode and the
     * terminal does not raise the signal itself, so we call
     * {@code terminal.raise(Signal.INT)} explicitly to exercise the same path.
     */
    @Test
    void interruptFlagClearedAfterUserInterrupt() throws Exception {
        assertTimeoutPreemptively(TIMEOUT, () -> {
            PipedInputStream in = new PipedInputStream();
            PipedOutputStream feeder = new PipedOutputStream(in);

            Terminal terminal = TerminalBuilder.builder()
                    .type("ansi")
                    .streams(in, new ByteArrayOutputStream())
                    .build();
            terminal.setSize(Size.of(80, 24));

            try (terminal) {
                LineReader reader =
                        LineReaderBuilder.builder().terminal(terminal).build();

                // Send Ctrl-C after a short delay to allow readLine() to
                // enter raw mode and install the INT signal handler.
                // Raise Signal.INT first (mimicking SignalInterceptingInputStream)
                // so the handler sets the thread's interrupt flag, then write
                // the 0x03 byte so the INTERRUPT widget fires.
                Thread sender = new Thread(
                        () -> {
                            try {
                                TimeUnit.MILLISECONDS.sleep(INPUT_DELAY_MS);
                                terminal.raise(Terminal.Signal.INT);
                                feeder.write(CTRL_C);
                                feeder.flush();
                                // Keep thread alive so PipedInputStream doesn't
                                // raise "Write end dead".
                                Thread.sleep(Long.MAX_VALUE);
                            } catch (InterruptedException ok) {
                                // Expected: sender.interrupt() in the finally block
                                // terminates this thread after the test completes.
                            } catch (Exception e) {
                                // IOException from feeder.write — test will fail
                                // on its own via the readLine() timeout.
                            }
                        },
                        "ctrl-c-sender");
                sender.setDaemon(true);
                sender.start();

                try {
                    assertThrows(UserInterruptException.class, () -> reader.readLine());

                    assertFalse(
                            Thread.currentThread().isInterrupted(),
                            "Thread interrupt flag should be clear after " + "UserInterruptException (see #2039)");
                } finally {
                    sender.interrupt();
                }
            }
        });
    }
}

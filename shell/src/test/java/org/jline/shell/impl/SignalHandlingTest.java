/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.shell.impl;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.shell.*;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for signal handling in {@link DefaultCommandDispatcher}.
 */
public class SignalHandlingTest {

    private Terminal terminal;
    private DefaultCommandDispatcher dispatcher;

    @BeforeEach
    void setUp() throws IOException {
        terminal = TerminalBuilder.builder().dumb(true).build();
        dispatcher = new DefaultCommandDispatcher(terminal);
        dispatcher.addGroup(new SimpleCommandGroup("test", new SleepCommand(), new EchoCommand()));
    }

    static class SleepCommand extends AbstractCommand {
        SleepCommand() {
            super("sleep");
        }

        @Override
        public Object execute(CommandSession session, String[] args) throws Exception {
            long millis = args.length > 0 ? Long.parseLong(args[0]) : 5000;
            Thread.sleep(millis);
            return "slept";
        }
    }

    static class EchoCommand extends AbstractCommand {
        EchoCommand() {
            super("echo");
        }

        @Override
        public Object execute(CommandSession session, String[] args) {
            String msg = String.join(" ", args);
            session.out().println(msg);
            return msg;
        }
    }

    @Test
    void interruptSleepingCommand() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        Thread execThread = new Thread(() -> {
            started.countDown();
            try {
                dispatcher.execute("sleep 10000");
            } catch (Exception e) {
                if (e instanceof InterruptedException
                        || (e.getCause() != null && e.getCause() instanceof InterruptedException)) {
                    interrupted.set(true);
                }
            }
        });
        execThread.start();

        assertTrue(started.await(2, TimeUnit.SECONDS));
        // Give the command a moment to start sleeping
        Thread.sleep(100);

        dispatcher.interruptCurrentCommand();

        execThread.join(5000);
        assertFalse(execThread.isAlive(), "Thread should have terminated");
        assertTrue(interrupted.get(), "Command should have been interrupted");
    }

    @Test
    void interruptWithNoRunningCommandIsNoop() {
        // Should not throw
        dispatcher.interruptCurrentCommand();
    }

    @Test
    void commandContinuesAfterInterrupt() throws Exception {
        // Execute a quick command after an interrupt to verify dispatcher is still functional
        dispatcher.interruptCurrentCommand(); // no-op, nothing running
        Object result = dispatcher.execute("echo hello");
        assertEquals("hello", result);
    }
}

/*
 * Copyright (c) 2002-2025, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.curses;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jline.curses.impl.GUIImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify thread safety of the curses GUI implementation,
 * particularly around signal handling and Display access.
 */
public class ThreadSafetyTest {

    /**
     * Test that concurrent access to Display through signal handlers
     * doesn't cause ConcurrentModificationException.
     */
    @Test
    public void testConcurrentDisplayAccess() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder()
                .system(false)
                .streams(System.in, System.out)
                .build()) {
            GUIImpl gui = new GUIImpl(terminal);

            // Create a simple window
            Window window = Curses.window()
                    .title("Test Window")
                    .component(Curses.textArea())
                    .build();

            gui.addWindow(window);

            AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(2);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                // Simulate concurrent access - one thread doing normal operations
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < 50; i++) {
                            // Simulate normal GUI operations that might access Display
                            gui.getTerminal().getSize();
                            Thread.sleep(2);
                        }
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("ConcurrentModification")) {
                            exceptionOccurred.set(true);
                        }
                    } finally {
                        latch.countDown();
                    }
                });

                // Another thread simulating signal handler calls
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < 50; i++) {
                            // Simulate WINCH signal by calling the signal handler
                            try {
                                terminal.raise(Terminal.Signal.WINCH);
                            } catch (Exception e) {
                                // Ignore signal handling errors in test environment
                            }
                            Thread.sleep(2);
                        }
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("ConcurrentModification")) {
                            exceptionOccurred.set(true);
                        }
                    } finally {
                        latch.countDown();
                    }
                });

            } finally {
                executor.shutdown();
            }

            // Wait for both threads to complete
            assertTrue(latch.await(15, TimeUnit.SECONDS), "Test threads should complete within 15 seconds");

            // Verify no ConcurrentModificationException occurred
            assertFalse(
                    exceptionOccurred.get(),
                    "No ConcurrentModificationException should occur with proper synchronization");
        }
    }

    /**
     * Test that the cost map in Display can handle concurrent access
     * without throwing ConcurrentModificationException.
     */
    @Test
    public void testDisplayCostMapThreadSafety() throws Exception {
        try (Terminal terminal = TerminalBuilder.builder()
                .system(false)
                .streams(System.in, System.out)
                .build()) {
            // Create Display instance
            org.jline.utils.Display display = new org.jline.utils.Display(terminal, true);

            AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
            CountDownLatch latch = new CountDownLatch(2);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                // Two threads accessing the cost calculation concurrently
                for (int t = 0; t < 2; t++) {
                    executor.submit(() -> {
                        try {
                            for (int i = 0; i < 50; i++) {
                                // Access methods that use the cost map
                                display.resize(24, 80);
                                Thread.sleep(2);
                            }
                        } catch (Exception e) {
                            if (e.getMessage() != null && e.getMessage().contains("ConcurrentModification")) {
                                exceptionOccurred.set(true);
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            } finally {
                executor.shutdown();
            }

            assertTrue(latch.await(15, TimeUnit.SECONDS), "Test threads should complete within 15 seconds");
            assertFalse(exceptionOccurred.get(), "ConcurrentHashMap should prevent ConcurrentModificationException");
        }
    }
}

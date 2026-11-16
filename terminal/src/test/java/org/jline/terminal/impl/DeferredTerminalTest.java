/*
 * Copyright (c) the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeferredTerminalTest {

    private Terminal createDumbTerminal() throws IOException {
        return new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
    }

    @Test
    void writerBuffersAndReplays() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.writer().print("hello");
        deferred.writer().flush();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal real = new DumbTerminal(new ByteArrayInputStream(new byte[0]), out);
        deferred.complete(real);

        assertTrue(out.toString().contains("hello"));
    }

    @Test
    void writerDelegatesAfterComplete() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal real = new DumbTerminal(new ByteArrayInputStream(new byte[0]), out);
        deferred.complete(real);

        deferred.writer().print("after");
        deferred.writer().flush();
        assertTrue(out.toString().contains("after"));
    }

    @Test
    void getTypeReturnsTERMBeforeReady() {
        DeferredTerminal deferred = new DeferredTerminal();
        assertFalse(deferred.isReady());
        assertNotNull(deferred.getType());
    }

    @Test
    void getTypeReturnsDumbWithoutTERM() {
        DeferredTerminal deferred = new DeferredTerminal();
        String type = deferred.getType();
        // Either TERM env var or "dumb"
        assertNotNull(type);
    }

    @Test
    void getTypeDelegatesAfterComplete() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        Terminal real = createDumbTerminal();
        deferred.complete(real);
        assertEquals(real.getType(), deferred.getType());
        deferred.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void completedTerminalOverridesPendingColorPreference(boolean color) throws Exception {
        DeferredTerminal deferred = new DeferredTerminal(color);
        AttributedString text = new AttributedString("hello", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        try {
            assertEquals(color ? "\u001b[31mhello\u001b[0m" : "hello", text.toAnsi(deferred));
            Terminal real = new DumbTerminal(
                    "test",
                    color ? Terminal.TYPE_DUMB : Terminal.TYPE_DUMB_COLOR,
                    new ByteArrayInputStream(new byte[0]),
                    new ByteArrayOutputStream(),
                    null);
            deferred.complete(real);
            assertEquals(color ? "hello" : "\u001b[31mhello\u001b[0m", text.toAnsi(deferred));
        } finally {
            deferred.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void builderColorPreferenceAppliesBeforeAndAfterCompletion(boolean color) throws Exception {
        assertBuilderColorPreference(color, color);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    void builderColorPreferenceOverridesSystemProperty(boolean color) throws Exception {
        String previous = System.getProperty(TerminalBuilder.PROP_DUMB_COLOR);
        try {
            System.setProperty(TerminalBuilder.PROP_DUMB_COLOR, Boolean.toString(!color));
            assertBuilderColorPreference(color, color);
        } finally {
            restoreColorProperty(previous);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @ResourceLock(Resources.SYSTEM_PROPERTIES)
    void builderUsesSystemPropertyColorPreference(boolean color) throws Exception {
        String previous = System.getProperty(TerminalBuilder.PROP_DUMB_COLOR);
        try {
            System.setProperty(TerminalBuilder.PROP_DUMB_COLOR, Boolean.toString(color));
            assertBuilderColorPreference(null, color);
        } finally {
            restoreColorProperty(previous);
        }
    }

    private void restoreColorProperty(String previous) {
        if (previous == null) {
            System.clearProperty(TerminalBuilder.PROP_DUMB_COLOR);
        } else {
            System.setProperty(TerminalBuilder.PROP_DUMB_COLOR, previous);
        }
    }

    private void assertBuilderColorPreference(Boolean color, boolean expectedColor) throws Exception {
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TerminalBuilder builder = TerminalBuilder.builder()
                .dumb(true)
                .ffm(false)
                .jni(false)
                .exec(false)
                .graphemeCluster(false)
                .env(name -> {
                    if ("TERM".equals(name)) {
                        building.countDown();
                        try {
                            if (!release.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Terminal construction was not released");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        return "dumb";
                    }
                    return null;
                });
        if (color != null) {
            builder.color(color);
        }
        DeferredTerminal deferred = (DeferredTerminal) builder.buildDeferred();
        CompletableFuture<Terminal> completed = new CompletableFuture<>();
        deferred.onComplete(completed::complete);
        AttributedString text = new AttributedString("hello", AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
        String expected = expectedColor ? "\u001b[31mhello\u001b[0m" : "hello";
        try {
            assertTrue(building.await(5, TimeUnit.SECONDS));
            assertFalse(deferred.isReady());
            assertEquals(expected, text.toAnsi(deferred));
            release.countDown();
            Terminal terminal = completed.get(5, TimeUnit.SECONDS);
            assertEquals(expected, text.toAnsi(terminal));
            assertEquals(expected, text.toAnsi(deferred));
        } finally {
            release.countDown();
            deferred.close();
        }
    }

    @Test
    void onCompleteBeforeComplete() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        AtomicReference<Terminal> received = new AtomicReference<>();
        deferred.onComplete(received::set);

        Terminal real = createDumbTerminal();
        deferred.complete(real);
        assertEquals(real, received.get());
        deferred.close();
    }

    @Test
    void onCompleteAfterComplete() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        Terminal real = createDumbTerminal();
        deferred.complete(real);

        AtomicReference<Terminal> received = new AtomicReference<>();
        deferred.onComplete(received::set);
        assertEquals(real, received.get());
        deferred.close();
    }

    @Test
    void onCompleteExactlyOnce() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        int[] count = {0};
        deferred.onComplete(t -> count[0]++);
        deferred.complete(createDumbTerminal());
        assertEquals(1, count[0]);
        deferred.close();
    }

    @Test
    void completeExceptionallyDrainsBuffer() {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.writer().print("drained");
        deferred.writer().flush();
        deferred.completeExceptionally(new RuntimeException("test"));

        assertFalse(deferred.isReady());
        // After exceptional completion, writer should still work (falls back to stdout)
        deferred.writer().print("more");
        deferred.writer().flush();
        assertTrue(true, "no exception thrown");
    }

    @Test
    void closeBeforeCompleteDoesNotThrow() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.writer().print("buffered");
        deferred.writer().flush();
        deferred.close();
        assertFalse(deferred.isReady());
    }

    @Test
    void closeAfterCompleteClosesTerminal() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        Terminal real = createDumbTerminal();
        deferred.complete(real);
        assertTrue(deferred.isReady());
        deferred.close();
    }

    @Test
    void closeAfterExceptionalCompletionDoesNotThrow() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.completeExceptionally(new IOException("build failed"));
        deferred.close();
        assertFalse(deferred.isReady());
    }

    @Test
    void closeIsIdempotent() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.close();
        deferred.close();
        assertFalse(deferred.isReady());
    }

    @Test
    void getTerminalBlocksUntilReady() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch waiting = new CountDownLatch(1);
        CompletableFuture<Terminal> result = new CompletableFuture<>();

        Thread t = new Thread(() -> {
            started.countDown();
            waiting.countDown();
            result.complete(deferred.getTerminal());
        });
        t.setDaemon(true);
        t.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertTrue(waiting.await(5, TimeUnit.SECONDS));
        assertFalse(result.isDone());

        Terminal real = createDumbTerminal();
        deferred.complete(real);
        assertEquals(real, result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void getTerminalAfterCancelThrows() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        deferred.close();
        assertThrows(IllegalStateException.class, deferred::getTerminal);
    }

    @Test
    void capabilitiesReturnDefaultsBeforeReady() {
        DeferredTerminal deferred = new DeferredTerminal();
        assertFalse(deferred.getBooleanCapability(org.jline.utils.InfoCmp.Capability.auto_left_margin));
        assertTrue(deferred.getNumericCapability(org.jline.utils.InfoCmp.Capability.max_colors) >= 256);
        assertNotNull(deferred.getPalette());
        assertTrue(deferred.getColumns() > 0);
        assertTrue(deferred.getRows() > 0);
    }
}

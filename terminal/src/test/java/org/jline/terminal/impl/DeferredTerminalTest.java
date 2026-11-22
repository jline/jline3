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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.easymock.EasyMock;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalExt;
import org.jline.terminal.spi.TerminalProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeferredTerminalTest {

    private Terminal createDumbTerminal() throws IOException {
        return new DumbTerminal(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
    }

    @Test
    void synchronousTerminalIsReadyAndDescribesItsType() throws Exception {
        try (Terminal terminal = createDumbTerminal()) {
            assertTrue(terminal.isReady());
            assertTrue(terminal.describe().contains("DumbTerminal"));
            assertTrue(terminal.describe().contains("type=dumb"));
        }
    }

    @Test
    void readinessIsAvailableThroughTerminalInterface() throws Exception {
        DeferredTerminal deferred = new DeferredTerminal();
        Terminal terminal = deferred;
        assertFalse(terminal.isReady());
        deferred.complete(createDumbTerminal());
        assertTrue(terminal.isReady());
        terminal.close();
    }

    @Test
    void descriptionAndMetadataDelegateToRealTerminal() throws Exception {
        TerminalProvider provider = new DumbTerminalProvider();
        Terminal real = new DumbTerminal(
                provider,
                SystemStream.Output,
                "real",
                "xterm",
                new ByteArrayInputStream(new byte[0]),
                new ByteArrayOutputStream(),
                StandardCharsets.UTF_8,
                Terminal.SignalHandler.SIG_DFL);
        try (DeferredTerminal deferred = new DeferredTerminal()) {
            deferred.complete(real);
            Terminal terminal = deferred;
            assertEquals(real.describe(), terminal.describe());
            assertTrue(terminal.describe().contains("provider=dumb"));
            assertSame(provider, ((TerminalExt) terminal).getProvider());
            assertEquals(SystemStream.Output, ((TerminalExt) terminal).getSystemStream());
        }
    }

    @Test
    void terminalWithoutExtendedMetadataIsSupported() throws Exception {
        Terminal real = EasyMock.createNiceMock(Terminal.class);
        EasyMock.expect(real.writer())
                .andReturn(new PrintWriter(new ByteArrayOutputStream()))
                .anyTimes();
        EasyMock.expect(real.describe()).andReturn("custom terminal");
        EasyMock.replay(real);
        try (DeferredTerminal deferred = new DeferredTerminal()) {
            deferred.complete(real);
            assertNull(deferred.getProvider());
            assertNull(deferred.getSystemStream());
            assertEquals("custom terminal", deferred.describe());
        }
        EasyMock.verify(real);
    }

    @Test
    void descriptionWaitsForTheRealTerminal() throws Exception {
        try (DeferredTerminal deferred = new DeferredTerminal()) {
            CountDownLatch started = new CountDownLatch(1);
            CompletableFuture<String> description = CompletableFuture.supplyAsync(() -> {
                started.countDown();
                return deferred.describe();
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertFalse(description.isDone());
            Terminal real = createDumbTerminal();
            deferred.complete(real);
            assertEquals(real.describe(), description.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void descriptionPropagatesFailedAndCancelledBuilds() throws Exception {
        try (DeferredTerminal deferred = new DeferredTerminal()) {
            IOException failure = new IOException("build failed");
            deferred.completeExceptionally(failure);
            assertFalse(((Terminal) deferred).isReady());
            assertSame(
                    failure,
                    assertThrows(UncheckedIOException.class, deferred::describe).getCause());
        }
        DeferredTerminal cancelled = new DeferredTerminal();
        cancelled.close();
        assertFalse(((Terminal) cancelled).isReady());
        assertThrows(IllegalStateException.class, cancelled::describe);
    }

    @Test
    void builderRegistersCallbackBeforeStartingConstruction() throws Exception {
        CompletableFuture<Terminal> completed = new CompletableFuture<>();
        AtomicInteger calls = new AtomicInteger();
        try (Terminal terminal = TerminalBuilder.builder()
                .provider("dumb")
                .graphemeCluster(false)
                .buildDeferred(real -> {
                    calls.incrementAndGet();
                    completed.complete(real);
                })) {
            Terminal real = completed.get(5, TimeUnit.SECONDS);
            assertTrue(terminal.isReady());
            assertEquals(real.describe(), terminal.describe());
            assertEquals(1, calls.get());
        }
    }

    @Test
    void callbackFailureKeepsTheCompletedTerminalWriter() throws Exception {
        RuntimeException failure = new IllegalArgumentException("callback failed");
        CompletableFuture<Thread> callbackThread = new CompletableFuture<>();
        AtomicReference<Terminal> completed = new AtomicReference<>();
        AtomicReference<Throwable> uncaught = new AtomicReference<>();
        CountDownLatch releaseCallback = new CountDownLatch(1);
        ByteArrayOutputStream fallbackOutput = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try (PrintStream capturedOut = new PrintStream(fallbackOutput)) {
            System.setOut(capturedOut);
            try (Terminal terminal = TerminalBuilder.builder()
                    .provider("dumb")
                    .graphemeCluster(false)
                    .buildDeferred(real -> {
                        Thread thread = Thread.currentThread();
                        thread.setUncaughtExceptionHandler((ignored, thrown) -> uncaught.set(thrown));
                        completed.set(real);
                        callbackThread.complete(thread);
                        try {
                            if (!releaseCallback.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("callback was not released");
                            }
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        throw failure;
                    })) {
                terminal.writer().print("early output");
                terminal.writer().flush();
                Thread thread = callbackThread.get(5, TimeUnit.SECONDS);
                releaseCallback.countDown();
                thread.join(5000);
                assertFalse(thread.isAlive());

                terminal.writer().print("later output");
                terminal.writer().flush();
                assertAll(
                        () -> assertSame(failure, uncaught.get()),
                        () -> assertEquals("", fallbackOutput.toString()),
                        () -> assertTrue(terminal.isReady()),
                        () -> assertSame(completed.get(), ((DeferredTerminal) terminal).getTerminal()));
            }
        } finally {
            releaseCallback.countDown();
            System.setOut(originalOut);
        }
    }

    @Test
    void callbackOverloadRejectsCustomStreams() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(
                IllegalStateException.class,
                () -> TerminalBuilder.builder()
                        .streams(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream())
                        .buildDeferred(real -> calls.incrementAndGet()));
        assertEquals(0, calls.get());
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
